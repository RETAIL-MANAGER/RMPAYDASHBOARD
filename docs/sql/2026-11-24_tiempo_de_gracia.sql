-- =====================================================================
-- 2026-11-24 - Tiempo de gracia (tiempo gratis) otorgado por administrador
-- Base de datos: SQL Server (RMPAY).
--
-- Hibernate 6 creo la tabla activity_log con una restriccion CHECK que
-- solo admite los valores de ActivityType que existian en ese momento.
-- ddl-auto=update NO actualiza esa restriccion, por lo que sin este
-- script el registro del tiempo de gracia (TERMINAL_GRACE_GRANTED)
-- falla y la operacion se revierte completa (no se otorga el tiempo).
--
-- Ejecutar ANTES de desplegar el backend. Idempotente.
-- =====================================================================

DECLARE @ck sysname;
DECLARE @definition nvarchar(max);
DECLARE @sql nvarchar(max);

SELECT @ck = cc.name, @definition = cc.definition
FROM sys.check_constraints cc
JOIN sys.columns c
  ON c.object_id = cc.parent_object_id
 AND c.column_id = cc.parent_column_id
WHERE cc.parent_object_id = OBJECT_ID('dbo.activity_log')
  AND c.name = 'activity_type';

IF @ck IS NOT NULL AND @definition NOT LIKE '%TERMINAL_GRACE_GRANTED%'
BEGIN
    SET @sql = N'ALTER TABLE dbo.activity_log DROP CONSTRAINT ' + QUOTENAME(@ck);
    EXEC sp_executesql @sql;
END

IF NOT EXISTS (
    SELECT 1
    FROM sys.check_constraints cc
    JOIN sys.columns c
      ON c.object_id = cc.parent_object_id
     AND c.column_id = cc.parent_column_id
    WHERE cc.parent_object_id = OBJECT_ID('dbo.activity_log')
      AND c.name = 'activity_type')
BEGIN
    ALTER TABLE dbo.activity_log ADD CONSTRAINT CK_activity_log_activity_type
    CHECK (activity_type IN (
        'CLIENT_REGISTERED',
        'PAYMENT_RECEIVED',
        'TERMINAL_ACTIVATED',
        'CLIENT_DEACTIVATED',
        'CLIENT_UPDATED',
        'TERMINAL_DEACTIVATED',
        'PAYMENT_FAILED',
        'TERMINAL_GRACE_GRANTED'
    ));
END
GO

-- ---------------------------------------------------------------------
-- Consulta (solo lectura): historial de tiempo de gracia otorgado
-- ---------------------------------------------------------------------
SELECT occurred_at, business_id, entity_id AS terminalId, detail, additional_data
FROM dbo.activity_log
WHERE activity_type = 'TERMINAL_GRACE_GRANTED'
ORDER BY occurred_at DESC;
GO
