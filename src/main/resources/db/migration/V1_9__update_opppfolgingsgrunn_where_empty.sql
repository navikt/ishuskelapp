UPDATE huskelapp
SET updated_at = now()
FROM huskelapp_versjon hv
WHERE hv.oppfolgingsgrunner IS NULL;

UPDATE huskelapp_versjon
SET oppfolgingsgrunner = 'ANNET'
WHERE oppfolgingsgrunner IS NULL;

ALTER TABLE huskelapp_versjon
    ALTER COLUMN oppfolgingsgrunner SET NOT NULL;
