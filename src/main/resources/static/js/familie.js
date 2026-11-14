// Die Bildschirme für Angehörige: Anmelden, Familie, QR-Einladung, Hilfeanfragen, Anleitungs-Editor.

import { ansicht, api, datum, ersetze, fehlerbox, h, ladeIch, zeige, zeigeLaden, zustand } from './basis.js';
import { sprache, t } from './sprache.js';

export async function familie() {
  const lauf = ansicht.nummer();
  zeigeLaden();
  const ich = await ladeIch(true);
  if (!ansicht.gilt(lauf)) return;
  if (!ich || ich.typ !== 'ANGEHOERIGER') {
    zeigeAnmeldung(Boolean(ich));
    return;
  }
  let familien;
  try {
    familien = await api('GET', '/api/familien');
    if (!ansicht.gilt(lauf)) return;
  } catch (e) {
    zeige(t('fam.titel'), fehlerbox(e.message));
    return;
  }
  if (familien.length === 0) {
    zeigeFamilieAnlegen(ich);
    return;
  }
  await zeigeUebersicht(ich, familien, lauf);
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
        // Die gewählte Sprache am Konto merken, damit Benachrichtigungen in dieser Sprache kommen
        api('PUT', '/api/ich/sprache', { json: { sprache: sprache() } }).catch(() => {});
        familie();
      } catch (e) {
        knopf.disabled = false;
        meldung.replaceChildren(fehlerbox(e.message));
      }
    },
  },
    neuesKonto && feld('anzeigename', t('fam.name'), 'text', { maxlength: '80', autocomplete: 'name' }),
    feld('email', t('fam.email'), 'email', { maxlength: '254', autocomplete: 'email' }),
    feld('passwort', neuesKonto ? t('fam.passwortNeu') : t('fam.passwort'), 'password', {
      minlength: neuesKonto ? '10' : null, maxlength: '72',
      autocomplete: neuesKonto ? 'new-password' : 'current-password',
    }),
    meldung,
    h('button', { type: 'submit' }, neuesKonto ? t('fam.kontoAnlegen') : t('fam.anmelden')),
  );

  zeige(neuesKonto ? t('fam.kontoTitel') : t('fam.titel'), [
    h('p', {}, t('fam.einleitung')),
    istSeniorGeraet && h('p', { class: 'hinweisbox' }, t('fam.seniorGeraet')),
    formular,
    h('button', { class: 'knopf-zweit', type: 'button', onclick: () => zeigeAnmeldung(istSeniorGeraet, !neuesKonto) },
      neuesKonto ? t('fam.habeKonto') : t('fam.neuesKonto')),
  ]);
}

function zeigeFamilieAnlegen(ich) {
  const meldung = h('div', {});
  zeige(t('fam.anlegen.titel'), [
    h('p', {}, t('fam.anlegen.text', { name: ich.anzeigename })),
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
      h('label', { for: 'name' }, t('fam.anlegen.feld')),
      h('input', { id: 'name', name: 'name', type: 'text', required: true, maxlength: '80', placeholder: t('fam.anlegen.beispiel') }),
      meldung,
      h('button', { type: 'submit' }, t('fam.anlegen.knopf')),
    ),
    kontoBereich(),
  ]);
}

// ---------- Übersicht ----------

async function zeigeUebersicht(ich, familien, lauf) {
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
  if (!ansicht.gilt(lauf)) return;
  zeige(t('fam.titel'), [
    h('p', {}, t('fam.angemeldetAls', { name: ich.anzeigename })),
    ...bereiche,
    benachrichtigungsBereich(),
    kontoBereich(),
  ]);
}

