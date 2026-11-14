// Die Bildschirme für Senioren: pro Bildschirm eine Aufgabe, große Schaltflächen, einfache Sprache.

import {
  ansicht, api, erkanntePlattform, fehlerbox, h, ladeIch, merkzettel, stelleGeraetSicher, zeige, zeigeLaden, zustand,
} from './basis.js';
import { t } from './sprache.js';

// ---------- Hauptseite: Was ist HalloKlick? ----------

function symbol(name) {
  return h('img', { class: 'symbol', src: `/bilder/${name}.svg`, alt: '', width: '72', height: '72' });
}

export function ueber() {
  const punkt = (bild, titel, text) => h('div', { class: 'karte karte-mit-bild' },
    symbol(bild),
    h('div', {}, h('h3', {}, titel), h('p', {}, text)));

  zeige(t('ueber.titel'), [
    h('p', { class: 'gross' }, t('ueber.einleitung')),
    h('h2', {}, t('ueber.was')),
    punkt('schloss', t('ueber.echt.titel'), t('ueber.echt.text')),
    punkt('finger', t('ueber.wie.titel'), t('ueber.wie.text')),
    punkt('person', t('ueber.familie.titel'), t('ueber.familie.text')),
    h('h2', {}, t('ueber.wichtig')),
    h('ul', { class: 'merkmale' },
      h('li', {}, t('ueber.punkt1')),
      h('li', {}, t('ueber.punkt2')),
      h('li', {}, t('ueber.punkt3')),
      h('li', {}, t('ueber.punkt4'))),
    h('button', {
      type: 'button',
      onclick: () => {
        merkzettel.schreibe('willkommen', 'gesehen');
        if (location.hash === '' || location.hash === '#/') start();
        else location.hash = '#/';
      },
    }, t('ueber.los')),
    zustand.einstellungen.familie && h('div', { class: 'karte' },
      h('h3', {}, t('ueber.angehoerige.titel')),
      h('p', {}, t('ueber.angehoerige.text')),
      h('a', { class: 'knopf knopf-zweit', href: '#/familie' }, t('start.angehoerigeLink'))),
  ], { zurueck: null });
}

// ---------- Start: drei große Schaltflächen ----------

export function start() {
  // Beim allerersten Besuch erklärt die Hauptseite, was HalloKlick ist
  if (merkzettel.lies('willkommen') !== 'gesehen') {
    ueber();
    return;
  }
  const an = zustand.einstellungen;
  zeige(t('start.titel'), [
    an.sicherheitscheck && h('a', { class: 'knopf start-knopf', href: '#/pruefen' },
      t('start.echt'), h('small', {}, t('start.echt.unter'))),
    an.anleitungen && h('a', { class: 'knopf start-knopf', href: '#/anleitungen' },
      t('start.wie'), h('small', {}, t('start.wie.unter'))),
    an.familie && h('a', { class: 'knopf start-knopf', href: '#/hilfe' },
      t('start.familie'), h('small', {}, t('start.familie.unter'))),
    h('p', { class: 'leise' }, t('start.kostenlos')),
    h('a', { class: 'knopf knopf-zweit', href: '#/ueber' }, t('fuss.ueber')),
    an.familie && h('p', { class: 'leise' },
      t('start.angehoerigeFrage'), ' ', h('a', { href: '#/familie' }, t('start.angehoerigeLink'))),
  ], { zurueck: null });
}

// ---------- "Ist das echt?" ----------

export function pruefen(vorbelegung = '', fehler = null) {
  const text = h('textarea', { id: 'nachricht', maxlength: '5000', autocomplete: 'off' });
  text.value = typeof vorbelegung === 'string' ? vorbelegung : '';
  const meldung = h('div', {}, fehler ? fehlerbox(fehler) : null);
  const dateiwahl = h('input', {
    class: 'dateiwahl', type: 'file', accept: 'image/*', 'aria-hidden': 'true', tabindex: '-1',
    onchange: () => dateiwahl.files[0] && pruefeFoto(dateiwahl.files[0], meldung),
  });

  const einfuegen = navigator.clipboard && navigator.clipboard.readText && h('button', {
    class: 'knopf-zweit', type: 'button',
    onclick: async () => {
      try {
        text.value = await navigator.clipboard.readText();
      } catch (e) {
        meldung.replaceChildren(h('p', { class: 'hinweisbox' }, t('pruefen.einfuegenFehler')));
      }
    },
  }, t('pruefen.einfuegen'));

  zeige(t('pruefen.titel'), [
    h('p', {}, t('pruefen.einleitung')),
    h('form', {
      onsubmit: (ereignis) => {
        ereignis.preventDefault();
        if (!text.value.trim()) {
          meldung.replaceChildren(fehlerbox(t('pruefen.leer')));
          return;
        }
        const formular = new FormData();
        formular.append('text', text.value);
        schickeZurPruefung(formular);
      },
    },
      h('label', { for: 'nachricht' }, t('pruefen.feld')),
      text,
      einfuegen,
      meldung,
      h('button', { type: 'submit' }, t('pruefen.knopf')),
    ),
    zustand.einstellungen.fotoPruefung && h('div', {},
      h('p', {}, t('pruefen.fotoFrage')),
      dateiwahl,
      h('button', { class: 'knopf-zweit', type: 'button', onclick: () => dateiwahl.click() },
        t('pruefen.fotoKnopf')),
    ),
    h('p', { class: 'leise' }, t('pruefen.nichtGespeichert')),
  ]);
}

