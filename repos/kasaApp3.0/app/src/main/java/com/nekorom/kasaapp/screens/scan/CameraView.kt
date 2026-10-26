package com.nekorom.kasaapp.screens.scan

import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.nekorom.kasaapp.MainViewModel
import com.nekorom.kasaapp.R
import com.nekorom.kasaapp.data.roomDB.entity.ProductEntity
import com.nekorom.kasaapp.domain.BarCodeAnalyser
import com.nekorom.kasaapp.navigation.BottomNavRoute
import com.nekorom.kasaapp.navigation.NavRoute
import com.nekorom.kasaapp.screens.edit.EditCodeViewModel
import com.nekorom.kasaapp.screens.settings.CamKey.FRONT_CAM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * Екран оптичного сканування штрихкодів та ручного введення акцизних марок (Camera & Barcode Scanner).
 *
 * Архітектура та функціонал:
 * 1. **CameraX + ML Kit**: Інтегрує [PreviewView] та [ImageAnalysis] через аналізатор [BarCodeAnalyser].
 * 2. **Звуковий супровід**: Відтворює касовий звуковий сигнал `R.raw.beep` при успішному зчитуванні.
 * 3. **Антидребезг (Cooldown)**: Призупиняє аналіз на 2000 мс після детекції коду для уникнення дублів.
 * 4. **Маршрутизація за режимами ([barNum])**:
 *    - `"1".."4"`: Запис розпізнаного ШК у відповідне поле товару (`barCode1`..`barCode4`) в екрані редагування.
 *    - `"51"`: Пошук товару в базі за ШК. Якщо товар відсутній — відкриває створення нової картки.
 *    - `"101"`: Режим продажу — додавання товару в поточний чек через [MainViewModel.sendCode] і перехід до списку.
 * 5. **Валідатор української акцизної марки**:
 *    - Формат державної акцизної марки України: 4 літери серії + 6 цифр номера (разом 10 символів, наприклад `АААА123456`).
 *    - Динамічне перемикання клавіатури: літери для перших 4 знаків, цифрова клавіатура після 4-го символу.
 *
 * @param navController Контролер навігації нижньої панелі.
 * @param editCodeViewModel ViewModel редагування номенклатури.
 * @param barNum Код операції / цільового слота для штрихкоду.
 * @param viewModel Головна [MainViewModel] касового вузла.
 * @param values Системні відступи [PaddingValues].
 * @param navHostControllerMain Головний кореневий NavController.
 * @param inNavController Внутрішній вкладений NavController сканера.
 */
