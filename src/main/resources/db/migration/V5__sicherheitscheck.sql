-- Wichtig: Hier steht KEIN Nachrichteninhalt. Nur Stufe und Kategorie.
CREATE TABLE sicherheitscheck (
    id           uuid PRIMARY KEY,
    konto_id     uuid        NOT NULL REFERENCES konto (id) ON DELETE CASCADE,
    quelle       varchar(10) NOT NULL CHECK (quelle IN ('TEXT', 'BILD')),
    stufe        varchar(10) NOT NULL CHECK (stufe IN ('ORANGE', 'ROT')),
    kategorie    varchar(20) NOT NULL,
    ki_verwendet boolean     NOT NULL DEFAULT false,
    erstellt_am  timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX sicherheitscheck_konto_idx ON sicherheitscheck (konto_id);

-- "War das hilfreich?" Höchstens eine Antwort pro Check.
CREATE TABLE feedback (
    check_id    uuid PRIMARY KEY REFERENCES sicherheitscheck (id) ON DELETE CASCADE,
    hilfreich   boolean     NOT NULL,
    erstellt_am timestamptz NOT NULL DEFAULT now()
);
