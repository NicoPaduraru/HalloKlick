-- Kostenbremse pro Person: wie viele KI-Checks hat ein Konto heute gemacht?
CREATE TABLE nutzung (
    konto_id   uuid NOT NULL REFERENCES konto (id) ON DELETE CASCADE,
    datum      date NOT NULL,
    ki_aufrufe int  NOT NULL DEFAULT 0,
    PRIMARY KEY (konto_id, datum)
);

-- Kostenbremse für alle zusammen: Verbrauch pro Tag, ohne Bezug zu einer Person.
-- Daraus entstehen das Monatsbudget und die interne Übersicht.
CREATE TABLE ki_tagesverbrauch (
    datum          date PRIMARY KEY,
    aufrufe        int    NOT NULL DEFAULT 0,
    eingabe_tokens bigint NOT NULL DEFAULT 0,
    ausgabe_tokens bigint NOT NULL DEFAULT 0
);
