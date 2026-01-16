package me.leoko.advancedban.hytale.utils.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Configuration extends ConfigurationSection {

    public Configuration() {
        super(new LinkedHashMap<>());
    }

    public Configuration(Map<String, Object> data) {
        // Defensive copy so callers can't mutate your internal state accidentally
        super((data == null) ? new LinkedHashMap<>() : new LinkedHashMap<>(data));
    }

    public Map<String, Object> getData() {
        return data;
    }

    private Object getValue(String path) {
        String[] parts = path.split("\\.");
        Object current = data;

        for (String part : parts) {
            if (!(current instanceof Map<?, ?> map)) return null;

            current = map.get(part);
            if (current == null) return null;
        }

        return current;
    }

    public Object get(String path) {
        return getValue(path);
    }

    public String getString(String path) {
        Object value = getValue(path);
        return (value != null) ? String.valueOf(value) : null;
    }

    public String getString(String path, String def) {
        String value = getString(path);
        return (value != null) ? value : def;
    }

    public int getInt(String path) {
        return getInt(path, 0);
    }

    public int getInt(String path, int def) {
        Object value = getValue(path);

        if (value instanceof Number n) {
            return n.intValue();
        }

        if (value instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ignored) {
            }
        }

        return def;
    }

    public double getDouble(String path) {
        return getDouble(path, 0);
    }

    public double getDouble(String path, double def) {
        Object value = getValue(path);

        if (value instanceof Number n) {
            return n.doubleValue();
        }

        if (value instanceof String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ignored) {
            }
        }

        return def;
    }

    public long getLong(String path) {
        return getLong(path, 0);
    }

    public long getLong(String path, long def) {
        Object value = getValue(path);

        if (value instanceof Number n) {
            return n.longValue();
        }

        if (value instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ignored) {
            }
        }

        return def;
    }

    public boolean getBoolean(String path) {
        return getBoolean(path, false);
    }

    public boolean getBoolean(String path, boolean def) {
        Object value = getValue(path);

        if (value instanceof Boolean b) {
            return b;
        }

        if (value instanceof String s) {
            return Boolean.parseBoolean(s);
        }

        return def;
    }

    public List<String> getStringList(String path) {
        Object value = getValue(path);

        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object o : list) {
                if (o != null) {
                    result.add(String.valueOf(o));
                }
            }
            return result;
        }

        return Collections.emptyList();
    }

    public List<Integer> getIntegerList(String path) {
        Object value = getValue(path);

        if (!(value instanceof List<?> list)) {
            return Collections.emptyList();
        }

        List<Integer> result = new ArrayList<>();

        for (Object o : list) {
            if (o instanceof Number n) {
                result.add(n.intValue());
            } else if (o instanceof String s) {
                try {
                    result.add(Integer.parseInt(s));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public ConfigurationSection getSection(String path) {
        Object value = getValue(path);

        if (value instanceof Map<?, ?> map) {
            return new ConfigurationSection((Map<String, Object>) map);
        }

        return null;
    }

    public void save(Path path) {
        try {
            Files.createDirectories(path.getParent());
            try (Writer w = Files.newBufferedWriter(path)) {
                ConfigUtils.yaml.dump(this.data, w);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to save config to " + path.toAbsolutePath(), ex);
        }
    }

    public void save(File file) {
        save(file.toPath());
    }

    @SuppressWarnings("unchecked")
    public void set(String path, Object value) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path cannot be null/blank");
        }

        String[] parts = path.split("\\.");
        Map<String, Object> current = data;

        // Walk/create intermediate sections
        for (int i=0; i<parts.length-1; i++) {
            String part = parts[i];
            Object next = current.get(part);

            if (next == null) {
                Map<String, Object> created = new LinkedHashMap<>();
                current.put(part, created);
                current = created;
                continue;
            }

            if (next instanceof Map<?, ?>) {
                current = (Map<String, Object>) next;
                continue;
            }

            // If a scalar/list is in the way, replace it with a section
            Map<String, Object> created = new LinkedHashMap<>();
            current.put(part, created);
            current = created;
        }

        String lastKey = parts[parts.length - 1];

        if (value == null) {
            // Common config behaviour: null means "remove"
            current.remove(lastKey);
        } else {
            current.put(lastKey, value);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> createSection(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path cannot be null/blank");
        }

        String[] parts = path.split("\\.");
        Map<String, Object> current = data;

        for (String part : parts) {
            Object next = current.get(part);

            if (next == null) {
                // Create missing section
                Map<String, Object> created = new LinkedHashMap<>();
                current.put(part, created);
                current = created;
                continue;
            }

            if (next instanceof Map<?, ?> map) {
                current = (Map<String, Object>) map;
                continue;
            }

            // Something exists but it's NOT a section
            throw new IllegalStateException(
                    "Cannot create section '" + path +
                            "' because '" + part + "' is not a section"
            );
        }

        return current;
    }

    public void load(File file) {
        try {
            data.clear();
            if (Files.notExists(file.toPath())) return; // empty config if file doesn't exist

            try (InputStream in = Files.newInputStream(file.toPath())) {
                Object loaded = ConfigUtils.yaml.load(in);

                if (loaded instanceof Map<?, ?> map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> typed = (Map<String, Object>) map;
                    data.putAll(new LinkedHashMap<>(typed));
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load config from " + file.getAbsolutePath(), ex);
        }
    }

    public static Configuration loadConfiguration(File file) {
        Configuration config = new Configuration();
        config.load(file);

        return config;
    }
}
