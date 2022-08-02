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
import org.husky.emed.ch.cda.generated.artdecor.enums.MedicationDosageQualifier;
import org.husky.emed.ch.cda.xml.CceDocumentMarshaller;
import org.husky.emed.ch.enums.ActSubstanceAdminSubstitutionCode;
import org.openehealth.ipf.commons.ihe.xds.core.responses.RetrievedDocument;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.math.BigInteger;
import java.time.*;
import java.util.*;

import static ch.hug.mtpgenerator.models.enums.DosageInstructionsEnums.*;

public class OnDemandMedicationTreatmentPlanAssembler extends GenericCdaDocumentGenerator<MedicationTreatmentPlanDocument> {

    /**
     * The digester of CDA-CH-EMED documents.
     */
    private final CceDocumentDigester cceDocumentDigester;

    /**
     * Constructor.
     */
    public OnDemandMedicationTreatmentPlanAssembler(final CceDocumentDigester cceDocumentDigester) throws Exception {
        super();
        this.cceDocumentDigester = Objects.requireNonNull(cceDocumentDigester);
    }

    public RetrievedDocument assemble(final MtpModel medicationTreatment) throws Exception {
        final UUID mtpUniqueId = UUID.fromString("c11263f1-0001-0050-0001-000000000000");
        final var mtpDocument = this.createCceMtp(mtpUniqueId, medicationTreatment);

        final String mtpDocumentXml = CceDocumentMarshaller.marshall(mtpDocument);

        System.out.println(mtpDocumentXml);

        return new RetrievedDocument();
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
        switch (medicationTreatment.getDosageInstructionType()) {
            case NORMAL, NARRATIVE -> sa.getTemplateId()
                    .add(MedicationTreatmentPlanEntryContentModule.getPredefinedTemplateId136141193761531471());
            case SPLIT -> sa.getTemplateId()
                    .add(MedicationTreatmentPlanEntryContentModule.getPredefinedTemplateId13614119376153149());
            default -> throw new RuntimeException("The dosage instructions type is unknown");
        }

        sa.setHl7Id(new II(documentUniqueId.toString(), null));
        sa.setText(new ED(null, new TEL(narrativeTextId)));

        if (medicationTreatment.getDosageInstructionType() != NARRATIVE) {
            final var ivlts = new IVLTS();

            final Instant startDate = medicationTreatment.getStart_date().isEmpty() ? null :
                    LocalDate.parse(medicationTreatment.getStart_date()).atStartOfDay().atZone(ZoneId.of("Europe/Zurich")).toInstant();

            final Instant stopDate = medicationTreatment.getStop_date().isEmpty() ? null :
                    LocalDate.parse(medicationTreatment.getStop_date()).atStartOfDay().atZone(ZoneId.of("Europe/Zurich")).toInstant();

            final var low = Optional.ofNullable(startDate)
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

            final var high = Optional.ofNullable(stopDate)
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

        switch (medicationTreatment.getDosageInstructionType()) {
            case NORMAL -> {
                var timings = medicationTreatment.getDoses_quantity().entrySet().stream()
                        .filter(s -> !s.getValue().equals("0"))
                        .distinct()
                        .toList();

                if (timings.isEmpty()) {
                    throw new RuntimeException("Encountered a normal dosage instructions without timing or " +
                            "quantity");
                }

                sa.setDoseQuantity(new IVLPQ(timings.get(0).getValue(), medicationTreatment.getDose_quantity_unit().getCodeValue()));

                if (timings.size() == 1) {
                    final var eivlts = DosageInstructionsStartStopFrequency.getPredefinedEffectiveTimeA();
                    final var eivlEvent = new EIVLEvent();
                    eivlEvent.setCode(timings.get(0).getKey().getCodeValue());
                    eivlts.setEvent(eivlEvent);
                    sa.getEffectiveTime().add(eivlts);
                } else {
                    final var sxprts = new SXPRTS();
                    sxprts.setOperator(SetOperator.A);
                    for (final var timing : timings) {
                        final var eivlts = new EIVLTS();
                        final var eivlEvent = new EIVLEvent();
                        eivlEvent.setCode(timing.getKey().getCodeValue());
                        eivlts.setEvent(eivlEvent);
                        if (!sxprts.getComp().isEmpty()) {
                            eivlts.setOperator(SetOperator.I);
                        }
                        sxprts.getComp().add(eivlts);
                    }
                    sa.getEffectiveTime().add(sxprts);
                }
            }
            case SPLIT -> {
                var i = 1;
                for (final var intake : medicationTreatment.getDoses_quantity().entrySet()) {
                    if (!intake.getValue().equals("0")) {
                        final POCDMT000040EntryRelationship erDosageInstr =
                                MedicationTreatmentPlanEntryContentModule.getPredefinedEntryRelationshipCompNull();
                        final var sequence = new INT();
                        sequence.setValue(BigInteger.valueOf(i++));
                        erDosageInstr.setSequenceNumber(sequence);
                        final POCDMT000040SubstanceAdministration sa2 = new POCDMT000040SubstanceAdministration();
                        sa2.setMoodCode(XDocumentSubstanceMood.INT);
                        sa2.getClassCode().add("SBADM");

                        final var eivlts = DosageInstructionsStartStopFrequency.getPredefinedEffectiveTimeA();
                        final var eivlEvent = new EIVLEvent();
                        eivlEvent.setCode(intake.getKey().getCodeValue());
                        eivlts.setEvent(eivlEvent);
                        sa2.getEffectiveTime().add(eivlts);

                        sa2.setDoseQuantity(new IVLPQ(intake.getValue(), medicationTreatment.getDose_quantity_unit().getCodeValue()));


                        sa2.setConsumable(this.createEmptyConsumable());
                        erDosageInstr.setSubstanceAdministration(sa2);
                        sa.getEntryRelationship().add(erDosageInstr);
                    }
                }
            }
            case NARRATIVE -> {
                final var saNarrative = new DosageInstructionsNonStructuredEntryContentModule();
                saNarrative.setText(new ED(
                        medicationTreatment.getNarrative_instructions(),
                        new TEL(narrativeTextId)
                ));
                final var erNarrative = new POCDMT000040EntryRelationship();
                erNarrative.setTypeCode(XActRelationshipEntryRelationship.COMP);
                erNarrative.setSubstanceAdministration(saNarrative);
                sa.getEntryRelationship().add(erNarrative);
            }
        }

        // Treatment reason
        if (!medicationTreatment.getReason().isEmpty()) {
            final var erTreatmentReason = new POCDMT000040EntryRelationship();
            erTreatmentReason.setTypeCode(XActRelationshipEntryRelationship.RSON);
            final var obsTreatmentReason = new TreatmentReasonEntryContentModule();
            obsTreatmentReason.setText(new ED(medicationTreatment.getReason(), new TEL(narrativeTextId)));
            obsTreatmentReason.setStatusCode(new CS("completed"));
            erTreatmentReason.setObservation(obsTreatmentReason);
            sa.getEntryRelationship().add(erTreatmentReason);
        }

        // Fulfilment instructions
        if (!medicationTreatment.getFulfillment_instruction().isEmpty()) {
            final var erFulfilmentInstructions = new POCDMT000040EntryRelationship();
            erFulfilmentInstructions.setTypeCode(XActRelationshipEntryRelationship.SUBJ);
            erFulfilmentInstructions.setInversionInd(true);
            final var actFulfilmentInstructions = new IhemedicationFulFillmentInstructions();
            actFulfilmentInstructions.setText(new ED(medicationTreatment.getFulfillment_instruction(), new TEL(narrativeTextId)));
            erFulfilmentInstructions.setAct(actFulfilmentInstructions);
        }

        // Quantity to dispense
        if (!medicationTreatment.getAmount_to_dispense().isEmpty()) {
            final var qdSupply = new PrescribedQuantityEntryContentModule();
            qdSupply.setQuantity(new PQ(
                    medicationTreatment.getAmount_to_dispense(),
                    "1"
            ));
            final var qdEr = new POCDMT000040EntryRelationship();
            qdEr.setTypeCode(XActRelationshipEntryRelationship.COMP);
            qdEr.setSupply(qdSupply);
            sa.getEntryRelationship().add(qdEr);
        }

        // Substitution permissions
        final var substitutionPermission = medicationTreatment.isSubstitution_authorized() ?
                ActSubstanceAdminSubstitutionCode.EQUIVALENT_L1 : ActSubstanceAdminSubstitutionCode.NONE_L1;
        final POCDMT000040Act spAct = new IhesubstitutionPermissionContentModule();
        spAct.setCode(substitutionPermission.getCD());
        spAct.setStatusCode(new CS("completed"));
        final var spEr = new POCDMT000040EntryRelationship();
        spEr.setTypeCode(XActRelationshipEntryRelationship.COMP);
        spEr.setAct(spAct);
        sa.getEntryRelationship().add(spEr);


        // In reserve
        final var inReserve = (medicationTreatment.isIn_reserve()) ?
                MedicationDosageQualifier.AS_REQUIRED_QUALIFIER_VALUE :
                MedicationDosageQualifier.REGULAR_QUALIFIER_VALUE;
        final var irAct = new MedicationInReserveEntryContentModule();
        irAct.setStatusCode(MedicationInReserveEntryContentModule.getPredefinedStatusCodeCompleted());
        irAct.setCode(inReserve.getCD());
        final var irEr = new POCDMT000040EntryRelationship();
        irEr.setTypeCode(XActRelationshipEntryRelationship.COMP);
        irEr.setAct(irAct);
        sa.getEntryRelationship().add(irEr);

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

    /**
     * Creates an empty consumable.
     */
    protected POCDMT000040Consumable createEmptyConsumable() {
        final var consumable = new POCDMT000040Consumable();
        final var manufacturedProduct = new POCDMT000040ManufacturedProduct();
        final var manufacturedMaterial = new POCDMT000040Material();
        manufacturedMaterial.getNullFlavor().add("NA");
        manufacturedProduct.setManufacturedMaterial(manufacturedMaterial);
        consumable.setManufacturedProduct(manufacturedProduct);
        return consumable;
    }
}
