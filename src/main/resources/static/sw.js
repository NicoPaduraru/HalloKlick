// Service Worker: macht die Web-App installierbar, hält die Oberfläche für schlechte
// Verbindungen bereit und zeigt Benachrichtigungen (Web Push) an.
//
// Bei jeder Änderung an den Dateien der Oberfläche die Versionsnummer erhöhen.
const VERSION = 'halloklick-v3';

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
      .then((namen) => Promise.all(namen.filter((name) => name !== VERSION).map((name) => caches.delete(name))))
      .then(() => self.clients.claim()),
  );
});

self.addEventListener('fetch', (ereignis) => {
  const anfrage = ereignis.request;
  const adresse = new URL(anfrage.url);
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
      .catch(() => caches.match(anfrage).then((gespeichert) => gespeichert || caches.match('/'))),
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
