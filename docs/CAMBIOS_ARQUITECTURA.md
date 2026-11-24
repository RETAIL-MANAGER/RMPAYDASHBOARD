# Cambios de arquitectura

Registro fechado de cambios en integraciones, flujos entre componentes, dependencias y comunicación entre sistemas.

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
