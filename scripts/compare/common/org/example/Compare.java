package org.example;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Feeds the same single-threaded inputs to a build through its Access adapter and
 * writes every observable result, one line per case, so two builds can be diffed.
 */
public class Compare {
    public static void main(String[] args) throws Exception {
        String[] crops = {"Tomato", "Strawberry", "Lettuce", "Spinach", "Chili", "Cucumber", "Kale"};
        Object[][] sensors = {
                {"Temperature", 0, 45},
                {"Humidity", 20, 105},
                {"Soil", 10, 105},
                {"Light", 290, 1210},
                {"Nutrient", 10, 105}};
        String[] controls = {"Cooling Fan", "Water Pump", "Nutrient Injector", "Humidifier",
                "Dehumidifier", "Grow Light", "Unknown"};
        double[][] starts = {{24, 70, 60, 600, 70}, {18, 30, 100, 1000, 100},
                {20, 35, 85, 900, 90}, {40, 100, 10, 300, 10}};

        PrintStream realOut = System.out;
        PrintStream silent = new PrintStream(OutputStream.nullOutputStream());
        System.setOut(silent);

        List<String> lines = new ArrayList<>();
        for (String crop : crops) {
            for (Object[] sensor : sensors) {
                String name = (String) sensor[0];
                for (int value = (int) sensor[1]; value <= (int) sensor[2]; value++) {
                    Access.reset();
                    Access.process("CH1", crop, name, value);
                    lines.add(crop + "," + name + "," + value + " -> " + Access.snapshot("CH1"));
                }
            }
        }
        for (String control : controls) {
            for (double[] start : starts) {
                Access.reset();
                Access.set("CH1", start[0], start[1], start[2], start[3], start[4]);
                ByteArrayOutputStream captured = new ByteArrayOutputStream();
                System.setOut(new PrintStream(captured));
                Access.activate("CH1", control);
                System.setOut(silent);
                lines.add("DEVICE " + control + " -> " + Access.snapshot("CH1") + " last="
                        + Access.lastAction("CH1") + " out=" + captured.toString().replace("\n", "|"));
            }
        }

        System.setOut(realOut);
        Files.write(Paths.get(args[0]), lines);
    }
}
