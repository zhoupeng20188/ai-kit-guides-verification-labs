package com.example.migratorlab;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;

/**
 * Prints what the Environment actually resolved for the old and the new key,
 * so a run can distinguish "the key was ignored" from "the key was reported".
 * Enabled only with --probe=true.
 */
@Component
@ConditionalOnProperty(name = "probe", havingValue = "true")
public class KeyProbe implements ApplicationRunner {

    private final ConfigurableEnvironment environment;
    private final ResourcesBindingProbe bound;

    public KeyProbe(ConfigurableEnvironment environment, ResourcesBindingProbe bound) {
        this.environment = environment;
        this.bound = bound;
    }

    @Override
    public void run(ApplicationArguments args) {
        System.out.println("PROBE old-name   spring.resources.add-mappings      = " + environment.getProperty("spring.resources.add-mappings"));
        System.out.println("PROBE new-name   spring.web.resources.add-mappings  = " + environment.getProperty("spring.web.resources.add-mappings"));
        System.out.println("PROBE bound      spring.web.resources.addMappings   = " + bound.isAddMappings());
        System.out.println("PROBE property sources, in order:");
        for (PropertySource<?> source : environment.getPropertySources()) {
            System.out.println("PROBE   - " + source.getName());
        }
    }
}