/**
 * Fotos werden im Handy verkleinert und als JPEG neu gezeichnet. Dabei fallen Ort und
 * Gerätedaten weg, und auch iPhone-Fotos (HEIC) werden lesbar.
 */
async function pruefeFoto(datei, meldung) {
  try {
    const bild = await createImageBitmap(datei);
    const faktor = Math.min(1, 1568 / Math.max(bild.width, bild.height));
    const flaeche = document.createElement('canvas');
    flaeche.width = Math.max(1, Math.round(bild.width * faktor));
    flaeche.height = Math.max(1, Math.round(bild.height * faktor));
    flaeche.getContext('2d').drawImage(bild, 0, 0, flaeche.width, flaeche.height);
    const jpeg = await new Promise((fertig) => flaeche.toBlob(fertig, 'image/jpeg', 0.85));
    if (!jpeg) throw new Error('kein Bild');
    const formular = new FormData();
    formular.append('bild', jpeg, 'foto.jpg');
    await schickeZurPruefung(formular);
  } catch (e) {
    meldung.replaceChildren(fehlerbox(t('pruefen.fotoFehler')));
  }
}

async function schickeZurPruefung(formular) {
  zeigeLaden(t('pruefen.laeuft'));
  try {
    await stelleGeraetSicher();
    zeigeErgebnis(await api('POST', '/api/checks', { formular }));
  } catch (e) {
    pruefen(formular.get('text') || '', e.message);
  }
}

function zeigeErgebnis(ergebnis) {
  const rot = ergebnis.stufe === 'ROT';
  const feedback = h('div', { class: 'karte' });
  const gibFeedback = async (hilfreich) => {
    try {
      await api('POST', `/api/checks/${ergebnis.id}/feedback`, { json: { hilfreich } });
    } catch (e) {
      // Feedback ist freiwillig: Ein Fehler hier soll niemanden aufhalten
    }
    feedback.replaceChildren(h('p', {}, t('ergebnis.danke')));
  };
  feedback.replaceChildren(
    h('p', {}, h('strong', {}, t('ergebnis.hilfreich'))),
    h('div', { class: 'reihe' },
      h('button', { class: 'knopf-zweit', type: 'button', onclick: () => gibFeedback(true) }, t('allgemein.ja')),
      h('button', { class: 'knopf-zweit', type: 'button', onclick: () => gibFeedback(false) }, t('allgemein.nein'))),
  );

  // Die Farbe ist nie das einzige Zeichen: Die Stufe steht immer auch als Wort da.
  const marke = !ergebnis.geprueft
    ? t('ergebnis.nichtGeprueft')
    : (rot ? t('ergebnis.vorsicht') : t('ergebnis.keineWarnzeichen'));

  zeige(null, [
    h('section', { class: 'ergebnis ' + (rot ? 'ergebnis-rot' : 'ergebnis-orange') },
      h('span', { class: 'stufe-marke' }, marke),
      h('h1', {}, ergebnis.titel),
      h('p', {}, ergebnis.erklaerung),
      ergebnis.merkmale.length > 0 && h('div', {},
        h('p', {}, h('strong', {}, t('ergebnis.aufgefallen'))),
        h('ul', { class: 'merkmale' }, ergebnis.merkmale.map((merkmal) => h('li', {}, merkmal)))),
      h('p', {}, h('strong', {}, t('ergebnis.tun'))),
      h('p', {}, ergebnis.naechsterSchritt),
      ergebnis.zusatzHinweis && h('p', {}, ergebnis.zusatzHinweis),
    ),
    h('p', { class: 'hinweisbox' }, ergebnis.hinweis),
    zustand.einstellungen.familie && h('a', { class: 'knopf', href: '#/hilfe' }, t('ergebnis.familieFragen')),
    h('button', { class: 'knopf-zweit', type: 'button', onclick: () => pruefen() }, t('ergebnis.nochEine')),
    ergebnis.geprueft && feedback,
  ], { zurueck: '#/', zurueckText: t('allgemein.zumAnfang') });
}

