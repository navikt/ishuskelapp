-- Fix any existing rows where multiple versions of the same oppfolgingsoppgave are marked as latest.
-- Keep only the newest version (highest id) marked as latest.
UPDATE HUSKELAPP_VERSJON
SET latest = FALSE
WHERE latest = TRUE
  AND id NOT IN (
      SELECT MAX(id)
      FROM HUSKELAPP_VERSJON
      WHERE latest = TRUE
      GROUP BY huskelapp_id
  );

-- Replace the non-unique index with a unique partial index so the database enforces
-- that at most one version per huskelapp_id can be marked as latest.
DROP INDEX IF EXISTS IX_HUSKELAPP_VERSJON_LATEST;

CREATE UNIQUE INDEX IX_HUSKELAPP_VERSJON_LATEST ON HUSKELAPP_VERSJON (huskelapp_id) WHERE latest = TRUE;
