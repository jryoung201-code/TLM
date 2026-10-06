package com.jryoung.moneygraph;

import com.jryoung.moneygraph.model.GraphData;
import com.jryoung.moneygraph.model.GraphStore;
import com.jryoung.moneygraph.screen.MoneyGraphScreen;
import com.jryoung.moneygraph.util.BalanceParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Locale;

public class MoneyGraphClient implements ClientModInitializer {
    public static final String MOD_ID = "moneygraph";
    public static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MOD_ID);
    public static MoneyGraphClient INSTANCE;
    private GraphStore store;
    private KeyMapping openKey;
    private long waitingForBalUntil;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        store = new GraphStore();

        KeyMapping.Category category = KeyMapping.Category.register(
                net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "controls")
        );
        openKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.moneygraph.open", InputConstants.Type.KEYSYM, InputConstants.KEY_F7, category
        ));

        registerCommands();

        ClientSendMessageEvents.COMMAND.register(command -> {
            String normalized = command.trim().toLowerCase(Locale.ROOT);
            if (normalized.equals("bal") || normalized.startsWith("bal ")) {
                waitingForBalUntil = System.currentTimeMillis() + 7500L;
            }
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> tryCapture(message.getString()));
        ClientReceiveMessageEvents.CHAT.register(message -> tryCapture(message.getString()));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.consumeClick()) {
                if (client.screen == null) client.setScreen(new MoneyGraphScreen(null));
            }
            if (waitingForBalUntil > 0L && System.currentTimeMillis() > waitingForBalUntil) {
                waitingForBalUntil = 0L;
            }
        });

        LOGGER.info("MoneyGraph initialized");
    }

    private void tryCapture(String message) {
        if (waitingForBalUntil <= 0L) return;
        if (System.currentTimeMillis() > waitingForBalUntil) {
            waitingForBalUntil = 0L;
            return;
        }
        BigInteger balance = BalanceParser.parse(message);
        if (balance == null) return;
        store.addSelectedPoint(balance, Instant.now().toEpochMilli());
        waitingForBalUntil = 0L;
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            GraphData graph = store.getSelected();
            client.player.displayClientMessage(Component.literal(
                    "§aMoneyGraph: added " + balance + " to " + graph.getName()
            ), false);
        }
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal("moneygraph")
                        .executes(c -> openScreen())
                        .then(ClientCommandManager.literal("create").then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                                .executes(c -> { String name = StringArgumentType.getString(c, "name").trim(); boolean ok = store.create(name); feedback(ok ? "Created and selected graph: " + name : "Could not create graph."); return ok ? 1 : 0; })))
                        .then(ClientCommandManager.literal("delete").then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                                .executes(c -> { String name = StringArgumentType.getString(c, "name").trim(); boolean ok = store.delete(name); feedback(ok ? "Deleted graph: " + name : "Could not delete graph."); return ok ? 1 : 0; })))
                        .then(ClientCommandManager.literal("select").then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                                .executes(c -> { String name = StringArgumentType.getString(c, "name").trim(); boolean ok = store.select(name); feedback(ok ? "Selected graph: " + name : "Graph not found: " + name); return ok ? 1 : 0; })))
                        .then(ClientCommandManager.literal("clear").then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                                .executes(c -> { String name = StringArgumentType.getString(c, "name").trim(); boolean ok = store.clear(name); feedback(ok ? "Cleared points from: " + name : "Graph not found: " + name); return ok ? 1 : 0; })))
                        .then(ClientCommandManager.literal("rename").then(ClientCommandManager.argument("oldName", StringArgumentType.string())
                                .then(ClientCommandManager.argument("newName", StringArgumentType.greedyString())
                                        .executes(c -> { String oldName = StringArgumentType.getString(c, "oldName").trim(); String newName = StringArgumentType.getString(c, "newName").trim(); boolean ok = store.rename(oldName, newName); feedback(ok ? "Renamed graph to: " + newName : "Could not rename graph."); return ok ? 1 : 0; }))))
                        .then(ClientCommandManager.literal("remove-point").then(ClientCommandManager.argument("graph", StringArgumentType.string())
                                .then(ClientCommandManager.argument("index", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0))
                                        .executes(c -> { String graph = StringArgumentType.getString(c, "graph"); int index = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "index"); boolean ok = store.removePoint(graph, index); feedback(ok ? "Removed point #" + index : "Could not remove that point."); return ok ? 1 : 0; }))))
                        .then(ClientCommandManager.literal("list").executes(c -> { feedback("Graphs: " + store.getGraphs().stream().map(GraphData::getName).reduce((a,b)->a+", "+b).orElse("none")); return 1; }))
                        .then(ClientCommandManager.literal("help").executes(c -> { feedback("/moneygraph | create <name> | delete <name> | select <name> | clear <name> | rename <old> <new> | remove-point <graph> <index> | list"); return 1; }))
        ));
    }

    private int openScreen() {
        Minecraft.getInstance().setScreen(new MoneyGraphScreen(null));
        return 1;
    }

    private void feedback(String text) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) client.player.displayClientMessage(Component.literal("§bMoneyGraph§7: " + text), false);
    }

    public GraphStore getStore() {
        return store;
    }
}