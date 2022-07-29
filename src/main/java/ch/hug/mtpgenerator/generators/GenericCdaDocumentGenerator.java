package ch.hug.mtpgenerator.generators;

import ch.hug.mtpgenerator.commons.specs.Hl7Urns;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.husky.common.ch.enums.ConfidentialityCode;
import org.husky.common.ch.enums.ParticipationFunction;
import org.husky.common.enums.AdministrativeGender;
import org.husky.common.enums.CodeSystems;
import org.husky.common.enums.NullFlavor;
import org.husky.common.hl7cdar2.*;
import org.husky.common.utils.time.DateTimes;
import org.husky.emed.ch.cda.generated.artdecor.CdachHeaderCustodian;
import org.husky.emed.ch.cda.generated.artdecor.CdachHeaderInformationRecipient;
import org.husky.emed.ch.cda.generated.artdecor.CdachHeaderPatient;
import org.husky.emed.ch.cda.generated.artdecor.CdachOtherAuthor;
import org.husky.emed.ch.cda.validation.CdaChEmedValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * Abstract generator of CDA-CH-EMED documents. It contains helper methods for the derived generators.
 *
 * @author Quentin Ligier
 */
public abstract class GenericCdaDocumentGenerator<T extends POCDMT000040ClinicalDocument> {
    private static final Logger log = LoggerFactory.getLogger(GenericCdaDocumentGenerator.class);

    /**
     * Validator of the structure of CDA-CH-EMED documents.
     */
    protected final CdaChEmedValidator cceStructureValidator;


    /**
     * Constructor.
     *
     */
    protected GenericCdaDocumentGenerator() throws Exception {
        try {
            this.cceStructureValidator = new CdaChEmedValidator();
        } catch (final IOException | SAXException | XPathExpressionException exception) {
            log.error("Error while initializing the Husky CCE validator");
            throw new Exception("Error in the Husky library");
        }
    }

    protected void initializeCceDocumentHeader(final T document,
                                               final UUID uniqueId,
                                               final CE documentCode,
                                               final String documentTitle,
                                               final String patientMpiPid) {
        final var documentId = new II(uniqueId.toString());
        final var author = this.author(Instant.now());
        final var custodian = this.custodian();
        final POCDMT000040RecordTarget recordTarget;
        recordTarget = this.recordTarget(
                patientMpiPid,
                "Potter",
                "Harry",
                LocalDate.of(1987, 1, 1),
                AdministrativeGender.MALE);

        final var informationRecipient = this.informationRecipient();

        document.getRealmCode().add(new CS("CHE"));
        document.setId(documentId);
        document.setCode(documentCode);
        document.setTitle(new ST(documentTitle, "fr-CH"));
        document.setEffectiveTime(DateTimes.toDatetimeTs(Instant.now()));
        document.setConfidentialityCode(ConfidentialityCode.NORMALLY_ACCESSIBLE.getCE());
        document.setLanguageCode(new CS("fr-CH"));
        document.setSetId(documentId);
        document.setVersionNumber(new INT(1));
        document.getRecordTarget().add(recordTarget);
        document.getAuthor().add(author);
        document.setCustodian(custodian);
        document.getInformationRecipient().add(informationRecipient);
    }

    protected void initializeCceDocumentBody(final T document,
                                             final POCDMT000040Section section) {
        final var component2 = new POCDMT000040Component2();
        document.setComponent(component2);

        final var structuredBody = new POCDMT000040StructuredBody();
        component2.setStructuredBody(structuredBody);

        final var component3 = new POCDMT000040Component3();
        structuredBody.getComponent().add(component3);
        component3.setSection(section);
    }

