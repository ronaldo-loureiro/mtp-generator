package ch.hug.mtpgenerator.ondemand;

import ch.hug.mtpgenerator.commons.specs.Hl7Urns;
import ch.hug.mtpgenerator.generators.GenericCdaDocumentGenerator;
import ch.hug.mtpgenerator.models.MtpModel;
import ch.hug.mtpgenerator.models.enums.ProductCodeType;
import ch.hug.mtpgenerator.utils.CceSetters;
import org.husky.common.enums.CodeSystems;
import org.husky.common.hl7cdar2.*;
import org.husky.common.utils.time.DateTimes;
import org.husky.emed.ch.cda.generated.artdecor.*;
import org.husky.emed.ch.cda.generated.artdecor.enums.MedicationDosageQualifier;
import org.husky.emed.ch.cda.xml.CceDocumentMarshaller;
import org.husky.emed.ch.enums.ActSubstanceAdminSubstitutionCode;
import org.husky.emed.ch.enums.CceDocumentType;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static ch.hug.mtpgenerator.models.enums.DosageInstructionsEnums.NARRATIVE;

public class OnDemandMedicationTreatmentPlanAssembler extends GenericCdaDocumentGenerator<MedicationTreatmentPlanDocument> {

    /**
     * Constructor.
     */
    public OnDemandMedicationTreatmentPlanAssembler() throws Exception {
        super();
    }

    /**
     * Assembles a medication treatment plan from a MtpModel to a CDA-CH-EMED
     * @param medicationTreatment The information necessary to generate the mtp
     * @return String of the mtp generated
     * @throws Exception
     */
    public String assemble(final MtpModel medicationTreatment) throws Exception {
        final UUID mtpUniqueId = UUID.fromString("c11263f1-0001-0050-0001-000000000000");
        final var mtpDocument = this.createCceMtp(mtpUniqueId, medicationTreatment);

        final String mtpDocumentXml = CceDocumentMarshaller.marshall(mtpDocument);

        this.cceStructureValidator.validate(mtpDocumentXml, CceDocumentType.MTP);

        return mtpDocumentXml;
    }

    /**
     * Creates a CDA-CH-EMED MTP document from a MtpModel
     *
     * @param documentUniqueId  The document unique id
     * @param medicationTreatment The information necessary to generate the MTP
     * @return The generated CDA-CH-EMED MTP document
     */
    MedicationTreatmentPlanDocument createCceMtp(final UUID documentUniqueId, MtpModel medicationTreatment) {
        final var document = new MedicationTreatmentPlanDocument();
        this.initializeCceDocumentHeader(
            document,
            documentUniqueId,
            new DocumentCodeMedicationTreatmentPlan(),
            "Décision thérapeutique relative à la médication",
            "2.998",
            medicationTreatment.getDocumentLanguageCode()
        );

        final var section = new MedicationTreatmenPlanSectionContentModule();
        this.initializeCceDocumentBody(document, section);
        section.setId(new II(documentUniqueId.toString()));
        section.setTitle(new ST("Plan de traitement médicamenteux", medicationTreatment.getDocumentLanguageCode().getCodeValue()));

        // Generate narrative text
        final var narrativeText = new StrucDocText();
        final var narrativeTextId = "ref";
        narrativeText.setID(narrativeTextId);
        narrativeText.getContent().add("No narrative provided yet.");
        CceSetters.narrativeText(document, narrativeText);

        final var entry = new POCDMT000040Entry();
        entry.setSubstanceAdministration(createEntryMTP(medicationTreatment, "#" + narrativeTextId));
        section.setHl7Entry(entry);

        return document;
    }

