package de.digitalbegleiter.gemeinsam;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Zufällige Schlüssel und Codes erzeugen und hashen. In der Datenbank liegt immer nur der Hash. */
public final class Schluessel {

    private static final SecureRandom ZUFALL = new SecureRandom();

    /** Ohne I, L, O, 0 und 1: Diese Zeichen verwechselt man leicht beim Abtippen. */
    private static final String CODE_ZEICHEN = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    private Schluessel() {
    }

    /** Langer, zufälliger Schlüssel (256 Bit), zum Beispiel für ein Gerät. */
    public static String neuerToken() {
        byte[] bytes = new byte[32];
        ZUFALL.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Kurzer Code zum Abtippen, zum Beispiel für eine Einladung. */
    public static String neuerCode(int laenge) {
        StringBuilder code = new StringBuilder(laenge);
        for (int i = 0; i < laenge; i++) {
            code.append(CODE_ZEICHEN.charAt(ZUFALL.nextInt(CODE_ZEICHEN.length())));
        }
        return code.toString();
    }

    public static String sha256(String wert) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(wert.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 fehlt", e);
        }
    }

    /** Vergleich, der immer gleich lange dauert (Schutz vor Zeitmessungs-Angriffen). */
    public static boolean gleich(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
