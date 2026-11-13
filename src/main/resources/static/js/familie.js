// Die Bildschirme für Angehörige: Anmelden, Familie, QR-Einladung, Hilfeanfragen, Anleitungs-Editor.

import { api, datum, ersetze, fehlerbox, h, ladeIch, zeige, zeigeLaden, zustand } from './basis.js';

const ROLLEN = { SENIOR: 'Elternteil', ANGEHOERIGER: 'Angehöriger', VERWALTER: 'Angehöriger (Verwalter)' };
const PLATTFORMEN = { ANDROID: 'Android', IOS: 'iPhone', SONSTIGE: 'anderes Gerät', ALLE: 'Alle Handys' };

export async function familie() {
  zeigeLaden();
  const ich = await ladeIch(true);
  if (!ich || ich.typ !== 'ANGEHOERIGER') {
    zeigeAnmeldung(Boolean(ich));
    return;
  }
  let familien;
  try {
    familien = await api('GET', '/api/familien');
  } catch (e) {
    zeige('Für Angehörige', fehlerbox(e.message));
    return;
  }
  if (familien.length === 0) {
    zeigeFamilieAnlegen(ich);
    return;
  }
  await zeigeUebersicht(ich, familien);
}

// ---------- Anmelden und Registrieren ----------

function zeigeAnmeldung(istSeniorGeraet, neuesKonto = false) {
  const meldung = h('div', {});
  const feld = (id, text, typ, weitere = {}) => [
    h('label', { for: id }, text),
    h('input', { id, name: id, type: typ, required: true, ...weitere }),
  ];

  const formular = h('form', {
    onsubmit: async (ereignis) => {
      ereignis.preventDefault();
      const daten = Object.fromEntries(new FormData(ereignis.target));
      const knopf = ereignis.target.querySelector('button[type=submit]');
      knopf.disabled = true;
      try {
        zustand.ich = await api('POST', neuesKonto ? '/api/auth/registrieren' : '/api/auth/anmelden', { json: daten });
        familie();
      } catch (e) {
        knopf.disabled = false;
        meldung.replaceChildren(fehlerbox(e.message));
      }
    },
  },
    neuesKonto && feld('anzeigename', 'Ihr Name', 'text', { maxlength: '80', autocomplete: 'name' }),
    feld('email', 'E-Mail-Adresse', 'email', { maxlength: '254', autocomplete: 'email' }),
    feld('passwort', neuesKonto ? 'Passwort (mindestens 10 Zeichen)' : 'Passwort', 'password', {
      minlength: neuesKonto ? '10' : null, maxlength: '72',
      autocomplete: neuesKonto ? 'new-password' : 'current-password',
    }),
    meldung,
    h('button', { type: 'submit' }, neuesKonto ? 'Konto anlegen' : 'Anmelden'),
  );

  zeige(neuesKonto ? 'Konto für Angehörige anlegen' : 'Für Angehörige', [
    h('p', {}, 'Hier verbinden Sie sich mit Ihren Eltern, sehen ihre Hilfeanfragen und schreiben eigene Anleitungen.'),
    istSeniorGeraet && h('p', { class: 'hinweisbox' },
      'Dieses Gerät wird gerade von einem Elternteil benutzt. Am besten melden Sie sich auf Ihrem eigenen Gerät an.'),
    formular,
    h('button', { class: 'knopf-zweit', type: 'button', onclick: () => zeigeAnmeldung(istSeniorGeraet, !neuesKonto) },
      neuesKonto ? 'Ich habe schon ein Konto' : 'Neues Konto anlegen'),
  ]);
}

function zeigeFamilieAnlegen(ich) {
  const meldung = h('div', {});
  zeige('Familie anlegen', [
    h('p', {}, `Hallo ${ich.anzeigename}. Legen Sie zuerst Ihre Familie an. Danach können Sie Ihre Eltern einladen.`),
    h('form', {
      onsubmit: async (ereignis) => {
        ereignis.preventDefault();
        try {
          await api('POST', '/api/familien', { json: { name: new FormData(ereignis.target).get('name') } });
          familie();
        } catch (e) {
          meldung.replaceChildren(fehlerbox(e.message));
        }
      },
    },
      h('label', { for: 'name' }, 'Name der Familie'),
      h('input', { id: 'name', name: 'name', type: 'text', required: true, maxlength: '80', placeholder: 'zum Beispiel: Familie Müller' }),
      meldung,
      h('button', { type: 'submit' }, 'Familie anlegen'),
    ),
    kontoBereich(),
  ]);
}

// ---------- Übersicht ----------

