CREATE TABLE familie (
    id           uuid PRIMARY KEY,
    name         varchar(80) NOT NULL,
    erstellt_von uuid        REFERENCES konto (id) ON DELETE SET NULL,
    erstellt_am  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE mitgliedschaft (
    familie_id     uuid        NOT NULL REFERENCES familie (id) ON DELETE CASCADE,
    konto_id       uuid        NOT NULL REFERENCES konto (id) ON DELETE CASCADE,
    rolle          varchar(20) NOT NULL CHECK (rolle IN ('SENIOR', 'ANGEHOERIGER', 'VERWALTER')),
    beigetreten_am timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (familie_id, konto_id)
);

CREATE INDEX mitgliedschaft_konto_idx ON mitgliedschaft (konto_id);

-- Einmal-Code für den QR-Code, 15 Minuten gültig. Gespeichert wird nur der Hash.
CREATE TABLE einladung (
    id               uuid PRIMARY KEY,
    familie_id       uuid        NOT NULL REFERENCES familie (id) ON DELETE CASCADE,
    code_hash        varchar(64) NOT NULL UNIQUE,
    -- So soll der Senior in der Familie heißen, zum Beispiel "Mama"
    name_fuer_senior varchar(80),
    gueltig_bis      timestamptz NOT NULL,
    verwendet        boolean     NOT NULL DEFAULT false,
    erstellt_am      timestamptz NOT NULL DEFAULT now()
);

-- Der Inhalt (geteilter_text) ist nur da, wenn der Senior ihn ausdrücklich teilt.
-- Hilfeanfragen werden nach 30 Tagen automatisch gelöscht.
CREATE TABLE hilfeanfrage (
    id             uuid PRIMARY KEY,
    von_konto_id   uuid        NOT NULL REFERENCES konto (id) ON DELETE CASCADE,
    familie_id     uuid        NOT NULL REFERENCES familie (id) ON DELETE CASCADE,
    thema          varchar(40) NOT NULL,
    geteilter_text varchar(2000),
    status         varchar(10) NOT NULL DEFAULT 'OFFEN' CHECK (status IN ('OFFEN', 'ERLEDIGT')),
    erstellt_am    timestamptz NOT NULL DEFAULT now(),
    erledigt_am    timestamptz
);

CREATE INDEX hilfeanfrage_familie_idx ON hilfeanfrage (familie_id, status);
CREATE INDEX hilfeanfrage_erstellt_idx ON hilfeanfrage (erstellt_am);

-- Eigene Anleitungen gehören zu einer Familie und verschwinden mit ihr.
ALTER TABLE anleitung
    ADD CONSTRAINT anleitung_familie_fk FOREIGN KEY (familie_id) REFERENCES familie (id) ON DELETE CASCADE;
