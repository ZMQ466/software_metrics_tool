package com.demo.app;

import com.demo.lib.MathUtil;

public class BaseService {
    protected int counter = 0;
    protected String name = "base";

    public void inc(int step) {
        if (step <= 0) {
            step = 1;
        }
        for (int i = 0; i < step; i++) {
            counter++;
        }
        normalizeName();
    }

    public int computeScore(int a, int b) {
        int best = MathUtil.max(a, b);
        if (best > 10 && name != null) {
            counter += best;
        }
        return counter;
    }

    public void rename(String newName) {
        if (newName == null) {
            return;
        }
        name = newName.trim();
    }

    protected void normalizeName() {
        if (name == null) {
            name = "";
        }
        name = name.toLowerCase();
    }
}

