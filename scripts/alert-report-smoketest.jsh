//run jshell --class-path target\classes scripts\alert-report-smoketest.jsh
// use /exit to exit

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;

import org.example.alert.Alert;
import org.example.alert.EnvironmentalObservation;
import org.example.alert.StandardAlertEvaluator;
import org.example.crop.CropProfile;
import org.example.crop.CropBuilder;
import org.example.report.Report;
import org.example.report.ReportSample;
import org.example.report.ReportType;
import org.example.report.StandardReportGenerator;
import org.example.sensor.SensorType;

System.out.println("===== ALERT TEST =====");

CropProfile profile = new CropBuilder("Tomato").temperatureRange(22, 28).humidityRange(60, 80).soilMoistureRange(45, 60).lightLevelRange(500, 1000).build();

EnumMap<SensorType, Double> readings = new EnumMap<>(SensorType.class);
readings.put(SensorType.TEMPERATURE, 31.0);
readings.put(SensorType.HUMIDITY, 70.0);
readings.put(SensorType.SOIL_MOISTURE, 40.0);
readings.put(SensorType.LIGHT_LEVEL, 700.0);
readings.put(SensorType.NUTRIENT_LEVEL, 20.0);

EnvironmentalObservation observation = new EnvironmentalObservation("Chamber 1", "Tomato", Instant.now(), readings);

List<Alert> alerts = new StandardAlertEvaluator().evaluate(observation, profile);

System.out.println("Alerts generated: " + alerts.size());
alerts.forEach(System.out::println);

System.out.println();
System.out.println("===== SENSOR FAILURE TEST =====");

readings.remove(SensorType.HUMIDITY);

EnvironmentalObservation failedObservation = new EnvironmentalObservation("Chamber 1", "Tomato", Instant.now(), readings);

List<Alert> failureAlerts = new StandardAlertEvaluator().evaluate(failedObservation, profile);

System.out.println("Alerts generated: " + failureAlerts.size());
failureAlerts.forEach(System.out::println);

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