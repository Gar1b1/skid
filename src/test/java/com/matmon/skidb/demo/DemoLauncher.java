package com.matmon.skidb.demo;

import com.matmon.skidb.ui.SkiDBUI;

import java.util.LinkedHashMap;
import java.util.Map;

/** Instructor-only: opens the UI on the demo adapter, pre-filled with the exam's example collection. */
public class DemoLauncher {

    public static void main(String[] args) throws Exception {
        InMemoryDemoAdapter db = new InMemoryDemoAdapter();
        Map<String, String> schema = new LinkedHashMap<>();
        schema.put("firstName", "String");
        schema.put("lastName", "String");
        schema.put("age", "Integer");
        schema.put("isCommander", "Boolean");
        db.createCollection("matmonDB", schema);
        db.addRecord("matmonDB", "1", Map.of("firstName", "Israel", "lastName", "Israeli", "age", 21, "isCommander", true));
        db.addRecord("matmonDB", "2", Map.of("firstName", "Ploni", "lastName", "Almoni", "age", 19, "isCommander", false));
        db.addRecord("matmonDB", "3", Map.of("firstName", "John", "lastName", "Doe", "age", 20, "isCommander", false));
        SkiDBUI.launch(db);
    }
}
