package com.jryoung.moneygraph.model;

import java.util.ArrayList;
import java.util.List;

public class GraphData {
    private String name;
    private List<BalancePoint> points = new ArrayList<>();
    public GraphData() {}
    public GraphData(String name) { this.name = name; }
    public String getName() { return name == null || name.isBlank() ? "Main" : name; }
    public void setName(String name) { this.name = name; }
    public List<BalancePoint> getPoints() { if (points == null) points = new ArrayList<>(); return points; }
}