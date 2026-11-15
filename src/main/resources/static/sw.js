// Service Worker: macht die Web-App installierbar, hält die Oberfläche für schlechte
// Verbindungen bereit und zeigt Benachrichtigungen (Web Push) an.
//
// Bei jeder Änderung an den Dateien der Oberfläche die Versionsnummer erhöhen.
const VERSION = 'halloklick-v9';

// Hier liegt kurz, was jemand an HalloKlick geteilt hat, bis die App es abholt.
const GETEILT = 'halloklick-geteilt';

const OBERFLAECHE = [
  '/',
  '/app.css',
  '/app.js',
  '/js/basis.js',
  '/js/senior.js',
  '/js/familie.js',
  '/js/sprache.js',
  '/js/sprachen/de.js',
  '/js/sprachen/en.js',
  '/js/sprachen/it.js',
  '/manifest.json',
  '/icons/icon-192.png',
  '/icons/icon-512.png',
];

self.addEventListener('install', (ereignis) => {
  ereignis.waitUntil(caches.open(VERSION).then((speicher) => speicher.addAll(OBERFLAECHE)).then(() => self.skipWaiting()));
});

self.addEventListener('activate', (ereignis) => {
  ereignis.waitUntil(
    caches.keys()
      .then((namen) => Promise.all(namen.filter((name) => name !== VERSION && name !== GETEILT).map((name) => caches.delete(name))))
      .then(() => self.clients.claim()),
  );
});

self.addEventListener('fetch', (ereignis) => {
  const anfrage = ereignis.request;
  const adresse = new URL(anfrage.url);
  // "Teilen an HalloKlick": Die geteilte Nachricht bleibt im Handy und geht direkt an die App
  if (anfrage.method === 'POST' && adresse.origin === self.location.origin && adresse.pathname === '/teilen') {
    ereignis.respondWith(nimmGeteiltesAn(anfrage));
    return;
  }
  // Alles, was Daten betrifft, geht immer frisch an den Server und wird nie zwischengespeichert
  if (anfrage.method !== 'GET' || adresse.origin !== self.location.origin
      || adresse.pathname.startsWith('/api/') || adresse.pathname.startsWith('/actuator/')) {
    return;
  }
  // Oberfläche: zuerst das Netz (damit Änderungen sofort ankommen), ohne Netz der Zwischenspeicher
  ereignis.respondWith(
    fetch(anfrage)
      .then((antwort) => {
        if (antwort.ok) {
          const kopie = antwort.clone();
          caches.open(VERSION).then((speicher) => speicher.put(anfrage, kopie));
        }
        return antwort;
      })
      // Ohne Netz: die gespeicherte Datei. Nur beim Öffnen einer Seite ersatzweise die Startseite,
      // nie für Skripte oder Bilder (sonst bekäme ein Skript HTML geliefert).
      .catch(() => caches.match(anfrage).then((gespeichert) => gespeichert
        || (anfrage.mode === 'navigate' ? caches.match('/') : Response.error()))),
  );
});

// ---------- Benachrichtigungen ----------

self.addEventListener('push', (ereignis) => {
  let daten = { titel: 'HalloKlick', text: 'Es gibt etwas Neues.', ziel: '/#/familie' };
  try {
    daten = { ...daten, ...ereignis.data.json() };
  } catch (e) {
    // Nachricht ohne lesbaren Inhalt: Standardtext zeigen
  }
  ereignis.waitUntil(self.registration.showNotification(daten.titel, {
    body: daten.text,
    icon: '/icons/icon-192.png',
    badge: '/icons/icon-192.png',
    data: { ziel: daten.ziel },
  }));
});

self.addEventListener('notificationclick', (ereignis) => {
  ereignis.notification.close();
  // Nur Ziele auf unserer eigenen Seite öffnen
  const ziel = new URL(ereignis.notification.data?.ziel || '/#/familie', self.location.origin);
  if (ziel.origin !== self.location.origin) return;
  ereignis.waitUntil(self.clients.openWindow(ziel.href));
});

// ---------- Teilen an HalloKlick (Web Share Target) ----------

/**
 * Wer in WhatsApp oder SMS lange auf eine Nachricht tippt und "Teilen" und dann "HalloKlick" wählt,
 * landet hier. Text oder Foto werden kurz im Handy abgelegt. Die App holt sie auf der Seite
 * #/geteilt ab, entfernt sie dort wieder und prüft sie sofort.
 */
async function nimmGeteiltesAn(anfrage) {
  try {
    const formular = await anfrage.formData();
    const text = [formular.get('text'), formular.get('url')]
      .filter((teil) => typeof teil === 'string' && teil.trim())
      .join('\n');
    const bild = formular.get('bild');
    const ablage = await caches.open(GETEILT);
    await ablage.delete('/geteilt/text');
    await ablage.delete('/geteilt/bild');
    if (text) {
      await ablage.put('/geteilt/text', new Response(text, { headers: { 'Content-Type': 'text/plain; charset=utf-8' } }));
    } else if (bild && typeof bild !== 'string' && bild.size > 0) {
      await ablage.put('/geteilt/bild', new Response(bild, { headers: { 'Content-Type': bild.type || 'image/jpeg' } }));
    }
  } catch (e) {
    // Dann eben ohne Inhalt: Die App zeigt die leere Seite "Ist das echt?"
  }
  return Response.redirect('/#/geteilt', 303);
}
