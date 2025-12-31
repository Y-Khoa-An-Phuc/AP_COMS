-- SQL Server DDL Migration for One-Time Tokens Feature
-- This migration creates the one_time_tokens table for storing reusable tokens

-- Create one_time_tokens table (only if it doesn't exist)
IF OBJECT_ID('dbo.one_time_tokens', 'U') IS NULL
BEGIN
    CREATE TABLE one_time_tokens (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        token NVARCHAR(128) NOT NULL UNIQUE,
        user_id BIGINT NOT NULL,
        type NVARCHAR(50) NOT NULL,
        used BIT NOT NULL DEFAULT 0,
        created_at DATETIME2 NOT NULL,
        CONSTRAINT fk_one_time_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

    -- Create index on token for fast lookups
    CREATE UNIQUE INDEX idx_token ON one_time_tokens(token);

    -- Create composite index on user_id and type for batch operations
    CREATE INDEX idx_user_type ON one_time_tokens(user_id, type);
END

-- Note: With hibernate.ddl-auto=update, Hibernate will automatically create this table
-- when the application starts. This SQL is provided for manual migration scenarios
-- or if you want more control over the migration process.
