package com.demo.lib;

public class MathUtil {
    public static int max(int a, int b) {
        return a > b ? a : b;
    }

    public static boolean inRange(int x, int low, int high) {
        if (x < low || x > high) {
            return false;
        }
        return true;
    }
}

