package ch.hug.mtpgenerator.ondemand;

import ch.hug.mtpgenerator.commons.specs.Hl7Urns;
import ch.hug.mtpgenerator.generators.GenericCdaDocumentGenerator;
import ch.hug.mtpgenerator.models.MtpModel;
import ch.hug.mtpgenerator.utils.CceSetters;
import org.husky.common.enums.CodeSystems;
import org.husky.common.hl7cdar2.*;
import org.husky.common.utils.time.DateTimes;
import org.husky.emed.ch.cda.digesters.CceDocumentDigester;
import org.husky.emed.ch.cda.generated.artdecor.*;
import org.husky.emed.ch.enums.TimingEventAmbu;
import org.husky.emed.ch.models.common.MedicationDosageIntake;
import org.openehealth.ipf.commons.ihe.xds.core.responses.RetrievedDocument;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static ch.hug.mtpgenerator.models.enums.DosageInstructionsEnums.*;

public class OnDemandMedicationTreatmentPlanAssembler extends GenericCdaDocumentGenerator<MedicationTreatmentPlanDocument> {

    /**
     * The digester of CDA-CH-EMED documents.
     */
    private final CceDocumentDigester cceDocumentDigester;

    /**
     * Constructor.
     */
    protected OnDemandMedicationTreatmentPlanAssembler(final CceDocumentDigester cceDocumentDigester) throws Exception {
        super();
        this.cceDocumentDigester = Objects.requireNonNull(cceDocumentDigester);
    }

    public RetrievedDocument assemble() {
        final UUID mtpUniqueId = UUID.fromString("c11263f1-0001-0050-0001-000000000000");

        // ...
    }

