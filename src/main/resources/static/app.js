// Einstieg der Web-App: entscheidet anhand der Adresse (#/...), welcher Bildschirm gezeigt wird.

import { ansicht, api, fehlerbox, h, ladeEinstellungen, zeige, zustand } from './js/basis.js';
import { setzeSprache, sprache, SPRACHEN, t } from './js/sprache.js';
import * as senior from './js/senior.js';
import * as angehoerige from './js/familie.js';

function zeigeBildschirm() {
  ansicht.neu();
  const teile = location.hash.replace(/^#\/?/, '').split('/').filter(Boolean).map(decodeURIComponent);
  const [seite, ...rest] = teile;
  try {
    switch (seite) {
      case undefined:
        return senior.start();
      case 'ueber':
        return senior.ueber();
      case 'pruefen':
        return senior.pruefen();
      case 'anleitungen':
        return senior.anleitungen();
      case 'anleitung':
        return senior.anleitung(rest[0], rest[1]);
      case 'hilfe':
        return senior.hilfe();
      case 'verbinden':
        return senior.verbinden(rest[0] || '');
      case 'meine-daten':
        return senior.meineDaten();
      case 'familie':
        return rest[0] === 'anleitung' ? angehoerige.anleitungBearbeiten(rest[1], rest[2]) : angehoerige.familie();
      default:
        return zeige(t('allgemein.seiteFehlt'),
          h('a', { class: 'knopf', href: '#/' }, t('allgemein.zumAnfang')), { zurueck: null });
    }
  } catch (e) {
    return zeige(t('allgemein.schiefgegangen'), fehlerbox(t('allgemein.nochmal')));
  }
}

/** Die Sprachwahl steht gut sichtbar oben auf jeder Seite, nicht in einem versteckten Menü. */
function zeigeSprachwahl() {
  const leiste = document.getElementById('sprachen');
  leiste.setAttribute('aria-label', t('kopf.sprache'));
  leiste.replaceChildren(...SPRACHEN.map((eintrag) => h('button', {
    type: 'button',
    class: 'sprach-knopf' + (eintrag.kuerzel === sprache() ? ' gewaehlt' : ''),
    lang: eintrag.kuerzel,
    'aria-pressed': String(eintrag.kuerzel === sprache()),
    onclick: () => wechsleSprache(eintrag.kuerzel),
  }, eintrag.name)));
}

function zeigeFuss() {
  document.getElementById('fuss').replaceChildren(
    h('a', { href: '#/ueber' }, t('fuss.ueber')),
    h('a', { href: '#/meine-daten' }, t('fuss.meineDaten')),
    h('a', { href: '/datenschutz.html' }, t('fuss.datenschutz')),
    h('a', { href: '/impressum.html' }, t('fuss.impressum')),
  );
}

function wechsleSprache(kuerzel) {
  if (kuerzel === sprache()) return;
  setzeSprache(kuerzel);
  senior.vergissAnleitungen();
  // Ist jemand angemeldet, merkt sich der Server die Sprache für Benachrichtigungen
  if (zustand.ich) {
    api('PUT', '/api/ich/sprache', { json: { sprache: kuerzel } }).catch(() => {});
  }
  zeigeSprachwahl();
  zeigeFuss();
  zeigeBildschirm();
}

window.addEventListener('hashchange', zeigeBildschirm);

// Der Service Worker macht die Seite installierbar und zeigt Benachrichtigungen an
if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('/sw.js').catch(() => {
    // Ohne Service Worker läuft die App trotzdem, nur eben nicht installierbar
  });
}

zeigeSprachwahl();
zeigeFuss();
await ladeEinstellungen();
zeigeBildschirm();