    protected MedicationTreatmentPlanEntryContentModule createEntryMTP(MtpModel medicationTreatment, String narrativeTextId) {
        final var substanceAdministration = new MedicationTreatmentPlanEntryContentModule();
        substanceAdministration.setMoodCode(XDocumentSubstanceMood.INT); // SPEC: get original
        switch (medicationTreatment.getDosageInstructionType()) {
            case NORMAL, NARRATIVE -> substanceAdministration.getTemplateId()
                    .add(MedicationTreatmentPlanEntryContentModule.getPredefinedTemplateId136141193761531471());
            case SPLIT -> substanceAdministration.getTemplateId()
                    .add(MedicationTreatmentPlanEntryContentModule.getPredefinedTemplateId13614119376153149());
            default -> throw new RuntimeException("The dosage instructions type is unknown");
        }

        substanceAdministration.setHl7Id(new II(medicationTreatment.getUuid(), null));
        substanceAdministration.setText(new ED(null, new TEL(narrativeTextId)));

        if (medicationTreatment.getDosageInstructionType() != NARRATIVE) {
            final var ivlts = new IVLTS();

            final Instant startDate = medicationTreatment.getStartDate().isEmpty() ? null :
                    LocalDate.parse(medicationTreatment.getStartDate()).atStartOfDay().atZone(ZoneId.of("Europe/Zurich")).toInstant();

            final Instant stopDate = medicationTreatment.getStopDate().isEmpty() ? null :
                    LocalDate.parse(medicationTreatment.getStopDate()).atStartOfDay().atZone(ZoneId.of("Europe/Zurich")).toInstant();

            final var low = Optional.ofNullable(startDate)
                    .map(DateTimes::toDateTs)
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
                    .map(DateTimes::toDateTs)
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

            substanceAdministration.getEffectiveTime().add(ivlts);

            if (medicationTreatment.getRouteCode() != null) {
                substanceAdministration.setRouteCode(medicationTreatment.getRouteCode().getCE(medicationTreatment.getDocumentLanguageCode()));
            }
        }

        // Repeat number
        final var repeatNumber = new IVLINT();
        if (medicationTreatment.getRepeatNumber() != null && medicationTreatment.getRepeatNumber() >= 0) {
            repeatNumber.setValue(BigInteger.valueOf(medicationTreatment.getRepeatNumber()));
        } else {
            repeatNumber.getNullFlavor().add("NI");
        }
        substanceAdministration.setRepeatNumber(repeatNumber);

        substanceAdministration.setConsumable(generateConsumable(medicationTreatment));

        switch (medicationTreatment.getDosageInstructionType()) {
            case NORMAL -> {
                var timings = medicationTreatment.getDosesQuantity().entrySet().stream()
                        .filter(s -> !s.getValue().equals("0"))
                        .distinct()
                        .toList();

                if (timings.isEmpty()) {
                    throw new RuntimeException("Encountered a normal dosage instructions without timing or " +
                            "quantity");
                }

                substanceAdministration.setDoseQuantity(new IVLPQ(timings.get(0).getValue(), medicationTreatment.getDoseQuantityUnit().getCodeValue()));

                if (timings.size() == 1) {
                    final var eivlts = DosageInstructionsStartStopFrequency.getPredefinedEffectiveTimeA();
                    final var eivlEvent = new EIVLEvent();
                    eivlEvent.setCode(timings.get(0).getKey().getCodeValue());
                    eivlts.setEvent(eivlEvent);
                    substanceAdministration.getEffectiveTime().add(eivlts);
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
                    substanceAdministration.getEffectiveTime().add(sxprts);
                }
            }
            case SPLIT -> {
                var i = 1;
                for (final var intake : medicationTreatment.getDosesQuantity().entrySet()) {
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

                        sa2.setDoseQuantity(new IVLPQ(intake.getValue(), medicationTreatment.getDoseQuantityUnit().getCodeValue()));

                        sa2.setConsumable(this.createEmptyConsumable());
                        erDosageInstr.setSubstanceAdministration(sa2);
                        substanceAdministration.getEntryRelationship().add(erDosageInstr);
                    }
                }
            }
            case NARRATIVE -> {
                final var saNarrative = new DosageInstructionsNonStructuredEntryContentModule();
                saNarrative.setText(new ED(
                        medicationTreatment.getNarrativeInstructions(),
                        new TEL(narrativeTextId)
                ));
                final var erNarrative = new POCDMT000040EntryRelationship();
                erNarrative.setTypeCode(XActRelationshipEntryRelationship.COMP);
                erNarrative.setSubstanceAdministration(saNarrative);
                substanceAdministration.getEntryRelationship().add(erNarrative);
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
            substanceAdministration.getEntryRelationship().add(erTreatmentReason);
        }

        // Fulfilment instructions
        if (!medicationTreatment.getFulfillmentInstructions().isEmpty()) {
            final var erFulfilmentInstructions = new POCDMT000040EntryRelationship();
            erFulfilmentInstructions.setTypeCode(XActRelationshipEntryRelationship.SUBJ);
            erFulfilmentInstructions.setInversionInd(true);

            final var actFulfilmentInstructions = new IhemedicationFulFillmentInstructions();
            actFulfilmentInstructions.setText(new ED(medicationTreatment.getFulfillmentInstructions(), new TEL(narrativeTextId)));
            actFulfilmentInstructions.setStatusCode(new CS("completed"));

            erFulfilmentInstructions.setAct(actFulfilmentInstructions);
            substanceAdministration.getEntryRelationship().add(erFulfilmentInstructions);
        }

        // Patient medication instructions
        if (!medicationTreatment.getPatientInstructions().isEmpty()) {
            final var erPatientInstructions = new POCDMT000040EntryRelationship();
            erPatientInstructions.setTypeCode(XActRelationshipEntryRelationship.SUBJ);
            erPatientInstructions.setInversionInd(true);

            final var actPatientInstructions = new IhepatientMedicationInstructions();
            actPatientInstructions.setText(new ED(medicationTreatment.getPatientInstructions(), new TEL(narrativeTextId)));
            actPatientInstructions.setStatusCode(new CS("completed"));

            erPatientInstructions.setAct(actPatientInstructions);
            substanceAdministration.getEntryRelationship().add(erPatientInstructions);
        }

        // Quantity to dispense
        if (!medicationTreatment.getAmountToDispense().isEmpty()) {
            final var qdSupply = new PrescribedQuantityEntryContentModule();
            qdSupply.setQuantity(new PQ(
                    medicationTreatment.getAmountToDispense(),
                    "1"
            ));
            final var qdEr = new POCDMT000040EntryRelationship();
            qdEr.setTypeCode(XActRelationshipEntryRelationship.COMP);
            qdEr.setSupply(qdSupply);
            substanceAdministration.getEntryRelationship().add(qdEr);
        }

        substanceAdministration.getEntryRelationship().add(createSubstitutionPermissions(medicationTreatment));
        substanceAdministration.getEntryRelationship().add(createInReserve(medicationTreatment));

        return substanceAdministration;
    }

