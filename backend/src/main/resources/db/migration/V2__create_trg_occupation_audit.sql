-- Create audit trigger for Occupation.
-- Note: Table name is dbo.occupations based on JPA mapping. Adjust if needed.
IF OBJECT_ID('dbo.trg_Occupation_Audit', 'TR') IS NOT NULL
    DROP TRIGGER dbo.trg_Occupation_Audit;

DECLARE @sql NVARCHAR(MAX) = N'
CREATE TRIGGER dbo.trg_Occupation_Audit
ON dbo.occupations
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @actor NVARCHAR(128) = TRY_CAST(SESSION_CONTEXT(N''actor'') AS NVARCHAR(128));

    INSERT INTO dbo.hg_occupations (
        op, id, Oid,
        name, Oname,
        description, Odescription,
        created_at, Ocreated_at,
        updated_at, Oupdated_at,
        actor
    )
    SELECT
        ''C'', i.id, ''M'',
        i.name, ''M'',
        i.description, ''M'',
        i.created_at, ''M'',
        i.updated_at, ''M'',
        @actor
    FROM inserted i
    LEFT JOIN deleted d ON d.id = i.id
    WHERE d.id IS NULL;

    INSERT INTO dbo.hg_occupations (
        op, id, Oid,
        name, Oname,
        description, Odescription,
        created_at, Ocreated_at,
        updated_at, Oupdated_at,
        actor
    )
    SELECT
        ''U'', i.id, NULL,
        i.name,
        CASE
            WHEN (i.name <> d.name) OR (i.name IS NULL AND d.name IS NOT NULL) OR (i.name IS NOT NULL AND d.name IS NULL)
                THEN ''M''
            ELSE NULL
        END,
        i.description,
        CASE
            WHEN (i.description <> d.description) OR (i.description IS NULL AND d.description IS NOT NULL) OR (i.description IS NOT NULL AND d.description IS NULL)
                THEN ''M''
            ELSE NULL
        END,
        i.created_at,
        CASE
            WHEN (i.created_at <> d.created_at) OR (i.created_at IS NULL AND d.created_at IS NOT NULL) OR (i.created_at IS NOT NULL AND d.created_at IS NULL)
                THEN ''M''
            ELSE NULL
        END,
        i.updated_at,
        CASE
            WHEN (i.updated_at <> d.updated_at) OR (i.updated_at IS NULL AND d.updated_at IS NOT NULL) OR (i.updated_at IS NOT NULL AND d.updated_at IS NULL)
                THEN ''M''
            ELSE NULL
        END,
        @actor
    FROM inserted i
    INNER JOIN deleted d ON d.id = i.id;

    INSERT INTO dbo.hg_occupations (
        op, id, Oid,
        name, Oname,
        description, Odescription,
        created_at, Ocreated_at,
        updated_at, Oupdated_at,
        actor
    )
    SELECT
        ''D'', d.id, NULL,
        d.name, NULL,
        d.description, NULL,
        d.created_at, NULL,
        d.updated_at, NULL,
        @actor
    FROM deleted d
    LEFT JOIN inserted i ON i.id = d.id
    WHERE i.id IS NULL;
END
';

EXEC sp_executesql @sql;