function familienBereich(details, anfragen, anleitungen) {
  const verwalter = details.meineRolle === 'VERWALTER';

  // --- Offene Hilfeanfragen ---
  const anfragenListe = anfragen.length === 0
    ? h('p', {}, t('fam.anfragen.keine'))
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
      }, t('fam.anfragen.erledigt'))));

  // --- Mitglieder und Geräte ---
  const mitglieder = details.mitglieder.map((mitglied) => h('div', { class: 'eintrag' },
    h('p', {}, h('strong', {}, mitglied.anzeigename), ' · ', t('rolle.' + mitglied.rolle)),
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
          h('p', {}, t('fam.einladen.so')),
          h('ol', {},
            h('li', {}, t('fam.einladen.schritt1')),
            h('li', {}, t('fam.einladen.schritt2'))),
          h('img', { class: 'qr', alt: t('fam.einladen.qrAlt'), src: 'data:image/svg+xml,' + encodeURIComponent(neu.qrSvg) }),
          h('p', { class: 'einladungscode' }, neu.code),
          h('p', { class: 'leise' }, t('fam.einladen.gilt', { datum: datum(neu.gueltigBis) })));
      } catch (e) {
        einladung.replaceChildren(fehlerbox(e.message));
      }
    },
  },
    h('label', { for: 'name-' + details.id }, t('fam.einladen.feld')),
    h('input', { id: 'name-' + details.id, name: 'nameFuerSenior', type: 'text', maxlength: '80', placeholder: t('fam.einladen.beispiel') }),
    h('button', { type: 'submit' }, t('fam.einladen.knopf')));

  // --- Eigene Anleitungen ---
  const anleitungsListe = anleitungen.length === 0
    ? h('p', {}, t('fam.anl.keine'))
    : anleitungen.map((anleitung) => h('div', { class: 'eintrag' },
      h('p', {}, h('strong', {}, anleitung.titel), ' · ', t('plattform.' + anleitung.plattform),
        ' · ', t('fam.anl.schritte', { anzahl: anleitung.schritte.length })),
      h('a', { class: 'knopf knopf-zweit knopf-klein', href: `#/familie/anleitung/${details.id}/${anleitung.id}` },
        t('fam.anl.bearbeiten'))));

  return h('section', {},
    h('h2', {}, details.name),
    h('div', { class: 'karte' }, h('h3', {}, t('fam.anfragen.titel')), anfragenListe),
    h('div', { class: 'karte' }, h('h3', {}, t('fam.mitglieder')), mitglieder),
    verwalter && h('div', { class: 'karte' }, h('h3', {}, t('fam.einladen.titel')), einladungsFormular, einladung),
    h('div', { class: 'karte' },
      h('h3', {}, t('fam.anl.titel')),
      h('p', { class: 'leise' }, t('fam.anl.erklaerung')),
      anleitungsListe,
      h('a', { class: 'knopf knopf-zweit', href: `#/familie/anleitung/${details.id}` }, t('fam.anl.neu'))));
}

function geraeteZeile(geraet, verwalter) {
  const zeile = h('div', {});
  const normal = () => ersetze(zeile,
    h('p', { class: 'leise' },
      t('fam.geraet.zeile', { plattform: t('plattform.' + geraet.plattform), datum: datum(geraet.zuletztGesehen) }),
      geraet.widerrufen ? ' · ' + t('fam.geraet.gesperrt') : ''),
    verwalter && !geraet.widerrufen && h('button', { class: 'knopf-gefahr knopf-klein', type: 'button', onclick: nachfragen },
      t('fam.geraet.sperren')));
  const nachfragen = () => zeile.replaceChildren(
    h('p', { class: 'fehlerbox' }, t('fam.geraet.wirklich')),
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
    }, t('fam.geraet.ja')),
    h('button', { class: 'knopf-klein', type: 'button', onclick: normal }, t('allgemein.nein')));
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
    h('h3', {}, t('fam.push.titel')),
    !moeglich && h('p', {}, schluessel ? t('fam.push.nichtMoeglich') : t('fam.push.nichtEingerichtet')),
    moeglich && h('p', {}, t('fam.push.text')),
    moeglich && h('button', {
      class: 'knopf-zweit', type: 'button',
      onclick: async () => {
        try {
          if (await Notification.requestPermission() !== 'granted') {
            meldung.replaceChildren(h('p', { class: 'hinweisbox' }, t('fam.push.abgelehnt')));
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
          meldung.replaceChildren(h('p', { class: 'hinweisbox' }, t('fam.push.an')));
        } catch (e) {
          meldung.replaceChildren(fehlerbox(t('fam.push.fehler') + (e.status ? ' ' + e.message : '')));
        }
      },
    }, t('fam.push.knopf')),
    meldung);
}

