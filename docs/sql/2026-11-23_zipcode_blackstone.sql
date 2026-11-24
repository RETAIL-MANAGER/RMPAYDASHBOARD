-- =====================================================================
-- 2026-11-23 - Zip code obligatorio en BlackStone (BMS Pay)
-- Base de datos: SQL Server (RMPAY). Nombres segun
-- PhysicalNamingStrategyStandardImpl (application.properties).
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) MIGRACION (ejecutar ANTES de desplegar el backend si en produccion
--    hibernate_ddl_auto no es "update"). Idempotente.
-- ---------------------------------------------------------------------
IF COL_LENGTH('dbo.PaymentData', 'zipCode') IS NULL
BEGIN
    ALTER TABLE dbo.PaymentData ADD zipCode VARCHAR(10) NULL;
END
GO

-- ---------------------------------------------------------------------
-- 2) DIAGNOSTICO (solo lectura): zip de la direccion del negocio
-- ---------------------------------------------------------------------
SELECT
  CASE WHEN pd.usingAutomaticPayment = 1 THEN 'AUTOPAGO'
       WHEN b.enable = 1 THEN 'ACTIVO' ELSE 'INACTIVO' END AS segmento,
  COUNT(*) AS total,
  SUM(CASE WHEN a.addressId IS NULL THEN 1 ELSE 0 END) AS sin_direccion,
  SUM(CASE WHEN a.addressId IS NOT NULL AND LTRIM(RTRIM(ISNULL(a.zipcode,''))) = '' THEN 1 ELSE 0 END) AS zip_vacio,
  SUM(CASE WHEN a.zipcode <> LTRIM(RTRIM(a.zipcode)) THEN 1 ELSE 0 END) AS zip_con_espacios,
  SUM(CASE WHEN LTRIM(RTRIM(a.zipcode)) <> ''
            AND LTRIM(RTRIM(a.zipcode)) NOT LIKE '[0-9][0-9][0-9][0-9][0-9]' THEN 1 ELSE 0 END) AS zip_no_5_digitos,
  SUM(CASE WHEN LEN(LTRIM(RTRIM(a.zipcode))) IN (3,4)
            AND LTRIM(RTRIM(a.zipcode)) NOT LIKE '%[^0-9]%' THEN 1 ELSE 0 END) AS posible_cero_perdido
FROM dbo.Business b
LEFT JOIN dbo.Address a      ON a.addressId  = b.addressId
LEFT JOIN dbo.PaymentData pd ON pd.paymentId = b.paymentId
GROUP BY CASE WHEN pd.usingAutomaticPayment = 1 THEN 'AUTOPAGO'
              WHEN b.enable = 1 THEN 'ACTIVO' ELSE 'INACTIVO' END;
GO

-- ---------------------------------------------------------------------
-- 3) DIAGNOSTICO (solo lectura, ejecutar despues de la migracion):
--    tokens sin zip valido. Estos negocios NO podran cobrar con token
--    (manual ni automatico) hasta registrar de nuevo la tarjeta.
-- ---------------------------------------------------------------------
SELECT b.businessId, b.name, b.enable, pd.usingAutomaticPayment
FROM dbo.Business b
JOIN dbo.PaymentData pd ON pd.paymentId = b.paymentId
WHERE pd.zipCode IS NULL
   OR LTRIM(RTRIM(pd.zipCode)) NOT LIKE '[0-9][0-9][0-9][0-9][0-9]'
ORDER BY pd.usingAutomaticPayment DESC, b.businessId;
GO
