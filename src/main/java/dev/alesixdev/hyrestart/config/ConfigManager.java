package dev.alesixdev.hyrestart.config;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Logger;

public class ConfigManager {
    private static final Logger LOGGER = Logger.getLogger(ConfigManager.class.getName());
    private ConfigData configData;
    private File configFile;
    private final Yaml yaml;

    public ConfigManager() {
        this.configData = new ConfigData();

        LoaderOptions loaderOptions = new LoaderOptions();
        Constructor constructor = new Constructor(ConfigData.class, loaderOptions);

        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setPrettyFlow(true);

        Representer representer = new Representer(dumperOptions);
        representer.getPropertyUtils().setSkipMissingProperties(true);

        this.yaml = new Yaml(constructor, representer, dumperOptions, loaderOptions);
    }

    public void setup(File pluginFolder) {
        if (!pluginFolder.exists()) {
            pluginFolder.mkdirs();
        }

        configFile = new File(pluginFolder, "config.yml");

        if (!configFile.exists()) {
            try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                    LOGGER.info(configData.getMessages().getDefaultConfigCreated());
                } else {
                    createDefaultConfig();
                }
            } catch (Exception e) {
                LOGGER.severe(configData.getMessages().getErrorCreatingConfig());
                createDefaultConfig();
            }
        }

        load();
    }

    private void createDefaultConfig() {
        configData = new ConfigData();
        save();
    }

    public void load() {
        try (InputStream inputStream = new FileInputStream(configFile)) {
            ConfigData loadedData = yaml.loadAs(inputStream, ConfigData.class);
            if (loadedData != null) {
                this.configData = loadedData;
            }
            LOGGER.info(configData.getMessages().getConfigLoaded());
        } catch (Exception e) {
            LOGGER.severe(configData.getMessages().getErrorLoadingConfig().replace("{error}", e.getMessage()));
            this.configData = new ConfigData();
        }
    }

    public void save() {
        try (FileWriter writer = new FileWriter(configFile)) {
            yaml.dump(configData, writer);
            LOGGER.info(configData.getMessages().getConfigSaved());
        } catch (Exception e) {
            LOGGER.severe(configData.getMessages().getErrorSavingConfig().replace("{error}", e.getMessage()));
            e.printStackTrace();
        }
    }

    public ConfigData getData() {
        return configData;
    }
}
