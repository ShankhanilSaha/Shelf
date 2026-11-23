package com.example.shelf.core.model

/** ISBN-10 / ISBN-13 validation and normalization. */
object Isbn {
    private const val ISBN10_LENGTH = 10
    private const val ISBN13_LENGTH = 13
    private const val ISBN10_MODULUS = 11
    private const val ISBN13_MODULUS = 10
    private const val ISBN13_ODD_WEIGHT = 3
    private const val CHECK_DIGIT_TEN = 10
    private val booklandPrefixes = listOf("978", "979")

    /**
     * Removes spaces and hyphens, uppercases a trailing `x`, and returns the ISBN when it is a
     * valid ISBN-10 or ISBN-13. Returns null otherwise.
     */
    fun normalize(raw: String): String? {
        val compact = raw.filterNot { it == '-' || it.isWhitespace() }.uppercase()
        return compact.takeIf { isValidIsbn13(it) || isValidIsbn10(it) }
    }

    /** True for an EAN-13 barcode that encodes a book (Bookland prefix 978 or 979) with a valid checksum. */
    fun isBookBarcode(ean13: String): Boolean = isValidIsbn13(ean13) && booklandPrefixes.any { ean13.startsWith(it) }

    fun isValidIsbn13(value: String): Boolean {
        if (value.length != ISBN13_LENGTH || !value.all(Char::isDigit)) return false
        val sum = value.dropLast(1).mapIndexed { index, char ->
            val digit = char.digitToInt()
            if (index % 2 == 0) digit else digit * ISBN13_ODD_WEIGHT
        }.sum()
        val check = (ISBN13_MODULUS - sum % ISBN13_MODULUS) % ISBN13_MODULUS
        return check == value.last().digitToInt()
    }

    fun isValidIsbn10(value: String): Boolean {
        val body = value.dropLast(1)
        val checkValue = when (val last = value.lastOrNull()) {
            null -> null
            'X' -> CHECK_DIGIT_TEN
            in '0'..'9' -> last.digitToInt()
            else -> null
        }
        if (value.length != ISBN10_LENGTH || checkValue == null || !body.all(Char::isDigit)) return false
        val sum = body.mapIndexed { index, char -> char.digitToInt() * (ISBN10_LENGTH - index) }.sum() + checkValue
        return sum % ISBN10_MODULUS == 0
    }
}
