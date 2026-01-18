package dev.alesixdev.hyrestart.config;

public class WarningConfig {
    private int seconds;
    private String message;
    private String discordTime;

    public WarningConfig() {
    }

    public WarningConfig(int seconds, String message, String discordTime) {
        this.seconds = seconds;
        this.message = message;
        this.discordTime = discordTime;
    }

    public int getSeconds() {
        return seconds;
    }

    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDiscordTime() {
        return discordTime;
    }

    public void setDiscordTime(String discordTime) {
        this.discordTime = discordTime;
    }

    public String getWarningKey() {
        return "warning_" + seconds;
    }
}
