package me.leoko.advancedban.hytale.utils.config;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class ConfigurationSection {

    protected final Map<String, Object> data;

    ConfigurationSection(Map<String, Object> data) {
        this.data = data;
    }

    public Object get(String key) {
        return data.get(key);
    }

    public boolean contains(String key) {
        return data.containsKey(key);
    }

    public Set<String> getKeys(boolean deep) {
        Set<String> keys = new LinkedHashSet<>();

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            keys.add(entry.getKey());

            if (deep && entry.getValue() instanceof Map<?, ?> map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> child = (Map<String, Object>) map;

                ConfigurationSection section = new ConfigurationSection(child);
                for (String subKey : section.getKeys(true)) {
                    keys.add(entry.getKey() + "." + subKey);
                }
            }
        }

        return keys;
    }

    public ConfigurationSection getSection(String key) {
        Object value = data.get(key);
        if (value instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> typed = (Map<String, Object>) map;
            return new ConfigurationSection(typed);
        }
        return null;
    }

    public Map<String, Object> getValues() {
        return Collections.unmodifiableMap(data);
    }
}
