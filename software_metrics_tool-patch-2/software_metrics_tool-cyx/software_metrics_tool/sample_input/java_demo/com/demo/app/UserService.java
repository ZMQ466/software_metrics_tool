package com.demo.app;

import com.demo.lib.MathUtil;

public class UserService extends BaseService {
    private final DataStore store = new DataStore();

    public String loadUser(int id) {
        String raw = store.fetch(id);
        if (raw == null) {
            store.save(id, "user-" + id);
            raw = store.fetch(id);
        }

        int level;
        switch (MathUtil.max(id, 0)) {
            case 0:
                level = 0;
                break;
            case 1:
            case 2:
                level = 1;
                break;
            default:
                level = 2;
                break;
        }

        try {
            inc(level + 1);
            return name + ":" + raw;
        } catch (RuntimeException ex) {
            rename("error");
            return null;
        }
    }
}

