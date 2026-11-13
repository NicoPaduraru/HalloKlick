package de.halloklick.check;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import de.halloklick.gemeinsam.ApiFehler;

/**
 * Prüft ein hochgeladenes Foto und kodiert es neu.
 *
 * <p>Beim Neu-Kodieren werden nur die Bildpunkte übernommen. Alle Metadaten
 * (Aufnahmeort, Gerät, Datum) fallen dabei weg. Das Foto wird nie gespeichert.
 */
@Component
class BildPruefung {

    static final int HOECHSTE_GROESSE_BYTES = 5 * 1024 * 1024;
    /** Schutz vor "Bild-Bomben": winzige Dateien, die beim Öffnen riesig werden. */
    private static final long HOECHSTE_BILDPUNKTE = 50_000_000L;
    /** Längste Seite nach dem Verkleinern. Mehr bringt der KI nichts und kostet nur. */
    private static final int LAENGSTE_SEITE = 1568;

    /** @return das Bild als neu kodiertes JPEG ohne Metadaten */
    byte[] pruefeUndKodiereNeu(byte[] datei) {
        if (datei == null || datei.length == 0) {
            throw ApiFehler.ungueltig("Das Foto ist leer. Bitte versuchen Sie es noch einmal.");
        }
        if (datei.length > HOECHSTE_GROESSE_BYTES) {
            throw new ApiFehler(HttpStatus.PAYLOAD_TOO_LARGE, "Das Foto ist zu groß. Es darf höchstens 5 MB haben.");
        }
        // Der Dateityp wird am Inhalt erkannt, nicht am Namen: Den Namen kann jeder fälschen.
        if (istHeic(datei)) {
            throw new ApiFehler(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Dieses Fotoformat (HEIC) können wir noch nicht lesen. Bitte machen Sie ein Bildschirmfoto "
                    + "und schicken Sie das.");
        }
        if (!istJpeg(datei) && !istPng(datei)) {
            throw new ApiFehler(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Das ist kein Foto, das wir lesen können. Erlaubt sind JPEG und PNG.");
        }
        try {
            BufferedImage bild = lies(datei);
            return schreibeJpeg(verkleinere(bild));
        } catch (IOException | RuntimeException e) {
            if (e instanceof ApiFehler fehler) {
                throw fehler;
            }
            throw new ApiFehler(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Wir konnten das Foto nicht öffnen. Bitte versuchen Sie es mit einem anderen Foto.");
        }
    }

    private static BufferedImage lies(byte[] datei) throws IOException {
        try (ImageInputStream eingang = ImageIO.createImageInputStream(new ByteArrayInputStream(datei))) {
            Iterator<ImageReader> leser = ImageIO.getImageReaders(eingang);
            if (!leser.hasNext()) {
                throw new IOException("kein Leser");
            }
            ImageReader reader = leser.next();
            try {
                reader.setInput(eingang, true, true);
                // Erst die Maße prüfen, dann erst das ganze Bild in den Speicher laden
                long punkte = (long) reader.getWidth(0) * reader.getHeight(0);
                if (punkte > HOECHSTE_BILDPUNKTE) {
                    throw new ApiFehler(HttpStatus.PAYLOAD_TOO_LARGE, "Das Foto ist zu groß.");
                }
                BufferedImage bild = reader.read(0);
                if (bild == null) {
                    throw new IOException("leer");
                }
                return bild;
            } finally {
                reader.dispose();
            }
        }
    }

    private static BufferedImage verkleinere(BufferedImage bild) {
        int breite = bild.getWidth();
        int hoehe = bild.getHeight();
        double faktor = Math.min(1.0, (double) LAENGSTE_SEITE / Math.max(breite, hoehe));
        int neueBreite = Math.max(1, (int) Math.round(breite * faktor));
        int neueHoehe = Math.max(1, (int) Math.round(hoehe * faktor));
        // Auf weißen Grund zeichnen: JPEG kennt keine Durchsichtigkeit
        BufferedImage ziel = new BufferedImage(neueBreite, neueHoehe, BufferedImage.TYPE_INT_RGB);
        Graphics2D stift = ziel.createGraphics();
        try {
            stift.setColor(Color.WHITE);
            stift.fillRect(0, 0, neueBreite, neueHoehe);
            stift.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            stift.drawImage(bild, 0, 0, neueBreite, neueHoehe, null);
        } finally {
            stift.dispose();
        }
        return ziel;
    }

    private static byte[] schreibeJpeg(BufferedImage bild) throws IOException {
        ImageWriter schreiber = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream ausgang = new ByteArrayOutputStream();
        try (ImageOutputStream strom = ImageIO.createImageOutputStream(ausgang)) {
            ImageWriteParam parameter = schreiber.getDefaultWriteParam();
            parameter.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parameter.setCompressionQuality(0.85f);
            schreiber.setOutput(strom);
            // Metadaten = null: Es wird nichts außer den Bildpunkten geschrieben
            schreiber.write(null, new IIOImage(bild, null, null), parameter);
        } finally {
            schreiber.dispose();
        }
        return ausgang.toByteArray();
    }

    static boolean istJpeg(byte[] d) {
        return d.length > 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF;
    }

    static boolean istPng(byte[] d) {
        return d.length > 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G';
    }

    static boolean istHeic(byte[] d) {
        if (d.length < 12 || d[4] != 'f' || d[5] != 't' || d[6] != 'y' || d[7] != 'p') {
            return false;
        }
        String marke = new String(d, 8, 4, java.nio.charset.StandardCharsets.US_ASCII);
        return marke.startsWith("hei") || marke.startsWith("hev") || marke.equals("mif1") || marke.equals("msf1");
    }
}
