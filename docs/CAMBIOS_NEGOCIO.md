# Cambios a nivel de negocio

Registro fechado, en lenguaje sencillo, de lo que cambia para los usuarios y la operación.

---

## 2026-11-24 — Tiempo de gracia (tiempo gratis) para clientes

**Descripción**
- Desde **Editar cliente**, un administrador puede dar **tiempo gratis** a una terminal (botón **"Dar gracia"**) o a todas las terminales del negocio (**"Gracia a todas"**).
- Se elige la cantidad en **días (máximo 30)** u **horas (máximo 720)** y se escribe un **motivo obligatorio**.
- Si la terminal está **vigente**, el tiempo se suma a su fecha de vencimiento. Por ejemplo: vence el 10, se dan 5 días y pasa a vencer el 15.
- Si está **vencida o desactivada**, se **reactiva de inmediato** y el tiempo cuenta desde ese momento. Por ejemplo: está vencida, se dan 2 días y queda activa por 2 días desde ahora.
- Solo el rol **administrador (`ROLE_MANAGER`)** ve los botones y puede usar la función. Los usuarios de solo lectura y los clientes no pueden.
- Cada vez que se da tiempo queda registrado quién lo dio, cuánto, por qué y las fechas anterior y nueva.

**Motivo:** permitir cortesías, periodos de prueba y tiempo de gracia mientras se resuelve un pago.

**Elementos afectados:** pantalla de edición del cliente (solo administradores), vigencia de las terminales y registro de actividad.

**Riesgos / acciones operativas**
- **El tiempo es un regalo:** si el cliente paga durante la gracia, el mes pagado empieza cuando termina la gracia.
- **El cobro automático se corre:** con autopago, el cobro ocurre en la nueva fecha de vencimiento.
- **Las horas no cortan a la hora exacta:** la desactivación automática corre una vez al día. Una gracia de pocas horas deja la terminal activa hasta la siguiente revisión diaria, aunque la sesión de la app sí vence a la hora indicada.
- **Negocio desactivado:** si el negocio fue desactivado manualmente, dar gracia a sus terminales no lo reactiva.
- **Script previo:** antes de publicar esta versión hay que ejecutar en la base de datos el script `docs/sql/2026-11-24_tiempo_de_gracia.sql`.

**Cómo se verificó:** compilación y pruebas unitarias del backend, prueba del script en la base local (revertida) y compilación del front. Pendiente: prueba funcional en el portal.

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
