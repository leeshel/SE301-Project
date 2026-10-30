package org.example.alert;

import org.example.crop.CropProfile;

import java.util.List;

@FunctionalInterface
public interface AlertEvaluator {
    List<Alert> evaluate(EnvironmentalObservation observation, CropProfile profile);
}
