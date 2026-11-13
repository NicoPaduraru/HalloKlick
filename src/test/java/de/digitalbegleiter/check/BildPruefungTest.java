package de.digitalbegleiter.check;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import de.digitalbegleiter.gemeinsam.ApiFehler;

class BildPruefungTest {

    private final BildPruefung pruefung = new BildPruefung();

    private static byte[] bild(String format, int breite, int hoehe) throws Exception {
        int typ = format.equals("png") ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        ByteArrayOutputStream aus = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(breite, hoehe, typ), format, aus);
        return aus.toByteArray();
    }

    /** Baut einen Kommentar-Abschnitt in ein JPEG ein, so wie Kameras Ort und Gerät hineinschreiben. */
    private static byte[] mitMetadaten(byte[] jpeg, String geheim) {
        byte[] text = geheim.getBytes(StandardCharsets.US_ASCII);
        int laenge = text.length + 2;
        ByteArrayOutputStream aus = new ByteArrayOutputStream();
        aus.write(jpeg, 0, 2);
        aus.write(0xFF);
        aus.write(0xFE);
        aus.write(laenge >> 8);
        aus.write(laenge & 0xFF);
        aus.writeBytes(text);
        aus.write(jpeg, 2, jpeg.length - 2);
        return aus.toByteArray();
    }

    @Test
    void metadatenWerdenBeimNeuKodierenEntfernt() throws Exception {
        byte[] original = mitMetadaten(bild("jpg", 200, 100), "ORT:Musterstrasse-12-GERAET:Handy");
        assertThat(new String(original, StandardCharsets.ISO_8859_1)).contains("Musterstrasse");

        byte[] neu = pruefung.pruefeUndKodiereNeu(original);

        assertThat(BildPruefung.istJpeg(neu)).isTrue();
        assertThat(new String(neu, StandardCharsets.ISO_8859_1)).doesNotContain("Musterstrasse");
        BufferedImage gelesen = ImageIO.read(new ByteArrayInputStream(neu));
        assertThat(gelesen.getWidth()).isEqualTo(200);
        assertThat(gelesen.getHeight()).isEqualTo(100);
    }

    @Test
    void pngWirdZuJpeg() throws Exception {
        assertThat(BildPruefung.istJpeg(pruefung.pruefeUndKodiereNeu(bild("png", 50, 50)))).isTrue();
    }

    @Test
    void grosseBilderWerdenVerkleinert() throws Exception {
        byte[] neu = pruefung.pruefeUndKodiereNeu(bild("jpg", 4000, 2000));
        BufferedImage gelesen = ImageIO.read(new ByteArrayInputStream(neu));
        assertThat(gelesen.getWidth()).isEqualTo(1568);
        assertThat(gelesen.getHeight()).isEqualTo(784);
    }

    @Test
    void derDateitypWirdAmInhaltErkanntNichtAmNamen() {
        assertThatThrownBy(() -> pruefung.pruefeUndKodiereNeu("<html>kein Bild</html>".getBytes()))
                .isInstanceOf(ApiFehler.class)
                .extracting(e -> ((ApiFehler) e).status()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void heicWirdMitVerstaendlichemHinweisAbgelehnt() {
        byte[] heic = {0, 0, 0, 24, 'f', 't', 'y', 'p', 'h', 'e', 'i', 'c', 0, 0, 0, 0};
        assertThatThrownBy(() -> pruefung.pruefeUndKodiereNeu(heic))
                .isInstanceOf(ApiFehler.class)
                .hasMessageContaining("HEIC");
    }

    @Test
    void kaputtesBildMitRichtigemAnfangWirdAbgelehnt() {
        byte[] kaputt = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3, 4, 5, 6, 7};
        assertThatThrownBy(() -> pruefung.pruefeUndKodiereNeu(kaputt)).isInstanceOf(ApiFehler.class);
    }

    @Test
    void zuGrosseDateiWirdAbgelehnt() {
        byte[] riesig = new byte[BildPruefung.HOECHSTE_GROESSE_BYTES + 1];
        assertThatThrownBy(() -> pruefung.pruefeUndKodiereNeu(riesig))
                .isInstanceOf(ApiFehler.class)
                .extracting(e -> ((ApiFehler) e).status()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @Test
    void leereDateiWirdAbgelehnt() {
        assertThatThrownBy(() -> pruefung.pruefeUndKodiereNeu(new byte[0])).isInstanceOf(ApiFehler.class);
    }
}
