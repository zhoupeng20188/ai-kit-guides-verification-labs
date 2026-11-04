package com.example.migratorlab;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

/**
 * Simulates a common legacy pattern: a @Configuration class that pulls its own
 * properties file. The file is read while the configuration class is being
 * parsed, which is later than the event the migrator listens to.
 */
@Configuration
@Profile("psource")
@PropertySource("classpath:legacy.properties")
public class LegacyPropertySourceConfig {
}
