package com.retailmanager.rmpaydashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.retailmanager.rmpaydashboard.utils.GraceTimeUtils;

class GraceTimeUtilsTest {

    private static final Instant NOW = Instant.parse("2026-11-24T12:00:00Z");

    @Test
    void toDurationAcceptsDaysAndHoursWithinLimits() {
        assertEquals(Duration.ofDays(1), GraceTimeUtils.toDuration(1, "DAYS"));
        assertEquals(Duration.ofDays(30), GraceTimeUtils.toDuration(30, "DAYS"));
        assertEquals(Duration.ofHours(2), GraceTimeUtils.toDuration(2, "HOURS"));
        assertEquals(Duration.ofHours(720), GraceTimeUtils.toDuration(720, "HOURS"));
    }

    @Test
    void toDurationRejectsInvalidValues() {
        assertNull(GraceTimeUtils.toDuration(0, "DAYS"));
        assertNull(GraceTimeUtils.toDuration(-1, "HOURS"));
        assertNull(GraceTimeUtils.toDuration(null, "DAYS"));
        assertNull(GraceTimeUtils.toDuration(31, "DAYS"));
        assertNull(GraceTimeUtils.toDuration(721, "HOURS"));
        assertNull(GraceTimeUtils.toDuration(1, "WEEKS"));
        assertNull(GraceTimeUtils.toDuration(1, null));
    }

    @Test
    void activeTerminalExtendsFromCurrentExpiration() {
        Instant expiration = NOW.plus(Duration.ofDays(5));
        assertEquals(expiration.plus(Duration.ofDays(2)),
                GraceTimeUtils.calculateExpirationDate(expiration, Duration.ofDays(2), NOW));
    }

    @Test
    void expiredTerminalExtendsFromNow() {
        Instant expiration = NOW.minus(Duration.ofDays(10));
        assertEquals(NOW.plus(Duration.ofHours(48)),
                GraceTimeUtils.calculateExpirationDate(expiration, Duration.ofHours(48), NOW));
    }

    @Test
    void terminalWithoutExpirationExtendsFromNow() {
        assertEquals(NOW.plus(Duration.ofDays(1)),
                GraceTimeUtils.calculateExpirationDate(null, Duration.ofDays(1), NOW));
    }
}
