package com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class Strings {
    public static String join(Iterable<String> strings, String sep, String prefix, String suffix) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (String value : strings) {
            if (!first) {
                builder.append(sep);
            }
            if (prefix != null) {
                builder.append(prefix);
            }
            builder.append(value);
            if (suffix != null) {
                builder.append(suffix);
            }
            first = false;
        }
        return builder.toString();
    }

    public static String repeat(String str, int num) {
        String[] values = new String[num];
        Arrays.fill(values, str);
        return join(Arrays.asList(values), "", "", "");
    }

    public static List<String> formatTable(List<String[]> rows) {
        Integer[] maxLengths = new Integer[rows.get(0).length];
        for (String[] row : rows) {
            if (maxLengths.length != row.length) {
                throw new IllegalStateException("mismatched columns");
            }
            for (int i = 0; i < maxLengths.length; i++) {
                if (maxLengths[i] == null || maxLengths[i] < row[i].length()) {
                    maxLengths[i] = row[i].length();
                }
            }
        }

        List<String> lines = new LinkedList<String>();
        for (String[] row : rows) {
            for (int i = 0; i < maxLengths.length; i++) {
                String pad = repeat(" ", maxLengths[i] - row[i].length());
                row[i] = row[i] + pad;
            }
            lines.add(join(Arrays.asList(row), " ", "", ""));
        }
        return lines;
    }

    public static class ToStringComparator implements Comparator<Object> {
        @Override
        public int compare(Object o1, Object o2) {
            return o1.toString().compareTo(o2.toString());
        }
    }
}
