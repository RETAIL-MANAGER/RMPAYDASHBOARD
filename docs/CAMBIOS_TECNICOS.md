# Cambios técnicos

Registro fechado de archivos, clases, métodos, validaciones, base de datos, configuración y pruebas.

---

## 2026-11-24 — Tiempo de gracia (tiempo gratis) otorgado por administrador

**Motivo:** que el administrador pueda extender o reactivar terminales por días u horas sin registrar un pago.

### Backend (`rmpaydashboard`)

| Archivo | Tipo | Cambio |
|---|---|---|
| `services/DTO/GraceTimeDTO.java` | NUEVO | `amount` (`@NotNull`, `@Min(1)`), `unit` (`DAYS` \| `HOURS`), `reason` (`@NotBlank`, máx. 250). Constantes `MAX_DAYS = 30` y `MAX_HOURS = 720`. |
| `utils/GraceTimeUtils.java` | NUEVO | `toDuration(amount, unit)` devuelve `null` si el valor es inválido o supera el tope. `calculateExpirationDate(actual, gracia, ahora)` suma a la fecha actual si sigue vigente y, si no, cuenta desde ahora. |
| `models/enums/ActivityType.java` | MODIFICADO | Nuevo valor `TERMINAL_GRACE_GRANTED`, agregado al final. Ningún valor se renombró. |
| `services/services/TerminalService/ITerminalService.java` | MODIFICADO | Nuevos métodos `grantGraceTime(String, GraceTimeDTO, String)` y `grantGraceTimeToBusiness(Long, GraceTimeDTO, String)`. Las firmas existentes no cambian. |
| `services/services/TerminalService/TerminalService.java` | MODIFICADO | Implementación `@Transactional` de los dos métodos, más los privados `applyGraceTime`, `registerGraceTimeActivity` e `invalidGraceTimeResponse`. Fija `enable = true` y la nueva `expirationDate`. Si la terminal es la principal, pone `null` en `priorNotification`, `lastDayNotification` y `afterNotification` del negocio, igual que `doPayment`. Responde 200 con `[{terminalId, name, previousExpirationDate, newExpirationDate, previousEnable, enable}]`, o 400 `{"message": ...}`. |
| `controller/TerminalController.java` | MODIFICADO | `POST /api/terminals/{terminalId}/grace` y `POST /api/business/{businessId}/grace`, los dos con `@PreAuthorize("hasAuthority('ROLE_MANAGER')")`. El usuario administrador sale de `Principal`. |
| `src/test/.../GraceTimeUtilsTest.java` | NUEVO | 5 pruebas: límites, valores inválidos, terminal vigente, vencida y sin fecha. |

**Auditoría:** `activity_log` con `entity_type = TERMINAL`. `userId` y `userName` corresponden al dueño del negocio, como en los demás registros. El administrador queda en `additional_data.grantedBy` y en `detail`. `additional_data` también guarda `amount`, `unit`, `reason`, `previousExpirationDate`, `newExpirationDate`, `previousStatus` y `newStatus`. El historial se consulta con el endpoint existente `GET /api/activity-logs/type/TERMINAL_GRACE_GRANTED` o con `GET /api/activity-logs/business/{id}`.

**Contrato de la API:** solo se agregaron dos endpoints. Un usuario sin `ROLE_MANAGER` recibe 401 (handler `AccessDeniedException` existente). Si falla la validación del DTO, la respuesta es 400 con un mapa campo → mensaje (handler existente).

### Frontend (`FrontWebServiceRMpay`)

| Archivo | Cambio |
|---|---|
| `src/app/services/terminal.service.ts` | Nuevos `grantGraceTime(terminalId, body)` y `grantGraceTimeToBusiness(businessId, body)`. |
| `components/pages/client/edit-client/edit-client.component.ts` | `TerminalService` se obtiene con `inject()`, así que el constructor no cambia. Se agregaron `isManager` (rol `ROLE_MANAGER` leído del token con `LoginService.getUser()`), `openGraceTimeModal(terminal?)` (modal SweetAlert con cantidad, unidad, motivo y vista previa de la fecha), `grantGraceTime` y los auxiliares de cálculo y de mensajes de error. Al terminar, actualiza la fecha y el estado de las terminales en la tabla. |
| `.../edit-client.component.html` | Columna "Acciones" con el botón "Dar gracia" y el botón "Gracia a todas" en el encabezado, los dos solo para administradores. La fecha de expiración ahora muestra la hora (`dd/MM/yyyy HH:mm`). |
| `.../edit-client.component.css`, `src/styles.scss` | Estilos del encabezado y del modal. Van en el archivo global porque SweetAlert se dibuja fuera del componente. |

### Base de datos / scripts
- `docs/sql/2026-11-24_tiempo_de_gracia.sql`: hay que ejecutarlo **antes de desplegar**. Hibernate 6.4 creó `activity_log` con un CHECK sobre `activity_type` que solo admite los valores de entonces, y `ddl-auto=update` no lo actualiza. El script busca la restricción por columna, porque su nombre es autogenerado y cambia entre bases. La elimina y la vuelve a crear como `CK_activity_log_activity_type` con el valor nuevo. Es idempotente e incluye una consulta de historial de solo lectura.

### Riesgos
- Las gracias en horas se aplican en la próxima corrida diaria de `deactivateExpiredTerminals`. La rutina no se modificó.
- La gracia no cambia `Business.enable`. Un negocio desactivado manualmente sigue desactivado.
- Los cobros automáticos (`findTerminalsForPayment`, `expirationDate <= hoy`) se mueven a la nueva fecha.
- Si el cliente paga mientras dura la gracia, la duración pagada se suma después de la gracia, porque `InvoiceServices` extiende desde la fecha de expiración vigente.

### Cómo se probó
- `./mvnw -o compile`: BUILD SUCCESS.
- `./mvnw -o test -Dtest=GraceTimeUtilsTest,ZipCodeUtilsTest`: 10 pruebas, 0 fallos.
- Script SQL en la base local (`RMPAY`), dentro de `BEGIN TRAN ... ROLLBACK`:
  - La primera ejecución reemplaza la restricción.
  - La segunda no cambia nada (es idempotente).
  - Un `INSERT` con `TERMINAL_GRACE_GRANTED` pasa.
  - El ROLLBACK deja la base sin cambios.
  - La primera versión del script tenía un error de sintaxis (`EXEC('...' + QUOTENAME(...))`) y se corrigió con `sp_executesql`.
- Front: `npx ng build --configuration development`, sin errores en los archivos modificados.
- No probado: el endpoint con el backend levantado ni la pantalla en el navegador.

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
