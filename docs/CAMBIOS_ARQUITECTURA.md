# Cambios de arquitectura

Registro fechado de cambios en integraciones, flujos entre componentes, dependencias y comunicación entre sistemas.

---

## 2026-11-24 — Tiempo de gracia (tiempo gratis) otorgado por administrador

**Descripción**
- Un administrador (`ROLE_MANAGER`) puede dar tiempo gratis, en días u horas, a una terminal o a todas las terminales de un negocio, desde la pantalla de edición del cliente.
- Se implementó con la opción A aprobada: el tiempo se suma a `Terminal.expirationDate`. No hay un campo de gracia separado, así que todo lo que ya usa esa fecha la respeta sin cambios: la rutina diaria de desactivación, el JWT de la app, el estado activo o inactivo en el front, los reportes y los cobros automáticos.
- Cada otorgamiento queda auditado en `activity_log` con el tipo nuevo `TERMINAL_GRACE_GRANTED`.

**Motivo:** permitir cortesías, periodos de prueba y tiempo de gracia sin registrar un pago.

**Flujo resultante**
```
Front edit-client (solo ROLE_MANAGER: botón "Dar gracia" / "Gracia a todas")
  -> POST /api/terminals/{terminalId}/grace  |  POST /api/business/{businessId}/grace
     (@PreAuthorize hasAuthority('ROLE_MANAGER'))
  -> TerminalService.grantGraceTime / grantGraceTimeToBusiness (una transacción)
       vigente:            expirationDate = expirationDate + tiempo
       vencida/sin fecha:  expirationDate = ahora + tiempo
       enable = true; si es la principal, se reinician los avisos de vencimiento del negocio
       -> ActivityLog TERMINAL_GRACE_GRANTED (si falla, se revierte todo)
```

**Elementos afectados**
- Backend: `TerminalController`, `ITerminalService`, `TerminalService`, el enum `ActivityType`, el nuevo `GraceTimeDTO` y el nuevo `utils/GraceTimeUtils`.
- Base de datos: la restricción CHECK de `activity_log.activity_type` (script `docs/sql/2026-11-24_tiempo_de_gracia.sql`).
- Front: `terminal.service.ts` y el componente `edit-client`.

**Riesgos**
- Si se despliega sin el script SQL, cualquier intento de dar gracia falla con 500 y no se otorga nada.
- La desactivación sigue corriendo una vez al día. Una gracia en horas deja la terminal activa hasta la siguiente corrida diaria, aunque el JWT de la app sí vence a la hora exacta.
- La primera vez que se usa `@PreAuthorize` en el proyecto es en estos dos endpoints. El resto de endpoints sigue sin control por rol.

**Cómo se verificó**
- Compilación del backend y pruebas unitarias (`GraceTimeUtilsTest`, 5 pruebas).
- Script SQL ejecutado en la base local dentro de una transacción con ROLLBACK.
- Compilación del front.
- No probado: llamadas reales al endpoint ni la pantalla en el navegador.

---

## 2026-11-23 — Zip code obligatorio en BlackStone (BMS Pay)

**Descripción**
- El token de tarjeta (`PaymentData`) ahora guarda el zip code con el que se creó en BlackStone. Los cobros con token (manual y automático) usan ese zip en lugar del zip de la dirección del negocio.
- Se agregó una barrera de validación del zip antes de cada llamada a BlackStone (`sale`, `GetTokenForCard`, `SaleWithToken`): ninguna petición sale con zip nulo, vacío o mal formado.
- Frontend (FrontWebServiceRMpay): los formularios que capturan zip exigen exactamente 5 dígitos.

**Motivo:** BlackStone hizo obligatorio el zip code en las transacciones.

**Flujo resultante**
```
Front (formulario, 5 dígitos)
  -> Backend servicio (ZipCodeUtils.normalize; si inválido -> error claro, no se llama a BlackStone)
     -> Crear token: zip enviado a GetTokenForCard se guarda en PaymentData.zipCode
     -> Cobro con token (manual / BackgroundRoutinesService.automaticPayments -> InvoiceServices.doPayment "TOKEN"):
        usa PaymentData.zipCode; si el token no tiene zip -> se rechaza (registrar tarjeta de nuevo)
  -> BlackStone.* (segunda barrera: normaliza; si inválido no llama al proveedor)
```

**Elementos afectados**
- Backend: `BlackStone`, `InvoiceServices`, `BusinessService`, `UserService`, `TerminalService`, entidad `PaymentData`, nueva utilidad `utils/ZipCodeUtils`.
- Base de datos: tabla `PaymentData`, nueva columna `zipCode` (nullable).
- Frontend: `helpers/utils.ts` y 5 formularios (registro, negocio, perfil, edición de cliente, información de pago).
- Sin nuevas dependencias. La serialización (Gson en `ConsumerApi`) no se modificó; los nombres de campo enviados (`ZipCode` / `zipCode`) no cambiaron.

**Riesgos**
- Tokens existentes sin zip guardado dejan de poder cobrarse (decisión aprobada: rechazar) hasta que el cliente registre la tarjeta de nuevo. Afecta pagos automáticos.
- Los nombres de campo y el formato exigido por BlackStone por endpoint no están confirmados con la documentación oficial (pendiente con el proveedor).

**Cómo se verificó**
- Compilación del backend (`mvnw compile`) y prueba unitaria `ZipCodeUtilsTest` (5 pruebas OK).
- Compilación del frontend (`ng build`).
- No se probó contra el ambiente de BlackStone ni contra una base de datos real.