    /**
     * Creates the 'RecordTarget' structure of a patient.
     *
     * @param patientMpiPid    The patient MPI-PID value.
     * @param familyName       The patient family name or {@code null} if it's not known.
     * @param givenName        The patient given name or {@code null} if it's not known.
     * @param patientBirthDate The patient birth date or {@code null} if it's not known.
     * @param patientGender    The patient gender or {@code null} if it's not known.
     * @return the created record target.
     */
    protected POCDMT000040RecordTarget recordTarget(final String patientMpiPid,
                                                    @Nullable final String familyName,
                                                    @Nullable final String givenName,
                                                    @Nullable final LocalDate patientBirthDate,
                                                    @Nullable final AdministrativeGender patientGender) {
        final var recordTarget = new CdachHeaderPatient();

        final var patientRole = new POCDMT000040PatientRole();
        final var ii = new II();
        ii.setRoot("2.998");
        ii.setExtension(patientMpiPid);

        patientRole.getId().add(ii);

        final var patient = new POCDMT000040Patient();
        if (patientGender != null) {
            patient.setAdministrativeGenderCode(patientGender.getCE());
        } else {
            patient.setAdministrativeGenderCode(new CE());
            patient.getAdministrativeGenderCode().getNullFlavor().add(NullFlavor.UNKNOWN_L1_CODE);
        }

        final var name = new PN();
        name.getUse().add("L");
        patient.getName().add(name);

        final var enFamily = new EnFamily();
        if (familyName != null) {
            enFamily.setXmlMixed(familyName);
        } else {
            enFamily.getNullFlavor().add(NullFlavor.UNKNOWN_L1_CODE);
        }
        name.getContent().add(new JAXBElement<>(new QName(Hl7Urns.HL7V3_URN, "family"), EnFamily.class, enFamily));

        final var enGiven = new EnGiven();
        if (givenName != null) {
            enGiven.setXmlMixed(givenName);
        } else {
            enGiven.getNullFlavor().add(NullFlavor.UNKNOWN_L1_CODE);
        }
        name.getContent().add(new JAXBElement<>(new QName(Hl7Urns.HL7V3_URN, "given"), EnGiven.class, enGiven));

        if (patientBirthDate != null) {
            patient.setBirthTime(DateTimes.toDateTs(patientBirthDate));
        } else {
            final var unknownTs = new TS();
            unknownTs.getNullFlavor().add(NullFlavor.UNKNOWN_L1_CODE);
            patient.setBirthTime(unknownTs);
        }

        patientRole.setPatient(patient);
        recordTarget.setHl7PatientRole(patientRole);
        return recordTarget;
    }

    /**
     * Creates the PMP author.
     *
     * @param authorshipInstant The instant of the authorship.
     * @return the created CCE author.
     */
    protected POCDMT000040Author author(final Instant authorshipInstant) {
        final var author = new CdachOtherAuthor();
        author.setFunctionCode(ParticipationFunction.COMPOSER_SOFTWARE.getCE());
        author.setTime(DateTimes.toDatetimeTs(authorshipInstant));

        final POCDMT000040AssignedAuthor assignedAuthor = new POCDMT000040AssignedAuthor();
        assignedAuthor.getId().add(new II(CodeSystems.GLN.getCodeSystemId(), "7601002860123"));

        final POCDMT000040AuthoringDevice authoringDevice = new POCDMT000040AuthoringDevice();
        authoringDevice.setSoftwareName(new SC("CARA"));
        authoringDevice.setManufacturerModelName(new SC("2.51.1.3"));

        assignedAuthor.setAssignedAuthoringDevice(authoringDevice);
        author.setAssignedAuthor(assignedAuthor);
        return author;
    }

    /**
     * Creates the PMP custodian organization.
     *
     * @return the created CCE custodian.
     */
    protected POCDMT000040Custodian custodian() {
        final POCDMT000040CustodianOrganization custodianOrganization = new POCDMT000040CustodianOrganization();
        custodianOrganization.setName(new ON("Hausarzt", "L"));
        custodianOrganization.getId().add(new II("2.51.1.3", "7601002729611"));

        final POCDMT000040AssignedCustodian assignedCustodian = new POCDMT000040AssignedCustodian();
        assignedCustodian.setRepresentedCustodianOrganization(custodianOrganization);

        final CdachHeaderCustodian custodian = new CdachHeaderCustodian();
        custodian.setAssignedCustodian(assignedCustodian);
        return custodian;
    }

    /**
     * Creates the PMP information recipient.
     *
     * @return the created CCE information recipient.
     */
    protected POCDMT000040InformationRecipient informationRecipient() {
        final CdachHeaderInformationRecipient informationRecipient = new CdachHeaderInformationRecipient();
        informationRecipient.setTypeCode(XInformationRecipient.PRCP);
        final POCDMT000040IntendedRecipient intendedRecipient = new POCDMT000040IntendedRecipient();
        informationRecipient.setHl7IntendedRecipient(intendedRecipient);
        final POCDMT000040Organization organization = new POCDMT000040Organization();
        intendedRecipient.setReceivedOrganization(organization);
        organization.getName().add(new ON("Hausarzt", "L"));

        return informationRecipient;
    }
}