// ---------- "Wie mache ich das?" ----------

function meinePlattform() {
  const gemerkt = merkzettel.lies('plattform');
  if (gemerkt === 'ANDROID' || gemerkt === 'IOS') return gemerkt;
  const erkannt = erkanntePlattform();
  return erkannt === 'SONSTIGE' ? null : erkannt;
}

/** Nur einfache Namen aus unserem Bilder-Ordner sind erlaubt, nie eine fremde Adresse. */
function schrittBild(bildPfad, klasse) {
  if (!bildPfad || !/^[a-z0-9-]+$/.test(bildPfad)) return null;
  // alt bleibt leer: Der Text des Schritts sagt alles, das Bild ist nur eine Stütze für das Auge
  return h('img', { class: klasse, src: `/bilder/${bildPfad}.svg`, alt: '' });
}

export async function anleitungen(suchwort = '') {
  const lauf = ansicht.nummer();
  const plattform = meinePlattform();
  const feld = h('input', { id: 'suche', type: 'search', value: suchwort, placeholder: t('anleitungen.beispiel') });
  const ergebnisse = h('div', {}, h('p', { class: 'laden', role: 'status' }, t('allgemein.laden')));

  const waehlePlattform = (neu) => {
    merkzettel.schreibe('plattform', neu);
    anleitungen(feld.value);
  };
  const plattformKnopf = (wert) => h('button', {
    class: plattform === wert ? '' : 'knopf-zweit', type: 'button', 'aria-pressed': String(plattform === wert),
    onclick: () => waehlePlattform(wert),
  }, t('plattform.' + wert));

  zeige(t('anleitungen.titel'), [
    h('form', {
      role: 'search',
      onsubmit: (ereignis) => {
        ereignis.preventDefault();
        anleitungen(feld.value);
      },
    },
      h('label', { for: 'suche' }, t('anleitungen.frage')),
      feld,
      h('button', { type: 'submit' }, t('anleitungen.suchen')),
    ),
    h('p', {}, h('strong', {}, t('anleitungen.ihrHandy')), ' ',
      plattform ? t('plattform.' + plattform) : t('anleitungen.nichtGewaehlt')),
    h('div', { class: 'reihe' }, plattformKnopf('ANDROID'), plattformKnopf('IOS')),
    h('h2', {}, suchwort.trim() ? t('anleitungen.gefunden') : t('anleitungen.alle')),
    ergebnisse,
  ]);

  try {
    const abfrage = new URLSearchParams();
    if (suchwort.trim()) abfrage.set('suche', suchwort.trim());
    if (plattform) abfrage.set('plattform', plattform);
    const gefunden = await api('GET', '/api/anleitungen?' + abfrage);
    if (!ansicht.gilt(lauf)) return;
    if (gefunden.length === 0) {
      ergebnisse.replaceChildren(h('p', { class: 'hinweisbox' }, t('anleitungen.keine')));
      return;
    }
    ergebnisse.replaceChildren(h('ul', { class: 'liste' }, gefunden.map((anleitung) => h('li', {},
      h('a', { class: 'knopf knopf-zweit knopf-mit-bild', href: `#/anleitung/${anleitung.id}/1` },
        schrittBild(anleitung.bildPfad, 'listen-bild'),
        h('span', {},
          anleitung.titel,
          h('small', {}, (anleitung.quelle === 'FAMILIE' ? t('anleitungen.vonFamilie') + ' · ' : '')
            + t('plattform.' + anleitung.plattform))))))));
  } catch (e) {
    ergebnisse.replaceChildren(fehlerbox(e.message));
  }
}

const geladeneAnleitungen = new Map();

