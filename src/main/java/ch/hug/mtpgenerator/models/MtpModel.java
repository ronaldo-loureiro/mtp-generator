package ch.hug.mtpgenerator.models;

import ch.hug.mtpgenerator.models.enums.DosageInstructionsEnums;
import ch.hug.mtpgenerator.models.enums.MtpType;
import ch.hug.mtpgenerator.models.enums.ProductCodeType;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.husky.common.enums.LanguageCode;
import org.husky.emed.ch.enums.PharmaceuticalDoseFormEdqm;
import org.husky.emed.ch.enums.RegularUnitCodeAmbu;
import org.husky.emed.ch.enums.RouteOfAdministrationAmbu;
import org.husky.emed.ch.enums.TimingEventAmbu;

import java.util.*;


@Data
public class MtpModel {

    private static String DEFAULT_UUID = "00000000-0000-0000-0000-000000000000";

    private static String DEFAULT_DOSE_QUANTITY = "0";

    private static LanguageCode DEFAULT_LANGUAGE_CODE = LanguageCode.FRENCH;

    private String uuid;

    private MtpType mtpType;

    private String startDate;
    private String stopDate;

    private Integer repeatNumber;

    private RouteOfAdministrationAmbu routeCode;

    private RegularUnitCodeAmbu doseQuantityUnit;

    private Map<TimingEventAmbu, String> dosesQuantity;

    private String narrativeInstructions;

    private ProductCodeType productCodeType;

    private String productCode;
    private String productName;
    private PharmaceuticalDoseFormEdqm formCode;

    private String fulfillmentInstructions;
    private String reason;
    private String patientInstructions;

    private boolean inReserve;
    private boolean substitutionAuthorized;

    private String amountToDispense;

    private LanguageCode documentLanguageCode;

    public MtpModel() {
        this.documentLanguageCode = DEFAULT_LANGUAGE_CODE;
        this.uuid = DEFAULT_UUID;
        this.mtpType = MtpType.structured_instructions;
        this.repeatNumber = 0;

        this.dosesQuantity = new LinkedHashMap<>();
        this.dosesQuantity.put(TimingEventAmbu.MORNING, DEFAULT_DOSE_QUANTITY);
        this.dosesQuantity.put(TimingEventAmbu.NOON, DEFAULT_DOSE_QUANTITY);
        this.dosesQuantity.put(TimingEventAmbu.EVENING, DEFAULT_DOSE_QUANTITY);
        this.dosesQuantity.put(TimingEventAmbu.NIGHT, DEFAULT_DOSE_QUANTITY);

        this.productCodeType = ProductCodeType.GTIN;
        this.inReserve = false;
        this.substitutionAuthorized = true;

    }

    public DosageInstructionsEnums getDosageInstructionType() {
        return switch (this.getMtpType()) {
            case structured_instructions -> {
                List<String> dosage_quantity = this.getDosesQuantity().values().stream()
                        .filter(q -> !q.equals("0"))
                        .distinct()
                        .toList();
                if (dosage_quantity.size() == 1) {
                    yield DosageInstructionsEnums.NORMAL;
                } else if (dosage_quantity.size() > 1) {
                    yield DosageInstructionsEnums.SPLIT;
                } else {
                    throw new RuntimeException("Dosage should be specified !");
                }
            }
            case narrative_instructions -> DosageInstructionsEnums.NARRATIVE;
        };
    }

}
