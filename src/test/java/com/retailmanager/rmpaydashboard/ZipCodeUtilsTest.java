package com.retailmanager.rmpaydashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.retailmanager.rmpaydashboard.utils.ZipCodeUtils;

class ZipCodeUtilsTest {

    @Test
    void acceptsFiveDigitsAndKeepsLeadingZeros() {
        assertEquals("00901", ZipCodeUtils.normalize("00901"));
        assertEquals("12345", ZipCodeUtils.normalize("12345"));
    }

    @Test
    void trimsSurroundingSpaces() {
        assertEquals("00901", ZipCodeUtils.normalize("  00901 "));
    }

    @Test
    void rejectsNullEmptyAndBlank() {
        assertNull(ZipCodeUtils.normalize(null));
        assertNull(ZipCodeUtils.normalize(""));
        assertNull(ZipCodeUtils.normalize("     "));
    }

    @Test
    void rejectsInvalidFormats() {
        assertNull(ZipCodeUtils.normalize("901"));
        assertNull(ZipCodeUtils.normalize("0901"));
        assertNull(ZipCodeUtils.normalize("190001"));
        assertNull(ZipCodeUtils.normalize("00901-1234"));
        assertNull(ZipCodeUtils.normalize("ABCDE"));
        assertNull(ZipCodeUtils.normalize("zipcode"));
        assertNull(ZipCodeUtils.normalize("009 01"));
    }

    @Test
    void isValidMatchesNormalize() {
        assertTrue(ZipCodeUtils.isValid(" 00901"));
        assertFalse(ZipCodeUtils.isValid("1234"));
    }
}
