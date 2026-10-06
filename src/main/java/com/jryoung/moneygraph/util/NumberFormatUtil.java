package com.jryoung.moneygraph.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public final class NumberFormatUtil {
    private NumberFormatUtil() {}
    public static String compact(BigInteger value) {
        if (value == null) return "0";
        BigDecimal n = new BigDecimal(value); String[] suffixes = {"", "K", "M", "B", "T", "Q"}; int tier = 0; BigDecimal thousand = BigDecimal.valueOf(1000);
        while (n.abs().compareTo(thousand) >= 0 && tier < suffixes.length - 1) { n = n.divide(thousand, 3, RoundingMode.HALF_UP); tier++; }
        if (tier == 0) return value.toString(); return new DecimalFormat("0.##").format(n) + suffixes[tier];
    }
    public static String exact(BigInteger value) {
        if (value == null) return "0"; String s = value.toString(); StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) { if (i > 0 && (s.length() - i) % 3 == 0) out.append(','); out.append(s.charAt(i)); } return out.toString();
    }
}