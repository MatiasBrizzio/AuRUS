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

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

public class GeneticAlgorithm<C extends Chromosome<C>, T extends Comparable<T>> {

	private static final int ALL_PARENTAL_CHROMOSOMES = Integer.MAX_VALUE;

	private class ChromosomesComparator implements Comparator<C> {

		private final Map<C, T> cache = new WeakHashMap<C, T>();

		@Override
		public int compare(C chr1, C chr2) {
			T fit1 = this.fit(chr1);
			T fit2 = this.fit(chr2);
			int ret = fit1.compareTo(fit2);
			return ret;
		}

		public T fit(C chr) {
			T fit = this.cache.get(chr);
			if (fit == null) {
				fit = GeneticAlgorithm.this.fitnessFunc.calculate(chr);
				this.cache.put(chr, fit);
			}
			return fit;
		};

		public void clearCache() {
			this.cache.clear();
		}
	}

	private final ChromosomesComparator chromosomesComparator;

	private final Fitness<C, T> fitnessFunc;

	private Population<C> population;

	// listeners of genetic algorithm iterations (handle callback afterwards)
	private final List<IterartionListener<C, T>> iterationListeners = new LinkedList<IterartionListener<C, T>>();

	private boolean terminate = false;

	// number of parental chromosomes, which survive (and move to new
	// population)
	private int parentChromosomesSurviveCount = ALL_PARENTAL_CHROMOSOMES;
	
	// Percentage of chromosomes that are selected for crossover
	private int CROSSOVER_RATE = 10; 
	// Probability with which the mutation is applied to each chromosome
	private int MUTATION_RATE = 100;
	
	private int iteration = 0;

	public GeneticAlgorithm(Population<C> population, Fitness<C, T> fitnessFunc) {
		this.population = population;
		this.fitnessFunc = fitnessFunc;
		this.chromosomesComparator = new ChromosomesComparator();
		this.population.sortPopulationByFitness(this.chromosomesComparator);
	}

	public void evolve() {
		int parentPopulationSize = this.population.getSize();

		Population<C> newPopulation = new Population<C>();

		for (int i = 0; (i < parentPopulationSize) && (i < this.parentChromosomesSurviveCount); i++) {
			newPopulation.addChromosome(this.population.getChromosomeByIndex(i));
		}

		// apply mutation
		Random r = new Random();
		for (int i = 0; i < parentPopulationSize; i++) {
			int mut = r.nextInt(100);
			if (mut < MUTATION_RATE){
				C chromosome = this.population.getChromosomeByIndex(i);
				C mutated = chromosome.mutate();
				newPopulation.addChromosome(mutated);
			}
		}
		
		// apply crossover
		int numOfCrossovers = Math.max(10, parentPopulationSize*(CROSSOVER_RATE/100));
		for (int i = 0; i < numOfCrossovers; i++) {
			C chromosome = this.population.getRandomChromosome();
			C otherChromosome = this.population.getRandomChromosome();
			List<C> crossovered = chromosome.crossover(otherChromosome);
			for (C c : crossovered) {
				newPopulation.addChromosome(c);
			}
		}
		
		newPopulation.sortPopulationByFitness(this.chromosomesComparator);
		newPopulation.trim(parentPopulationSize);
		this.population = newPopulation;
	}

	public void evolve(int count) {
		this.terminate = false;

		for (int i = 0; i < count; i++) {
			if (this.terminate) {
				break;
			}
			this.evolve();
			this.iteration = i;
			for (IterartionListener<C, T> l : this.iterationListeners) {
				l.update(this);
			}
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

	public C getWorst() {
		return this.population.getChromosomeByIndex(this.population.getSize() - 1);
	}

	public void setParentChromosomesSurviveCount(int parentChromosomesCount) {
		this.parentChromosomesSurviveCount = parentChromosomesCount;
	}
	
	public int getParentChromosomesSurviveCount() {
		return this.parentChromosomesSurviveCount;
	}
	
	public void setMutationRate(int mutationRate) {
		this.MUTATION_RATE = mutationRate;
	}
	
	public int getMutationRate() {
		return MUTATION_RATE;
	}

	public void setCrossoverRate(int crossoverRate) {
		this.CROSSOVER_RATE = crossoverRate;
	}
	
	public int getCrossoverRate() {
		return CROSSOVER_RATE;
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

	public void clearCache() {
		this.chromosomesComparator.clearCache();
	}
}
