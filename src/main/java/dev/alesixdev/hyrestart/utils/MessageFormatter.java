package dev.alesixdev.hyrestart.utils;

import com.hypixel.hytale.server.core.Message;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageFormatter {

    private static final Map<String, Color> COLOR_MAP = new HashMap<>();
    private static final Pattern COLOR_TAG_PATTERN = Pattern.compile("<([a-z_]+)>(.*?)</\\1>");

    static {
        COLOR_MAP.put("black", new Color(0, 0, 0));
        COLOR_MAP.put("dark_blue", new Color(0, 0, 170));
        COLOR_MAP.put("dark_green", new Color(0, 170, 0));
        COLOR_MAP.put("dark_aqua", new Color(0, 170, 170));
        COLOR_MAP.put("dark_red", new Color(170, 0, 0));
        COLOR_MAP.put("dark_purple", new Color(170, 0, 170));
        COLOR_MAP.put("gold", new Color(255, 170, 0));
        COLOR_MAP.put("gray", new Color(170, 170, 170));
        COLOR_MAP.put("dark_gray", new Color(85, 85, 85));
        COLOR_MAP.put("blue", new Color(85, 85, 255));
        COLOR_MAP.put("green", new Color(85, 255, 85));
        COLOR_MAP.put("aqua", new Color(85, 255, 255));
        COLOR_MAP.put("red", new Color(255, 85, 85));
        COLOR_MAP.put("light_purple", new Color(255, 85, 255));
        COLOR_MAP.put("yellow", new Color(255, 255, 85));
        COLOR_MAP.put("white", new Color(255, 255, 255));
    }

    public static Color parseColor(String colorName) {
        if (colorName == null) {
            return null;
        }
        return COLOR_MAP.get(colorName.toLowerCase());
    }

    public static Message createColoredMessage(String text) {
        List<Message> segments = new ArrayList<>();
        Matcher matcher = COLOR_TAG_PATTERN.matcher(text);
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                String plainText = text.substring(lastEnd, matcher.start());
                segments.add(Message.raw(plainText));
            }

            String colorName = matcher.group(1);
            String content = matcher.group(2);
            Color color = parseColor(colorName);

            if (color != null) {
                segments.add(Message.raw(content).color(color));
            } else {
                segments.add(Message.raw(content));
            }

            lastEnd = matcher.end();
        }

        if (lastEnd < text.length()) {
            String plainText = text.substring(lastEnd);
            segments.add(Message.raw(plainText));
        }

        if (segments.isEmpty()) {
            return Message.raw(text);
        }

        return Message.join(segments.toArray(new Message[0]));
    }

    @Deprecated
    public static Message createColoredMessage(String text, String colorName) {
        if (colorName == null || colorName.isEmpty()) {
            return Message.raw(text);
        }
        Color color = parseColor(colorName);
        if (color == null) {
            return Message.raw(text);
        }
        return Message.raw(text).color(color);
    }

    @Deprecated
    public static Message createColoredMessage(String text, Color color) {
        if (color == null) {
            return Message.raw(text);
        }
        return Message.raw(text).color(color);
    }
}
