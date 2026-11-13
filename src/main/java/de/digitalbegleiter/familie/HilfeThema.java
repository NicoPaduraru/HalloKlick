package de.digitalbegleiter.familie;

/** Die Themen, aus denen ein Senior bei "Familie fragen" wählen kann. */
public enum HilfeThema {
    KOMISCHE_NACHRICHT("Ich habe eine komische Nachricht bekommen"),
    HANDY_HILFE("Ich komme am Handy nicht weiter"),
    BITTE_ANRUFEN("Bitte ruf mich an"),
    ETWAS_ANDERES("Etwas anderes");

    private final String text;

    HilfeThema(String text) {
        this.text = text;
    }

    public String text() {
        return text;
    }
}
