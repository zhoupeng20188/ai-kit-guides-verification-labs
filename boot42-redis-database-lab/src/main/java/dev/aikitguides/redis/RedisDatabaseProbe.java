package dev.aikitguides.redis;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Reports two different answers to "which Redis database will this app use?":
 * the value written in configuration, and the value the auto-configured
 * connection factory was actually built with.
 *
 * No Redis server is contacted. Lettuce creates its client lazily, so the
 * application context starts and closes without a network connection. This
 * proves what the factory was constructed with, not what a server received.
 */
@SpringBootApplication
public class RedisDatabaseProbe {

    public static void main(String[] args) {
        SpringApplication.run(RedisDatabaseProbe.class, args).close();
    }

    @Bean
    CommandLineRunner probe(Environment env, RedisConnectionFactory factory) {
        return args -> {
            String caseId = env.getProperty("probe.case", "unlabelled");
            String propertyDb = env.getProperty("spring.data.redis.database");
            String url = env.getProperty("spring.data.redis.url");
            String nodes = env.getProperty("spring.data.redis.masterreplica.nodes");

            Object configuration = findField(factory, "configuration");
            String type = configuration == null ? "none" : configuration.getClass().getSimpleName();
            Integer factoryDb = readDatabase(configuration);

            System.out.println("PROBE case=" + caseId + " boot=" + SpringBootVersion.getVersion());
            System.out.println("PROBE source.property.database=" + (propertyDb == null ? "absent" : propertyDb)
                    + " source.url=" + (url == null ? "absent" : url)
                    + " source.nodes=" + (nodes == null ? "absent" : nodes.split(",").length));
            System.out.println("PROBE factory.type=" + type
                    + " factory.database=" + (factoryDb == null ? "unreadable" : factoryDb));

            String want = env.getProperty("probe.expect");
            if (want == null) {
                System.out.println("PROBE assert=skipped");
                return;
            }
            int expected = Integer.parseInt(want);
            boolean ok = factoryDb != null && factoryDb == expected;
            System.out.println("PROBE assert expected=" + expected + " result=" + (ok ? "PASS" : "FAIL"));
            if (!ok) {
                throw new IllegalStateException("expected database " + expected
                        + " but the connection factory has " + factoryDb);
            }
        };
    }

    static Object findField(Object target, String name) {
        Class<?> c = target.getClass();
        while (c != null && c != Object.class) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(target);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            } catch (IllegalAccessException e) {
                return null;
            }
        }
        return null;
    }

    static Integer readDatabase(Object configuration) {
        if (configuration == null) {
            return null;
        }
        try {
            Method m = configuration.getClass().getMethod("getDatabase");
            Object v = m.invoke(configuration);
            return v instanceof Number n ? n.intValue() : null;
        } catch (ReflectiveOperationException e) {
            Object v = findField(configuration, "database");
            return v instanceof Number n ? n.intValue() : null;
        }
    }
}
