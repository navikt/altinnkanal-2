package no.nav.altinnkanal.mvc;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

@ControllerAdvice
public class UserLoggedInAdvice {
    @ModelAttribute
    public void addAttributes(Principal principal, Model model) throws Exception {
        String username = principal == null ? null : principal.getName();
        model.addAttribute("ident", username);
    }
}
