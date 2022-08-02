package ch.hug.mtpgenerator.models;

import ch.hug.mtpgenerator.models.enums.DosageInstructionsEnums;
import ch.hug.mtpgenerator.models.enums.MtpType;
import ch.hug.mtpgenerator.models.enums.ProductCodeType;
import lombok.Data;
import org.husky.emed.ch.enums.PharmaceuticalDoseFormEdqm;
import org.husky.emed.ch.enums.RegularUnitCodeAmbu;
import org.husky.emed.ch.enums.RouteOfAdministrationAmbu;
import org.husky.emed.ch.enums.TimingEventAmbu;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.util.*;


@Data
public class MtpModel {

    private String uuid;

    private MtpType mtp_type;

    private String start_date;
    private String stop_date;

    private Integer repeat_number;

    private RouteOfAdministrationAmbu route_code;

    private RegularUnitCodeAmbu dose_quantity_unit;

    private Map<TimingEventAmbu, String> doses_quantity;

    private String narrative_instructions;

    private ProductCodeType product_code_type;

    private String product_code;
    private String product_name;
    private PharmaceuticalDoseFormEdqm form_code;

    private String fulfillment_instruction;
    private String reason;

    private boolean in_reserve;
    private boolean substitution_authorized;

    private String amount_to_dispense;

    public MtpModel() {
        this.mtp_type = MtpType.structured_instructions;
        this.repeat_number = 0;

        this.doses_quantity = new LinkedHashMap<>();
        this.doses_quantity.put(TimingEventAmbu.MORNING, "0");
        this.doses_quantity.put(TimingEventAmbu.NOON, "0");
        this.doses_quantity.put(TimingEventAmbu.EVENING, "0");
        this.doses_quantity.put(TimingEventAmbu.NIGHT, "0");

        this.product_code_type = ProductCodeType.GTIN;
        this.in_reserve = false;
        this.substitution_authorized = true;

    }

    public DosageInstructionsEnums getDosageInstructionType() {
        return switch (this.getMtp_type()) {
            case structured_instructions -> {
                List<String> dosage_quantity = this.getDoses_quantity().values().stream()
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
