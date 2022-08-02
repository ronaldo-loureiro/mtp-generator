package ch.hug.mtpgenerator.controllers;

import ch.hug.mtpgenerator.models.MtpModel;
import ch.hug.mtpgenerator.ondemand.OnDemandMedicationTreatmentPlanAssembler;
import org.husky.emed.ch.cda.digesters.CceDocumentDigester;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;


@Controller
public class MtpController {

    @PostMapping("/mtp")
    public String createMTP(@ModelAttribute MtpModel mtp, final Model model) throws Exception {
        OnDemandMedicationTreatmentPlanAssembler onDemandMedicationTreatmentPlanAssembler =
                new OnDemandMedicationTreatmentPlanAssembler(new CceDocumentDigester());
        onDemandMedicationTreatmentPlanAssembler.assemble(mtp);
        return "home";
    }
}
