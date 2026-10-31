package org.example;

public class SVFRMS {
    public static void main(String[] args) throws Exception {
        FarmSystemFacade farm = new FarmSystemFacade();
        farm.addChamber("CH1", "Tomato");
        farm.addChamber("CH2", "Strawberry");
        farm.addChamber("CH3", "Lettuce");
        farm.addChamber("CH4", "Chili");
        farm.runSimulation();
    }
}
