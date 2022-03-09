package com.hwx.chatique.p2p;

import android.util.Base64;

public class StringUtils {

    public static String stringXOR(String a, String b) {
        if (a != null & b != null) {
            if (a.length() < b.length()) {
                b = b.substring(0, a.length());
            }

            if (b.length() < a.length()) {
                a = a.substring(0, b.length());
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < a.length(); i++)
                sb.append((char) (a.charAt(i) + b.charAt(i % b.length())));

            return Base64.encodeToString(sb.toString().getBytes(), Base64.DEFAULT);
        } else {
            return "null";
        }
    }

    /*
     * Converts a byte to hex digit and writes to the supplied buffer
     */
    public static void byte2hex(byte b, StringBuffer buf) {
        char[] hexChars = {'0', '1', '2', '3', '4', '5', '6', '7', '8',
                '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        int high = ((b & 0xf0) >> 4);
        int low = (b & 0x0f);
        buf.append(hexChars[high]);
        buf.append(hexChars[low]);
    }

    /*
     * Converts a byte array to hex string
     */
    public static String toHexString(byte[] block) {
        StringBuffer buf = new StringBuffer();
        int len = block.length;
        for (int i = 0; i < len; i++) {
            byte2hex(block[i], buf);
            if (i < len - 1) {
                buf.append(":");
            }
        }
        return buf.toString();
    }

}