async function zeigeUebersicht(ich, familien) {
  const bereiche = [];
  for (const kurz of familien) {
    try {
      const [details, anfragen, anleitungen] = await Promise.all([
        api('GET', `/api/familien/${kurz.id}`),
        api('GET', `/api/familien/${kurz.id}/hilfeanfragen?status=OFFEN`),
        api('GET', `/api/familien/${kurz.id}/anleitungen`),
      ]);
      bereiche.push(familienBereich(details, anfragen, anleitungen));
    } catch (e) {
      bereiche.push(fehlerbox(e.message));
    }
  }
  zeige('Für Angehörige', [
    h('p', {}, `Angemeldet als ${ich.anzeigename}.`),
    ...bereiche,
    benachrichtigungsBereich(),
    kontoBereich(),
  ]);
}

function familienBereich(details, anfragen, anleitungen) {
  const verwalter = details.meineRolle === 'VERWALTER';

  // --- Offene Hilfeanfragen ---
  const anfragenListe = anfragen.length === 0
    ? h('p', {}, 'Im Moment gibt es keine offenen Anfragen.')
    : anfragen.map((anfrage) => h('div', { class: 'eintrag' },
      h('p', {}, h('strong', {}, anfrage.vonName), ': ', anfrage.themaText),
      h('p', { class: 'leise' }, datum(anfrage.erstelltAm)),
      anfrage.geteilterText && h('p', { class: 'zitat' }, anfrage.geteilterText),
      h('button', {
        class: 'knopf-klein', type: 'button',
        onclick: async (ereignis) => {
          ereignis.target.disabled = true;
          try {
            await api('PATCH', `/api/hilfeanfragen/${anfrage.id}`, { json: { status: 'ERLEDIGT' } });
            familie();
          } catch (e) {
            ereignis.target.replaceWith(fehlerbox(e.message));
          }
        },
      }, 'Als erledigt markieren')));

  // --- Mitglieder und Geräte ---
  const mitglieder = details.mitglieder.map((mitglied) => h('div', { class: 'eintrag' },
    h('p', {}, h('strong', {}, mitglied.anzeigename), ' · ', ROLLEN[mitglied.rolle]),
    mitglied.geraete.map((geraet) => geraeteZeile(geraet, verwalter))));

  // --- Einladung ---
  const einladung = h('div', {});
  const einladungsFormular = verwalter && h('form', {
    onsubmit: async (ereignis) => {
      ereignis.preventDefault();
      try {
        const neu = await api('POST', `/api/familien/${details.id}/einladungen`, {
          json: { nameFuerSenior: new FormData(ereignis.target).get('nameFuerSenior') },
        });
        einladung.replaceChildren(
          h('p', {}, 'So geht es auf dem Handy Ihrer Mutter oder Ihres Vaters:'),
          h('ol', {},
            h('li', {}, 'Mit der Handy-Kamera diesen QR-Code scannen und den Link öffnen.'),
            h('li', {}, 'Oder in der App auf „Familie fragen“ tippen und diesen Code eintippen:')),
          h('img', { class: 'qr', alt: 'QR-Code für die Einladung', src: 'data:image/svg+xml,' + encodeURIComponent(neu.qrSvg) }),
          h('p', { class: 'einladungscode' }, neu.code),
          h('p', { class: 'leise' }, `Der Code gilt einmal und nur bis ${datum(neu.gueltigBis)} (15 Minuten).`));
      } catch (e) {
        einladung.replaceChildren(fehlerbox(e.message));
      }
    },
  },
    h('label', { for: 'name-' + details.id }, 'Wie soll die Person heißen?'),
    h('input', { id: 'name-' + details.id, name: 'nameFuerSenior', type: 'text', maxlength: '80', placeholder: 'zum Beispiel: Mama' }),
    h('button', { type: 'submit' }, 'Einladung mit QR-Code erzeugen'));

  // --- Eigene Anleitungen ---
  const anleitungsListe = anleitungen.length === 0
    ? h('p', {}, 'Noch keine eigenen Anleitungen.')
    : anleitungen.map((anleitung) => h('div', { class: 'eintrag' },
      h('p', {}, h('strong', {}, anleitung.titel), ' · ', PLATTFORMEN[anleitung.plattform],
        ` · ${anleitung.schritte.length} Schritte`),
      h('a', { class: 'knopf knopf-zweit knopf-klein', href: `#/familie/anleitung/${details.id}/${anleitung.id}` },
        'Bearbeiten')));

  return h('section', {},
    h('h2', {}, details.name),
    h('div', { class: 'karte' }, h('h3', {}, 'Offene Hilfeanfragen'), anfragenListe),
    h('div', { class: 'karte' }, h('h3', {}, 'Mitglieder'), mitglieder),
    verwalter && h('div', { class: 'karte' }, h('h3', {}, 'Eltern einladen'), einladungsFormular, einladung),
    h('div', { class: 'karte' },
      h('h3', {}, 'Eigene Anleitungen'),
      h('p', { class: 'leise' }, 'Zum Beispiel: „So rufst du mich per WhatsApp an“. Nur Ihre Familie sieht sie.'),
      anleitungsListe,
      h('a', { class: 'knopf knopf-zweit', href: `#/familie/anleitung/${details.id}` }, 'Neue Anleitung schreiben')));
}

