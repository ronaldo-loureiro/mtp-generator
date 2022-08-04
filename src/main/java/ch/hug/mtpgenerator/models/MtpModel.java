package ch.hug.mtpgenerator.models;

import ch.hug.mtpgenerator.models.enums.DosageInstructionsEnums;
import ch.hug.mtpgenerator.models.enums.MtpType;
import ch.hug.mtpgenerator.models.enums.ProductCodeType;
import lombok.Data;
import org.husky.emed.ch.enums.PharmaceuticalDoseFormEdqm;
import org.husky.emed.ch.enums.RegularUnitCodeAmbu;
import org.husky.emed.ch.enums.RouteOfAdministrationAmbu;
import org.husky.emed.ch.enums.TimingEventAmbu;

import java.util.*;


@Data
public class MtpModel {

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

    private String fulfillmentInstruction;
    private String reason;

    private boolean inReserve;
    private boolean substitutionAuthorized;

    private String amountToDispense;

    public MtpModel() {
        this.mtpType = MtpType.structured_instructions;
        this.repeatNumber = 0;

        this.dosesQuantity = new LinkedHashMap<>();
        this.dosesQuantity.put(TimingEventAmbu.MORNING, "0");
        this.dosesQuantity.put(TimingEventAmbu.NOON, "0");
        this.dosesQuantity.put(TimingEventAmbu.EVENING, "0");
        this.dosesQuantity.put(TimingEventAmbu.NIGHT, "0");

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
