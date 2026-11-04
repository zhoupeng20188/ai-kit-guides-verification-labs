package com.example.migratorlab;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Records what Spring actually bound for the web resources properties, after the
 * migrator has had its chance to rename anything. Defaults match Boot's own
 * defaults so a missing key is visible as "still the default".
 */
@Component
@ConfigurationProperties("spring.web.resources")
public class ResourcesBindingProbe {

    private boolean addMappings = true;

    public boolean isAddMappings() {
        return addMappings;
    }

    public void setAddMappings(boolean addMappings) {
        this.addMappings = addMappings;
    }
}
