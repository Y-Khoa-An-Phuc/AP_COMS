-- SQL Server DDL Migration for Temporary Password Feature
-- This migration adds the must_change_password and temporary_password columns to the users table

-- Add must_change_password column (only if it doesn't exist)
IF COL_LENGTH('dbo.users', 'must_change_password') IS NULL
BEGIN
    ALTER TABLE users
    ADD must_change_password BIT NOT NULL DEFAULT 0;
END

-- Add temporary_password column (only if it doesn't exist)
IF COL_LENGTH('dbo.users', 'temporary_password') IS NULL
BEGIN
    ALTER TABLE users
    ADD temporary_password BIT NOT NULL DEFAULT 0;
END

-- Optional: Create an index for faster queries on must_change_password (only if it doesn't exist)
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.users') AND name = 'idx_users_must_change_password')
BEGIN
    CREATE INDEX idx_users_must_change_password ON users(must_change_password);
END

-- Note: With hibernate.ddl-auto=update, Hibernate will automatically create these columns
-- when the application starts. This SQL is provided for manual migration scenarios
-- or if you want more control over the migration process.