function kontoBereich() {
  return h('div', { class: 'karte' },
    h('h3', {}, t('fam.konto.titel')),
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
    }, t('fam.konto.abmelden')),
    h('a', { class: 'knopf knopf-zweit', href: '#/meine-daten' }, t('fam.konto.daten')));
}

// ---------- Anleitungs-Editor ----------

export async function anleitungBearbeiten(familieId, anleitungId) {
  const lauf = ansicht.nummer();
  zeigeLaden();
  let vorhandene = null;
  try {
    if (anleitungId) {
      const alle = await api('GET', `/api/familien/${familieId}/anleitungen`);
      vorhandene = alle.find((eintrag) => eintrag.id === anleitungId) || null;
    }
  } catch (e) {
    zeige(t('anleitung.titel'), fehlerbox(e.message), { zurueck: '#/familie' });
    return;
  }

  if (!ansicht.gilt(lauf)) return;

  const meldung = h('div', {});
  const schrittListe = h('div', {});
  const schrittFeld = (text = '') => {
    const eingabe = h('textarea', { maxlength: '600', 'aria-label': t('editor.schrittText') });
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
      }, t('editor.entfernen')));
    return block;
  };
  const nummeriere = () => [...schrittListe.children].forEach((block, stelle) => {
    block.querySelector('.schritt-zaehler').textContent = t('editor.schritt', { nummer: stelle + 1 });
  });
  (vorhandene ? vorhandene.schritte.map((schritt) => schritt.text) : ['']).forEach(
    (text) => schrittListe.append(schrittFeld(text)));
  nummeriere();

  const titel = h('input', { id: 'titel', type: 'text', required: true, maxlength: '120', placeholder: t('editor.beispiel') });
  titel.value = vorhandene ? vorhandene.titel : '';
  const plattform = h('select', { id: 'plattform' },
    ['ALLE', 'ANDROID', 'IOS'].map((wert) =>
      h('option', { value: wert, selected: vorhandene && vorhandene.plattform === wert }, t('plattform.' + wert))));

  const loeschbereich = h('div', {});
  const zeigeLoeschen = () => ersetze(loeschbereich, vorhandene && h('button', {
    class: 'knopf-gefahr', type: 'button',
    onclick: () => loeschbereich.replaceChildren(
      h('p', { class: 'fehlerbox' }, t('editor.wirklich')),
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
      }, t('editor.jaLoeschen')),
      h('button', { type: 'button', onclick: zeigeLoeschen }, t('editor.neinBehalten'))),
  }, t('editor.loeschen')));
  zeigeLoeschen();

  zeige(vorhandene ? t('editor.titelBearbeiten') : t('editor.titelNeu'), [
    h('p', {}, t('editor.hinweis')),
    h('form', {
      onsubmit: async (ereignis) => {
        ereignis.preventDefault();
        const schritte = [...schrittListe.querySelectorAll('textarea')].map((feld) => feld.value.trim()).filter(Boolean);
        if (schritte.length === 0) {
          meldung.replaceChildren(fehlerbox(t('editor.mindestens')));
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
      h('label', { for: 'titel' }, t('editor.feldTitel')),
      titel,
      h('label', { for: 'plattform' }, t('editor.feldHandy')),
      plattform,
      schrittListe,
      h('button', {
        class: 'knopf-zweit', type: 'button',
        onclick: () => {
          schrittListe.append(schrittFeld());
          nummeriere();
        },
      }, t('editor.hinzufuegen')),
      meldung,
      h('button', { type: 'submit' }, t('editor.speichern')),
    ),
    loeschbereich,
  ], { zurueck: '#/familie' });
}
