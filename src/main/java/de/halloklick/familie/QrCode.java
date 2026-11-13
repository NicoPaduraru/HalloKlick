package de.halloklick.familie;

import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/** Zeichnet einen QR-Code als SVG-Bild (Text, den der Browser als Bild anzeigt). */
final class QrCode {

    private QrCode() {
    }

    static String alsSvg(String inhalt) {
        BitMatrix punkte;
        try {
            punkte = new QRCodeWriter().encode(inhalt, BarcodeFormat.QR_CODE, 0, 0, Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                    EncodeHintType.MARGIN, 2));
        } catch (WriterException e) {
            throw new IllegalStateException("QR-Code konnte nicht erzeugt werden", e);
        }
        int breite = punkte.getWidth();
        int hoehe = punkte.getHeight();
        StringBuilder pfad = new StringBuilder();
        for (int y = 0; y < hoehe; y++) {
            for (int x = 0; x < breite; x++) {
                if (punkte.get(x, y)) {
                    pfad.append('M').append(x).append(' ').append(y).append("h1v1h-1z");
                }
            }
        }
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 " + breite + " " + hoehe
                + "\" shape-rendering=\"crispEdges\"><rect width=\"100%\" height=\"100%\" fill=\"#fff\"/>"
                + "<path fill=\"#000\" d=\"" + pfad + "\"/></svg>";
    }
}
