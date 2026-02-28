package edu.taylors.io.capstone.eservices.entity;

public enum FileCategory {
    PASSPORT("Passport"),
    VISA("Visa"),
    CERTIFICATE("Certificate"),
    TRANSCRIPT("Transcript"),
    ID_CARD("ID Card"),
    PHOTO("Photo"),
    MEDICAL("Medical Document"),
    INSURANCE("Insurance"),
    OTHER("Other");

    private final String displayName;

    FileCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}