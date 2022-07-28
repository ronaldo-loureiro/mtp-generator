package ch.hug.mtpgenerator.controllers;

import ch.hug.mtpgenerator.models.MtpModel;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;


@Controller
public class MtpController {

    @PostMapping("/mtp")
    public String createMTP(@ModelAttribute MtpModel mtp, final Model model) {
        return "home";
    }
}
