ALTER VIEW HUSKELAPP_VERSJON_LATEST RENAME TO HUSKELAPP_VERSJON_LATEST_OLD;

CREATE TABLE HUSKELAPP_VERSJON_LATEST
(
    id                 SERIAL PRIMARY KEY,
    uuid               CHAR(36)    NOT NULL UNIQUE,
    huskelapp_id       INTEGER REFERENCES HUSKELAPP (id) ON DELETE CASCADE,
    created_at         timestamptz NOT NULL,
    created_by         VARCHAR(7)  NOT NULL,
    tekst              TEXT,
    oppfolgingsgrunner TEXT        NOT NULL,
    frist              DATE
);

INSERT INTO HUSKELAPP_VERSJON_LATEST (uuid, huskelapp_id, created_at, created_by, tekst, oppfolgingsgrunner, frist)
SELECT uuid, huskelapp_id, created_at, created_by, tekst, oppfolgingsgrunner, frist from HUSKELAPP_VERSJON
WHERE id IN (SELECT latest_versjon_id FROM HUSKELAPP_VERSJON_LATEST_OLD);

DROP VIEW HUSKELAPP_VERSJON_LATEST_OLD;

DELETE FROM HUSKELAPP_VERSJON
WHERE uuid IN (SELECT uuid FROM HUSKELAPP_VERSJON_LATEST);
