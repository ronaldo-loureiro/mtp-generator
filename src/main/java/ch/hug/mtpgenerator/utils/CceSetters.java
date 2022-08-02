package ch.hug.mtpgenerator.utils;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.husky.common.hl7cdar2.*;
import org.husky.emed.ch.cda.generated.artdecor.CdachEntryObservationMedia;
import org.husky.emed.ch.cda.generated.artdecor.CdachSectionOriginalRepresentationCoded;
import org.husky.emed.ch.cda.utils.CdaR2Utils;
import org.husky.emed.ch.cda.utils.TemplateIds;

import java.util.Base64;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

/**
 * Setter utilities for CDA-CH-EMED documents.
 *
 * @see CdaR2Utils
 **/
public class CceSetters {

    /**
     * This class is not instantiable.
     */
    private CceSetters() {
    }

    /**
     * Sets the narrative text in a CDA-CH-EMED document.
     *
     * @param document      The CDA-CH-EMED document.
     * @param narrativeText The narrative text
     */
    public static void narrativeText(final POCDMT000040ClinicalDocument document,
                                     final StrucDocText narrativeText) {
        Objects.requireNonNull(document, "document shall not be null in narrativeText()");
        Objects.requireNonNull(narrativeText, "narrativeText shall not be null in narrativeText()");

        final var section = Optional.ofNullable(document.getComponent())
            .map(POCDMT000040Component2::getStructuredBody)
            .map(POCDMT000040StructuredBody::getComponent)
            .orElseGet(Collections::emptyList)
            .stream()
            .map(POCDMT000040Component3::getSection)
            .filter(Objects::nonNull)
            .filter(sect -> !TemplateIds.isInList(TemplateIds.ORIGINAL_REPRESENTATION_SECTION, sect.getTemplateId()))
            .filter(sect -> !TemplateIds.isInList(TemplateIds.REMARKS_SECTION, sect.getTemplateId()))
            .findAny()
            .orElseThrow();
        section.setText(narrativeText);
    }

    /**
     * Sets the PDF representation in a CDA-CH-EMED document.
     *
     * @param document     The CDA-CH-EMED document.
     * @param pdfContent   The PDF content.
     * @param languageCode The language code of the PDF content or {@code null} if it isn't known.
     */
    public static void pdfRepresentation(final POCDMT000040ClinicalDocument document,
                                         final byte[] pdfContent,
                                         @Nullable final String languageCode) {
        Objects.requireNonNull(document, "document shall not be null in pdfRepresentation()");
        Objects.requireNonNull(pdfContent, "pdfContent shall not be null in pdfRepresentation()");

        final var components = Optional.ofNullable(document.getComponent())
            .map(POCDMT000040Component2::getStructuredBody)
            .map(POCDMT000040StructuredBody::getComponent)
            .orElseThrow();
        final var component = components.stream()
            .filter(comp -> comp.getSection() != null)
            .filter(comp -> TemplateIds.isInList(TemplateIds.ORIGINAL_REPRESENTATION_SECTION, comp.getSection().getTemplateId()))
            .findAny()
            .orElseGet(() -> {
                final var comp = new POCDMT000040Component3();
                components.add(comp);
                return comp;
            });
        final var section = new CdachSectionOriginalRepresentationCoded();
        section.setTitle(new ST("Représentation originale")); // Fixed value
        component.setSection(section);

        final var observationMedia = new CdachEntryObservationMedia();
        section.getEntry().get(0).setObservationMedia(observationMedia);
        observationMedia.setIDAttr("pdf");
        observationMedia.setValue(new ED(Base64.getEncoder().encodeToString(pdfContent)));
        observationMedia.getValue().setRepresentation(BinaryDataEncoding.B_64);
        observationMedia.getValue().setMediaType("application/pdf");
        if (languageCode != null) {
            observationMedia.setLanguageCode(new CS(languageCode));
        } else {
            observationMedia.getNullFlavor().add("UNK");
        }

        final var factory = new ObjectFactory();
        section.setText(new StrucDocText());
        final var renderMultiMedia = new StrucDocRenderMultiMedia();
        renderMultiMedia.getReferencedObject().add(observationMedia);
        section.getText().getContent().add("Représentation PDF du document:");
        section.getText().getContent().add(factory.createStrucDocTextRenderMultiMedia(renderMultiMedia));
    }
}
