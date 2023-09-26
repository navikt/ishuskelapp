CREATE TABLE HUSKELAPP
(
    id                 SERIAL PRIMARY KEY,
    uuid               CHAR(36)    NOT NULL UNIQUE,
    personident        VARCHAR(11) NOT NULL,
    created_at         timestamptz NOT NULL,
    updated_at         timestamptz NOT NULL,
    is_active          BOOLEAN NOT NULL
);

CREATE INDEX IX_HUSKELAPP_PERSONIDENT on HUSKELAPP (personident);

CREATE TABLE HUSKELAPP_VERSJON
(
    id                 SERIAL PRIMARY KEY,
    uuid               CHAR(36)    NOT NULL UNIQUE,
    huskelapp_id       INTEGER REFERENCES HUSKELAPP (id) ON DELETE CASCADE,
    created_at         timestamptz NOT NULL,
    created_by         VARCHAR(7)  NOT NULL,
    tekst              TEXT
);

CREATE INDEX IX_HUSKELAPP_VERJSON_HUSKELAPP_ID on HUSKELAPP_VERSJON (huskelapp_id);