function geraeteZeile(geraet, verwalter) {
  const zeile = h('div', {});
  const normal = () => ersetze(zeile,
    h('p', { class: 'leise' }, `${PLATTFORMEN[geraet.plattform]} · zuletzt benutzt: ${datum(geraet.zuletztGesehen)}`,
      geraet.widerrufen ? ' · gesperrt' : ''),
    verwalter && !geraet.widerrufen && h('button', { class: 'knopf-gefahr knopf-klein', type: 'button', onclick: nachfragen },
      'Handy verloren? Gerät sperren'));
  const nachfragen = () => zeile.replaceChildren(
    h('p', { class: 'fehlerbox' }, 'Dieses Gerät wirklich sperren? Es kann danach nicht mehr auf die Familie zugreifen.'),
    h('button', {
      class: 'knopf-gefahr knopf-klein', type: 'button',
      onclick: async () => {
        try {
          await api('DELETE', `/api/geraete/${geraet.id}`);
          familie();
        } catch (e) {
          zeile.replaceChildren(fehlerbox(e.message));
        }
      },
    }, 'Ja, sperren'),
    h('button', { class: 'knopf-klein', type: 'button', onclick: normal }, 'Nein'));
  normal();
  return zeile;
}

// ---------- Benachrichtigungen (Web Push) ----------

function base64UrlZuBytes(text) {
  const base64 = (text + '='.repeat((4 - (text.length % 4)) % 4)).replace(/-/g, '+').replace(/_/g, '/');
  return Uint8Array.from(atob(base64), (zeichen) => zeichen.charCodeAt(0));
}

function benachrichtigungsBereich() {
  const schluessel = zustand.einstellungen.pushSchluessel;
  const moeglich = schluessel && 'serviceWorker' in navigator && 'PushManager' in window;
  const meldung = h('div', {});
  return h('div', { class: 'karte' },
    h('h3', {}, 'Benachrichtigungen'),
    !moeglich && h('p', {}, schluessel
      ? 'Dieser Browser kann keine Benachrichtigungen empfangen. Auf dem iPhone geht es, wenn Sie die App zum Home-Bildschirm hinzufügen.'
      : 'Benachrichtigungen sind auf diesem Server noch nicht eingerichtet. Neue Anfragen sehen Sie hier in der App.'),
    moeglich && h('p', {}, 'Lassen Sie sich benachrichtigen, wenn Ihre Eltern um Hilfe bitten.'),
    moeglich && h('button', {
      class: 'knopf-zweit', type: 'button',
      onclick: async () => {
        try {
          if (await Notification.requestPermission() !== 'granted') {
            meldung.replaceChildren(h('p', { class: 'hinweisbox' },
              'Sie haben Benachrichtigungen nicht erlaubt. Sie können das in den Einstellungen Ihres Browsers ändern.'));
            return;
          }
          const dienst = await navigator.serviceWorker.ready;
          const abo = await dienst.pushManager.subscribe({
            userVisibleOnly: true, applicationServerKey: base64UrlZuBytes(schluessel),
          });
          const daten = abo.toJSON();
          await api('POST', '/api/push-abos', {
            json: { endpoint: daten.endpoint, p256dh: daten.keys.p256dh, auth: daten.keys.auth },
          });
          meldung.replaceChildren(h('p', { class: 'hinweisbox' }, 'Benachrichtigungen sind auf diesem Gerät eingeschaltet.'));
        } catch (e) {
          meldung.replaceChildren(fehlerbox('Das Einschalten hat nicht geklappt. ' + (e.status ? e.message : '')));
        }
      },
    }, 'Auf diesem Gerät einschalten'),
    meldung);
}

function kontoBereich() {
  return h('div', { class: 'karte' },
    h('h3', {}, 'Mein Konto'),
    h('button', {
      class: 'knopf-zweit', type: 'button',
      onclick: async () => {
        try {
          await api('POST', '/api/auth/abmelden');
        } finally {
          zustand.ich = undefined;
          location.hash = '#/';
        }
      },
    }, 'Abmelden'),
    h('a', { class: 'knopf knopf-zweit', href: '#/meine-daten' }, 'Meine Daten ansehen oder löschen'));
}

