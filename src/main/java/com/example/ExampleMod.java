package com.gamble;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DonutGamblerMod implements ClientModInitializer {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Random random = new Random();

    private static final Pattern PAY_PATTERN = Pattern.compile(
        "received\\s+\\$?([0-9,.]+)\\s*([kmb]?)\\s+from\\s+([a-zA-Z0-9_]{3,16})", 
        Pattern.CASE_INSENSITIVE
    );

    private static int tickCounter = 0;
    private static final List<String> playerQueue = new ArrayList<>();
    private static int queueIndex = 0;
    public static boolean isRunning = true;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!isRunning || mc.player == null || mc.getNetworkHandler() == null) return;

            tickCounter++;
            if (tickCounter >= 100) { // 5 giây
                tickCounter = 0;
                sendNextPromoMessage();
            }
        });
    }

    private static void sendNextPromoMessage() {
        if (mc.getNetworkHandler() == null || mc.player == null) return;

        if (playerQueue.isEmpty() || queueIndex >= playerQueue.size()) {
            playerQueue.clear();
            Collection<PlayerListEntry> entries = mc.getNetworkHandler().getPlayerList();
            String myName = mc.player.getName().getString();

            for (PlayerListEntry entry : entries) {
                String name = entry.getProfile().getName();
                if (name != null && !name.equalsIgnoreCase(myName) && !name.startsWith("!")) {
                    playerQueue.add(name);
                }
            }
            queueIndex = 0;
        }

        if (playerQueue.isEmpty()) return;

        String target = playerQueue.get(queueIndex++);
        sendCommandSafe("msg " + target + " [Casino 45%] /pay me for a 45% chance to double your money (2x payout)!");
    }

    public static void handleIncomingChat(String rawMessage) {
        if (mc.player == null) return;

        Matcher matcher = PAY_PATTERN.matcher(rawMessage);
        if (matcher.find()) {
            try {
                String numberPart = matcher.group(1).replace(",", "");
                String suffix = matcher.group(2).toLowerCase();
                String sender = matcher.group(3);

                double baseAmount = Double.parseDouble(numberPart);
                double multiplier;

                switch (suffix) {
                    case "k":
                        multiplier = 1_000.0;
                        break;
                    case "m":
                        multiplier = 1_000_000.0;
                        break;
                    case "b":
                        multiplier = 1_000_000_000.0;
                        break;
                    default:
                        multiplier = 1.0;
                        break;
                }

                long totalAmount = (long) (baseAmount * multiplier);
                if (totalAmount <= 0) return;

                int roll = random.nextInt(100) + 1;

                if (roll <= 45) {
                    long winAmount = totalAmount * 2;
                    sendCommandSafe("pay " + sender + " " + winAmount);
                    sendCommandSafe("msg " + sender + " Congratulations! You won! Returned 2x: $" + String.format("%,d", winAmount));
                } else {
                    sendCommandSafe("msg " + sender + " Better luck next time! (45% win rate). Thanks for playing!");
                }
            } catch (Exception ignored) {
            }
        }
    }

    // Hàm gửi lệnh chuẩn trên bản 1.21+
    private static void sendCommandSafe(String command) {
        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendCommand(command);
        }
    }
}
