// Die Bildschirme für Senioren: pro Bildschirm eine Aufgabe, große Schaltflächen, einfache Sprache.

import {
  api, erkanntePlattform, fehlerbox, h, ladeIch, merkzettel, stelleGeraetSicher, zeige, zeigeLaden, zustand,
} from './basis.js';

// ---------- Start: drei große Schaltflächen ----------

export function start() {
  const an = zustand.einstellungen;
  zeige('Wobei können wir helfen?', [
    an.sicherheitscheck && h('a', { class: 'knopf start-knopf', href: '#/pruefen' },
      'Ist das echt?', h('small', {}, 'Eine Nachricht prüfen')),
    an.anleitungen && h('a', { class: 'knopf start-knopf', href: '#/anleitungen' },
      'Wie mache ich das?', h('small', {}, 'Schritt für Schritt erklärt')),
    an.familie && h('a', { class: 'knopf start-knopf', href: '#/hilfe' },
      'Familie fragen', h('small', {}, 'Um Hilfe bitten')),
    h('p', { class: 'leise' }, 'Die Hilfe ist kostenlos. Sie brauchen kein Passwort.'),
    an.familie && h('p', { class: 'leise' },
      'Sie helfen Ihren Eltern? ', h('a', { href: '#/familie' }, 'Zum Bereich für Angehörige')),
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
        meldung.replaceChildren(h('p', { class: 'hinweisbox' },
          'Das Einfügen hat nicht geklappt. Tippen Sie lange in das Feld und wählen Sie „Einfügen“.'));
      }
    },
  }, 'Kopierten Text einfügen');

  zeige('Ist das echt?', [
    h('p', {}, 'Haben Sie eine Nachricht bekommen, die Ihnen komisch vorkommt? Wir schauen sie uns an.'),
    h('form', {
      onsubmit: (ereignis) => {
        ereignis.preventDefault();
        if (!text.value.trim()) {
          meldung.replaceChildren(fehlerbox('Bitte fügen Sie zuerst den Text der Nachricht ein.'));
          return;
        }
        const formular = new FormData();
        formular.append('text', text.value);
        schickeZurPruefung(formular, meldung);
      },
    },
      h('label', { for: 'nachricht' }, 'Text der Nachricht'),
      text,
      einfuegen,
      meldung,
      h('button', { type: 'submit' }, 'Jetzt prüfen'),
    ),
    zustand.einstellungen.fotoPruefung && h('div', {},
      h('p', {}, 'Oder machen Sie ein Foto von der Nachricht:'),
      dateiwahl,
      h('button', { class: 'knopf-zweit', type: 'button', onclick: () => dateiwahl.click() },
        'Foto aufnehmen oder auswählen'),
    ),
    h('p', { class: 'leise' }, 'Wir speichern den Inhalt Ihrer Nachricht nicht.'),
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
    await schickeZurPruefung(formular, meldung);
  } catch (e) {
    meldung.replaceChildren(fehlerbox(
      'Wir konnten das Foto nicht öffnen. Bitte versuchen Sie es noch einmal oder fügen Sie den Text ein.'));
  }
}

async function schickeZurPruefung(formular, meldung) {
  zeigeLaden('Wir prüfen die Nachricht …');
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
  const frageFeedback = () => feedback.replaceChildren(
    h('p', {}, h('strong', {}, 'War das hilfreich?')),
    h('div', { class: 'reihe' },
      h('button', { class: 'knopf-zweit', type: 'button', onclick: () => gibFeedback(true) }, 'Ja'),
      h('button', { class: 'knopf-zweit', type: 'button', onclick: () => gibFeedback(false) }, 'Nein')),
  );
  const gibFeedback = async (hilfreich) => {
    try {
      await api('POST', `/api/checks/${ergebnis.id}/feedback`, { json: { hilfreich } });
    } catch (e) {
      // Feedback ist freiwillig: Ein Fehler hier soll niemanden aufhalten
    }
    feedback.replaceChildren(h('p', {}, 'Danke für Ihre Antwort.'));
  };
  frageFeedback();

  // Die Farbe ist nie das einzige Zeichen: Die Stufe steht immer auch als Wort da.
  const marke = !ergebnis.geprueft ? 'Nicht geprüft' : (rot ? 'Vorsicht' : 'Keine Warnzeichen');

  zeige(null, [
    h('section', { class: 'ergebnis ' + (rot ? 'ergebnis-rot' : 'ergebnis-orange') },
      h('span', { class: 'stufe-marke' }, marke),
      h('h1', {}, ergebnis.titel),
      h('p', {}, ergebnis.erklaerung),
      ergebnis.merkmale.length > 0 && h('div', {},
        h('p', {}, h('strong', {}, 'Das ist uns aufgefallen:')),
        h('ul', { class: 'merkmale' }, ergebnis.merkmale.map((merkmal) => h('li', {}, merkmal)))),
      h('p', {}, h('strong', {}, 'Was Sie jetzt tun können:')),
      h('p', {}, ergebnis.naechsterSchritt),
      ergebnis.zusatzHinweis && h('p', {}, ergebnis.zusatzHinweis),
    ),
    h('p', { class: 'hinweisbox' }, ergebnis.hinweis),
    zustand.einstellungen.familie && h('a', { class: 'knopf', href: '#/hilfe' }, 'Familie fragen'),
    h('button', { class: 'knopf-zweit', type: 'button', onclick: () => pruefen() }, 'Noch eine Nachricht prüfen'),
    ergebnis.geprueft && feedback,
  ], { zurueck: '#/', zurueckText: 'Zum Anfang' });
}

