package me.leoko.advancedban.hytale.utils.config;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigUtils {

    private final JavaPlugin javaPlugin;
    public static Yaml yaml;

    public Path pluginDirectory;
    private final Path pluginConfigPath;

    public ConfigUtils(JavaPlugin javaPlugin, String pluginDirectoryFolderName) {
        this.javaPlugin = javaPlugin;

        this.pluginDirectory = Path.of("mods/" + pluginDirectoryFolderName);
        this.pluginConfigPath = pluginDirectory.resolve("config.yml");

        DumperOptions opts = new DumperOptions();
        opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        opts.setPrettyFlow(true);
        opts.setIndent(2);

        yaml = new Yaml(opts);
    }

    public Configuration loadResource(String resourceName) {
        try {
            Files.createDirectories(pluginDirectory);

            // If missing, copy the resource to disk (preserves exactly what's in the jar)
            if (Files.notExists(pluginConfigPath)) {
                try (InputStream in = javaPlugin.getClass().getClassLoader().getResourceAsStream(resourceName)) {
                    if (in == null) {
                        throw new IllegalStateException(
                                "Missing " + resourceName + " in src/main/resources (it must be packaged in the jar)"
                        );
                    }
                    Files.copy(in, pluginConfigPath);
                }
            }

            // Load defaults from the jar (resources folder version. This allows us to be able to look up values
            // that we haven't added to the physical server config.yml even though they exist)
            Map<String, Object> defaults;
            try (InputStream in = javaPlugin.getClass().getClassLoader().getResourceAsStream(resourceName)) {
                if (in == null) {
                    throw new IllegalStateException(
                            "Missing " + resourceName + " in src/main/resources (it must be packaged in the jar)"
                    );
                }
                defaults = yaml.load(in);
            }
            if (defaults == null) defaults = new LinkedHashMap<>();

            // Load the server config from the disk
            Map<String, Object> server;
            try (InputStream in = Files.newInputStream(pluginConfigPath)) {
                server = yaml.load(in);
            }
            if (server == null) server = new LinkedHashMap<>();

            // Merge in memory: defaults <- server overrides
            Map<String, Object> merged = deepMerge(new LinkedHashMap<>(defaults), server);

            // Store merged config in memory (do NOT write back to disk)
            return new Configuration(merged);

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deepMerge(Map<String, Object> defaults, Map<String, Object> overrides) {
        if (defaults == null) defaults = new LinkedHashMap<>();
        if (overrides == null) return defaults;

        for (Map.Entry<String, Object> e : overrides.entrySet()) {
            String key = e.getKey();
            Object overrideVal = e.getValue();
            Object defaultVal = defaults.get(key);

            if (defaultVal instanceof Map && overrideVal instanceof Map) {
                Map<String, Object> mergedChild = deepMerge(
                        (Map<String, Object>) defaultVal,
                        (Map<String, Object>) overrideVal
                );
                defaults.put(key, mergedChild);
            } else {
                // server value wins (scalar/list/map/etc.)
                defaults.put(key, overrideVal);
            }
        }
        return defaults;
    }
}
