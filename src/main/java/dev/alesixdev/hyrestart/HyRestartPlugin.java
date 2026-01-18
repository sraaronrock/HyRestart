package dev.alesixdev.hyrestart;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.alesixdev.hyrestart.config.ConfigManager;
import dev.alesixdev.hyrestart.scheduler.RestartScheduler;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.logging.Logger;

public class HyRestartPlugin extends JavaPlugin {
    private static final Logger LOGGER = Logger.getLogger(HyRestartPlugin.class.getName());
    private ConfigManager configManager;
    private RestartScheduler restartScheduler;

    public HyRestartPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        initializeConfig();
        startScheduler();
        LOGGER.info(configManager.getData().getMessages().getPluginEnabled());
    }

    private void initializeConfig() {
        File pluginFolder = new File("mods/HyRestart");
        configManager = new ConfigManager();
        configManager.setup(pluginFolder);
    }

    private void startScheduler() {
        restartScheduler = new RestartScheduler(configManager.getData());
        restartScheduler.start();
        LOGGER.info(configManager.getData().getMessages().getNextRestart().replace("{time}", String.valueOf(restartScheduler.getNextRestartTime())));
    }

    public void reloadConfig() {
        LOGGER.info(configManager.getData().getMessages().getReloadingConfig());

        stopScheduler();
        configManager.reload();
        startScheduler();

        LOGGER.info(configManager.getData().getMessages().getConfigReloaded());
    }

    private void stopScheduler() {
        if (restartScheduler != null) {
            restartScheduler.stop();
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public RestartScheduler getRestartScheduler() {
        return restartScheduler;
    }
}
