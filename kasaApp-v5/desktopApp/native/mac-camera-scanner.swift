import Foundation
import AVFoundation
import Vision
import CoreImage
import AppKit

// Примусове виведення без буферизації рядків
setbuf(stdout, nil)

final class CameraScanner: NSObject, AVCaptureVideoDataOutputSampleBufferDelegate {
    private let session = AVCaptureSession()
    private let videoOutput = AVCaptureVideoDataOutput()
    private let sessionQueue = DispatchQueue(label: "com.kasa.scanner.session")
    private let ciContext = CIContext(options: [.useSoftwareRenderer: false])
    
    private var lastFrameTime: TimeInterval = 0
    private let frameInterval: TimeInterval = 0.09 // ~11 кадрів/сек для плавного прев'ю без лагів
    private var lastBarcode: String = ""
    private var lastBarcodeTime: TimeInterval = 0

    private lazy var barcodeRequest: VNDetectBarcodesRequest = {
        let req = VNDetectBarcodesRequest { [weak self] request, error in
            guard let self = self, error == nil,
                  let results = request.results as? [VNBarcodeObservation] else { return }
            
            let now = Date().timeIntervalSince1970
            for barcode in results {
                guard let payload = barcode.payloadStringValue?.trimmingCharacters(in: .whitespacesAndNewlines),
                      !payload.isEmpty else { continue }
                
                // Дебаунс однакових кодів протягом 1.8 сек для запобігання спаму
                if payload == self.lastBarcode && (now - self.lastBarcodeTime) < 1.8 {
                    continue
                }
                self.lastBarcode = payload
                self.lastBarcodeTime = now
                print("BARCODE:\(payload)")
                fflush(stdout)
                break
            }
        }
        req.symbologies = [
            .ean13, .ean8, .upce, .qr, .code128, .code39, .code93, .itf14, .dataMatrix, .aztec, .pdf417
        ]
        return req
    }()

    func start() {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            setupAndStartSession()
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
                if granted {
                    self?.setupAndStartSession()
                } else {
                    print("ERROR:PERMISSION_DENIED")
                    fflush(stdout)
                    exit(1)
                }
            }
        default:
            print("ERROR:PERMISSION_DENIED")
            fflush(stdout)
            exit(1)
        }
    }

    private func findCameraDevice() -> AVCaptureDevice? {
        if let dev = AVCaptureDevice.default(for: .video) {
            return dev
        }
        var types: [AVCaptureDevice.DeviceType] = [.builtInWideAngleCamera]
        if #available(macOS 14.0, *) {
            types.append(.continuityCamera)
            types.append(.external)
        }
        let session = AVCaptureDevice.DiscoverySession(
            deviceTypes: types,
            mediaType: .video,
            position: .unspecified
        )
        if let first = session.devices.first {
            return first
        }
        return AVCaptureDevice.devices().first { $0.hasMediaType(.video) }
    }

    private func setupAndStartSession() {
        sessionQueue.async { [weak self] in
            guard let self = self else { return }
            self.session.beginConfiguration()

            if self.session.canSetSessionPreset(.hd1280x720) {
                self.session.sessionPreset = .hd1280x720
            } else if self.session.canSetSessionPreset(.vga640x480) {
                self.session.sessionPreset = .vga640x480
            } else {
                self.session.sessionPreset = .high
            }

            guard let device = self.findCameraDevice(),
                  let input = try? AVCaptureDeviceInput(device: device),
                  self.session.canAddInput(input) else {
                print("ERROR:NO_CAMERA")
                fflush(stdout)
                self.session.commitConfiguration()
                return
            }

            self.session.addInput(input)

            self.videoOutput.alwaysDiscardsLateVideoFrames = true
            self.videoOutput.videoSettings = [
                kCVPixelBufferPixelFormatTypeKey as String: Int(kCVPixelFormatType_32BGRA)
            ]
            self.videoOutput.setSampleBufferDelegate(self, queue: self.sessionQueue)

            if self.session.canAddOutput(self.videoOutput) {
                self.session.addOutput(self.videoOutput)
            }

            self.session.commitConfiguration()
            self.session.startRunning()
            print("READY")
            fflush(stdout)
        }
    }

    func stop() {
        sessionQueue.async { [weak self] in
            guard let self = self else { return }
            if self.session.isRunning {
                self.session.stopRunning()
            }
            exit(0)
        }
    }

    func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }

        // 1. Оптичне розпізнавання штрих-коду через Apple Vision на оригінальному кадрі високої роздільної здатності
        let handler = VNImageRequestHandler(cvPixelBuffer: pixelBuffer, orientation: .up, options: [:])
        try? handler.perform([barcodeRequest])

        // 2. Генерація стисненого оптимізованого прев'ю-кадру для Compose UI
        let now = Date().timeIntervalSince1970
        if (now - lastFrameTime) >= frameInterval {
            lastFrameTime = now
            let ciImage = CIImage(cvPixelBuffer: pixelBuffer)
            let width = ciImage.extent.width
            let targetWidth = 640.0
            let scale = targetWidth / max(width, 1.0)
            
            // Масштабування для швидкого декодування в Compose та дзеркальне відображення для комфорту користувача
            let scaled = ciImage.transformed(by: CGAffineTransform(scaleX: scale, y: scale))
            let mirrored = scaled.transformed(
                by: CGAffineTransform(scaleX: -1, y: 1).translatedBy(x: -scaled.extent.width, y: 0)
            )

            if let colorSpace = CGColorSpace(name: CGColorSpace.sRGB),
               let jpegData = ciContext.jpegRepresentation(
                   of: mirrored,
                   colorSpace: colorSpace,
                   options: [CIImageRepresentationOption(rawValue: kCGImageDestinationLossyCompressionQuality as String): 0.55]
               ) {
                let base64 = jpegData.base64EncodedString()
                print("FRAME:\(base64)")
                fflush(stdout)
            }
        }
    }
}

let scanner = CameraScanner()
scanner.start()

// Моніторинг закриття процесу від Desktop Compose (EOF або команда STOP у stdin)
DispatchQueue.global().async {
    let handle = FileHandle.standardInput
    while true {
        let data = handle.availableData
        if data.isEmpty {
            scanner.stop()
            break
        }
        if let str = String(data: data, encoding: .utf8)?.trimmingCharacters(in: .whitespacesAndNewlines), str == "STOP" {
            scanner.stop()
            break
        }
    }
}

RunLoop.main.run()