@RequiresApi(Build.VERSION_CODES.M)
@androidx.camera.core.ExperimentalGetImage
@Composable
fun CameraView(
    navController: NavController?,
    editCodeViewModel: EditCodeViewModel?,
    barNum: String?,
    viewModel: MainViewModel? = null,
    values: PaddingValues,
    navHostControllerMain: NavHostController,
    inNavController: NavHostController? = null
) {
    val sharedPref = LocalContext.current.getSharedPreferences(
        stringResource(R.string.SHASED_PRREF_camera_mode),
        Context.MODE_PRIVATE
    )
    val camSet = sharedPref.getString(stringResource(id = R.string.SHASED_PRREF_camera_mode), "")
    val cameraSelector = if (camSet == FRONT_CAM) CameraSelector.DEFAULT_FRONT_CAMERA
    else CameraSelector.DEFAULT_BACK_CAMERA


    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    val context = LocalContext.current

    val bar = remember {
        mutableStateOf("")
    }
    val products = editCodeViewModel?.products


    Box {
        Column {

            AndroidView(
                modifier = Modifier
                    // .size(width = 250.dp, height = 250.dp)
                    .fillMaxWidth()
                    .weight(0.5f),
                factory = { context ->
                    val cameraManager =
                        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

                    //cameraManager.setTorchMode(cameraManager.cameraIdList[0], true)
                    val cameraExecutor = Executors.newSingleThreadExecutor()
                    val previewView = PreviewView(context).also {
                        it.scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder()

                            .build()
                            .also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                        val imageCapture = ImageCapture.Builder().build()


                        val imageAnalyzer = ImageAnalysis.Builder()
                            .build()
                            .also {
                                it.setAnalyzer(cameraExecutor, BarCodeAnalyser { barcodes ->

                                    Toast.makeText(
                                        context,
                                        "${barcodes[0].displayValue}|| ${barcodes.size}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    bar.value = ""
                                    barcodes.forEach { barcode ->
                                        when (barNum) {
                                            "1" -> {
                                                editCodeViewModel?.product?.barCode1 =
                                                    barcode.displayValue ?: ""
                                                navHostControllerMain.navigateUp()
                                            }


                                            "2" -> {
                                                editCodeViewModel?.product?.barCode2 =
                                                    barcode.displayValue ?: ""
                                                navHostControllerMain.navigateUp()
                                            }


                                            "3" -> {
                                                editCodeViewModel?.product?.barCode3 =
                                                    barcode.displayValue ?: ""
                                                navHostControllerMain.navigateUp()
                                            }

                                            "4" -> {
                                                editCodeViewModel?.product?.barCode4 =
                                                    barcode.displayValue ?: ""
                                                navHostControllerMain.navigateUp()
                                            }

                                            "51" -> {
                                                val index = products?.indexOfFirst {
                                                    it.barCode1 == barcode.displayValue ||
                                                            it.barCode2 == barcode.displayValue ||
                                                            it.barCode3 == barcode.displayValue ||
                                                            it.barCode4 == barcode.displayValue
                                                }

                                                if (index == -1) {
                                                    editCodeViewModel?.product = ProductEntity()
                                                    editCodeViewModel?.product?.barCode1 =
                                                        barcode.displayValue ?: ""
                                                    navController?.navigate(NavRoute.EditProductScreen.route + "/new")
                                                } else {
                                                    editCodeViewModel?.product =
                                                        products?.toList()?.getOrNull(index ?: -1)
                                                            ?: ProductEntity()
                                                }
                                            }

                                            "101" -> {

                                                viewModel?.sendCode(barcode.displayValue ?: "")
                                                inNavController?.navigate(BottomNavRoute.ListScreenIn.route)
                                            }

                                            else -> {
                                                viewModel?.sendCode(barcode.displayValue ?: "")
                                            }

                                        }
                                        Toast.makeText(context, "$barNum", Toast.LENGTH_SHORT)
                                            .show()

                                        bar.value =
                                            bar.value + "\n" + barcode.displayValue + "\t|Format:" + barcode.format + "\t|ValueType:" + barcode.valueType
                                    }

                                    cameraProvider.unbindAll()
                                    mediaPlayer = MediaPlayer.create(context, R.raw.beep)
                                    mediaPlayer?.start()
                                    CoroutineScope(Dispatchers.Default).launch {
                                        delay(2000)

                                        withContext(Dispatchers.Main) {
                                            try {
                                                // Unbind use cases before rebinding


                                                // Bind use cases to camera
                                                cameraProvider.bindToLifecycle(
                                                    context as ComponentActivity,
                                                    cameraSelector,
                                                    preview,
                                                    imageCapture,
                                                    it
                                                )// context

                                            } catch (exc: Exception) {
                                                Log.e("DEBUG", "Use case binding failed", exc)
                                            }
                                        }

                                    }


                                })
                            }



                        try {
                            // Unbind use cases before rebinding
                            cameraProvider.unbindAll()

                            // Bind use cases to camera
                            cameraProvider.bindToLifecycle(
                                context as ComponentActivity,
                                cameraSelector,
                                preview,
                                imageCapture,
                                imageAnalyzer
                            )// context

                        } catch (exc: Exception) {
                            Log.e("DEBUG", "Use case binding failed", exc)
                        }
                    }, ContextCompat.getMainExecutor(context))
                    previewView
                },

                )
            // Нижня частина екрана: ручне введення акцизної марки для алкогольних/тютюнових виробів
            Column(Modifier.weight(0.5f)) {
                var isButtonEnabled by remember { mutableStateOf(false) }

                var exciseField by remember {
                    mutableStateOf("")
                }
                var keyboardOptions by remember {
                    mutableStateOf(KeyboardOptions(keyboardType = KeyboardType.Text))
                }

                // Поле введення акцизної марки: валідує перші 4 літери та наступні 6 цифр
                TextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height = 85.dp),
                    value = exciseField,
                    onValueChange = { newValue ->
                        exciseField = if (
                            newValue.take(4).all { it.isLetter() } &&
                            newValue.drop(4).all { it.isDigit() })
                            newValue.uppercase().take(10)
                        else
                            exciseField

                        // Кнопка активна тільки при повному збігу формату (10 символів: 4 літери + 6 цифр)
                        isButtonEnabled = exciseField.length == 10 &&
                                exciseField.take(4).all { it.isLetter() } &&
                                exciseField.drop(4).all { it.isDigit() }

                        // Перемикання клавіатури на цифрову після введення 4 літер серії марки
                        if (exciseField.length >= 4)
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        else
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                capitalization = KeyboardCapitalization.Characters
                            )
                    },
                    label = { Text(text = "АКЦИЗНИЙ НОМЕР") },
                    keyboardOptions = keyboardOptions,
                    textStyle = LocalTextStyle.current.copy(fontSize = 40.sp)
                )
                if (isButtonEnabled) {
                    Button(onClick = {
                        viewModel?.sendCode(exciseField)
                        exciseField = ""
                        mediaPlayer = MediaPlayer.create(context, R.raw.beep)
                        mediaPlayer?.start()
                        if (barNum == "101") {
                            inNavController?.navigate(BottomNavRoute.ListScreenIn.route)
                        }
                        isButtonEnabled = false

                    }) {
                        Text("Відправити")
                    }
                }





                Text(text = bar.value)

            }


        }
        Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.End) {
            Button(onClick = {
                navHostControllerMain.navigate(NavRoute.SettingsCam.route)
            }) {
                Text(text = "вибір камери")
            }
        }
    }

}
