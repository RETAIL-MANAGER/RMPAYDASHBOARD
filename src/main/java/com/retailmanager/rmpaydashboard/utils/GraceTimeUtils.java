package com.retailmanager.rmpaydashboard.utils;

import java.time.Duration;
import java.time.Instant;

import com.retailmanager.rmpaydashboard.services.DTO.GraceTimeDTO;

/**
 * Cálculos del tiempo de gracia (tiempo gratis) que un administrador otorga a una terminal.
 */
public final class GraceTimeUtils {

    public static final String INVALID_GRACE_MESSAGE = "El tiempo de gracia debe ser de 1 a "
            + GraceTimeDTO.MAX_DAYS + " días o de 1 a " + GraceTimeDTO.MAX_HOURS + " horas";

    private GraceTimeUtils() {
    }

    /**
     * Convierte la cantidad y unidad a una duración.
     *
     * @return la duración, o null si la cantidad o la unidad no son válidas o superan el máximo
     */
    public static Duration toDuration(Integer amount, String unit) {
        if (amount == null || amount < 1 || unit == null) {
            return null;
        }
        if (GraceTimeDTO.UNIT_DAYS.equals(unit) && amount <= GraceTimeDTO.MAX_DAYS) {
            return Duration.ofDays(amount);
        }
        if (GraceTimeDTO.UNIT_HOURS.equals(unit) && amount <= GraceTimeDTO.MAX_HOURS) {
            return Duration.ofHours(amount);
        }
        return null;
    }

    /**
     * Nueva fecha de expiración: si la terminal sigue vigente se suma a su fecha de expiración;
     * si ya venció (o no tiene fecha) se cuenta desde ahora.
     */
    public static Instant calculateExpirationDate(Instant currentExpirationDate, Duration grace, Instant now) {
        Instant base = (currentExpirationDate == null || currentExpirationDate.isBefore(now))
                ? now
                : currentExpirationDate;
        return base.plus(grace);
    }
}
