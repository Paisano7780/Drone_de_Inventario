package com.paisano.droneinventoryscanner.data.parser

/**
 * DataParser - Parses raw scanner input and cleans it
 * Removes control characters (CR/LF) and validates the input
 */
class DataParser {

    /**
     * Parse raw input from scanner
     * @param rawInput The raw string received from the scanner
     * @return Cleaned code or null if invalid
     */
    fun parseRawInput(rawInput: String): String? {
        if (rawInput.isEmpty()) {
            return null
        }

        // Remove common control characters: CR (\r), LF (\n), and whitespace
        val cleaned = rawInput
            .replace("\r", "")
            .replace("\n", "")
            .trim()

        // Return null if the cleaned string is empty
        if (cleaned.isEmpty()) {
            return null
        }

        return cleaned
    }

    /**
     * Validate if a code is valid (not empty, no control characters)
     * @param code The code to validate
     * @return true if valid, false otherwise
     */
    fun isValidCode(code: String): Boolean {
        if (code.isEmpty()) {
            return false
        }

        // Check for control characters (ASCII < 32 except space)
        return !code.any { it.code < 32 }
    }
}
