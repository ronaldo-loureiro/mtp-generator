package ch.hug.mtpgenerator.models;

import ch.hug.mtpgenerator.models.enums.DosageInstructionsEnums;
import ch.hug.mtpgenerator.models.enums.MtpType;
import ch.hug.mtpgenerator.models.enums.ProductCodeType;
import lombok.Data;
import org.husky.emed.ch.enums.PharmaceuticalDoseFormEdqm;
import org.husky.emed.ch.enums.RegularUnitCodeAmbu;
import org.husky.emed.ch.enums.RouteOfAdministrationAmbu;

import java.time.Instant;
import java.util.Optional;


@Data
public class MtpModel {

    private String uuid;

    private MtpType mtp_type;

    private Instant start_date;
    private Instant stop_date;

    private Integer repeat_number;

    private RouteOfAdministrationAmbu route_code;

    private RegularUnitCodeAmbu dose_quantity_unit;
    private String dose_quantity_morning;
    private String dose_quantity_noon;
    private String dose_quantity_evening;
    private String dose_quantity_night;

    private String narrative_instructions;

    private ProductCodeType product_code_type;

    private String product_code;
    private String product_name;
    private PharmaceuticalDoseFormEdqm form_code;

    private Optional<String> fulfillment_instruction;
    private Optional<String> reason;

    private boolean in_reserve;
    private boolean substitution_authorized;

    private Optional<String> amount_to_dispense;

    private DosageInstructionsEnums dosage_instructions_type;

    public MtpModel() {
        this.mtp_type = MtpType.structured_instructions;
        this.repeat_number = 0;
        this.dose_quantity_morning = "0";
        this.dose_quantity_noon = "0";
        this.dose_quantity_evening = "0";
        this.dose_quantity_night = "0";
        this.product_code_type = ProductCodeType.GTIN;
        this.in_reserve = false;
        this.substitution_authorized = true;

    }

}
