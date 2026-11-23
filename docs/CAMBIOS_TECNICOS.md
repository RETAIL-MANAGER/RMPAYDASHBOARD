# Cambios técnicos

Registro fechado de archivos, clases, métodos, validaciones, base de datos, configuración y pruebas.

---

## 2026-11-23 — Zip code obligatorio en BlackStone (BMS Pay)

**Motivo:** requisito obligatorio de BlackStone: el zip code debe viajar siempre, con valor válido, en todas las transacciones.

### Backend (`rmpaydashboard`)

| Archivo | Tipo | Cambio |
|---|---|---|
| `utils/ZipCodeUtils.java` | NUEVO | `normalize(String)`: hace `trim` y acepta solo `^\d{5}$`; devuelve `null` si es inválido. `isValid(String)`. Constante `INVALID_ZIP_MESSAGE`. No rellena ceros ni recorta ZIP+4. |
| `models/PaymentData.java` | MODIFICADO | Nuevo campo `zipCode` (`@Column(nullable = true, length = 10)`). El `@AllArgsConstructor` de Lombok gana un parámetro; verificado que no se usa en el código (`new PaymentData(` con argumentos: 0 usos). |
| `services/services/Payment/BlackStone.java` | MODIFICADO | En `paymentWithCreditCard` y `paymentWithToken`: normaliza el zip; si es inválido lanza `ConsumeAPIException(msg, 400)` (ya declarada) sin llamar al proveedor. En `getToken`: si es inválido devuelve `null` (contrato documentado). Las firmas no cambian. |
| `services/services/InvoiceServices/InvoiceServices.java` | MODIFICADO | `doPayment` "CREDIT-CARD": normaliza el zip de la dirección del negocio; si es inválido envía `notifyErrorPayment` y responde 406 `{"msg": ...}`. `doPayment` "TOKEN" (manual y automático): usa `PaymentData.zipCode`; si es nulo o inválido envía `notifyErrorPayment` y responde 406 (tokens antiguos rechazados). `validateDataForCreateToken`: valida y normaliza `zipCode` (400 `{"message": ...}`). `createToken`: guarda `PaymentData.zipCode`. `testPayment`: zip fijo `"190001"` → `"00901"`. |
| `services/services/BusinessService/BusinessService.java` | MODIFICADO | `save`, "CREDIT-CARD": valida el zip antes de cobrar (400 `{"msg": ...}`), usa el zip normalizado en `paymentWithCreditCard` y `getToken`, y lo guarda en `PaymentData.zipCode`. |
| `services/services/UserService/UserService.java` | MODIFICADO | `registryWithBusiness`, "CREDIT-CARD": igual que `BusinessService.save`. |
| `services/services/TerminalService/TerminalService.java` | MODIFICADO | `buyTerminal`, "CREDIT-CARD": igual, con el zip de la dirección del negocio en la base de datos. |
| `src/test/.../ZipCodeUtilsTest.java` | NUEVO | 5 pruebas unitarias de normalización. |

**Cambios de contrato de la API (aprobados)**
- `GET` token (`InvoiceServices.getToken`): la respuesta incluye `zipCode` (antes siempre `null`). El front ya lo tenía en `ITokenResponse` y lo muestra enmascarado.
- Nuevas respuestas de error: 400 en registro, compra de terminal y creación de token; 406 en `doPayment` cuando no hay un zip válido. El formato es el mismo `HashMap` que ya usaba cada método.
- No cambió el JSON enviado a BlackStone: mismos campos `ZipCode` (sale / SaleWithToken) y `zipCode` (GetTokenForCard). Gson sin `serializeNulls`; el campo ya no puede ser nulo gracias a la validación previa.

### Frontend (`FrontWebServiceRMpay`)

| Archivo | Cambio |
|---|---|
| `src/app/helpers/utils.ts` | Nueva constante `Utils.PATTERN_ZIP_CODE = /^\d{5}$/`. |
| `components/register/register.component.ts/.html` | `zipcode`: `Validators.pattern`; `maxLength` 30 → 5; mensaje de error. No se modificó `onInitFormPrueba` (formulario de prueba). |
| `components/pages/business/create/create.component.ts` + `business/form/form.component.html` | `Validators.pattern`; `maxlength` 200 → 5; mensaje de error. |
| `components/pages/client/edit-client/edit-client.component.ts/.html` | `Validators.pattern`; mensaje de error. |
| `components/pages/profile/profile.component.ts/.html` | `Validators.pattern` (se agregó `import { Utils }`); mensaje de error. |
| `components/pages/profile/payment-information/payment-information.component.ts/.html` | `zipCode` del formulario de creación de token: `Validators.required` + `pattern`; mensaje de error. |

### Base de datos / scripts
- `docs/sql/2026-11-23_zipcode_blackstone.sql`:
  1. Migración idempotente `ALTER TABLE dbo.PaymentData ADD zipCode VARCHAR(10) NULL`. Ejecutarla antes de desplegar si en producción `hibernate_ddl_auto` no es `update`; ese valor no se pudo verificar porque viene de una variable de entorno.
  2. Diagnóstico de solo lectura de zips de negocios (vacíos, con espacios, que no tienen 5 dígitos, con posibles ceros perdidos), por segmento.
  3. Listado de solo lectura de tokens sin zip válido (los que se rechazarán).
- No hay backfill de tokens existentes (decisión aprobada).

### Riesgos
- Tokens antiguos se rechazan; los pagos automáticos de esos negocios fallan hasta registrar de nuevo la tarjeta.
- El rechazo en el cobro automático repite reintentos y correos (lógica de reintentos existente en `BackgroundRoutinesService.automaticPayments`, no modificada).
- Formularios de edición de negocios con zip inválido existente no se podrán guardar hasta corregirlo.
- Nombre de campo y formato exigido por endpoint: pendiente de confirmar con BlackStone.

### Cómo se probó
- `./mvnw -o compile`: BUILD SUCCESS.
- `./mvnw -o test -Dtest=ZipCodeUtilsTest`: 5 pruebas, 0 fallos.
- Frontend: `npx ng build --configuration development` (ver resultado en el changelog de la entrega).
- No probado: llamadas reales a BlackStone, migración contra la base de datos y pruebas end-to-end en el navegador.
