package com.paisano.droneinventoryscanner.data.parser

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for DataParser
 * Tests raw string parsing and validation
 */
class DataParserTest {

    private lateinit var parser: DataParser

    @Before
    fun setup() {
        parser = DataParser()
    }

    @Test
    fun `parseRawInput should remove CR and LF characters`() {
        // Test with CR
        val input1 = "12345\r"
        assertEquals("12345", parser.parseRawInput(input1))

        // Test with LF
        val input2 = "12345\n"
        assertEquals("12345", parser.parseRawInput(input2))

        // Test with CR+LF
        val input3 = "12345\r\n"
        assertEquals("12345", parser.parseRawInput(input3))
    }

    @Test
    fun `parseRawInput should handle clean strings`() {
        val input = "ABC123"
        assertEquals("ABC123", parser.parseRawInput(input))
    }

    @Test
    fun `parseRawInput should return null for empty strings`() {
        val input = ""
        assertNull(parser.parseRawInput(input))
    }

    @Test
    fun `parseRawInput should return null for only control characters`() {
        val input1 = "\r\n"
        assertNull(parser.parseRawInput(input1))

        val input2 = "\r"
        assertNull(parser.parseRawInput(input2))

        val input3 = "\n"
        assertNull(parser.parseRawInput(input3))
    }

    @Test
    fun `parseRawInput should trim whitespace`() {
        val input = "  12345  "
        assertEquals("12345", parser.parseRawInput(input))
    }

    @Test
    fun `parseRawInput should handle complex mixed input`() {
        val input = "\r\n  ABC123XYZ  \r\n"
        assertEquals("ABC123XYZ", parser.parseRawInput(input))
    }

    @Test
    fun `isValidCode should return true for alphanumeric codes`() {
        assertTrue(parser.isValidCode("12345"))
        assertTrue(parser.isValidCode("ABC123"))
        assertTrue(parser.isValidCode("TEST-CODE-123"))
    }

    @Test
    fun `isValidCode should return false for empty string`() {
        assertFalse(parser.isValidCode(""))
    }

    @Test
    fun `isValidCode should return false for control characters`() {
        assertFalse(parser.isValidCode("123\r45"))
        assertFalse(parser.isValidCode("123\n45"))
        assertFalse(parser.isValidCode("ABC\u0001DEF"))
    }

    @Test
    fun `isValidCode should allow spaces`() {
        assertTrue(parser.isValidCode("CODE 123"))
        assertTrue(parser.isValidCode("MULTI WORD CODE"))
    }
}
