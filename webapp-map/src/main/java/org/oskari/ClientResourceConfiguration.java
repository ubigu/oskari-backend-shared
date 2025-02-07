package org.oskari;

import fi.nls.oskari.spring.extension.OskariParam;
import fi.nls.oskari.util.PropertyUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ClientResourceConfiguration  implements WebMvcConfigurer {

    public ClientResourceConfiguration(){
        System.out.println("Loading oskari-override.properties");
        PropertyUtil.loadProperties("/oskari-override.properties");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        System.out.println("Registering /dist as static resource location");
        registry.addResourceHandler("/dist/**")
                .addResourceLocations("/dist/")
                .setCachePeriod(60*60*8) // Cache static files for 8 hours
        ;
    }
}
