-- Mehrere Sprachen (Deutsch, Englisch, Italienisch, später weitere).

-- In welcher Sprache ein Konto die App benutzt. Damit kommen Benachrichtigungen in der richtigen Sprache an.
ALTER TABLE konto ADD COLUMN sprache varchar(5) NOT NULL DEFAULT 'de';

-- In welcher Sprache eine Anleitung geschrieben ist.
ALTER TABLE anleitung ADD COLUMN sprache varchar(5) NOT NULL DEFAULT 'de';

-- Die Volltextsuche muss die Sprache kennen (Wortstämme und Füllwörter sind in jeder Sprache anders).
-- Darum wird die berechnete Such-Spalte neu angelegt. Sie enthält keine eigenen Daten,
-- PostgreSQL füllt sie aus Titel, Kategorie und Stichwörtern von selbst wieder.
DROP INDEX anleitung_suchtext_idx;
ALTER TABLE anleitung DROP COLUMN suchtext;
ALTER TABLE anleitung ADD COLUMN suchtext tsvector GENERATED ALWAYS AS (
    to_tsvector(
        CASE sprache
            WHEN 'de' THEN 'german'::regconfig
            WHEN 'en' THEN 'english'::regconfig
            WHEN 'it' THEN 'italian'::regconfig
            ELSE 'simple'::regconfig
        END,
        titel || ' ' || kategorie || ' ' || stichworte)) STORED;

CREATE INDEX anleitung_suchtext_idx ON anleitung USING gin (suchtext);
CREATE INDEX anleitung_sprache_idx ON anleitung (sprache);
