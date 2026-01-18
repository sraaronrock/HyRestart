package dev.alesixdev.hyrestart.scheduler;

import dev.alesixdev.hyrestart.config.ConfigData;
import dev.alesixdev.hyrestart.config.WarningConfig;
import dev.alesixdev.hyrestart.utils.DiscordWebhook;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class RestartScheduler {
    private static final Logger LOGGER = Logger.getLogger(RestartScheduler.class.getName());
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final int CHECK_INTERVAL_SECONDS = 30;
    private static final int WARNING_WINDOW_SECONDS = 30;
    private static final int RESTART_THRESHOLD_SECONDS = 45;
    private static final int SHUTDOWN_DELAY_MS = 3000;

    private final ConfigData config;
    private final ScheduledExecutorService scheduler;
    private final DiscordWebhook discordWebhook;
    private final Set<String> sentWarnings;

    private LocalTime nextRestartTime;
    private boolean restartInProgress;

    public RestartScheduler(ConfigData config) {
        this.config = config;
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.discordWebhook = new DiscordWebhook(config.getDiscord().getWebhookUrl(), config.getMessages());
        this.sentWarnings = new HashSet<>();
        this.restartInProgress = false;
    }

    public void start() {
        calculateNextRestart();
        scheduler.scheduleAtFixedRate(
            this::checkRestartTime,
            0,
            CHECK_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );
        LOGGER.info(config.getMessages().getSchedulerStarted().replace("{time}", String.valueOf(nextRestartTime)));
    }

    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void calculateNextRestart() {
        LocalTime currentTime = LocalTime.now();
        LocalTime closestTime = null;

        for (String timeStr : config.getRestartTimes()) {
            try {
                LocalTime restartTime = LocalTime.parse(timeStr, TIME_FORMATTER);
                if (restartTime.isAfter(currentTime) && (closestTime == null || restartTime.isBefore(closestTime))) {
                    closestTime = restartTime;
                }
            } catch (Exception e) {
                LOGGER.severe(config.getMessages().getInvalidTimeFormat().replace("{time}", timeStr));
            }
        }

        if (closestTime == null && !config.getRestartTimes().isEmpty()) {
            try {
                closestTime = LocalTime.parse(config.getRestartTimes().get(0), TIME_FORMATTER);
            } catch (Exception e) {
                System.err.println("[HyRestart] Invalid time format: " + config.getRestartTimes().get(0));
            }
        }

        nextRestartTime = closestTime;
        sentWarnings.clear();
        restartInProgress = false;
    }

    private void checkRestartTime() {
        if (nextRestartTime == null || restartInProgress) {
            return;
        }

        try {
            long secondsUntilRestart = calculateSecondsUntilRestart();
            processWarnings(secondsUntilRestart);
            checkForRestart(secondsUntilRestart);
        } catch (Exception e) {
            LOGGER.severe(config.getMessages().getErrorInScheduler().replace("{error}", e.getMessage()));
            e.printStackTrace();
        }
    }

    private long calculateSecondsUntilRestart() {
        LocalTime currentTime = LocalTime.now();
        long seconds = java.time.Duration.between(currentTime, nextRestartTime).getSeconds();

        if (seconds < 0 && seconds >= -120) {
            return 0; // Trigger immediate restart
        }

        return seconds < 0 ? seconds + 24 * 3600 : seconds;
    }

    private void processWarnings(long secondsUntilRestart) {
        for (WarningConfig warning : config.getWarnings()) {
            String warningKey = warning.getWarningKey();

            if (shouldSendWarning(secondsUntilRestart, warning.getSeconds(), warningKey)) {
                sendWarning(warning);
                sentWarnings.add(warningKey);
            }
        }
    }

    private boolean shouldSendWarning(long secondsUntilRestart, int warningSeconds, String warningKey) {
        return secondsUntilRestart <= warningSeconds
            && secondsUntilRestart > (warningSeconds - WARNING_WINDOW_SECONDS)
            && !sentWarnings.contains(warningKey);
    }

    private void checkForRestart(long secondsUntilRestart) {
        if (secondsUntilRestart <= RESTART_THRESHOLD_SECONDS
            && !sentWarnings.contains("restart")) {
            sentWarnings.add("restart");
            restartInProgress = true;
            performRestart();
        }
    }

    private void sendWarning(WarningConfig warning) {
        broadcastMessage(warning.getMessage());

        if (config.getDiscord().isEnabled() && !config.getDiscord().getWebhookUrl().isEmpty()) {
            String description = config.getDiscord().getEmbedDescription()
                .replace("{time}", warning.getDiscordTime());
            discordWebhook.sendEmbed(
                config.getDiscord().getEmbedTitle(),
                description,
                config.getDiscord().getEmbedColor()
            );
        }

        LOGGER.info(config.getMessages().getSentRestartWarning().replace("{discordTime}", warning.getDiscordTime()));
    }

    private void broadcastMessage(String message) {
        try {
            List<PlayerRef> players = Universe.get().getPlayers();
            LOGGER.info(config.getMessages().getBroadcastingToPlayers()
                .replace("{count}", String.valueOf(players.size()))
                .replace("{message}", message));

            if (players.isEmpty()) {
                return;
            }

            Message msg = Message.raw(message);

            for (PlayerRef playerRef : players) {
                sendMessageToPlayer(playerRef, msg);
            }
        } catch (Exception e) {
            LOGGER.severe(config.getMessages().getErrorBroadcasting().replace("{error}", e.getMessage()));
            e.printStackTrace();
        }
    }

    private void sendMessageToPlayer(PlayerRef playerRef, Message msg) {
        try {
            Universe.get().getDefaultWorld().execute(() -> {
                try {
                    Ref<EntityStore> ref = playerRef.getReference();
                    if (ref != null) {
                        Store<EntityStore> store = ref.getStore();
                        Player player = store.getComponent(ref, Player.getComponentType());
                        if (player != null) {
                            player.sendMessage(msg);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.severe(config.getMessages().getErrorSendingToPlayer().replace("{username}", playerRef.getUsername()));
                }
            });
        } catch (Exception e) {
            LOGGER.severe(config.getMessages().getErrorSchedulingForPlayer().replace("{username}", playerRef.getUsername()));
        }
    }

    private void performRestart() {
        LOGGER.info(config.getMessages().getStartingRestart());

        if (config.getDiscord().isEnabled() && !config.getDiscord().getWebhookUrl().isEmpty()) {
            discordWebhook.sendEmbed(
                config.getDiscord().getFinalEmbedTitle(),
                config.getDiscord().getFinalEmbedDescription(),
                config.getDiscord().getEmbedColor()
            );
        }

        broadcastMessage(config.getFinalRestartMessage());

        try {
            Thread.sleep(SHUTDOWN_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        LOGGER.info(config.getMessages().getShuttingDown());
        System.exit(0);
    }

    public LocalTime getNextRestartTime() {
        return nextRestartTime;
    }
}
