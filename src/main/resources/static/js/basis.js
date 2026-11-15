// Gemeinsame Helfer für alle Bildschirme: Seiten bauen und mit dem Server sprechen.

import { sprache, t } from "./sprache.js";

export const app = document.getElementById('app');

/**
 * Baut ein HTML-Element. Texte werden immer als Text eingesetzt, nie als HTML.
 * So kann ein fremder Text (zum Beispiel eine geteilte Nachricht) keinen Schaden anrichten.
 */
export function h(tag, eigenschaften, ...kinder) {
  const element = document.createElement(tag);
  for (const [name, wert] of Object.entries(eigenschaften || {})) {
    if (wert === null || wert === undefined || wert === false) continue;
    if (name === 'class') element.className = wert;
    else if (name.startsWith('on')) element.addEventListener(name.slice(2), wert);
    else if (wert === true) element.setAttribute(name, '');
    else element.setAttribute(name, wert);
  }
  for (const kind of kinder.flat()) {
    if (kind === null || kind === undefined || kind === false) continue;
    element.append(kind instanceof Node ? kind : document.createTextNode(String(kind)));
  }
  return element;
}

/** Ersetzt den Inhalt eines Elements. Leere Werte (null, false) werden übersprungen. */
export function ersetze(element, ...kinder) {
  element.replaceChildren(...kinder.filter((kind) => kind !== null && kind !== undefined && kind !== false));
}

/** Zeigt einen Bildschirm an: Zurück-Schaltfläche, Überschrift, Inhalt. */
export function zeige(titel, inhalt, { zurueck = "#/", zurueckText = t("allgemein.zurueck") } = {}) {
  app.classList.remove('schritt-ansicht');
  document.body.classList.remove('schritt-modus');
  ersetze(app,
    zurueck ? h('a', { class: 'knopf knopf-zweit zurueck', href: zurueck }, '← ' + zurueckText) : null,
    titel ? h('h1', {}, titel) : null,
    ...[inhalt].flat(),
  );
  document.title = titel ? titel + " – HalloKlick" : "HalloKlick";
  app.focus();
  window.scrollTo(0, 0);
}

export function zeigeLaden(text = t("allgemein.laden")) {
  app.replaceChildren(h('p', { class: 'laden', role: 'status' }, text));
}

export function fehlerbox(text) {
  return h('p', { class: 'fehlerbox', role: 'alert' }, text);
}

// ---------- Server ----------

export class ApiFehler extends Error {
  constructor(status, text) {
    super(text);
    this.status = status;
  }
}

function cookie(name) {
  const treffer = document.cookie.split('; ').find((teil) => teil.startsWith(name + '='));
  return treffer ? decodeURIComponent(treffer.slice(name.length + 1)) : null;
}

/**
 * Schickt eine Anfrage an den Server.
 * Bei ändernden Anfragen geht das CSRF-Token mit (Schutz vor gefälschten Anfragen fremder Seiten).
 */
export async function api(methode, pfad, { json, formular } = {}) {
  // Der Server antwortet in der gewählten Sprache
  const kopf = { Accept: "application/json", "Accept-Language": sprache() };
  if (methode !== 'GET') {
    if (!cookie('XSRF-TOKEN')) {
      // Der Server legt das Token bei jeder Antwort ins Cookie
      await fetch('/api/einstellungen', { credentials: 'same-origin' });
    }
    kopf['X-XSRF-TOKEN'] = cookie('XSRF-TOKEN') || '';
  }
  let inhalt;
  if (json !== undefined) {
    kopf['Content-Type'] = 'application/json';
    inhalt = JSON.stringify(json);
  } else if (formular) {
    inhalt = formular;
  }

  let antwort;
  try {
    antwort = await fetch(pfad, { method: methode, headers: kopf, body: inhalt, credentials: 'same-origin' });
  } catch (e) {
    throw new ApiFehler(0, t("allgemein.keineVerbindung"));
  }
  if (antwort.status === 204) return null;
  const text = await antwort.text();
  let daten = null;
  try {
    daten = text ? JSON.parse(text) : null;
  } catch (e) {
    daten = null;
  }
  if (!antwort.ok) {
    const meldung = (daten && daten.detail) || t("allgemein.fehler");
    throw new ApiFehler(antwort.status, meldung);
  }
  return daten;
}

// ---------- Was die App über sich weiß ----------

export const zustand = {
  einstellungen: { sicherheitscheck: true, anleitungen: true, familie: true, fotoPruefung: false, pushSchluessel: null },
  /** undefined = noch nicht gefragt, null = nicht angemeldet */
  ich: undefined,
};

export async function ladeEinstellungen() {
  try {
    zustand.einstellungen = await api('GET', '/api/einstellungen');
  } catch (e) {
    // Ohne Verbindung gelten die Standardwerte
  }
}

export async function ladeIch(neu = false) {
  if (zustand.ich !== undefined && !neu) return zustand.ich;
  try {
    zustand.ich = await api('GET', '/api/ich');
  } catch (e) {
    zustand.ich = null;
  }
  return zustand.ich;
}

/** Android oder iPhone? Davon hängt ab, welche Anleitung passt. */
export function erkanntePlattform() {
  const kennung = navigator.userAgent || '';
  if (/android/i.test(kennung)) return 'ANDROID';
  if (/iphone|ipad|ipod/i.test(kennung)) return 'IOS';
  return 'SONSTIGE';
}

/**
 * Senioren brauchen kein Passwort: Beim ersten Mal legt die App still ein anonymes Gerät an.
 * Der Schlüssel dazu liegt in einem Cookie, das JavaScript nicht lesen kann.
 */
export async function stelleGeraetSicher() {
  const ich = await ladeIch();
  if (ich) return ich;
  await api('POST', '/api/geraete', { json: { plattform: erkanntePlattform() } });
  return ladeIch(true);
}

/** Kleiner Speicher im Browser, nur für Bequemlichkeit (zum Beispiel das gewählte Handy). */
export const merkzettel = {
  lies(name) {
    try {
      return localStorage.getItem(name);
    } catch (e) {
      return null;
    }
  },
  schreibe(name, wert) {
    try {
      localStorage.setItem(name, wert);
    } catch (e) {
      // Privates Fenster: dann eben ohne Merken
    }
  },
};

export function datum(isoText) {
  try {
    return new Date(isoText).toLocaleString(sprache(), { dateStyle: 'medium', timeStyle: 'short' });
  } catch (e) {
    return isoText;
  }
}

/**
 * Schutz vor veralteten Seiten: Manche Bildschirme warten auf den Server, bevor sie sich zeigen.
 * Hat der Nutzer in der Zwischenzeit woanders hingetippt, darf die alte Seite nicht mehr erscheinen.
 * Der Einstieg (app.js) zählt bei jedem Seitenwechsel hoch, die Bildschirme prüfen nach dem Warten.
 */
let ansichtNummer = 0;
export const ansicht = {
  neu() {
    ansichtNummer += 1;
  },
  nummer() {
    return ansichtNummer;
  },
  gilt(nummer) {
    return nummer === ansichtNummer;
  },
};
