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

import java.util.*;

public class Population<C extends Chromosome<C>> implements Iterable<C> {

    private final Random random = Settings.RANDOM_GENERATOR;
    private List<C> chromosomes;

    public Population() {
        chromosomes = new LinkedList<>();
    }

    public void addChromosome(C chromosome) {
        this.chromosomes.add(chromosome);
    }

    public int getSize() {
        return this.chromosomes.size();
    }

    public C getRandomChromosome() {
        int numOfChromosomes = this.chromosomes.size();
        // TODO improve random generator
        // maybe use pattern strategy ?
        int indx = this.random.nextInt(numOfChromosomes);
        return this.chromosomes.get(indx);
    }

    public C getChromosomeByIndex(int indx) {
        return this.chromosomes.get(indx);
    }

    public void sortPopulationByFitness(Comparator<C> chromosomesComparator) {
        Collections.shuffle(this.chromosomes);
        Collections.sort(this.chromosomes, chromosomesComparator);
    }

    public void shufflePopulation() {
        Collections.shuffle(this.chromosomes);
    }

    /**
     * shortening population till specific number
     */
    public void trim(int len) {
        this.chromosomes = this.chromosomes.subList(0, len);
    }

    @Override
    public Iterator<C> iterator() {
        return this.chromosomes.iterator();
    }

    public boolean contains(C chromosome) {
        return chromosomes.contains(chromosome);
    }
}