    MedicationTreatmentPlanDocument createCceMtp(final UUID documentUniqueId, MtpModel medicationTreatment) throws Exception {
        final var document = new MedicationTreatmentPlanDocument();
        this.initializeCceDocumentHeader(
            document,
            documentUniqueId,
            new DocumentCodeMedicationTreatmentPlan(),
            "Décision thérapeutique relative à la médication",
                "2.998"
        );

        final var section = new MedicationTreatmenPlanSectionContentModule();
        this.initializeCceDocumentBody(document, section);
        section.setTitle(new ST("Plan de traitement médicamenteux", "fr-CH"));

        // Generate narrative text
        final var narrativeText = new StrucDocText();
        final var narrativeTextId = "narrative";
        narrativeText.setID(narrativeTextId);
        narrativeText.getContent().add("See the PDF original representation");
        CceSetters.narrativeText(document, narrativeText);

        final var sa = new MedicationTreatmentPlanEntryContentModule();
        sa.setMoodCode(XDocumentSubstanceMood.INT); // SPEC: get original
        switch (medicationTreatment.getDosage_instructions_type()) {
            case NORMAL, NARRATIVE -> sa.getTemplateId()
                    .add(MedicationTreatmentPlanEntryContentModule.getPredefinedTemplateId136141193761531471());
            case SPLIT -> sa.getTemplateId()
                    .add(MedicationTreatmentPlanEntryContentModule.getPredefinedTemplateId13614119376153149());
            default -> throw new RuntimeException("The dosage instructions type is unknown");
        }

        sa.setHl7Id(new II(documentUniqueId.toString(), null));
        sa.setText(new ED(null, new TEL(narrativeTextId)));

        if (medicationTreatment.getDosage_instructions_type() != NARRATIVE) {
            final var ivlts = new IVLTS();
            final var low = Optional.ofNullable(medicationTreatment.getStart_date())
                    .map(DateTimes::toDatetimeTs)
                    .orElseGet(() -> {
                        final var ts = new TS();
                        ts.getNullFlavor().add("UNK");
                        return ts;
                    });
            ivlts.getRest().add(new JAXBElement<>(
                    new QName(Hl7Urns.HL7V3_URN, "low"),
                    TS.class,
                    low
            ));

            final var high = Optional.ofNullable(medicationTreatment.getStart_date())
                    .map(DateTimes::toDatetimeTs)
                    .orElseGet(() -> {
                        final var ts = new TS();
                        ts.getNullFlavor().add("UNK");
                        return ts;
                    });
            ivlts.getRest().add(new JAXBElement<>(
                    new QName(Hl7Urns.HL7V3_URN, "high"),
                    TS.class,
                    high
            ));

            sa.getEffectiveTime().add(ivlts);
        }

        final var repeatNumber = new IVLINT();
        if (medicationTreatment.getRepeat_number() != null && medicationTreatment.getRepeat_number() >= 0) {
            repeatNumber.setValue(BigInteger.valueOf(medicationTreatment.getRepeat_number()));
        } else {
            repeatNumber.getNullFlavor().add("NI");
        }
        sa.setRepeatNumber(repeatNumber);

        if (medicationTreatment.getRoute_code() != null) {
            sa.setRouteCode(medicationTreatment.getRoute_code().getCE());
        }

        sa.setConsumable(generateConsumable(medicationTreatment));

//        switch (medicationTreatment.getDosage_instructions_type()) {
//            case NORMAL -> {
//                if (timings.isEmpty()) {
//                    throw new RuntimeException("Encountered a normal dosage instructions without timing or " +
//                            "quantity");
//                }
//
//                final var dose = medicationTreatment.getDosageInstructions().getIntakes().get(0).getDoseQuantity();
//                sa.setDoseQuantity(new IVLPQ(dose.getValue(), dose.getUnit().getCodeValue()));
//
//                if (timings.size() == 1) {
//                    final var eivlts = DosageInstructionsStartStopFrequency.getPredefinedEffectiveTimeA();
//                    final var eivlEvent = new EIVLEvent();
//                    eivlEvent.setCode(timings.get(0).getCodeValue());
//                    eivlts.setEvent(eivlEvent);
//                    sa.getEffectiveTime().add(eivlts);
//                } else {
//                    final var sxprts = new SXPRTS();
//                    sxprts.setOperator(SetOperator.A);
//                    for (final var timing : timings) {
//                        final var eivlts = new EIVLTS();
//                        final var eivlEvent = new EIVLEvent();
//                        eivlEvent.setCode(timing.getCodeValue());
//                        eivlts.setEvent(eivlEvent);
//                        if (!sxprts.getComp().isEmpty()) {
//                            eivlts.setOperator(SetOperator.I);
//                        }
//                        sxprts.getComp().add(eivlts);
//                    }
//                    sa.getEffectiveTime().add(sxprts);
//                }
//            }
//            case SPLIT -> {
//                var i = 1;
//                for (final var intake : medicationTreatment.getDosageInstructions().getIntakes()) {
//                    final POCDMT000040EntryRelationship erDosageInstr =
//                            MedicationTreatmentPlanEntryContentModule.getPredefinedEntryRelationshipCompNull();
//                    final var sequence = new INT();
//                    sequence.setValue(BigInteger.valueOf(i++));
//                    erDosageInstr.setSequenceNumber(sequence);
//                    final POCDMT000040SubstanceAdministration sa2 = new POCDMT000040SubstanceAdministration();
//                    sa2.setMoodCode(XDocumentSubstanceMood.INT);
//                    sa2.getClassCode().add("SBADM");
//                    if (intake.getEventTiming() != null) {
//                        final var eivlts = DosageInstructionsStartStopFrequency.getPredefinedEffectiveTimeA();
//                        final var eivlEvent = new EIVLEvent();
//                        eivlEvent.setCode(intake.getEventTiming().getCodeValue());
//                        eivlts.setEvent(eivlEvent);
//                        sa2.getEffectiveTime().add(eivlts);
//                    }
//                    if (intake.getDoseQuantity() != null) {
//                        sa2.setDoseQuantity(new IVLPQ(intake.getDoseQuantity().getValue(), intake.getDoseQuantity().getUnit().getCodeValue()));
//                    }
//
//                    sa2.setConsumable(this.createEmptyConsumable());
//                    erDosageInstr.setSubstanceAdministration(sa2);
//                    sa.getEntryRelationship().add(erDosageInstr);
//                }
//            }
//            case NARRATIVE -> {
//                final var saNarrative = new DosageInstructionsNonStructuredEntryContentModule();
//                saNarrative.setText(new ED(
//                        medicationTreatment.getDosageInstructions().getNarrativeDosageInstructions(),
//                        new TEL(narrativeTextId)
//                ));
//                final var erNarrative = new POCDMT000040EntryRelationship();
//                erNarrative.setTypeCode(XActRelationshipEntryRelationship.COMP);
//                erNarrative.setSubstanceAdministration(saNarrative);
//                sa.getEntryRelationship().add(erNarrative);
//            }
//        }


        return document;
    }

    protected POCDMT000040Consumable generateConsumable(final MtpModel medicationTreatment) {
        final var manufacturedMaterial = new ManufacturedMaterialEntryContentModule();

        if (medicationTreatment.getProduct_code() != null) {
            manufacturedMaterial.setCode(new CE(
                    medicationTreatment.getProduct_code(),
                    CodeSystems.GTIN.getCodeSystemId(),
                    CodeSystems.GTIN.getCodeSystemName(),
                    medicationTreatment.getProduct_name()
            ));
        } else {
            final var ce = new CE();
            ce.getNullFlavor().add("NA");
            manufacturedMaterial.setCode(ce);
        }
        if (medicationTreatment.getProduct_name() != null) {
            manufacturedMaterial.setName(new EN(medicationTreatment.getProduct_name()));
        } else {
            manufacturedMaterial.setName(new EN());
            manufacturedMaterial.getName().getNullFlavor().add("NA");
        }

        if (medicationTreatment.getForm_code() != null) {
            manufacturedMaterial.setFormCode(medicationTreatment.getForm_code().getCE());
        }

        final var manufacturedProduct = new POCDMT000040ManufacturedProduct();
        manufacturedProduct.setManufacturedMaterial(manufacturedMaterial);
        manufacturedProduct.getTemplateId().add(new II("1.3.6.1.4.1.19376.1.5.3.1.4.7.2", null));
        manufacturedProduct.getTemplateId().add(new II("2.16.840.1.113883.10.20.1.53", null));

        final var consumable = new POCDMT000040Consumable();
        consumable.setManufacturedProduct(manufacturedProduct);
        return consumable;
    }
}
