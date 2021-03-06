package org.evrete.showcase.abs.town.types;

import org.evrete.api.Type;
import org.evrete.api.TypeField;
import org.evrete.api.TypeWrapper;

import java.util.HashMap;
import java.util.Map;

public class Entity {
    public final String type;
    private final Map<String, Boolean> flags = new HashMap<>();
    private final Map<String, Integer> numbers = new HashMap<>();
    private final Map<String, Entity> properties = new HashMap<>();

    public Entity(String type) {
        this.type = type;
    }

    public int getNumber(String name, int defaultValue) {
        return numbers.getOrDefault(name, defaultValue);
    }

    public boolean getFlag(String name, boolean defaultValue) {
        return flags.getOrDefault(name, defaultValue);
    }

    public Entity getProperty(String name) {
        return properties.get(name);
    }

    public Entity set(String name, boolean flag) {
        this.flags.put(name, flag);
        return this;
    }

    public void tmpClear() {
        if ("person".equals(type)) {
            int wakeup = numbers.get("wakeup");
            int id = numbers.get("id");
            Entity home = properties.get("home");
            Entity work = properties.get("work");

            if (home == null) {
                throw new IllegalStateException();
            }
            this.flags.clear();
            this.numbers.clear();
            this.properties.clear();
            set("id", id);
            set("wakeup", wakeup % (24 * 3600));
            set("home", home);
            set("work", work);
        }
    }

    public Entity set(String name, int number) {
        this.numbers.put(name, number);
        return this;
    }

    public Entity set(String name, Entity property) {
        if (property != null && property.getClass().isPrimitive()) {
            throw new IllegalArgumentException("Primitive types are not allowed");
        } else {
            this.properties.put(name, property);
        }
        return this;
    }


    @Override
    public String toString() {
        //return new GsonBuilder().setPrettyPrinting().create().toJson(this);
        return "{type='" + type + '\'' +
                ", flags=" + flags +
                ", numbers=" + numbers +
                ", props=" + properties +
                '}';
    }

    public static class EntityKnowledgeType extends TypeWrapper<Entity> {
        public EntityKnowledgeType(Type<Entity> delegate) {
            super(delegate);
        }

        @Override
        public TypeField getField(String name) {
            TypeField field = super.getField(name);
            if (field != null) return field;


            String[] parts = name.split("\\.");
            if (parts.length != 2) {
                return null;
            }

            String key = parts[1];
            switch (parts[0]) {
                case "flags":
                    return declareBooleanField(name, entity -> entity.getFlag(key, false));
                case "numbers":
                    return declareIntField(name, entity -> entity.getNumber(key, 0));
                case "properties":
                    return declareField(name, Entity.class, entity -> entity.getProperty(key));
                default:
                    throw new IllegalStateException();

            }
        }
    }

}