// ---------- "Wie mache ich das?" ----------

const PLATTFORM_NAMEN = { ANDROID: 'Android', IOS: 'iPhone', ALLE: 'Alle Handys' };

function meinePlattform() {
  const gemerkt = merkzettel.lies('plattform');
  if (gemerkt === 'ANDROID' || gemerkt === 'IOS') return gemerkt;
  const erkannt = erkanntePlattform();
  return erkannt === 'SONSTIGE' ? null : erkannt;
}

export async function anleitungen(suchwort = '') {
  const plattform = meinePlattform();
  const feld = h('input', { id: 'suche', type: 'search', value: suchwort, placeholder: 'zum Beispiel: Foto schicken' });
  const ergebnisse = h('div', {}, h('p', { class: 'laden', role: 'status' }, 'Einen Moment bitte …'));

  const waehlePlattform = (neu) => {
    merkzettel.schreibe('plattform', neu);
    anleitungen(feld.value);
  };

  zeige('Wie mache ich das?', [
    h('form', {
      role: 'search',
      onsubmit: (ereignis) => {
        ereignis.preventDefault();
        anleitungen(feld.value);
      },
    },
      h('label', { for: 'suche' }, 'Was möchten Sie tun?'),
      feld,
      h('button', { type: 'submit' }, 'Suchen'),
    ),
    h('p', {}, h('strong', {}, 'Ihr Handy: '), plattform ? PLATTFORM_NAMEN[plattform] : 'nicht gewählt'),
    h('div', { class: 'reihe' },
      h('button', {
        class: plattform === 'ANDROID' ? '' : 'knopf-zweit', type: 'button', 'aria-pressed': String(plattform === 'ANDROID'),
        onclick: () => waehlePlattform('ANDROID'),
      }, 'Android'),
      h('button', {
        class: plattform === 'IOS' ? '' : 'knopf-zweit', type: 'button', 'aria-pressed': String(plattform === 'IOS'),
        onclick: () => waehlePlattform('IOS'),
      }, 'iPhone')),
    h('h2', {}, suchwort.trim() ? 'Das haben wir gefunden' : 'Alle Anleitungen'),
    ergebnisse,
  ]);

  try {
    const abfrage = new URLSearchParams();
    if (suchwort.trim()) abfrage.set('suche', suchwort.trim());
    if (plattform) abfrage.set('plattform', plattform);
    const gefunden = await api('GET', '/api/anleitungen?' + abfrage);
    if (gefunden.length === 0) {
      ergebnisse.replaceChildren(h('p', { class: 'hinweisbox' },
        'Dazu haben wir leider keine Anleitung gefunden. Versuchen Sie ein anderes Wort oder fragen Sie Ihre Familie.'));
      return;
    }
    ergebnisse.replaceChildren(h('ul', { class: 'liste' }, gefunden.map((anleitung) => h('li', {},
      h('a', { class: 'knopf knopf-zweit', href: `#/anleitung/${anleitung.id}/1` },
        anleitung.titel,
        h('small', {}, (anleitung.quelle === 'FAMILIE' ? 'Von Ihrer Familie · ' : '')
          + PLATTFORM_NAMEN[anleitung.plattform]))))));
  } catch (e) {
    ergebnisse.replaceChildren(fehlerbox(e.message));
  }
}

const geladeneAnleitungen = new Map();

