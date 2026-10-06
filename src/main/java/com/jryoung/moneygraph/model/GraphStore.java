package com.jryoung.moneygraph.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jryoung.moneygraph.MoneyGraphClient;
import net.fabricmc.loader.api.FabricLoader;
import java.io.Reader;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GraphStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file = FabricLoader.getInstance().getConfigDir().resolve("moneygraph.json");
    private List<GraphData> graphs = new ArrayList<>();
    private String selectedGraph = "Main";
    public GraphStore() { load(); if (graphs.isEmpty()) { graphs.add(new GraphData("Main")); selectedGraph = "Main"; save(); } }
    public synchronized void load() {
        try {
            if (!Files.exists(file)) return;
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                SaveData data = GSON.fromJson(reader, SaveData.class);
                if (data != null) { graphs = data.graphs == null ? new ArrayList<>() : data.graphs; selectedGraph = data.selectedGraph == null ? "Main" : data.selectedGraph; }
            }
        } catch (Exception e) { MoneyGraphClient.LOGGER.error("Could not load MoneyGraph data", e); graphs = new ArrayList<>(); }
    }
    public synchronized void save() {
        try {
            Files.createDirectories(file.getParent());
            SaveData data = new SaveData(); data.graphs = graphs; data.selectedGraph = selectedGraph;
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) { GSON.toJson(data, writer); }
            Files.move(temp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) { MoneyGraphClient.LOGGER.error("Could not save MoneyGraph data", e); }
    }
    public synchronized List<GraphData> getGraphs() { return Collections.unmodifiableList(graphs); }
    public synchronized GraphData getSelected() {
        GraphData graph = find(selectedGraph);
        if (graph == null) { graph = graphs.isEmpty() ? null : graphs.get(0); if (graph != null) selectedGraph = graph.getName(); }
        return graph;
    }
    public synchronized GraphData find(String name) { if (name == null) return null; return graphs.stream().filter(g -> g.getName().equalsIgnoreCase(name)).findFirst().orElse(null); }
    public synchronized boolean create(String name) { if (name == null || name.isBlank() || find(name) != null) return false; graphs.add(new GraphData(name.trim())); selectedGraph = name.trim(); save(); return true; }
    public synchronized boolean delete(String name) { GraphData graph = find(name); if (graph == null || graphs.size() <= 1) return false; graphs.remove(graph); if (graph.getName().equalsIgnoreCase(selectedGraph)) selectedGraph = graphs.get(0).getName(); save(); return true; }
    public synchronized boolean select(String name) { GraphData graph = find(name); if (graph == null) return false; selectedGraph = graph.getName(); save(); return true; }
    public synchronized boolean clear(String name) { GraphData graph = find(name); if (graph == null) return false; graph.getPoints().clear(); save(); return true; }
    public synchronized boolean rename(String oldName, String newName) { GraphData graph = find(oldName); if (graph == null || newName == null || newName.isBlank() || find(newName) != null) return false; String previous = graph.getName(); graph.setName(newName.trim()); if (selectedGraph.equalsIgnoreCase(previous)) selectedGraph = graph.getName(); save(); return true; }
    public synchronized boolean removePoint(String name, int index) { GraphData graph = find(name); if (graph == null || index < 0 || index >= graph.getPoints().size()) return false; graph.getPoints().remove(index); save(); return true; }
    public synchronized void addSelectedPoint(BigInteger balance, long timestamp) { GraphData graph = getSelected(); if (graph == null) return; graph.getPoints().add(new BalancePoint(balance, timestamp)); save(); }
    public synchronized int graphCount() { return graphs.size(); }
    private static class SaveData { String selectedGraph; List<GraphData> graphs; }
}