// Mehrere Sprachen in der Web-App.
//
// Eine neue Sprache hinzufügen:
//   1. Datei js/sprachen/<kuerzel>.js anlegen (am besten de.js kopieren und übersetzen)
//   2. unten importieren und in SPRACHEN eintragen
//   3. die Datei in sw.js in die Liste OBERFLAECHE aufnehmen
// Fehlt in einer Sprache ein Text, wird der deutsche Text gezeigt.

import de from './sprachen/de.js';
import en from './sprachen/en.js';
import it from './sprachen/it.js';

export const SPRACHEN = [
  { kuerzel: 'de', name: 'Deutsch', texte: de },
  { kuerzel: 'en', name: 'English', texte: en },
  { kuerzel: 'it', name: 'Italiano', texte: it },
];

const MERKNAME = 'sprache';

function gemerkteSprache() {
  try {
    return localStorage.getItem(MERKNAME);
  } catch (e) {
    return null;
  }
}

function findeSprache(kuerzel) {
  return SPRACHEN.find((eintrag) => eintrag.kuerzel === kuerzel);
}

/** Beim ersten Besuch: die Sprache des Handys, wenn wir sie anbieten. Sonst Deutsch. */
function startSprache() {
  const gemerkt = findeSprache(gemerkteSprache());
  if (gemerkt) return gemerkt;
  for (const wunsch of navigator.languages || [navigator.language || 'de']) {
    const treffer = findeSprache(String(wunsch).slice(0, 2).toLowerCase());
    if (treffer) return treffer;
  }
  return SPRACHEN[0];
}

let aktuell = startSprache();
document.documentElement.lang = aktuell.kuerzel;

/** Kürzel der gewählten Sprache, zum Beispiel "de". */
export function sprache() {
  return aktuell.kuerzel;
}

export function setzeSprache(kuerzel) {
  const neu = findeSprache(kuerzel);
  if (!neu) return;
  aktuell = neu;
  document.documentElement.lang = neu.kuerzel;
  try {
    localStorage.setItem(MERKNAME, neu.kuerzel);
  } catch (e) {
    // Privates Fenster: Die Sprache gilt dann nur bis zum Schließen
  }
}

/**
 * Holt einen Text in der gewählten Sprache. Platzhalter wie {name} werden ersetzt:
 * t('familie.hallo', { name: 'Anna' })
 */
export function t(schluessel, werte = {}) {
  let text = aktuell.texte[schluessel];
  if (text === undefined) text = de[schluessel];
  if (text === undefined) return schluessel;
  return text.replace(/\{(\w+)\}/g, (ganz, name) => (werte[name] === undefined ? ganz : String(werte[name])));
}