// ---------- Anleitungs-Editor ----------

export async function anleitungBearbeiten(familieId, anleitungId) {
  zeigeLaden();
  let vorhandene = null;
  try {
    if (anleitungId) {
      const alle = await api('GET', `/api/familien/${familieId}/anleitungen`);
      vorhandene = alle.find((eintrag) => eintrag.id === anleitungId) || null;
    }
  } catch (e) {
    zeige('Anleitung', fehlerbox(e.message), { zurueck: '#/familie' });
    return;
  }

  const meldung = h('div', {});
  const schrittListe = h('div', {});
  const schrittFeld = (text = '') => {
    const eingabe = h('textarea', { maxlength: '600', 'aria-label': 'Text des Schritts' });
    eingabe.value = text;
    const block = h('div', { class: 'eintrag' },
      h('p', { class: 'schritt-zaehler' }),
      eingabe,
      h('button', {
        class: 'knopf-gefahr knopf-klein', type: 'button',
        onclick: () => {
          block.remove();
          nummeriere();
        },
      }, 'Diesen Schritt entfernen'));
    return block;
  };
  const nummeriere = () => [...schrittListe.children].forEach((block, stelle) => {
    block.querySelector('.schritt-zaehler').textContent = `Schritt ${stelle + 1}`;
  });
  (vorhandene ? vorhandene.schritte.map((schritt) => schritt.text) : ['']).forEach(
    (text) => schrittListe.append(schrittFeld(text)));
  nummeriere();

  const titel = h('input', { id: 'titel', type: 'text', required: true, maxlength: '120', placeholder: 'So rufst du mich per WhatsApp an' });
  titel.value = vorhandene ? vorhandene.titel : '';
  const plattform = h('select', { id: 'plattform' },
    Object.entries({ ALLE: 'Alle Handys', ANDROID: 'Android', IOS: 'iPhone' }).map(([wert, text]) =>
      h('option', { value: wert, selected: vorhandene && vorhandene.plattform === wert }, text)));

  const loeschbereich = h('div', {});
  const zeigeLoeschen = () => ersetze(loeschbereich, vorhandene && h('button', {
    class: 'knopf-gefahr', type: 'button',
    onclick: () => loeschbereich.replaceChildren(
      h('p', { class: 'fehlerbox' }, 'Diese Anleitung wirklich löschen? Das kann man nicht rückgängig machen.'),
      h('button', {
        class: 'knopf-gefahr', type: 'button',
        onclick: async () => {
          try {
            await api('DELETE', `/api/familien/${familieId}/anleitungen/${anleitungId}`);
            location.hash = '#/familie';
          } catch (e) {
            loeschbereich.replaceChildren(fehlerbox(e.message));
          }
        },
      }, 'Ja, löschen'),
      h('button', { type: 'button', onclick: zeigeLoeschen }, 'Nein, behalten')),
  }, 'Anleitung löschen'));
  zeigeLoeschen();

  zeige(vorhandene ? 'Anleitung bearbeiten' : 'Neue Anleitung', [
    h('p', {}, 'Schreiben Sie kurze Sätze. Pro Schritt nur eine Sache. Ihre Eltern sehen immer nur einen Schritt auf einmal.'),
    h('form', {
      onsubmit: async (ereignis) => {
        ereignis.preventDefault();
        const schritte = [...schrittListe.querySelectorAll('textarea')].map((feld) => feld.value.trim()).filter(Boolean);
        if (schritte.length === 0) {
          meldung.replaceChildren(fehlerbox('Bitte schreiben Sie mindestens einen Schritt.'));
          return;
        }
        const daten = { titel: titel.value, plattform: plattform.value, schritte };
        try {
          if (vorhandene) {
            await api('PUT', `/api/familien/${familieId}/anleitungen/${anleitungId}`, { json: daten });
          } else {
            await api('POST', `/api/familien/${familieId}/anleitungen`, { json: daten });
          }
          location.hash = '#/familie';
        } catch (e) {
          meldung.replaceChildren(fehlerbox(e.message));
        }
      },
    },
      h('label', { for: 'titel' }, 'Titel'),
      titel,
      h('label', { for: 'plattform' }, 'Für welches Handy?'),
      plattform,
      schrittListe,
      h('button', {
        class: 'knopf-zweit', type: 'button',
        onclick: () => {
          schrittListe.append(schrittFeld());
          nummeriere();
        },
      }, 'Schritt hinzufügen'),
      meldung,
      h('button', { type: 'submit' }, 'Speichern'),
    ),
    loeschbereich,
  ], { zurueck: '#/familie' });
}
