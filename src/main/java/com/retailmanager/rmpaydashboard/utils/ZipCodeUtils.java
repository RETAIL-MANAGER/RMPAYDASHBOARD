package com.retailmanager.rmpaydashboard.utils;

/**
 * Utilidades para el zip code requerido por BlackStone (BMS Pay).
 * BlackStone hizo obligatorio el zip code en las transacciones; este helper
 * garantiza que solo se envie un valor valido de 5 digitos.
 */
public class ZipCodeUtils {

    public static final String INVALID_ZIP_MESSAGE = "A valid 5-digit zip code is required to process card payments";

    private ZipCodeUtils() {
    }

    /**
     * Normaliza un zip code: elimina espacios al inicio y al final y valida que
     * tenga exactamente 5 digitos. No rellena con ceros ni recorta formatos ZIP+4.
     *
     * @param zipCode el zip code a normalizar
     * @return el zip code normalizado, o null si es nulo, vacio o invalido
     */
    public static String normalize(String zipCode) {
        if (zipCode == null) {
            return null;
        }
        String value = zipCode.trim();
        if (!value.matches("\\d{5}")) {
            return null;
        }
        return value;
    }

    /**
     * Indica si el zip code es valido luego de normalizarlo.
     *
     * @param zipCode el zip code a validar
     * @return true si es un zip code valido de 5 digitos
     */
    public static boolean isValid(String zipCode) {
        return normalize(zipCode) != null;
    }
}
