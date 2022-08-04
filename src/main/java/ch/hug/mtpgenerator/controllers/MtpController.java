package ch.hug.mtpgenerator.controllers;

import ch.hug.mtpgenerator.models.MtpModel;
import ch.hug.mtpgenerator.ondemand.OnDemandMedicationTreatmentPlanAssembler;
import org.husky.emed.ch.cda.digesters.CceDocumentDigester;
import org.husky.emed.ch.enums.PharmaceuticalDoseFormEdqm;
import org.husky.emed.ch.enums.RegularUnitCodeAmbu;
import org.husky.emed.ch.enums.RouteOfAdministrationAmbu;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;


@Controller
public class MtpController {

    @PostMapping("/mtp")
    public String createMTP(@ModelAttribute MtpModel mtp, final Model model) {
        try {
            OnDemandMedicationTreatmentPlanAssembler onDemandMedicationTreatmentPlanAssembler =
                    new OnDemandMedicationTreatmentPlanAssembler();
            String cdaXmlMtp = onDemandMedicationTreatmentPlanAssembler.assemble(mtp);

            model.addAttribute("cdaXmlMtp", cdaXmlMtp);

            return "xmlviewer";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("mtp", mtp);
            model.addAttribute("routeOfAdministrationAmbu", RouteOfAdministrationAmbu.values());
            model.addAttribute("regularUnitCodeAmbu", RegularUnitCodeAmbu.values());
            model.addAttribute("pharmaceuticalDoseFormEdqm", PharmaceuticalDoseFormEdqm.values());
        }
        return "home";
    }
}
