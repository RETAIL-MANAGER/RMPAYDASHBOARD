# Cambios a nivel de negocio

Registro fechado, en lenguaje sencillo, de lo que cambia para los usuarios y la operación.

---

## 2026-11-23 — Zip code obligatorio para pagos con tarjeta (BlackStone)

**Descripción**
- Todos los pagos con tarjeta ahora requieren un zip code válido de **5 dígitos** (por ejemplo `00901`). No se aceptan zip de 4 o 6 dígitos, con letras ni con formato `12345-6789`.
- Al **registrarse**, **comprar terminales** o **pagar con tarjeta**, si el negocio no tiene un zip válido, el pago se detiene con un mensaje claro y **no se cobra**.
- Al **guardar una tarjeta** para pagos automáticos, el zip que escribe el cliente queda guardado junto con la tarjeta y se usa en todos los cobros futuros con esa tarjeta.
- **Tarjetas guardadas antes de este cambio**: no tienen zip guardado. Sus cobros (manuales y automáticos) se **rechazan** con el mensaje "The saved card has no zip code. Please register the card again." y se envía el correo de error de pago. El cliente debe **volver a registrar la tarjeta**.
- En el portal, los formularios de dirección del negocio y de tarjeta muestran el aviso "El zip code debe tener exactamente 5 dígitos" y no dejan guardar un zip inválido.

**Motivo:** BlackStone hizo obligatorio el zip code en las transacciones; sin él, los pagos serían rechazados por el procesador.

**Elementos afectados:** registro de clientes, compra de terminales, pagos de facturas, pagos automáticos, guardado de tarjeta, edición de negocio/perfil.

**Riesgos / acciones operativas**
- Clientes con pagos automáticos y tarjeta antigua dejarán de cobrarse hasta registrar la tarjeta de nuevo. Usar la consulta 3 de `docs/sql/2026-11-23_zipcode_blackstone.sql` para obtener la lista y contactarlos **antes** del despliegue.
- Negocios con zip inválido en su dirección deben corregirlo (consulta 2 del mismo archivo).
- El proceso automático reintenta el cobro varias veces por día; un cliente con tarjeta antigua puede recibir varios correos de error el mismo día (comportamiento previo de reintentos, no modificado).

**Cómo se verificó:** compilación y pruebas unitarias del backend, compilación del frontend. Pendiente: prueba funcional en ambiente de pruebas de BlackStone.
