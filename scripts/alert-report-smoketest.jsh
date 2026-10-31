//run jshell --class-path target\classes scripts\alert-report-smoketest.jsh
// use /exit to exit

import java.time.Instant;
import java.util.List;

import org.example.alert.Alert;
import org.example.alert.AlertRule;
import org.example.alert.DefaultAlertRules;
import org.example.crop.CropProfile;
import org.example.crop.CropBuilder;
import org.example.report.Report;
import org.example.report.ReportSample;
import org.example.report.ReportType;
import org.example.report.StandardReportGenerator;
import org.example.sensor.SensorReading;
import org.example.sensor.SensorType;

System.out.println("===== ALERT TEST =====");

CropProfile profile = new CropBuilder("Tomato").temperatureRange(22, 28).humidityRange(60, 80).soilMoistureRange(45, 60).lightLevelRange(500, 1000).build();

List<AlertRule> rules = DefaultAlertRules.create();

List<SensorReading> readings = List.of(
    new SensorReading("Chamber 1", "Tomato", SensorType.TEMPERATURE, 31.0),
    new SensorReading("Chamber 1", "Tomato", SensorType.HUMIDITY, 70.0),
    new SensorReading("Chamber 1", "Tomato", SensorType.SOIL_MOISTURE, 40.0),
    new SensorReading("Chamber 1", "Tomato", SensorType.LIGHT_LEVEL, 700.0),
    new SensorReading("Chamber 1", "Tomato", SensorType.NUTRIENT_LEVEL, 20.0)
);

for (SensorReading reading : readings) {
    for (AlertRule rule : rules) {
        rule.check(reading, profile).ifPresent(System.out::println);
    }
}

System.out.println();
System.out.println("===== SENSOR FAILURE TEST =====");

SensorReading invalidReading = new SensorReading("Chamber 1", "Tomato", SensorType.HUMIDITY, Double.NaN);

for (AlertRule rule : rules) {
    rule.check(invalidReading, profile).ifPresent(System.out::println);
}

System.out.println();
System.out.println("===== REPORT TEST =====");

ReportSample sample = new ReportSample(
    Instant.parse("2026-10-30T00:00:00Z"),
    "Chamber 1",
    "Tomato",
    24.0,
    70.0,
    50.0,
    700.0,
    70.0,
    2.0,
    1.5,
    3.0
);

StandardReportGenerator generator = new StandardReportGenerator();

Instant periodStart = Instant.parse("2026-10-01T00:00:00Z");
Instant periodEnd = Instant.parse("2026-10-31T23:59:59Z");

for (ReportType type : ReportType.values()) {
    Report report = generator.generate(
        type,
        List.of(sample),
        periodStart,
        periodEnd
    );

    System.out.println();
    System.out.println(report.asText());
}

System.out.println();
System.out.println("===== TESTS COMPLETE =====");