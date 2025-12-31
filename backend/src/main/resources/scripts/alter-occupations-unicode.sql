-- SQL Server: ensure occupation text fields support Unicode (Vietnamese).
ALTER TABLE occupations
    ALTER COLUMN name NVARCHAR(255) NOT NULL;

ALTER TABLE occupations
    ALTER COLUMN description NVARCHAR(255) NULL;
