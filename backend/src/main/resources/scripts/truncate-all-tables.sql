-- ============================================
-- Script to TRUNCATE all tables in the database
-- ============================================
-- This will delete all data but keep the table structure intact
-- Run this script when you want to reset data while keeping the schema

USE AP_COMS;
GO

PRINT 'Starting truncate process...';
PRINT '============================================';

-- Step 1: Drop all foreign key constraints (we'll recreate them later)
PRINT 'Step 1: Dropping all foreign key constraints...';
DECLARE @dropConstraintsSql NVARCHAR(MAX) = '';

SELECT @dropConstraintsSql = @dropConstraintsSql +
    'ALTER TABLE ' + QUOTENAME(OBJECT_SCHEMA_NAME(parent_object_id)) + '.' +
    QUOTENAME(OBJECT_NAME(parent_object_id)) +
    ' DROP CONSTRAINT ' + QUOTENAME(name) + ';' + CHAR(13)
FROM sys.foreign_keys;

-- Execute the drop constraints SQL
IF LEN(@dropConstraintsSql) > 0
BEGIN
    EXEC sp_executesql @dropConstraintsSql;
    PRINT 'All foreign key constraints dropped.';
END
ELSE
BEGIN
    PRINT 'No foreign key constraints found.';
END
GO

-- Step 2: Truncate tables in correct order
PRINT '';
PRINT 'Step 2: Truncating tables...';

-- Truncate child tables first (tables with foreign keys)
IF EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'one_time_tokens')
BEGIN
    PRINT '  - Truncating table: one_time_tokens';
    TRUNCATE TABLE one_time_tokens;
END

IF EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'employees')
BEGIN
    PRINT '  - Truncating table: employees';
    TRUNCATE TABLE employees;
END

-- Truncate join tables (many-to-many relationships)
IF EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'user_roles')
BEGIN
    PRINT '  - Truncating table: user_roles';
    TRUNCATE TABLE user_roles;
END

IF EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'users')
BEGIN
    PRINT '  - Truncating table: users';
    TRUNCATE TABLE users;
END

-- Truncate parent tables (tables without dependencies)
IF EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'branches')
BEGIN
    PRINT '  - Truncating table: branches';
    TRUNCATE TABLE branches;
END

IF EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'occupations')
BEGIN
    PRINT '  - Truncating table: occupations';
    TRUNCATE TABLE occupations;
END

IF EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'roles')
BEGIN
    PRINT '  - Truncating table: roles';
    TRUNCATE TABLE roles;
END

PRINT 'All tables truncated successfully.';
GO

-- Step 3: Reset identity columns (only for tables with identity columns)
PRINT '';
PRINT 'Step 3: Resetting identity columns...';

-- Only reset identity for tables that have identity columns
IF EXISTS (SELECT * FROM sys.identity_columns WHERE object_id = OBJECT_ID('one_time_tokens'))
BEGIN
    PRINT '  - Resetting identity for: one_time_tokens';
    DBCC CHECKIDENT ('one_time_tokens', RESEED, 0);
END

IF EXISTS (SELECT * FROM sys.identity_columns WHERE object_id = OBJECT_ID('employees'))
BEGIN
    PRINT '  - Resetting identity for: employees';
    DBCC CHECKIDENT ('employees', RESEED, 0);
END

IF EXISTS (SELECT * FROM sys.identity_columns WHERE object_id = OBJECT_ID('users'))
BEGIN
    PRINT '  - Resetting identity for: users';
    DBCC CHECKIDENT ('users', RESEED, 0);
END

IF EXISTS (SELECT * FROM sys.identity_columns WHERE object_id = OBJECT_ID('branches'))
BEGIN
    PRINT '  - Resetting identity for: branches';
    DBCC CHECKIDENT ('branches', RESEED, 0);
END

IF EXISTS (SELECT * FROM sys.identity_columns WHERE object_id = OBJECT_ID('occupations'))
BEGIN
    PRINT '  - Resetting identity for: occupations';
    DBCC CHECKIDENT ('occupations', RESEED, 0);
END

IF EXISTS (SELECT * FROM sys.identity_columns WHERE object_id = OBJECT_ID('roles'))
BEGIN
    PRINT '  - Resetting identity for: roles';
    DBCC CHECKIDENT ('roles', RESEED, 0);
END

-- user_roles typically doesn't have an identity column
IF EXISTS (SELECT * FROM sys.identity_columns WHERE object_id = OBJECT_ID('user_roles'))
BEGIN
    PRINT '  - Resetting identity for: user_roles';
    DBCC CHECKIDENT ('user_roles', RESEED, 0);
END

PRINT 'Identity columns reset successfully.';
GO

-- Step 4: Recreate all foreign key constraints
PRINT '';
PRINT 'Step 4: Recreating foreign key constraints...';
DECLARE @createConstraintsSql NVARCHAR(MAX) = '';

-- Generate CREATE CONSTRAINT statements
SELECT @createConstraintsSql = @createConstraintsSql +
    'ALTER TABLE ' + QUOTENAME(OBJECT_SCHEMA_NAME(fk.parent_object_id)) + '.' +
    QUOTENAME(OBJECT_NAME(fk.parent_object_id)) +
    ' ADD CONSTRAINT ' + QUOTENAME(fk.name) +
    ' FOREIGN KEY (' +
        STUFF((
            SELECT ',' + QUOTENAME(COL_NAME(fkc.parent_object_id, fkc.parent_column_id))
            FROM sys.foreign_key_columns fkc
            WHERE fkc.constraint_object_id = fk.object_id
            ORDER BY fkc.constraint_column_id
            FOR XML PATH('')
        ), 1, 1, '') +
    ') REFERENCES ' +
    QUOTENAME(OBJECT_SCHEMA_NAME(fk.referenced_object_id)) + '.' +
    QUOTENAME(OBJECT_NAME(fk.referenced_object_id)) + '(' +
        STUFF((
            SELECT ',' + QUOTENAME(COL_NAME(fkc.referenced_object_id, fkc.referenced_column_id))
            FROM sys.foreign_key_columns fkc
            WHERE fkc.constraint_object_id = fk.object_id
            ORDER BY fkc.constraint_column_id
            FOR XML PATH('')
        ), 1, 1, '') +
    ')' +
    CASE WHEN fk.delete_referential_action = 1 THEN ' ON DELETE CASCADE' ELSE '' END +
    CASE WHEN fk.update_referential_action = 1 THEN ' ON UPDATE CASCADE' ELSE '' END +
    ';' + CHAR(13)
FROM sys.foreign_keys fk
WHERE fk.is_ms_shipped = 0; -- Only user-created constraints

-- Execute the create constraints SQL
IF LEN(@createConstraintsSql) > 0
BEGIN
    EXEC sp_executesql @createConstraintsSql;
    PRINT 'All foreign key constraints recreated.';
END
ELSE
BEGIN
    PRINT 'No foreign key constraints to recreate.';
END
GO

PRINT '';
PRINT '============================================';
PRINT 'All tables have been truncated successfully!';
PRINT 'You can now restart the application to seed initial data.';
PRINT '============================================';
GO
