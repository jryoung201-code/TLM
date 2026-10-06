package com.jryoung.moneygraph.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BalanceParser {
    private BalanceParser() {}
    private static final Pattern LABELED = Pattern.compile("(?i)(?:balance|bal|money|cash|coins?|purse|wallet)[^0-9$]{0,32}\\$?([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*([kmbtq])?");
    private static final Pattern CURRENCY = Pattern.compile("(?i)\\$([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*([kmbtq])?");
    private static final Pattern SUFFIXED = Pattern.compile("(?i)\\b([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*([kmbtq])\\b");
    public static BigInteger parse(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String text = raw.replace(' ', ' ').replace("\\u00a0", " ").trim();
        BigInteger result = parse(text, LABELED); if (result != null) return result;
        result = parse(text, CURRENCY); if (result != null) return result;
        return parse(text, SUFFIXED);
    }
    private static BigInteger parse(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text); if (!matcher.find()) return null;
        String number = matcher.group(1).replace(",", ""); String suffix = matcher.groupCount() >= 2 ? matcher.group(2) : null;
        try { BigDecimal value = new BigDecimal(number); if (suffix != null && !suffix.isBlank()) value = value.multiply(multiplier(suffix)); return value.setScale(0, RoundingMode.HALF_UP).toBigIntegerExact(); }
        catch (Exception ignored) { return null; }
    }
    private static BigDecimal multiplier(String suffix) {
        return switch (suffix.toLowerCase(Locale.ROOT)) {
            case "k" -> new BigDecimal("1000"); case "m" -> new BigDecimal("1000000"); case "b" -> new BigDecimal("1000000000");
            case "t" -> new BigDecimal("1000000000000"); case "q" -> new BigDecimal("1000000000000000"); default -> BigDecimal.ONE;
        };
    }
}