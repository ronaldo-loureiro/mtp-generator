package ch.hug.mtpgenerator.controllers;

import ch.hug.mtpgenerator.models.MtpModel;
import org.husky.common.enums.LanguageCode;
import org.husky.common.enums.ValueSetEnumInterface;
import org.husky.emed.ch.enums.PharmaceuticalDoseFormEdqm;
import org.husky.emed.ch.enums.RegularUnitCodeAmbu;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import org.husky.emed.ch.enums.RouteOfAdministrationAmbu;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    @GetMapping("/")
    public String getHome(final Model model) {

        MtpModel mtp = new MtpModel();
        model.addAttribute("mtp", new MtpModel());
        model.addAttribute("languageCode", mtp.getDocumentLanguageCode());

        Comparator<ValueSetEnumInterface> comparator = Comparator.comparing(e -> e.getDisplayName(mtp.getDocumentLanguageCode()));

        model.addAttribute("routeOfAdministrationAmbu", Arrays.stream(RouteOfAdministrationAmbu.values())
                .sorted(comparator)
                .collect(Collectors.toList()));

        model.addAttribute("regularUnitCodeAmbu", Arrays.stream(RegularUnitCodeAmbu.values())
                .sorted(comparator)
                .collect(Collectors.toList()));

        model.addAttribute("pharmaceuticalDoseFormEdqm", Arrays.stream(PharmaceuticalDoseFormEdqm.values())
                .sorted(comparator)
                .collect(Collectors.toList()));

        return "home";
    }
}
