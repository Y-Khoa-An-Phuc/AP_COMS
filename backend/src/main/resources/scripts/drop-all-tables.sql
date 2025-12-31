-- ============================================
-- Script to DROP all tables in the database
-- ============================================
-- WARNING: This will permanently delete all tables and data!
-- Run this script when you want to completely remove the schema

USE AP_COMS;
GO

-- Step 1: Disable all foreign key constraints
PRINT 'Disabling all foreign key constraints...';
EXEC sp_MSforeachtable 'ALTER TABLE ? NOCHECK CONSTRAINT ALL';
GO

-- Step 2: Drop tables in correct order (child tables first, then parent tables)
PRINT 'Dropping tables...';
PRINT '';

-- Drop child tables first (tables with foreign keys)
IF OBJECT_ID('one_time_tokens', 'U') IS NOT NULL
BEGIN
    PRINT 'Dropping table: one_time_tokens';
    DROP TABLE one_time_tokens;
END
GO

IF OBJECT_ID('employees', 'U') IS NOT NULL
BEGIN
    PRINT 'Dropping table: employees';
    DROP TABLE employees;
END
GO

-- Drop join tables (many-to-many relationships)
IF OBJECT_ID('user_roles', 'U') IS NOT NULL
BEGIN
    PRINT 'Dropping table: user_roles';
    DROP TABLE user_roles;
END
GO

IF OBJECT_ID('users', 'U') IS NOT NULL
BEGIN
    PRINT 'Dropping table: users';
    DROP TABLE users;
END
GO

-- Drop parent tables (tables without dependencies)
IF OBJECT_ID('branches', 'U') IS NOT NULL
BEGIN
    PRINT 'Dropping table: branches';
    DROP TABLE branches;
END
GO

IF OBJECT_ID('occupations', 'U') IS NOT NULL
BEGIN
    PRINT 'Dropping table: occupations';
    DROP TABLE occupations;
END
GO

IF OBJECT_ID('roles', 'U') IS NOT NULL
BEGIN
    PRINT 'Dropping table: roles';
    DROP TABLE roles;
END
GO

PRINT '';
PRINT '===========================================';
PRINT 'All tables have been dropped successfully!';
PRINT 'You can now restart the application to recreate the schema.';
PRINT '===========================================';
GO