/** Ein Schritt pro Bildschirm, mit sichtbarem "Weiter" und "Zurück". Keine Wischgesten. */
export async function anleitung(id, schrittNummer) {
  let daten = geladeneAnleitungen.get(id);
  if (!daten) {
    zeigeLaden();
    try {
      daten = await api('GET', '/api/anleitungen/' + encodeURIComponent(id));
      geladeneAnleitungen.set(id, daten);
    } catch (e) {
      zeige('Anleitung', fehlerbox(e.message), { zurueck: '#/anleitungen', zurueckText: 'Zur Suche' });
      return;
    }
  }
  const anzahl = daten.schritte.length;
  const nummer = Math.min(Math.max(parseInt(schrittNummer, 10) || 1, 1), anzahl);
  const schritt = daten.schritte[nummer - 1];
  const letzter = nummer === anzahl;

  zeige(daten.titel, [
    h('p', { class: 'schritt-zaehler' }, `Schritt ${nummer} von ${anzahl}`),
    h('p', { class: 'schritt-text' }, schritt.text),
    letzter
      ? h('a', { class: 'knopf', href: '#/' }, 'Fertig')
      : h('a', { class: 'knopf', href: `#/anleitung/${id}/${nummer + 1}` }, 'Weiter →'),
    nummer > 1 && h('a', { class: 'knopf knopf-zweit', href: `#/anleitung/${id}/${nummer - 1}` },
      '← Einen Schritt zurück'),
    !daten.geprueftAm && daten.quelle === 'SYSTEM' && h('p', { class: 'leise' },
      'Diese Anleitung ist ein Beispiel. Auf Ihrem Handy kann etwas anders heißen.'),
  ], { zurueck: '#/anleitungen', zurueckText: 'Zur Suche' });
}

// ---------- "Familie fragen" ----------

const THEMEN = [
  ['KOMISCHE_NACHRICHT', 'Ich habe eine komische Nachricht bekommen'],
  ['HANDY_HILFE', 'Ich komme am Handy nicht weiter'],
  ['BITTE_ANRUFEN', 'Bitte ruf mich an'],
  ['ETWAS_ANDERES', 'Etwas anderes'],
];

