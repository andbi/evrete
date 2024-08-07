package org.evrete.util;

import org.evrete.Configuration;
import org.evrete.api.Environment;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AbstractEnvironment implements Environment {
    private final Map<String, Object> properties = new HashMap<>();

    public AbstractEnvironment(AbstractEnvironment other) {
        this.properties.putAll(other.properties);
    }

    public AbstractEnvironment(Configuration configuration) {
        for(String key: configuration.stringPropertyNames()) {
            Object value = configuration.get(key);
            if(value != null) {
                properties.put(key, value);
            }
        }
    }

    @Override
    public Object set(String property, Object value) {
        synchronized (properties) {
            this.properties.put(property, value);
            return this;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public final <T> T get(String property) {
        return (T) properties.get(property);
    }

    @Override
    public final Collection<String> getPropertyNames() {
        return properties.keySet();
    }
}
