package com.demo.app;

import java.util.HashMap;
import java.util.Map;

public class DataStore {
    private final Map<Integer, String> db = new HashMap<>();

    public String fetch(int id) {
        if (db.containsKey(id)) {
            return db.get(id);
        }
        return null;
    }

    public void save(int id, String value) {
        if (value == null) {
            return;
        }
        db.put(id, value);
    }
}

