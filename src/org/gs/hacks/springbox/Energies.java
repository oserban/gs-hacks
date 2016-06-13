/*
 * Copyright (c) 2016. This is a file, part of the GS Hacks project
 *  Everything is provided as it is, without any licence and guarantee
 */
package org.gs.hacks.springbox;

/**
 * Represent the history of energy values for a layout algorithm.
 */
public class Energies {
    // Attributes

    /**
     * Global current energy (maybe actually updated).
     */
    protected double energy;

    /**
     * The last computed energy.
     */
    protected double lastEnergy;

    /**
     * The number of energy values remembered.
     */
    protected int energiesBuffer = 256;

    /**
     * A circular array of the last values of energy.
     */
    protected double[] energies = new double[energiesBuffer];

    /**
     * The current position in the energies array.
     */
    protected int energiesPos = 0;

    protected double energySum = 0;

    // Constructor

    // Access

    /**
     * The last computed energy value.
     *
     * @return The actual level of energy.
     */
    public double getEnergy() {
        return lastEnergy;
    }

    /**
     * The number of energy values remembered.
     */
    public int getBufferSize() {
        return energiesBuffer;
    }

    /**
     * A number in [0..1] with 1 meaning fully stabilised.
     *
     * @return A value that indicates the level of stabilisation in [0-1].
     */
    public double getStabilization() {
        // The stability is attained when the global energy of the graph do not
        // vary anymore.

        int range = 200;
        double eprev1 = getPreviousEnergyValue(range);
        double eprev2 = getPreviousEnergyValue(range - 10);
        double eprev3 = getPreviousEnergyValue(range - 20);
        double eprev = (eprev1 + eprev2 + eprev3) / 3.0;
        double diff = Math.abs(lastEnergy - eprev);

        diff = diff < 1 ? 1 : diff;

        return 1.0 / diff;
    }

    /**
     * A previous energy value.
     *
     * @param stepsBack The number of steps back in history.
     * @return The energy value at stepsBack in time.
     */
    public double getPreviousEnergyValue(int stepsBack) {
        if (stepsBack >= energies.length)
            stepsBack = energies.length - 1;

        int pos = (energies.length + (energiesPos - stepsBack))
                % energies.length;

        return energies[pos];
    }

    // Command

    /**
     * Accumulate some energy in the current energy.
     *
     * @param value The value to accumulate.
     */
    public void accumulateEnergy(double value) {
        energy += value;
    }

    /**
     * Add a the current accumulated energy value in the set.
     */
    public void storeEnergy() {
        energiesPos = (energiesPos + 1) % energies.length;

        energySum -= energies[energiesPos];
        energies[energiesPos] = energy;
        energySum += energy;
        lastEnergy = energy;
        energy = 0;
    }

    /**
     * Randomise the energies array.
     */
    protected void clearEnergies() {
        for (int i = 0; i < energies.length; ++i)
            energies[i] = ((Math.random() * 2000) - 1000);
    }
}