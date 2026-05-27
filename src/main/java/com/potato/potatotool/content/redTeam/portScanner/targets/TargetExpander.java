package com.potato.potatotool.content.redTeam.portScanner.targets;

import org.apache.commons.net.util.SubnetUtils;

import java.math.BigInteger;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public final class TargetExpander {
    private TargetExpander() {
    }

    public static Iterator<String> expand(final List<String> inputs) {
        final Iterator<String> source = inputs == null ? new ArrayList<String>().iterator() : inputs.iterator();
        return new Iterator<String>() {
            private Iterator<String> current = new ArrayList<String>().iterator();

            @Override
            public boolean hasNext() {
                while (!current.hasNext() && source.hasNext()) {
                    current = expandOne(source.next()).iterator();
                }
                return current.hasNext();
            }

            @Override
            public String next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return current.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static long estimateHosts(List<String> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return 0;
        }
        long total = 0;
        for (String input : inputs) {
            total += estimateOne(input);
            if (total < 0) {
                return Long.MAX_VALUE;
            }
        }
        return total;
    }

    private static List<String> expandOne(String raw) {
        List<String> result = new ArrayList<>();
        if (raw == null) {
            return result;
        }
        String value = normalizeHost(raw.trim());
        if (value.isEmpty()) {
            return result;
        }
        if (value.contains("/") && !value.contains(":")) {
            expandIpv4Cidr(value, result);
        } else if (value.contains("-") && !value.contains(":")) {
            expandIpv4Range(value, result);
        } else if (value.contains("/") && value.contains(":")) {
            expandIpv6Cidr(value, result);
        } else {
            result.add(value);
        }
        return result;
    }

    private static long estimateOne(String raw) {
        if (raw == null) {
            return 0;
        }
        String value = normalizeHost(raw.trim());
        if (value.isEmpty()) {
            return 0;
        }
        try {
            if (value.contains("/") && !value.contains(":")) {
                String mask = value.substring(value.indexOf('/') + 1);
                int prefix = Integer.parseInt(mask);
                if (prefix < 0 || prefix > 32) {
                    return 1;
                }
                long count = 1L << (32 - prefix);
                return Math.max(1L, count);
            }
            if (value.contains("-") && !value.contains(":")) {
                String[] parts = value.split("-", 2);
                long start = ipv4ToLong(parts[0].trim());
                String right = parts[1].trim();
                long end = right.contains(".") ? ipv4ToLong(right) : (start & 0xFFFFFF00L) + Integer.parseInt(right);
                return Math.max(1L, end - start + 1L);
            }
            if (value.contains("/") && value.contains(":")) {
                int prefix = Integer.parseInt(value.substring(value.indexOf('/') + 1));
                if (prefix < 120) {
                    return Long.MAX_VALUE;
                }
                return BigInteger.ONE.shiftLeft(128 - prefix).longValue();
            }
        } catch (Exception ignored) {
            return 1;
        }
        return 1;
    }

    private static void expandIpv4Cidr(String cidr, List<String> result) {
        try {
            SubnetUtils utils = new SubnetUtils(cidr);
            utils.setInclusiveHostCount(true);
            String[] addresses = utils.getInfo().getAllAddresses();
            for (String address : addresses) {
                result.add(address);
            }
        } catch (Exception e) {
            result.add(cidr);
        }
    }

    private static void expandIpv4Range(String value, List<String> result) {
        try {
            String[] parts = value.split("-", 2);
            long start = ipv4ToLong(parts[0].trim());
            String right = parts[1].trim();
            long end = right.contains(".") ? ipv4ToLong(right) : (start & 0xFFFFFF00L) + Integer.parseInt(right);
            if (end < start) {
                result.add(value);
                return;
            }
            long max = Math.min(end, start + 65535L);
            for (long current = start; current <= max; current++) {
                result.add(longToIpv4(current));
            }
        } catch (Exception e) {
            result.add(value);
        }
    }

    private static void expandIpv6Cidr(String value, List<String> result) {
        try {
            String[] parts = value.split("/", 2);
            int prefix = Integer.parseInt(parts[1]);
            if (prefix < 120) {
                result.add(parts[0]);
                return;
            }
            InetAddress address = InetAddress.getByName(parts[0]);
            if (!(address instanceof Inet6Address)) {
                result.add(parts[0]);
                return;
            }
            BigInteger base = new BigInteger(1, address.getAddress());
            BigInteger count = BigInteger.ONE.shiftLeft(128 - prefix);
            for (BigInteger i = BigInteger.ZERO; i.compareTo(count) < 0; i = i.add(BigInteger.ONE)) {
                result.add(InetAddress.getByAddress(toBytes(base.add(i), 16)).getHostAddress());
            }
        } catch (Exception e) {
            result.add(value);
        }
    }

    private static String normalizeHost(String raw) {
        String value = raw;
        if (value.startsWith("http://")) {
            return normalizeUrl(value);
        } else if (value.startsWith("https://")) {
            return normalizeUrl(value);
        }
        int slash = value.indexOf('/');
        if (slash >= 0) {
            String suffix = value.substring(slash + 1);
            if (!suffix.matches("\\d{1,3}")) {
                value = value.substring(0, slash);
            }
        }
        if (value.startsWith("[")) {
            int end = value.indexOf(']');
            if (end > 0) {
                return value.substring(1, end);
            }
        }
        int colon = value.indexOf(':');
        if (colon > 0 && value.indexOf(':', colon + 1) < 0) {
            value = value.substring(0, colon);
        }
        return value.trim();
    }

    private static String normalizeUrl(String value) {
        try {
            URI uri = URI.create(value);
            String host = uri.getHost();
            if (host != null && !host.trim().isEmpty()) {
                return host.trim();
            }
        } catch (Exception ignored) {
        }
        String stripped = value.startsWith("https://")
                ? value.substring("https://".length())
                : value.substring("http://".length());
        int slash = stripped.indexOf('/');
        if (slash >= 0) {
            stripped = stripped.substring(0, slash);
        }
        int colon = stripped.indexOf(':');
        if (colon > 0 && stripped.indexOf(':', colon + 1) < 0) {
            stripped = stripped.substring(0, colon);
        }
        return stripped.trim();
    }

    private static long ipv4ToLong(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid IPv4: " + ip);
        }
        long value = 0;
        for (String part : parts) {
            value = (value << 8) + Integer.parseInt(part);
        }
        return value & 0xFFFFFFFFL;
    }

    private static String longToIpv4(long value) {
        return ((value >> 24) & 0xFF) + "." + ((value >> 16) & 0xFF) + "." + ((value >> 8) & 0xFF) + "." + (value & 0xFF);
    }

    private static byte[] toBytes(BigInteger value, int length) {
        byte[] raw = value.toByteArray();
        byte[] result = new byte[length];
        int copyStart = Math.max(0, raw.length - length);
        int copyLength = Math.min(raw.length, length);
        System.arraycopy(raw, copyStart, result, length - copyLength, copyLength);
        return result;
    }
}
