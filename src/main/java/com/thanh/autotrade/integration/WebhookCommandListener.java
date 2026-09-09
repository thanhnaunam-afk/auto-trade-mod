package com.thanh.autotrade.integration;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lắng nghe commands từ Discord webhook.
 * Commands: !autotrade start/stop, !join, !disconnect, !pay, !status
 */
public class WebhookCommandListener {
    private final ConcurrentLinkedQueue<Command> commandQueue = new ConcurrentLinkedQueue<>();
    private static final Pattern PAY_PATTERN = Pattern.compile("!pay\\s+(\\S+)\\s+(\\d+)");
    private static final Pattern AUTOTRADE_PATTERN = Pattern.compile("!autotrade\\s+(start|stop)(?:\\s+(.+))?");
    private static final Pattern JOIN_PATTERN = Pattern.compile("!join(?:\\s+(.+))?");

    public void parseWebhookMessage(String message) {
        if (message == null || message.isBlank()) return;

        if (message.contains("!autotrade start")) {
            commandQueue.offer(new Command("START", ""));
        } else if (message.contains("!autotrade stop")) {
            commandQueue.offer(new Command("STOP", ""));
        } else if (message.contains("!join")) {
            commandQueue.offer(new Command("JOIN", ""));
        } else if (message.contains("!disconnect")) {
            commandQueue.offer(new Command("DISCONNECT", ""));
        } else if (message.contains("!pay")) {
            Matcher m = PAY_PATTERN.matcher(message);
            if (m.find()) {
                String player = m.group(1);
                long amount = Long.parseLong(m.group(2));
                commandQueue.offer(new Command("PAY", player + ":" + amount));
            }
        } else if (message.contains("!status")) {
            commandQueue.offer(new Command("STATUS", ""));
        }
    }

    public Command pollCommand() {
        return commandQueue.poll();
    }

    public static class Command {
        public String type; // START, STOP, JOIN, DISCONNECT, PAY, STATUS
        public String data;

        public Command(String type, String data) {
            this.type = type;
            this.data = data;
        }
    }
}