/** Ein Schritt pro Bildschirm, mit sichtbarem "Weiter" und "Zurück". Keine Wischgesten. */
export async function anleitung(id, schrittNummer) {
  const lauf = ansicht.nummer();
  const zurSuche = { zurueck: '#/anleitungen', zurueckText: t('anleitung.zurSuche') };
  let daten = geladeneAnleitungen.get(id);
  if (!daten) {
    zeigeLaden();
    try {
      daten = await api('GET', '/api/anleitungen/' + encodeURIComponent(id));
      geladeneAnleitungen.set(id, daten);
      if (!ansicht.gilt(lauf)) return;
    } catch (e) {
      zeige(t('anleitung.titel'), fehlerbox(e.message), zurSuche);
      return;
    }
  }
  const anzahl = daten.schritte.length;
  const nummer = Math.min(Math.max(parseInt(schrittNummer, 10) || 1, 1), anzahl);
  const schritt = daten.schritte[nummer - 1];
  const letzter = nummer === anzahl;

  zeige(daten.titel, [
    h('p', { class: 'schritt-zaehler' }, t('anleitung.schritt', { nummer, anzahl })),
    h('div', { class: 'schritt-text' },
      schrittBild(schritt.bildPfad, 'schritt-bild'),
      h('p', {}, schritt.text)),
    letzter
      ? h('a', { class: 'knopf', href: '#/' }, t('allgemein.fertig'))
      : h('a', { class: 'knopf', href: `#/anleitung/${id}/${nummer + 1}` }, t('anleitung.weiter')),
    nummer > 1 && h('a', { class: 'knopf knopf-zweit', href: `#/anleitung/${id}/${nummer - 1}` },
      t('anleitung.zurueck')),
    !daten.geprueftAm && daten.quelle === 'SYSTEM' && h('p', { class: 'leise' }, t('anleitung.beispiel')),
  ], zurSuche);
}

// ---------- "Familie fragen" ----------

const THEMEN = ['KOMISCHE_NACHRICHT', 'HANDY_HILFE', 'BITTE_ANRUFEN', 'ETWAS_ANDERES'];

export async function hilfe() {
  const lauf = ansicht.nummer();
  zeigeLaden();
  let familien = [];
  try {
    const ich = await stelleGeraetSicher();
    if (ich && ich.typ === 'ANGEHOERIGER') {
      zeige(t('hilfe.titel'), h('p', { class: 'hinweisbox' }, t('hilfe.alsAngehoeriger')));
      return;
    }
    familien = await api('GET', '/api/ich/familien');
    if (!ansicht.gilt(lauf)) return;
  } catch (e) {
    zeige(t('hilfe.titel'), fehlerbox(e.message));
    return;
  }

  if (familien.length === 0) {
    zeige(t('hilfe.titel'), [
      h('p', {}, t('hilfe.nichtVerbunden')),
      h('p', {}, t('hilfe.codeErklaerung')),
      h('a', { class: 'knopf', href: '#/verbinden' }, t('hilfe.habeCode')),
    ]);
    return;
  }

  const meldung = h('div', {});
  const textfeld = h('textarea', { id: 'geteilt', maxlength: '2000' });
  const textbereich = h('div', { hidden: true },
    h('label', { for: 'geteilt' }, t('hilfe.mitschickenFeld')), textfeld);
  const teilen = h('input', {
    type: 'checkbox',
    onchange: () => { textbereich.hidden = !teilen.checked; },
  });

  zeige(t('hilfe.titel'), [
    h('p', {}, t('hilfe.gehtAn'), ' ', h('strong', {}, familien.map((f) => f.name).join(', '))),
    h('form', {
      onsubmit: async (ereignis) => {
        ereignis.preventDefault();
        const thema = new FormData(ereignis.target).get('thema');
        if (!thema) {
          meldung.replaceChildren(fehlerbox(t('hilfe.themaFehlt')));
          return;
        }
        const knopf = ereignis.target.querySelector('button[type=submit]');
        knopf.disabled = true;
        try {
          await api('POST', '/api/hilfeanfragen', {
            json: { thema, geteilterText: teilen.checked && textfeld.value.trim() ? textfeld.value : null },
          });
          zeige(t('hilfe.fertig.titel'), [
            h('p', {}, t('hilfe.fertig.text')),
            h('p', { class: 'hinweisbox' }, t('hilfe.fertig.eilig')),
            h('a', { class: 'knopf', href: '#/' }, t('allgemein.fertig')),
          ], { zurueck: null });
        } catch (e) {
          knopf.disabled = false;
          meldung.replaceChildren(fehlerbox(e.message));
        }
      },
    },
      h('fieldset', { class: 'karte' },
        h('legend', {}, h('strong', {}, t('hilfe.worum'))),
        THEMEN.map((wert) => h('label', { class: 'wahl' },
          h('input', { type: 'radio', name: 'thema', value: wert }), t('thema.' + wert)))),
      // Der Senior entscheidet selbst, ob er den Inhalt mitschickt
      h('label', { class: 'wahl' }, teilen, t('hilfe.mitschicken')),
      textbereich,
      meldung,
      h('button', { type: 'submit' }, t('hilfe.senden')),
    ),
  ]);
}

