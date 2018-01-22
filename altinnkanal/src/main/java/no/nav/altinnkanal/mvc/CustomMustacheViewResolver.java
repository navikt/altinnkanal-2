package no.nav.altinnkanal.mvc;

import com.samskivert.mustache.Mustache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mustache.web.MustacheViewResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.resource.ResourceUrlProvider;

import javax.annotation.PostConstruct;
import java.util.Properties;

@Component
public class CustomMustacheViewResolver {

    private final ResourceUrlProvider resourceUrlProvider;
    private final MustacheViewResolver mustacheViewResolver;

    @Autowired
    public CustomMustacheViewResolver(ResourceUrlProvider resourceUrlProvider, MustacheViewResolver mustacheViewResolver) {
        this.resourceUrlProvider = resourceUrlProvider;
        this.mustacheViewResolver = mustacheViewResolver;
    }

    @PostConstruct
    public void enableContentVersioningForViewResolver() {
        Properties properties = new Properties();
        properties.put("url", (Mustache.Lambda) (frag, out) -> {
            String url = frag.execute();
            String resourceUrl = resourceUrlProvider.getForLookupPath(url);
            if (resourceUrl != null) {
                out.write(resourceUrl);
            } else {
                out.write(url);
            }
        });

        mustacheViewResolver.setAttributes(properties);

    }

}
