package ch.hug.mtpgenerator.controllers;

import ch.hug.mtpgenerator.models.MtpModel;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

//import org.husky.emed.ch.enums.RouteOfAdministrationAmbu;

@Controller
public class HomeController {


    @GetMapping("/")
    public String getHome(final Model model) {
        model.addAttribute("mtp", new MtpModel());
//        model.addAttribute("routeOfAdministrationAmbu", )
        return "home";
    }
}