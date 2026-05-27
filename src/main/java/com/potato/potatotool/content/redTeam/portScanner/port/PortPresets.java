package com.potato.potatotool.content.redTeam.portScanner.port;

public final class PortPresets {
    private static final int[] TOP_100 = new int[]{
        7, 9, 13, 21, 22, 23, 25, 26, 37, 53, 79, 80, 81, 88, 106, 110, 111, 113, 119,
        135, 139, 143, 144, 179, 199, 389, 427, 443, 444, 445, 465, 513, 514, 515, 543,
        544, 548, 554, 587, 631, 646, 873, 990, 993, 995, 1025, 1026, 1027, 1028, 1029,
        1110, 1433, 1720, 1723, 1755, 1900, 2000, 2001, 2049, 2121, 2717, 3000, 3128,
        3306, 3389, 3986, 4899, 5000, 5009, 5051, 5060, 5101, 5190, 5357, 5432, 5631,
        5666, 5800, 5900, 6000, 6001, 6646, 7070, 8000, 8008, 8009, 8080, 8081, 8443,
        8888, 9100, 9999, 10000, 32768, 49152, 49153, 49154, 49155, 49156, 49157
    };

    private static final int[] TOP_1000 = buildTop1000();

    private PortPresets() {
    }

    public static int[] top100() {
        return copy(TOP_100);
    }

    public static int[] top1000() {
        return copy(TOP_1000);
    }

    public static int[] all() {
        int[] ports = new int[65535];
        for (int i = 0; i < ports.length; i++) {
            ports[i] = i + 1;
        }
        return ports;
    }

    private static int[] buildTop1000() {
        int[] ports = new int[1000];
        for (int i = 0; i < ports.length; i++) {
            if (i < TOP_100.length) {
                ports[i] = TOP_100[i];
            } else {
                ports[i] = i + 1;
            }
        }
        return ports;
    }

    private static int[] copy(int[] source) {
        int[] result = new int[source.length];
        System.arraycopy(source, 0, result, 0, source.length);
        return result;
    }
}
