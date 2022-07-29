package ch.hug.mtpgenerator.utils;

import org.husky.common.enums.CodeSystems;
import org.husky.common.hl7cdar2.*;
import org.husky.common.utils.time.DateTimes;
import org.husky.emed.ch.cda.generated.artdecor.CdachOtherAuthor;
import org.husky.emed.ch.cda.generated.artdecor.MtpreferenceEntryContentModule;
import org.husky.emed.ch.cda.utils.IiUtils;
import org.husky.emed.ch.models.common.AddressDigest;
import org.husky.emed.ch.models.common.AuthorDigest;
import org.husky.emed.ch.models.common.EmedReference;
import org.husky.emed.ch.models.common.TelecomDigest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Creator utilities for CDA-CH-EMED documents.
 **/
public class CceCreators {

    /**
     * This class is not instantiable.
     */
    private CceCreators() {
    }

    /**
     * Creates an author.
     *
     * @param authorDigest The author digest.
     * @return the created CCE author.
     */
    public static POCDMT000040Author author(final AuthorDigest authorDigest) {
        Objects.requireNonNull(authorDigest, "authorDigest shall not be null in author()");

        final var author = new CdachOtherAuthor();
        if (authorDigest.getParticipationFunction() != null) {
            author.setFunctionCode(authorDigest.getParticipationFunction().getCE());
        }
        author.setTime(DateTimes.toDatetimeTs(authorDigest.getAuthorshipTimestamp()));

        final var assignedAuthor = new POCDMT000040AssignedAuthor();
        author.setAssignedAuthor(assignedAuthor);

        assignedAuthor.getId().add(new II(CodeSystems.GLN.getCodeSystemId(), authorDigest.getAuthorGln()));
        authorDigest.getOtherIds().stream()
                .map(IiUtils::fromQualifiedIdentifier)
                .forEach(ii -> assignedAuthor.getId().add(ii));

        assignedAuthor.getAddr().addAll(authorDigest.getAddresses().stream().map(CceCreators::address).toList());
        assignedAuthor.getTelecom().addAll(telecom(authorDigest.getTelecoms()));

        // Person author
        if (authorDigest.getFamilyName() != null || authorDigest.getGivenName() != null) {
            final var assignedPerson = new POCDMT000040Person();
            final var name = new PN();
            name.getUse().add("L");
            final var factory = new ObjectFactory();
            assignedPerson.getName().add(name);
            if (authorDigest.getFamilyName() != null) {
                name.getContent().add(factory.createEnFamily(new EnFamily(authorDigest.getFamilyName())));
            }
            if (authorDigest.getGivenName() != null) {
                name.getContent().add(factory.createEnGiven(new EnGiven(authorDigest.getGivenName())));
            }
            assignedAuthor.setAssignedPerson(assignedPerson);
        }

        // Device author
        if (authorDigest.getDeviceSoftwareName() != null) {
            final var authoringDevice = new POCDMT000040AuthoringDevice();
            authoringDevice.setSoftwareName(new SC(authorDigest.getDeviceSoftwareName()));
            if (authorDigest.getDeviceManufacturerModelName() != null) {
                authoringDevice.setManufacturerModelName(new SC(authorDigest.getDeviceManufacturerModelName()));
            }
            assignedAuthor.setAssignedAuthoringDevice(authoringDevice);
        }

        // Represented organization
        if (authorDigest.getOrganization() != null) {
            final var repOrganization = new POCDMT000040Organization();

            authorDigest.getOrganization().getIds().stream()
                    .map(IiUtils::fromQualifiedIdentifier)
                    .forEach(ii -> repOrganization.getId().add(ii));
            authorDigest.getOrganization().getNames().stream()
                    .map(ON::new)
                    .forEach(name -> repOrganization.getName().add(name));
            if (authorDigest.getOrganization().getTelecoms() != null) {
                repOrganization.getTelecom().addAll(telecom(authorDigest.getOrganization().getTelecoms()));
            }
            authorDigest.getOrganization().getAddresses().stream()
                    .map(CceCreators::address)
                    .forEach(address -> repOrganization.getAddr().add(address));

            assignedAuthor.setRepresentedOrganization(repOrganization);
        }

        return author;
    }

    /**
     * Creates an MTP reference entry, as per 2.16.756.5.30.1.1.10.4.45.
     *
     * @param mtpReference The MTP reference.
     * @return the created MTP reference entry.
     */
    public static POCDMT000040SubstanceAdministration mtpReferenceEntry(final EmedReference mtpReference) {
        Objects.requireNonNull(mtpReference, "mtpReference shall not be null in createMtpReferenceEntry()");
        final POCDMT000040SubstanceAdministration entry = new MtpreferenceEntryContentModule();
        entry.setMoodCode(XDocumentSubstanceMood.INT); //SPEC: get original
        entry.getId().add(new II(Objects.requireNonNull(mtpReference.getEntryId()).toString(), null));

        if (mtpReference.getDocumentId() != null) {
            final POCDMT000040ExternalDocument mtpExternalDoc = new POCDMT000040ExternalDocument();
            mtpExternalDoc.getId().add(new II(mtpReference.getDocumentId().toString(), null));
            final POCDMT000040Reference mtpDocRef = MtpreferenceEntryContentModule.getPredefinedReferenceXcrpt();
            mtpDocRef.setExternalDocument(mtpExternalDoc);
            entry.getReference().add(mtpDocRef);
        }

        return entry;
    }


    public static AD address(final AddressDigest addressDigest) {
        final var ad = new AD();
        final var factory = new ObjectFactory();
        if (addressDigest.getStreetName() != null) {
            ad.getContent().add(factory.createADStreetName(new AdxpStreetName(addressDigest.getStreetName())));
        }
        if (addressDigest.getHouseNumber() != null) {
            ad.getContent().add(factory.createADHouseNumber(new AdxpHouseNumber(addressDigest.getHouseNumber())));
        }
        if (addressDigest.getCity() != null) {
            ad.getContent().add(factory.createADCity(new AdxpCity(addressDigest.getCity())));
        }
        if (addressDigest.getPostalCode() != null) {
            ad.getContent().add(factory.createADPostalCode(new AdxpPostalCode(addressDigest.getPostalCode())));
        }
        if (addressDigest.getCountry() != null) {
            ad.getContent().add(factory.createADCountry(new AdxpCountry(addressDigest.getCountry())));
        }
        return ad;
    }

    public static List<TEL> telecom(final TelecomDigest telecomDigest) {
        final var tels = new ArrayList<TEL>(telecomDigest.size());
        for (final var mail : telecomDigest.getMails()) {
            tels.add(new TEL(mail));
        }
        for (final var phone : telecomDigest.getPhones()) {
            tels.add(new TEL(phone));
        }
        for (final var fax : telecomDigest.getFaxes()) {
            tels.add(new TEL(fax));
        }
        for (final var website : telecomDigest.getWebsites()) {
            tels.add(new TEL(website));
        }
        for (final var other : telecomDigest.getOthers()) {
            tels.add(new TEL(other));
        }
        return tels;
    }
}
