/*******************************************************************************
 * Copyright 2012 Yuriy Lagodiuk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.lagodiuk.ga;

import main.Settings;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.IntStream;

public class GeneticAlgorithm<C extends Chromosome<C>, T extends Comparable<T>> {

    private static final int ALL_PARENTAL_CHROMOSOMES = Integer.MAX_VALUE;
    private final ChromosomesComparator chromosomesComparator;
    private final Fitness<C, T> fitnessFunc;
    // listeners of genetic algorithm iterations (handle callback afterwards)
    private final List<IterartionListener<C, T>> iterationListeners = new LinkedList<IterartionListener<C, T>>();
    private Population<C> population;
    private boolean terminate = false;
    // number of parental chromosomes, which survive (and move to new
    // population)
    private int parentChromosomesSurviveCount = ALL_PARENTAL_CHROMOSOMES;
    // number of chromosomes visited during the search
    private int numberOfVisitedIndividuals = 0;
    //	private Set allVisitedChromosomes;
    // number of chromosomes visited during the search
    private int MAXIMUM_NUM_OF_INDIVIDUALS = Integer.MAX_VALUE;
    // Percentage of chromosomes that are selected for crossover
    private int CROSSOVER_RATE = 10;
    // Probability with which the mutation is applied to each chromosome
    private int MUTATION_RATE = 100;
    private int iteration = 0;
    private int EXECUTION_TIMEOUT = 0;//in seconds. No timeout by default.
    private Instant startRunningTime = null;
    private Instant iterationStartTime = null;


    public GeneticAlgorithm(Population<C> population, Fitness<C, T> fitnessFunc) {
        this.population = population;
        this.fitnessFunc = fitnessFunc;
        this.chromosomesComparator = new ChromosomesComparator();
        this.population.sortPopulationByFitness(this.chromosomesComparator);
    }

    /** Wall-clock deadline of the current run, or null when none is set.
     *  Held statically so the solver helpers can bound a single call by the
     *  time the run has left, rather than by their own per-call timeout alone. */
    private static volatile Instant
            runDeadline = null;

    /** Milis left before the run's deadline, or Long.MAX_VALUE if there is none.
     *  Never negative: a run past its deadline reports 0 and callers should not
     *  start new work. */
    public static long millisUntilDeadline() {
        Instant d = runDeadline;
        if (d == null) return Long.MAX_VALUE;
        // Usamos toMillis() para obtener precisión de 3 decimales
        long left = Duration.between(Instant.now(), d).toMillis();
        return Math.max(0, left);
    }

    public static boolean deadlinePassed() {
        return runDeadline != null && !Instant.now().isBefore(runDeadline);
    }



    public void evolve() {
        int parentPopulationSize = this.population.getSize();

        Population<C> newPopulation = new Population<C>();

        IntStream.iterate(0,
                        i -> (i < parentPopulationSize) && (i < this.parentChromosomesSurviveCount), i -> i + 1).
                mapToObj(i -> this.population.getChromosomeByIndex(i)).forEach(newPopulation::addChromosome);

        // apply mutation
        Random r = Settings.RANDOM_GENERATOR;
        IntStream.iterate(0, i -> (i < parentPopulationSize) && !terminate, i -> i + 1).forEach(i -> {
            int mut = r.nextInt(100);
            if (mut < MUTATION_RATE) {
                C chromosome = this.population.getChromosomeByIndex(i);
                C mutated = chromosome.mutate();
                if (mutated != null && !chromosome.equals(mutated)) {//!newPopulation.contains(mutated)) {
                    newPopulation.addChromosome(mutated);
                    //update number of visited chromosomes
                    this.numberOfVisitedIndividuals++;
//					allVisitedChromosomes.add(mutated);
                }
            }
            checkTermination();
        });

        // apply crossover
        int numOfCrossovers = Math.max(10, parentPopulationSize * (CROSSOVER_RATE / 100));

        for (int i = 0; (i < numOfCrossovers) && !terminate; i++) {
            C chromosome;
            C otherChromosome;

            chromosome = newPopulation.getRandomChromosome();
            otherChromosome = newPopulation.getRandomChromosome();

            List<C> crossovered = chromosome.crossover(otherChromosome);
            crossovered.stream().filter(c -> !c.equals(chromosome)).forEach(c -> {
                newPopulation.addChromosome(c);
                this.numberOfVisitedIndividuals++;
            });

            checkTermination();
        }

        newPopulation.sortPopulationByFitness(this.chromosomesComparator);
        this.population = newPopulation;
    }

    public void select() {
        //best selector
        if (population.getSize() > this.parentChromosomesSurviveCount) {
            if (Settings.GA_RANDOM_SELECTOR) {
                //first, disorder individuals before trimming
                population.shufflePopulation();
            }
            population.trim(this.parentChromosomesSurviveCount);
        }
    }

    public void evolve(int count) {
        this.terminate = false;
        if (startRunningTime == null) {
            startRunningTime = Instant.now();
            runDeadline = EXECUTION_TIMEOUT > 0
                    ? startRunningTime.plusSeconds(EXECUTION_TIMEOUT)
                    : null;
        }
        try {
            for (int i = 0; i < count; i++) {
                checkTermination();
                iterationStartTime = Instant.now();
                if (this.terminate) break;
                this.evolve();
                this.iteration = i;
                for (IterartionListener<C, T> l : this.iterationListeners) l.update(this);
                this.select();
                printExecutionTime();
            }
        } catch (DeadlineReached e) {
            // La población queda como la dejó el ordenamiento interrumpido.
            this.terminate = true;
            System.out.println("\nGA TIMEOUT REACHED. Proceeding to show solutions found so far...");
            // Forzamos la actualización del listener para imprimir el estado actual
            for (IterartionListener<C, T> l : this.iterationListeners) {
                l.update(this);
            }
            printExecutionTime();
        }
    }
    public void setTIMEOUT(int timeout) {
        this.EXECUTION_TIMEOUT = timeout;
        // Inicializar los contadores inmediatamente
        if (this.EXECUTION_TIMEOUT > 0 && startRunningTime == null) {
            startRunningTime = Instant.now();
            runDeadline = startRunningTime.plusSeconds(this.EXECUTION_TIMEOUT);
        }
    }

    /** Thrown to unwind out of a fitness evaluation once the run's deadline
     *  has passed. Caught in evolve(int), which then returns the population
     *  as it stands. */
    static final class DeadlineReached extends RuntimeException {
        DeadlineReached() { super(null, null, false, false); }  // sin stack trace
    }

    private void checkTermination() {
        if (numberOfVisitedIndividuals >= MAXIMUM_NUM_OF_INDIVIDUALS) {
            terminate();
            return;
        }
        if (EXECUTION_TIMEOUT > 0 && startRunningTime != null) {
            Duration current = Duration.between(startRunningTime, Instant.now());
            if (current.toSeconds() > EXECUTION_TIMEOUT) {
                System.out.println("GENETIC ALGORITHM TIMEOUT REACHED. Terminating the execution...");
                terminate();
                throw new DeadlineReached();
            }
        }
    }

    private void printExecutionTime() {
        Duration current = Duration.between(startRunningTime, Instant.now());
        if (iterationStartTime != null) {
            Duration itduration = Duration.between(iterationStartTime, Instant.now());
            String timeStr = String.format("Iteration Time: %s m  %s s", itduration.toMinutes(), itduration.toSecondsPart()) + "\n" +
                    String.format("Elapsed Time: %s m  %s s", current.toMinutes(), current.toSecondsPart());
            System.out.println(timeStr);
        }
    }

    public int getIteration() {
        return this.iteration;
    }

    public void terminate() {
        this.terminate = true;
    }

    public Population<C> getPopulation() {
        return this.population;
    }

    public C getBest() {
        return this.population.getChromosomeByIndex(0);
    }

    public void setMaximumNumberOfIndividuals(int MAXIMUM_NUM_OF_INDIVIDUALS) {
        this.MAXIMUM_NUM_OF_INDIVIDUALS = MAXIMUM_NUM_OF_INDIVIDUALS;
    }

    public int getNumberOfVisitedIndividuals() {
        return this.numberOfVisitedIndividuals;
    }

    public void setParentChromosomesSurviveCount(int parentChromosomesCount) {
        this.parentChromosomesSurviveCount = parentChromosomesCount;
    }

    public void setMutationRate(int mutationRate) {
        this.MUTATION_RATE = mutationRate;
    }

    public int getCrossoverRate() {
        return CROSSOVER_RATE;
    }

    public void setCrossoverRate(int crossoverRate) {
        this.CROSSOVER_RATE = crossoverRate;
    }

    public void addIterationListener(IterartionListener<C, T> listener) {
        this.iterationListeners.add(listener);
    }

    public void removeIterationListener(IterartionListener<C, T> listener) {
        this.iterationListeners.remove(listener);
    }

    public T fitness(C chromosome) {
        return this.chromosomesComparator.fit(chromosome);
    }

    private class ChromosomesComparator implements Comparator<C> {

        private final Map<C, T> cache = new WeakHashMap<C, T>();

        @Override
        public int compare(C chr1, C chr2) {
            T fit1 = this.fit(chr1);
            T fit2 = this.fit(chr2);
            return fit2.compareTo(fit1);
        }

        public T fit(C chr) {
            T fit = this.cache.get(chr);
            if (fit == null) {
                // Checked here as well as between offspring: a hard specification can
                // spend the whole deadline inside one population sort, during which no
                // breeding loop runs and checkTermination() is never reached.
                GeneticAlgorithm.this.checkTermination();
                fit = GeneticAlgorithm.this.fitnessFunc.calculate(chr);
                this.cache.put(chr, fit);
            }
            return fit;
        }
    }

}
