---
name: kasa-pos-architecture
description: Engineering and architectural rules for KasaApp POS ecosystem. Covers Datecs/Exellio fiscal printer protocol (CP1251, BCC, Nibble encoding), Ukrainian fiscal standards (VAT groups A-D, Excise 5%, UKTZED, mandatory excise stamp scanning), Pure Kotlin Core (KMP) isolation from Android SDK, tablet Compose POS UI, Bluetooth SPP transport, and Gemini 3.8 Flash optimization (80-220 line limits, Modular Triad, early returns). Use whenever modifying, designing, or testing KasaApp features, fiscal operations, or protocol implementations.
---

# KasaApp POS Ecosystem Architecture & Protocol Guide

This skill defines the technical rules and fiscal domain standards for developing the **KasaApp** POS ecosystem (Android-First POS, Pure KMP Core, Datecs fiscal printers, Bluetooth scales, and FastAPI backend).

---

## 1. Code Style & Flash Optimization (Non-Negotiable)

1. **File Size Strict Limit:**
   - 80–220 lines per file (absolute max 250 lines).
   - Never write massive bundles or God-classes.
2. **Modular Triad Pattern:**
   Every feature lives in its own package and is split into:
   - `[feature]_contract.kt`: Pure ontology (States, Events, DTOs, 50–80 lines).
   - `[feature]_logic.kt`: Pure Kotlin state machine / business logic (no Android SDK, early returns, no else, 80–150 lines).
   - `[feature]_transport.kt` / `[feature]_view.kt`: Android Compose UI, Ktor client, or Bluetooth SPP driver.
3. **Early Returns & Strict Typing:**
   - No deep nesting (> 3 levels).
   - No `Any?`, `Map<String, Any>`, or raw untyped dictionaries.
   - 100% complete implementations: no `// TODO` or `// ... existing code ...`.

---

## 2. Datecs / Exellio Fiscal Protocol Specification

### Frame Structure
$$\text{Frame} = \langle 0x01 \rangle + \text{Len} + \text{Seq} + \text{Cmd} + \langle 0x05 \rangle + \text{BCC} + \langle 0x03 \rangle$$

- **SOH (`0x01`):** Start of header.
- **Len (4 bytes):** Length from `Len` to `ENQ` inclusive. Encoded with Datecs Nibble Encoding.
- **Seq (1 byte):** Sequence number (`0x40`..`0x7C`), wrapping back to `0x40`.
- **Cmd:** Command string + tab-separated parameters (`\t`), terminated with `\r`.
- **ENQ (`0x05`):** Separator between body and BCC.
- **BCC (4 bytes):** Checksum of all unsigned bytes from `Len` to `ENQ` inclusive, Datecs Nibble Encoded.
- **ETX (`0x03`):** End of frame.

### Datecs Nibble Encoding
Each 4-bit nibble ($0..15$) is encoded as ASCII char `nibble + 0x30`:
- `0..9` $\rightarrow$ `'0'..'9'` (`0x30..0x39`)
- `10 (0xA)` $\rightarrow$ `':'` (`0x3A`)
- `11 (0xB)` $\rightarrow$ `';'` (`0x3B`)
- `12 (0xC)` $\rightarrow$ `'<'` (`0x3C`)
- `13 (0xD)` $\rightarrow$ `'='` (`0x3D`)
- `14 (0xE)` $\rightarrow$ `'>'` (`0x3E`)
- `15 (0xF)` $\rightarrow$ `'?'` (`0x3F`)

### Character Encoding & Communication
- **Text Encoding:** Always encode strings to `Windows-1251` (CP1251) to correctly render Ukrainian characters (`І`, `Ї`, `Є`, `Ґ`).
- **Delimiter:** Tab `\t` (`0x09`).
- **Terminator:** Carriage Return `\r` (`0x0D`).
- **Timeouts:**
  - Command response (`ansJob`): 700–1000 ms $\rightarrow$ retry (`try_to_fix`).
  - Critical timeout: 2000 ms $\rightarrow$ disconnect & reset (`!!!!!`).
  - Transaction debounce: 300 ms silence $\rightarrow$ `END_TRANSACTION`.
  - Keep-alive: RTC time read (`003>`) every 45 seconds over SPP.

---

## 3. Ukrainian Fiscal & Regulatory Requirements

1. **VAT Tax Groups:**
   - Group А: 20%
   - Group Б: 0%
   - Group В: 7% (pharmaceuticals/special)
   - Group Г: Exempt
   - Group Д: Not subject to tax
2. **Excise Duty:**
   - 5% retail excise tax for alcohol and tobacco products.
3. **UKTZED (УКТЗЕД):**
   - 10-digit customs code mandatory for all excisable goods.
4. **Excise Stamp Scanning:**
   - When selling excisable goods (`groupExcise == true`), modal scan prompt for paper or electronic excise stamp is mandatory before adding item to receipt.
5. **Fiscal Shifts & Reports:**
   - Fiscal shift cannot exceed 24 hours without closing.
   - **X-Report (`0045X\t`):** Midday audit report without zeroing memory.
   - **Z-Report (`0045Z\t`):** Daily closing report with fiscal memory zeroing.

---

## 4. Architectural Boundaries

- **`:core-model` & `:core-protocol` (Pure Kotlin / KMP):**
  - **Zero Android SDK dependencies.**
  - Fast JVM unit tests (< 0.5s).
  - Contains Datecs frame builder, BCC calculator, status byte decoders, and fiscal math.
- **`:app` (Android-First POS):**
  - Jetpack Compose tablet layout (10–12" landscape).
  - Bluetooth Classic SPP (`00001101-0000-1000-8000-00805F9B34FB`) for printer and scales.
  - USB-HID scanner listener (`KeyEvent.KEYCODE_ENTER`).
  - Room KMP for local catalog and offline receipt queuing.
  - Ktor HTTP client communicating with FastAPI backend.