// ---------- Mit der Familie verbinden ----------

export function verbinden(codeAusLink = '') {
  const feld = h('input', {
    id: 'code', class: 'code-eingabe', type: 'text', value: codeAusLink, maxlength: '20',
    autocomplete: 'off', autocapitalize: 'characters', spellcheck: 'false',
  });
  const meldung = h('div', {});

  zeige(t('verbinden.titel'), [
    h('p', {}, t('verbinden.erklaerung')),
    h('form', {
      onsubmit: async (ereignis) => {
        ereignis.preventDefault();
        if (!feld.value.trim()) {
          meldung.replaceChildren(fehlerbox(t('verbinden.leer')));
          return;
        }
        const knopf = ereignis.target.querySelector('button');
        knopf.disabled = true;
        try {
          await stelleGeraetSicher();
          const familie = await api('POST', '/api/geraete/koppeln', { json: { code: feld.value } });
          await ladeIch(true);
          zeige(t('verbinden.ok.titel'), [
            h('p', {}, t('verbinden.ok.text'), ' ', h('strong', {}, familie.name)),
            h('p', {}, t('verbinden.ok.tipp')),
            h('a', { class: 'knopf', href: '#/' }, t('allgemein.zumAnfang')),
          ], { zurueck: null });
        } catch (e) {
          knopf.disabled = false;
          meldung.replaceChildren(fehlerbox(e.status === 403 ? t('verbinden.alsAngehoeriger') : e.message));
        }
      },
    },
      h('label', { for: 'code' }, t('verbinden.feld')),
      feld,
      meldung,
      h('button', { type: 'submit' }, t('verbinden.knopf')),
    ),
  ]);
}

// ---------- Meine Daten (Auskunft und Löschen) ----------

export async function meineDaten() {
  const lauf = ansicht.nummer();
  zeigeLaden();
  const ich = await ladeIch(true);
  if (!ansicht.gilt(lauf)) return;
  if (!ich) {
    zeige(t('daten.titel'), [
      h('p', {}, t('daten.nichts')),
      h('p', { class: 'leise' }, t('daten.nichtsErklaerung')),
    ]);
    return;
  }

  const anzeige = h('div', {});
  const loeschbereich = h('div', {});
  const zeigeLoeschen = () => loeschbereich.replaceChildren(
    h('button', { class: 'knopf-gefahr', type: 'button', onclick: frageNach }, t('daten.loeschen')));
  const frageNach = () => loeschbereich.replaceChildren(
    h('div', { class: 'fehlerbox' },
      h('p', {}, t('daten.wirklich')),
      ich.typ === 'ANGEHOERIGER' && h('p', {}, t('daten.familieWarnung'))),
    h('button', {
      class: 'knopf-gefahr', type: 'button',
      onclick: async () => {
        try {
          await api('DELETE', '/api/ich');
          zustand.ich = null;
          zeige(t('daten.geloescht.titel'), [
            h('p', {}, t('daten.geloescht.text')),
            h('a', { class: 'knopf', href: '#/' }, t('allgemein.zumAnfang')),
          ], { zurueck: null });
        } catch (e) {
          loeschbereich.replaceChildren(fehlerbox(e.message));
        }
      },
    }, t('daten.jaLoeschen')),
    h('button', { type: 'button', onclick: zeigeLoeschen }, t('daten.neinLoeschen')));
  zeigeLoeschen();

  zeige(t('daten.titel'), [
    h('p', {}, ich.typ === 'SENIOR' ? t('daten.senior') : t('daten.angehoeriger')),
    h('button', {
      class: 'knopf-zweit', type: 'button',
      onclick: async () => {
        try {
          const daten = await api('GET', '/api/ich/daten');
          anzeige.replaceChildren(
            h('p', {}, t('daten.alles')),
            h('pre', { class: 'daten', tabindex: '0' }, JSON.stringify(daten, null, 2)));
        } catch (e) {
          anzeige.replaceChildren(fehlerbox(e.message));
        }
      },
    }, t('daten.ansehen')),
    anzeige,
    loeschbereich,
  ]);
}

/** Nach einem Sprachwechsel: geladene Anleitungen vergessen, sie sind in der alten Sprache. */
export function vergissAnleitungen() {
  geladeneAnleitungen.clear();
}