export async function hilfe() {
  zeigeLaden();
  let familien = [];
  try {
    const ich = await stelleGeraetSicher();
    if (ich && ich.typ === 'ANGEHOERIGER') {
      zeige('Familie fragen', h('p', { class: 'hinweisbox' },
        'Sie sind als Angehöriger angemeldet. Diese Seite ist für Ihre Eltern gedacht.'));
      return;
    }
    familien = await api('GET', '/api/ich/familien');
  } catch (e) {
    zeige('Familie fragen', fehlerbox(e.message));
    return;
  }

  if (familien.length === 0) {
    zeige('Familie fragen', [
      h('p', {}, 'Ihr Handy ist noch nicht mit Ihrer Familie verbunden.'),
      h('p', {}, 'Bitten Sie Ihre Tochter oder Ihren Sohn um einen Code. Den Code tippen Sie dann hier ein.'),
      h('a', { class: 'knopf', href: '#/verbinden' }, 'Ich habe einen Code'),
    ]);
    return;
  }

  const meldung = h('div', {});
  const textfeld = h('textarea', { id: 'geteilt', maxlength: '2000' });
  const textbereich = h('div', { hidden: true },
    h('label', { for: 'geteilt' }, 'Das möchte ich mitschicken'), textfeld);
  const teilen = h('input', {
    type: 'checkbox',
    onchange: () => { textbereich.hidden = !teilen.checked; },
  });

  zeige('Familie fragen', [
    h('p', {}, 'Ihre Nachricht geht an: ', h('strong', {}, familien.map((f) => f.name).join(', '))),
    h('form', {
      onsubmit: async (ereignis) => {
        ereignis.preventDefault();
        const thema = new FormData(ereignis.target).get('thema');
        if (!thema) {
          meldung.replaceChildren(fehlerbox('Bitte wählen Sie aus, worum es geht.'));
          return;
        }
        const knopf = ereignis.target.querySelector('button[type=submit]');
        knopf.disabled = true;
        try {
          await api('POST', '/api/hilfeanfragen', {
            json: { thema, geteilterText: teilen.checked && textfeld.value.trim() ? textfeld.value : null },
          });
          zeige('Ihre Familie weiß Bescheid', [
            h('p', {}, 'Wir haben Ihrer Familie geschrieben. Jemand meldet sich bei Ihnen.'),
            h('p', { class: 'hinweisbox' }, 'Ist es eilig? Dann rufen Sie Ihre Familie am besten gleich an.'),
            h('a', { class: 'knopf', href: '#/' }, 'Fertig'),
          ], { zurueck: null });
        } catch (e) {
          knopf.disabled = false;
          meldung.replaceChildren(fehlerbox(e.message));
        }
      },
    },
      h('fieldset', { class: 'karte' },
        h('legend', {}, h('strong', {}, 'Worum geht es?')),
        THEMEN.map(([wert, text]) => h('label', { class: 'wahl' },
          h('input', { type: 'radio', name: 'thema', value: wert }), text))),
      // Der Senior entscheidet selbst, ob er den Inhalt mitschickt
      h('label', { class: 'wahl' }, teilen, 'Ich möchte einen Text mitschicken, zum Beispiel die Nachricht'),
      textbereich,
      meldung,
      h('button', { type: 'submit' }, 'Senden'),
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

  zeige('Mit der Familie verbinden', [
    h('p', {}, 'Ihre Tochter oder Ihr Sohn hat einen Code für Sie. Er hat 8 Zeichen.'),
    h('form', {
      onsubmit: async (ereignis) => {
        ereignis.preventDefault();
        if (!feld.value.trim()) {
          meldung.replaceChildren(fehlerbox('Bitte geben Sie den Code ein.'));
          return;
        }
        const knopf = ereignis.target.querySelector('button');
        knopf.disabled = true;
        try {
          await stelleGeraetSicher();
          const familie = await api('POST', '/api/geraete/koppeln', { json: { code: feld.value } });
          await ladeIch(true);
          zeige('Das hat geklappt', [
            h('p', {}, 'Ihr Handy ist jetzt verbunden mit: ', h('strong', {}, familie.name)),
            h('p', {}, 'Wenn Sie Hilfe brauchen, tippen Sie auf „Familie fragen“.'),
            h('a', { class: 'knopf', href: '#/' }, 'Zum Anfang'),
          ], { zurueck: null });
        } catch (e) {
          knopf.disabled = false;
          meldung.replaceChildren(fehlerbox(e.status === 403
            ? 'Sie sind als Angehöriger angemeldet. Den Code gibt Ihre Mutter oder Ihr Vater auf dem eigenen Handy ein.'
            : e.message));
        }
      },
    },
      h('label', { for: 'code' }, 'Code'),
      feld,
      meldung,
      h('button', { type: 'submit' }, 'Verbinden'),
    ),
  ]);
}

// ---------- Meine Daten (Auskunft und Löschen) ----------

export async function meineDaten() {
  zeigeLaden();
  const ich = await ladeIch(true);
  if (!ich) {
    zeige('Meine Daten', [
      h('p', {}, 'Wir haben auf diesem Gerät noch nichts über Sie gespeichert.'),
      h('p', { class: 'leise' }, 'Erst wenn Sie eine Nachricht prüfen oder Ihre Familie fragen, '
        + 'legen wir ein anonymes Konto für dieses Gerät an. Ohne Namen und ohne Passwort.'),
    ]);
    return;
  }

  const anzeige = h('div', {});
  const loeschbereich = h('div', {});
  const zeigeLoeschen = () => loeschbereich.replaceChildren(
    h('button', { class: 'knopf-gefahr', type: 'button', onclick: frageNach }, 'Alle meine Daten löschen'));
  const frageNach = () => loeschbereich.replaceChildren(
    h('div', { class: 'fehlerbox' },
      h('p', {}, 'Wirklich alles löschen? Das kann man nicht rückgängig machen.'),
      ich.typ === 'ANGEHOERIGER' && h('p', {},
        'Sind Sie der einzige Angehörige, wird auch Ihre Familie mit allen Anfragen und eigenen Anleitungen gelöscht.')),
    h('button', {
      class: 'knopf-gefahr', type: 'button',
      onclick: async () => {
        try {
          await api('DELETE', '/api/ich');
          zustand.ich = null;
          zeige('Ihre Daten sind gelöscht', [
            h('p', {}, 'Wir haben alles gelöscht, was zu Ihnen gespeichert war.'),
            h('a', { class: 'knopf', href: '#/' }, 'Zum Anfang'),
          ], { zurueck: null });
        } catch (e) {
          loeschbereich.replaceChildren(fehlerbox(e.message));
        }
      },
    }, 'Ja, alles löschen'),
    h('button', { type: 'button', onclick: zeigeLoeschen }, 'Nein, nichts löschen'));
  zeigeLoeschen();

  zeige('Meine Daten', [
    h('p', {}, ich.typ === 'SENIOR'
      ? 'Für dieses Gerät gibt es ein anonymes Konto. Den Inhalt Ihrer Nachrichten speichern wir nicht.'
      : 'Sie sind als Angehöriger angemeldet.'),
    h('button', {
      class: 'knopf-zweit', type: 'button',
      onclick: async () => {
        try {
          const daten = await api('GET', '/api/ich/daten');
          anzeige.replaceChildren(
            h('p', {}, 'Das ist alles, was wir zu Ihnen gespeichert haben:'),
            h('pre', { class: 'daten', tabindex: '0' }, JSON.stringify(daten, null, 2)));
        } catch (e) {
          anzeige.replaceChildren(fehlerbox(e.message));
        }
      },
    }, 'Meine Daten ansehen'),
    anzeige,
    loeschbereich,
  ]);
}
