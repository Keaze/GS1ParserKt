GS1ParserKt

A small, dependency-light Kotlin library to parse GS1 barcodes into structured data.

It understands Application Identifiers (AIs), handles both fixed-length and variable-length fields, supports FNC1 and GS
separators, and returns a typed Result model describing either success (parsed values) or specific parse errors.

Features

- Detect GS1 barcodes (FNC1 prefix required)
- Parse multiple AIs from a single barcode
- Support for fixed-length and variable-length AIs (FNC1/GS delimited)
- Decimal handling for weight/measurement AIs (e.g., 310x)
- Clear, typed error reporting (NotAGs1Barcode, AiNotFound, ValueLengthError, GsNotFound)
- JSON-driven AI catalogue (src/main/resources/gs1.json)
- Simple Result type with map/flatMap, etc.
- Kotlin/JVM with Kotest tests

Getting started
Prerequisites

- JDK 11+

Build

- On Windows: .\gradlew.bat build
- On macOS/Linux: ./gradlew build

Run tests

- On Windows: .\gradlew.bat test
- On macOS/Linux: ./gradlew test

Installation/Usage as a library
Currently this repository is not published to a public artifact repository. You can:

- Include it as a composite build / Git submodule, or
- Copy the small set of sources into your project (see src/main/kotlin and src/main/resources/gs1.json).

Packages

- Library root: org.app
- Main classes:
    - org.app.gs1parser.GS1Scanner
    - org.app.gs1parser.data.Gs1Ai, AiValue, Gs1Success, Gs1ParseErrors
    - org.app.utils.ResultKt, JsonResourceReader

Quick examples

1) Detect GS1 format
   Kotlin:

````kotlin
val scanner = org.app.gs1parser.GS1Scanner.getDefaultParser().orThrow()
val isGs1 = scanner.isGs1Format("]C11012345") // true if starts with FNC1 and has payload

````

2) Parse a simple fixed-length AI (01 – GTIN)

````kotlin
val scanner = org.app.gs1parser.GS1Scanner.getDefaultParser().orThrow()
val barcode = "]C10112345678901234"
val result = scanner.parse(barcode)
if (result.isSuccessful) {
    val success = result.orThrow()
    val gtin = success.getById("01")?.value // "12345678901234"
}

````

3) Parse variable-length AI with GS separator (10 – Batch/Lot at most 20 chars)

````kotlin
// Default GS separator in this project is "<GS>" when using getDefaultParser()
val fnc1 = "]C1"
val gs = "<GS>"
val barcode = "${fnc1}10ABC123${gs}0112345678901234"
val res = scanner.parse(barcode)
val success = res.orThrow()
val batch = success.getById("10")?.value // "ABC123"
val gtin = success.getById("01")?.value  // "12345678901234"

````

4) Decimal handling (e.g., 3103 – Net weight in kg with 3 decimal places)

````kotlin
val code = "]C1" + "0199099999543210" + "3103001125" // 3103 => 0.01125? note: see gs1.json mapping
val parsed = scanner.parse(code).orThrow()
val weight = parsed.getById("3103")?.value // e.g., "001.125"

````

Error handling
parse returns ResultKt<Gs1Success, Gs1ParseErrors> where Gs1ParseErrors is a sealed class:

- NotAGs1Barcode — string is empty, missing FNC1, or nothing after FNC1
- AiNotFound(barcodeRemainder) — could not find an AI at current position
- ValueLengthError(aiId, aiLength, barcode) — fixed-length AI has too few digits
- GsNotFound(aiId, barcode) — variable-length AI exceeded max length without GS

Kotlin example:

````kotlin
when (val r = scanner.parse(input)) {
    is org.app.utils.ResultKt.Success -> {
        val data = r.value
        println(data.barcodeValues)
    }
    is org.app.utils.ResultKt.Failure -> {
        when (val err = r.error) {
            is org.app.gs1parser.data.Gs1ParseErrors.NotAGs1Barcode -> println("Not a GS1 barcode")
            is org.app.gs1parser.data.Gs1ParseErrors.AiNotFound -> println("Unknown AI at: ${err.barcode}")
            is org.app.gs1parser.data.Gs1ParseErrors.ValueLengthError -> println("AI ${err.aiId} requires ${err.aiLength} chars")
            is org.app.gs1parser.data.Gs1ParseErrors.GsNotFound -> println("Missing GS for AI ${err.aiId}")
        }
    }
}
````

Configuration
FNC1 and GS separator

- Defaults when using GS1Scanner.getDefaultParser():
    - FNC1 = "]C1"
    - GS = "<GS>"
- You can override them:

````kotlin
val scanner = org.app.gs1parser.GS1Scanner.getDefaultParser(fnc1 = "]C1", gs = "|").orThrow()
````

AI catalogue

- Defined in src/main/resources/gs1.json using kotlinx-serialization format matching Gs1Ai.
- Fields:
    - id: AI string (e.g., "01", "10", "3103")
    - length: max length for value (fixed AIs use exactly this length)
    - description, dataTitle: metadata only
    - fnc1: whether FNC1/GS separator is required (true for variable-length)
    - decimals: number of decimal places (0 for none)
    - shortName, dataType: metadata

Development
Run tests

- .\gradlew.bat test (Windows) or ./gradlew test (macOS/Linux)

Project structure

- src/main/kotlin — library sources
- src/main/resources/gs1.json — AI definitions
- src/test/kotlin — Kotest tests/examples

Result type helpers

- ResultKt provides orThrow(), map, flatMap, recover, etc. to work with success/failure flows.

Acknowledgments

- GS1 specifications for Application Identifiers

Current date

- 2025-08-22
