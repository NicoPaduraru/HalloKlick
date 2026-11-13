CREATE TABLE anleitung (
    id          uuid PRIMARY KEY,
    titel       varchar(120) NOT NULL,
    kategorie   varchar(40)  NOT NULL,
    plattform   varchar(10)  NOT NULL CHECK (plattform IN ('ANDROID', 'IOS', 'ALLE')),
    quelle      varchar(10)  NOT NULL CHECK (quelle IN ('SYSTEM', 'FAMILIE')),
    -- nur bei eigenen Anleitungen einer Familie gesetzt (Fremdschlüssel folgt mit der Tabelle familie)
    familie_id  uuid,
    -- zusätzliche Suchwörter, zum Beispiel "Bild senden verschicken"
    stichworte  varchar(400) NOT NULL DEFAULT '',
    geprueft_am date,
    erstellt_am timestamptz  NOT NULL DEFAULT now(),
    -- Volltextsuche auf Deutsch: PostgreSQL pflegt diese Spalte selbst
    suchtext    tsvector GENERATED ALWAYS AS (
        to_tsvector('german'::regconfig, titel || ' ' || kategorie || ' ' || stichworte)) STORED,
    CONSTRAINT anleitung_quelle_familie CHECK ((quelle = 'FAMILIE') = (familie_id IS NOT NULL))
);

CREATE INDEX anleitung_suchtext_idx ON anleitung USING gin (suchtext);
CREATE INDEX anleitung_familie_idx ON anleitung (familie_id);

CREATE TABLE anleitung_schritt (
    anleitung_id uuid         NOT NULL REFERENCES anleitung (id) ON DELETE CASCADE,
    nummer       int          NOT NULL CHECK (nummer >= 1),
    text         varchar(600) NOT NULL,
    bild_pfad    varchar(255),
    PRIMARY KEY (anleitung_id, nummer)
);
