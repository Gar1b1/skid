package com.matmon.skidb.demo;

import com.matmon.skidb.ui.SkiDBUI;

/** Instructor-only: opens the UI on the demo adapter, starting with an empty database. */
public class DemoLauncher {

    public static void main(String[] args) {
        SkiDBUI.launch(new InMemoryDemoAdapter());
    }
}
