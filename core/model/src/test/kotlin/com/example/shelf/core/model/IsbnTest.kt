package com.example.shelf.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IsbnTest {
    @Test
    fun `valid ISBN-13 passes the checksum`() {
        assertTrue(Isbn.isValidIsbn13("9780261103573"))
        assertTrue(Isbn.isValidIsbn13("9780306406157"))
    }

    @Test
    fun `ISBN-13 with a wrong check digit fails`() {
        assertFalse(Isbn.isValidIsbn13("9780261103574"))
    }

    @Test
    fun `valid ISBN-10 passes, including an X check digit`() {
        assertTrue(Isbn.isValidIsbn10("0306406152"))
        assertTrue(Isbn.isValidIsbn10("080442957X"))
    }

    @Test
    fun `ISBN-10 with letters in the body fails`() {
        assertFalse(Isbn.isValidIsbn10("03064A6152"))
        assertFalse(Isbn.isValidIsbn10("030640615"))
    }

    @Test
    fun `normalize strips hyphens and spaces and uppercases x`() {
        assertEquals("9780306406157", Isbn.normalize(" 978-0-306-40615-7 "))
        assertEquals("080442957X", Isbn.normalize("0-8044-2957-x"))
    }

    @Test
    fun `normalize rejects invalid input`() {
        assertNull(Isbn.normalize("12345"))
        assertNull(Isbn.normalize("978-0-306-40615-8"))
        assertNull(Isbn.normalize(""))
    }

    @Test
    fun `only Bookland EAN-13 barcodes are books`() {
        assertTrue(Isbn.isBookBarcode("9780306406157"))
        // A valid EAN-13 grocery barcode is not a book.
        assertFalse(Isbn.isBookBarcode("4006381333931"))
        assertFalse(Isbn.isBookBarcode("96385074"))
    }
}
