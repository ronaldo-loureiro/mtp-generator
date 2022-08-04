package ch.hug.mtpgenerator.models.enums;

import lombok.Getter;

public enum MtpType {
    structured_instructions("Structured instructions"),
    narrative_instructions("Narrative instructions");

    @Getter
    private String label;

    MtpType(String label) {
        this.label = label;
    }
}
