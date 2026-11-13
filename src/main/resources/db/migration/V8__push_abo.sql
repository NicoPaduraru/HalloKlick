-- Web-Push-Adresse eines Browsers. p256dh und auth sind die Schlüssel des Browsers,
-- mit denen die Benachrichtigung verschlüsselt wird.
CREATE TABLE push_abo (
    id          uuid PRIMARY KEY,
    konto_id    uuid          NOT NULL REFERENCES konto (id) ON DELETE CASCADE,
    endpoint    varchar(1000) NOT NULL UNIQUE,
    p256dh      varchar(200)  NOT NULL,
    auth        varchar(100)  NOT NULL,
    erstellt_am timestamptz   NOT NULL DEFAULT now()
);

CREATE INDEX push_abo_konto_idx ON push_abo (konto_id);
