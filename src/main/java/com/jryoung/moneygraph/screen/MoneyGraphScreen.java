package com.jryoung.moneygraph.screen;

import com.jryoung.moneygraph.MoneyGraphClient;
import com.jryoung.moneygraph.model.BalancePoint;
import com.jryoung.moneygraph.model.GraphData;
import com.jryoung.moneygraph.util.NumberFormatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MoneyGraphScreen extends Screen {
    private final Screen parent;
    private GraphData graph;
    private double zoom = 1.0;
    private double pan = 0;
    private int graphLeft, graphTop, graphRight, graphBottom;

    public MoneyGraphScreen(Screen parent) {
        super(Component.literal("MoneyGraph"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        graph = MoneyGraphClient.INSTANCE.getStore().getSelected();
        int buttonY = this.height - 32;
        addRenderableWidget(Button.builder(Component.literal("Create"), b -> feedback("Use /moneygraph create <name>.")).bounds(this.width - 380, buttonY, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Delete"), b -> deleteSelected()).bounds(this.width - 304, buttonY, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Clear"), b -> clearSelected()).bounds(this.width - 228, buttonY, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset"), b -> { zoom = 1.0; pan = 0; }).bounds(this.width - 152, buttonY, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> close()).bounds(this.width - 76, buttonY, 70, 20).build());
        graphLeft = 55; graphTop = 68; graphRight = this.width - 35; graphBottom = this.height - 70;
    }

    private void deleteSelected() {
        if (graph == null) return;
        String name = graph.getName();
        if (MoneyGraphClient.INSTANCE.getStore().delete(name)) graph = MoneyGraphClient.INSTANCE.getStore().getSelected();
    }

    private void clearSelected() {
        if (graph == null) return;
        MoneyGraphClient.INSTANCE.getStore().clear(graph.getName());
        graph = MoneyGraphClient.INSTANCE.getStore().getSelected();
    }

    private void feedback(String text) {
        if (minecraft != null && minecraft.player != null) minecraft.player.displayClientMessage(Component.literal("§bMoneyGraph§7: " + text), false);
    }

    private void close() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override public void onClose() { close(); }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        if (graph == null) {
            graphics.drawCenteredString(font, "No graph selected", width / 2, height / 2, 0xFFFF5555);
            super.render(graphics, mouseX, mouseY, delta);
            return;
        }
        graphics.drawString(font, "MoneyGraph", 18, 18, 0xFF55FFFF, true);
        graphics.drawString(font, "Graph: " + graph.getName(), 18, 34, 0xFFFFFFFF, false);
        renderStats(graphics);
        renderGraph(graphics, mouseX, mouseY);
        renderGraphList(graphics);
        super.render(graphics, mouseX, mouseY, delta);
    }

    private void renderStats(GuiGraphics g) {
        List<BalancePoint> points = graph.getPoints();
        BigInteger current = points.isEmpty() ? BigInteger.ZERO : points.get(points.size() - 1).getBalance();
        BigInteger start = points.isEmpty() ? BigInteger.ZERO : points.get(0).getBalance();
        BigInteger high = start, low = start;
        for (BalancePoint p : points) {
            if (p.getBalance().compareTo(high) > 0) high = p.getBalance();
            if (p.getBalance().compareTo(low) < 0) low = p.getBalance();
        }
        BigInteger change = current.subtract(start);
        g.drawString(font, "Current: " + NumberFormatUtil.compact(current), 150, 34, 0xFFFFFFFF, false);
        g.drawString(font, "Start: " + NumberFormatUtil.compact(start), 290, 34, 0xFFFFFFFF, false);
        g.drawString(font, "High: " + NumberFormatUtil.compact(high), 410, 34, 0xFF55FF55, false);
        g.drawString(font, "Low: " + NumberFormatUtil.compact(low), 530, 34, 0xFFFF7777, false);
        g.drawString(font, "Change: " + (change.signum() >= 0 ? "+" : "") + NumberFormatUtil.compact(change), 650, 34, change.signum() >= 0 ? 0xFF55FF55 : 0xFFFF5555, false);
    }

    private void renderGraphList(GuiGraphics g) {
        int x = 18, y = graphBottom + 17;
        g.drawString(font, "Graphs:", x, y, 0xFFAAAAAA, false);
        int cursor = x + 48;
        for (GraphData item : MoneyGraphClient.INSTANCE.getStore().getGraphs()) {
            String label = "[" + item.getName() + "]";
            g.drawString(font, label, cursor, y, item.getName().equalsIgnoreCase(graph.getName()) ? 0xFF55FFFF : 0xFFCCCCCC, false);
            cursor += font.width(label) + 8;
            if (cursor > width - 120) break;
        }
    }

    private void renderGraph(GuiGraphics g, int mouseX, int mouseY) {
        List<BalancePoint> points = graph.getPoints();
        int left = graphLeft, top = graphTop, right = graphRight, bottom = graphBottom;
        g.fill(left, top, right, bottom, 0xB0101014);
        g.fill(left, bottom - 1, right, bottom, 0xFF666666);
        g.fill(left, top, left + 1, bottom, 0xFF666666);
        if (points.isEmpty()) {
            g.drawCenteredString(font, "Run /bal to add your first point", (left + right) / 2, (top + bottom) / 2, 0xFFAAAAAA);
            return;
        }

        BigInteger min = points.get(0).getBalance(), max = min;
        for (BalancePoint p : points) {
            if (p.getBalance().compareTo(min) < 0) min = p.getBalance();
            if (p.getBalance().compareTo(max) > 0) max = p.getBalance();
        }
        if (min.equals(max)) { min = min.subtract(BigInteger.ONE); max = max.add(BigInteger.ONE); }
        double low = new BigDecimal(min).subtract(new BigDecimal(max.subtract(min)).multiply(BigDecimal.valueOf(0.08))).doubleValue();
        double high = new BigDecimal(max).add(new BigDecimal(max.subtract(min)).multiply(BigDecimal.valueOf(0.08))).doubleValue();

        for (int i = 0; i <= 5; i++) {
            double f = i / 5.0;
            int y = (int)(bottom - (bottom - top) * f);
            g.fill(left, y, right, y + 1, 0x33222222);
            BigInteger label = BigDecimal.valueOf(low + (high - low) * f).setScale(0, RoundingMode.HALF_UP).toBigInteger();
            g.drawString(font, NumberFormatUtil.compact(label), 8, y - 4, 0xFF888888, false);
        }

        int count = points.size();
        double usableWidth = Math.max(10, right - left - 10);
        double span = Math.max(1, count - 1) * zoom;
        List<Point2D> coords = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double x = left + 5 + ((i / span) * usableWidth) + pan;
            double v = new BigDecimal(points.get(i).getBalance()).doubleValue();
            double y = bottom - 5 - ((v - low) / (high - low)) * (bottom - top - 10);
            coords.add(new Point2D((int)x, (int)y));
        }
        for (int i = 1; i < coords.size(); i++) drawLine(g, coords.get(i-1), coords.get(i));
        for (int i = 0; i < coords.size(); i++) {
            Point2D p = coords.get(i);
            g.fill(p.x - 3, p.y - 3, p.x + 4, p.y + 4, 0xFFFFFFFF);
            if (mouseX >= p.x - 5 && mouseX <= p.x + 5 && mouseY >= p.y - 5 && mouseY <= p.y + 5) {
                BalancePoint point = points.get(i);
                String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date(point.getTimestamp()));
                g.renderComponentTooltip(font, List.of(Component.literal("Point #" + i), Component.literal("Balance: " + NumberFormatUtil.exact(point.getBalance())), Component.literal(date), Component.literal("Delete: /moneygraph remove-point " + graph.getName() + " " + i)), mouseX, mouseY);
            }
        }
    }

    private void drawLine(GuiGraphics g, Point2D a, Point2D b) {
        int dx = Math.abs(b.x - a.x), dy = Math.abs(b.y - a.y), sx = a.x < b.x ? 1 : -1, sy = a.y < b.y ? 1 : -1, err = dx - dy;
        int x = a.x, y = a.y;
        while (true) {
            g.fill(x, y, x + 2, y + 2, 0xFF55FFFF);
            if (x == b.x && y == b.y) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x += sx; }
            if (e2 < dx) { err += dx; y += sy; }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= graphLeft && mouseX <= graphRight && mouseY >= graphTop && mouseY <= graphBottom) {
            zoom = Math.max(0.25, Math.min(8.0, zoom * (verticalAmount > 0 ? 1.15 : 0.87)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private record Point2D(int x, int y) {}
}