    /**
     * Generates a CDA-CH-EMED consumable element from an internal medication product.
     *
     * @param medicationTreatment The medication treatment.
     * @return the created consumable element.
     */
    protected POCDMT000040Consumable generateConsumable(final MtpModel medicationTreatment) {
        final var manufacturedMaterial = new ManufacturedMaterialEntryContentModule();

        if (medicationTreatment.getProductCode() != null) {
            CE productCE = medicationTreatment.getProductCodeType() == ProductCodeType.GTIN ?
                    new CE(medicationTreatment.getProductCode(), CodeSystems.GTIN.getCodeSystemId(),
                    CodeSystems.GTIN.getCodeSystemName(), medicationTreatment.getProductName())
                    :
                    new CE(medicationTreatment.getProductCode(), CodeSystems.WHO_ATC_CODE.getCodeSystemId(),
                    CodeSystems.WHO_ATC_CODE.getCodeSystemName(), medicationTreatment.getProductName());

            manufacturedMaterial.setCode(productCE);

        } else {
            final var ce = new CE();
            ce.getNullFlavor().add("NA");
            manufacturedMaterial.setCode(ce);
        }

        if (medicationTreatment.getProductName() != null) {
            manufacturedMaterial.setName(new EN(medicationTreatment.getProductName()));
        } else {
            manufacturedMaterial.setName(new EN());
            manufacturedMaterial.getName().getNullFlavor().add("NA");
        }

        if (medicationTreatment.getFormCode() != null) {
            manufacturedMaterial.setPharmFormCode(medicationTreatment.getFormCode().getCE(medicationTreatment.getDocumentLanguageCode()));
        }

        final var manufacturedProduct = new POCDMT000040ManufacturedProduct();
        manufacturedProduct.setManufacturedMaterial(manufacturedMaterial);
        manufacturedProduct.setClassCode(RoleClassManufacturedProduct.MANU);
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

    protected POCDMT000040EntryRelationship createSubstitutionPermissions(MtpModel medicationTreatment) {
        final ActSubstanceAdminSubstitutionCode substitutionPermission = medicationTreatment.isSubstitutionAuthorized() ?
                ActSubstanceAdminSubstitutionCode.EQUIVALENT_L1 : ActSubstanceAdminSubstitutionCode.NONE_L1;
        final POCDMT000040Act spAct = new IhesubstitutionPermissionContentModule();
        spAct.setCode(substitutionPermission.getCD(medicationTreatment.getDocumentLanguageCode()));
        spAct.setStatusCode(new CS("completed"));
        final var spEr = new POCDMT000040EntryRelationship();
        spEr.setTypeCode(XActRelationshipEntryRelationship.COMP);
        spEr.setAct(spAct);
        return spEr;
    }

    protected POCDMT000040EntryRelationship createInReserve(MtpModel medicationTreatment) {
        final var inReserve = medicationTreatment.isInReserve() ?
                MedicationDosageQualifier.AS_REQUIRED_QUALIFIER_VALUE : MedicationDosageQualifier.REGULAR_QUALIFIER_VALUE;
        final var irAct = new MedicationInReserveEntryContentModule();
        irAct.setStatusCode(MedicationInReserveEntryContentModule.getPredefinedStatusCodeCompleted());
        irAct.setCode(inReserve.getCD());
        final var irEr = new POCDMT000040EntryRelationship();
        irEr.setTypeCode(XActRelationshipEntryRelationship.COMP);
        irEr.setAct(irAct);
        return irEr;
    }
}
