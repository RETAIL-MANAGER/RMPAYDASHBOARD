package com.retailmanager.rmpaydashboard.services.DTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos para que un administrador otorgue tiempo de gracia (tiempo gratis) a una terminal
 * o a todas las terminales de un negocio.
 */
@Getter @Setter
@NoArgsConstructor
public class GraceTimeDTO {

    public static final String UNIT_DAYS = "DAYS";
    public static final String UNIT_HOURS = "HOURS";
    public static final int MAX_DAYS = 30;
    public static final int MAX_HOURS = MAX_DAYS * 24;

    @NotNull(message = "La cantidad de tiempo de gracia es requerida")
    @Min(value = 1, message = "La cantidad de tiempo de gracia debe ser mayor que 0")
    private Integer amount;

    @NotBlank(message = "La unidad del tiempo de gracia es requerida")
    @Pattern(regexp = "DAYS|HOURS", message = "La unidad del tiempo de gracia debe ser DAYS o HOURS")
    private String unit;

    @NotBlank(message = "El motivo del tiempo de gracia es requerido")
    @Size(max = 250, message = "El motivo del tiempo de gracia no puede superar 250 caracteres")
    private String reason;
}
