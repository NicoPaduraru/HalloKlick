// Einstieg der Web-App: entscheidet anhand der Adresse (#/...), welcher Bildschirm gezeigt wird.

import { fehlerbox, h, ladeEinstellungen, zeige } from './js/basis.js';
import * as senior from './js/senior.js';
import * as angehoerige from './js/familie.js';

function zeigeBildschirm() {
  const teile = location.hash.replace(/^#\/?/, '').split('/').filter(Boolean).map(decodeURIComponent);
  const [seite, ...rest] = teile;
  try {
    switch (seite) {
      case undefined:
        return senior.start();
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
        return zeige('Diese Seite gibt es nicht', h('a', { class: 'knopf', href: '#/' }, 'Zum Anfang'), { zurueck: null });
    }
  } catch (e) {
    return zeige('Etwas ist schiefgegangen', fehlerbox('Bitte versuchen Sie es noch einmal.'));
  }
}

window.addEventListener('hashchange', zeigeBildschirm);

// Der Service Worker macht die Seite installierbar und zeigt Benachrichtigungen an
if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('/sw.js').catch(() => {
    // Ohne Service Worker läuft die App trotzdem, nur eben nicht installierbar
  });
}

await ladeEinstellungen();
zeigeBildschirm();
