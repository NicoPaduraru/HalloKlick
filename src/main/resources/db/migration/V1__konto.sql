-- Ein Konto ist entweder ein Senior (anonym, ohne E-Mail) oder ein Angehöriger (mit E-Mail und Passwort).
CREATE TABLE konto (
    id            uuid PRIMARY KEY,
    typ           varchar(20)  NOT NULL CHECK (typ IN ('SENIOR', 'ANGEHOERIGER')),
    anzeigename   varchar(80),
    email         varchar(254) UNIQUE,
    passwort_hash varchar(100),
    tarif         varchar(20)  NOT NULL DEFAULT 'KOSTENLOS' CHECK (tarif IN ('KOSTENLOS')),
    erstellt_am   timestamptz  NOT NULL DEFAULT now(),
    -- E-Mail und Passwort gibt es nur bei Angehörigen, dort sind sie Pflicht
    CONSTRAINT konto_anmeldedaten CHECK (
        (typ = 'ANGEHOERIGER' AND email IS NOT NULL AND passwort_hash IS NOT NULL)
        OR (typ = 'SENIOR' AND email IS NULL AND passwort_hash IS NULL))
);
