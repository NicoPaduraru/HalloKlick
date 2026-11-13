-- Ein Gerät ist der "Login" eines Seniors: kein Passwort, nur ein langer Schlüssel.
-- In der Datenbank liegt nur der Hash des Schlüssels.
CREATE TABLE geraet (
    id              uuid PRIMARY KEY,
    konto_id        uuid        NOT NULL REFERENCES konto (id) ON DELETE CASCADE,
    token_hash      varchar(64) NOT NULL UNIQUE,
    plattform       varchar(10) NOT NULL CHECK (plattform IN ('ANDROID', 'IOS', 'SONSTIGE')),
    zuletzt_gesehen timestamptz NOT NULL DEFAULT now(),
    widerrufen      boolean     NOT NULL DEFAULT false,
    erstellt_am     timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX geraet_konto_idx ON geraet (konto_id);
