package dev.alesixdev.hyrestart;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.alesixdev.hyrestart.config.ConfigManager;
import dev.alesixdev.hyrestart.scheduler.RestartScheduler;
import dev.alesixdev.hyrestart.utils.DiscordWebhook;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.logging.Logger;

public class HyRestartPlugin extends JavaPlugin {
    private static final Logger LOGGER = Logger.getLogger(HyRestartPlugin.class.getName());
    private ConfigManager configManager;
    private RestartScheduler restartScheduler;
    private DiscordWebhook discordWebhook;

    public HyRestartPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        initializeConfig();
        setupDiscordWebhook();
        startScheduler();

        LOGGER.info(configManager.getData().getMessages().getPluginEnabled());
    }

    @Override
    protected void start() {
        sendStartupWebhook();
    }

    @Override
    protected void shutdown() {
        stopScheduler();
        sendStopWebhook();
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

    private void stopScheduler() {
        if (restartScheduler != null) {
            restartScheduler.stop();
        }
    }

    private void setupDiscordWebhook() {
        discordWebhook = new DiscordWebhook(
            configManager.getData().getDiscord().getWebhookUrl(),
            configManager.getData().getMessages()
        );
    }


    private void sendStartupWebhook() {
        if (configManager.getData().getDiscord().isEnabled() &&
            !configManager.getData().getDiscord().getWebhookUrl().isEmpty()) {
            discordWebhook.sendEmbed(
                configManager.getData().getDiscord().getStartupEmbedTitle(),
                configManager.getData().getDiscord().getStartupEmbedDescription(),
                configManager.getData().getDiscord().getStartupEmbedColor()
            );
        }
    }

    private void sendStopWebhook(){
        if (configManager.getData().getDiscord().isEnabled() &&
            !configManager.getData().getDiscord().getWebhookUrl().isEmpty()) {
            discordWebhook.sendEmbed(
                    configManager.getData().getDiscord().getShutdownEmbedTitle(),
                    configManager.getData().getDiscord().getShutdownEmbedDescription(),
                    configManager.getData().getDiscord().getShutdownEmbedColor()
            );
        }
    }
}
