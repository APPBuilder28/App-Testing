package com.example

import com.example.core.security.JsonParseResult
import com.example.core.security.JsonSourceImporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonSourceImporterTest {

    @Test
    fun `valid json array parses all required fields`() {
        val raw = """
            [
              {
                "sourceName": "Living Room TV",
                "serverUrl": "http://iptv.example.com:8080",
                "username": "alice",
                "password": "secret",
                "expiryDate": "2027-05-01"
              },
              {
                "sourceName": "Backup Line",
                "serverUrl": "https://backup.example.net",
                "username": "bob",
                "password": "pw",
                "expiryDate": ""
              }
            ]
        """.trimIndent()

        val result = JsonSourceImporter.parseAndValidateJson(raw)
        assertTrue(result is JsonParseResult.Success)
        val items = (result as JsonParseResult.Success).sources
        assertEquals(2, items.size)
        assertEquals("Living Room TV", items[0].sourceName)
        assertEquals("http://iptv.example.com:8080", items[0].serverUrl)
        assertEquals("alice", items[0].username)
        assertEquals("secret", items[0].password)
        assertEquals("2027-05-01", items[0].expiryDate)
        assertEquals("", items[1].expiryDate)
    }

    @Test
    fun `malformed json returns error`() {
        val result = JsonSourceImporter.parseAndValidateJson("[{broken json")
        assertTrue(result is JsonParseResult.Error)
    }

    @Test
    fun `non-array json root returns schema error`() {
        val raw = """
            {
              "sourceName": "Living Room",
              "serverUrl": "http://example.com",
              "username": "u",
              "password": "p",
              "expiryDate": "2027-01-01"
            }
        """.trimIndent()
        val result = JsonSourceImporter.parseAndValidateJson(raw)
        assertTrue(result is JsonParseResult.Error)
    }

    @Test
    fun `missing expiryDate key fails exact schema validation`() {
        val raw = """
            [
              {
                "sourceName": "Living Room",
                "serverUrl": "http://example.com",
                "username": "u",
                "password": "p"
              }
            ]
        """.trimIndent()
        val result = JsonSourceImporter.parseAndValidateJson(raw)
        assertTrue(result is JsonParseResult.Error)
    }

    @Test
    fun `extra unexpected key fails exact schema validation`() {
        val raw = """
            [
              {
                "sourceName": "Living Room",
                "serverUrl": "http://example.com",
                "username": "u",
                "password": "p",
                "expiryDate": "2027-01-01",
                "extra": "bad"
              }
            ]
        """.trimIndent()
        val result = JsonSourceImporter.parseAndValidateJson(raw)
        assertTrue(result is JsonParseResult.Error)
    }

    @Test
    fun `blank serverUrl or username returns schema error`() {
        val raw = """
            [
              {
                "sourceName": "Living Room",
                "serverUrl": "  ",
                "username": "u",
                "password": "p",
                "expiryDate": "2027-01-01"
              }
            ]
        """.trimIndent()
        val result = JsonSourceImporter.parseAndValidateJson(raw)
        assertTrue(result is JsonParseResult.Error)
    }
}
