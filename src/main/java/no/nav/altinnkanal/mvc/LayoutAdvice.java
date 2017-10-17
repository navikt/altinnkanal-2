package no.nav.altinnkanal.mvc;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.io.Writer;

@ControllerAdvice
public class LayoutAdvice {

    @ModelAttribute("layout")
    public Mustache.Lambda layout() {
        return new Layout();
    }

    class Layout implements Mustache.Lambda {
        String body;

        @Override
        public void execute(Template.Fragment fragment, Writer writer) throws IOException {
            body = fragment.execute();
        }
    }
}
