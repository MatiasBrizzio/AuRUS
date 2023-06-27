# AuRUS

On this webpage, you will discover a set of guidelines that outline the process for replicating the experiments conducted in the research paper titled "Automated Repair of Unrealisable LTL Specifications Guided by Model Counting." The paper was presented at the Genetic and Evolutionary Computation Conference (GECCO) in 2023. By following these instructions, you can recreate the same experiments and obtain results that can be compared to those in the paper. The guidelines provided here will walk you through the necessary steps required to set up the experiment environment, execute the experiments, and analyze the results. 

## Installation Instructions

### REQUIREMENTS
- Java 11 or later.
- Strix reactive synthesis tool.
- Or use our Docker image (for Mac OS users or Linux users that prefer not to install all Strix dependencies).

### DOWNLOAD
You can download the tool from this link: [unreal-repair.zip](link).

### INSTALL
1. To compile our tool, run: `ant compile`.
2. We provide an Ant script (`build.xml` file) and the libraries required into the `lib` folder.
3. Set `JAVA_HOME` environmental variable.
4. By default, the tool will use the docker image, but you can use flag `-no-docker` if you have installed Strix on your computer.
5. Notice that, in case of using `-no-docker`, the tool will run the script `lib/strix_tlsf.sh`, that will execute the binary in `lib/new_strix/strix`.

### INSTALL DOCKER IMAGE
Follow these instructions to install the Docker image with the Strix installation:
1. Install Docker.
2. Move to `lib` and run: `docker build -t strix_image .`.
3. Run: `docker-machine create default`.
4. Run: `docker-machine env --shell cmd default`.

## Running the Experiments

In the folder `case-studies`, you can find the scripts with the specifications of each one of the case studies used in the paper.

### RUN THE TOOL
The tool takes as input a specification in TLSF format.  
To run our tool, you have to use the script `unreal-repair.sh`. 
For example, to run our motivating example Arbiter: 
```
./unreal-repair.sh case-studies/arbiter/arbiter.tlsf 
```
In our experimental evaluation, we ran the following command:
```
./unreal-repair.sh -Max=1000 -Gen=1000 -Pop=100 -k=20 -GATO=7200 -addA -out=result/arbiter/ case-studies/arbiter/arbiter.tlsf 
```
In the example, we indicate to the tool that 1000 is the maximum number of individuals to be generated, 100 is the population size per iteration, and assumptions can be added. The tool will save the realisable repairs in the directory that you indicate using the `-out` flag, or by default in the same directory where the input specification is.

We can indicate to the tool what are (genuine) solutions of reference, that will be used at the end of the analysis to study the quality of the learnt repairs, by adding `-ref=case-studies/arbiter/genuine/arbiter_fixed0.tlsf -ref=case-studies/arbiter/genuine/arbiter_fixed1.tlsf -ref=case-studies/arbiter/genuine/arbiter_fixed2.tlsf -ref=case-studies/arbiter/genuine/arbiter_fixed3.tlsf`.

### REPRODUCE THE EXPERIMENTS
In order to reproduce the experimental evaluation of the

 paper, we provide several scripts:
- `run-literature.sh`: it runs the case studies taken from the literature with the default configuration.
- `run-syntech.sh`: it runs the case studies taken from the SynTech benchmark with the default configuration.
- `run-syntcomp.sh`: it runs the case studies taken from the SyntComp benchmark with the default configuration.
- `run-benchmarks.sh`: it runs 10 times each case study. You can use the same script to run the random evaluation.
- `run-sensitivity-analysis.sh`: it runs the sensitivity analysis 10 times. Notice to configure script `run-all-sensitivity.sh` and `run-all-sensitivity-syntcomp.sh` first to enable/disable the different key properties that are part of the fitness function.

## Reading the Results

You can download the results of our experimental evaluation from this link: [Results.zip](link). 
We ran the algorithm 10 times for each case study. All the results that we obtained are reported in the result section.

You can use the script `read-results.sh` to extract a summary of results for each run.

Example:
```
./read-results.sh result/result-70-10-20/arbiter/arbiter-genuine
```
This command will show the results obtained for the Arbiter case study in the 10 runs with a configuration in which 0.7 was assigned to the realisability property, 0.1 to the syntactic distance, and 0.2 to the semantic distance.

## Flags to Configure the GA

Our tool offers many flags to run the Genetic Algorithm under different configurations.

Use `./unreal-repair.sh [flags] input-file.tlsf`

Flags:
- `onlySAT`: realisability checking is disabled in the fitness function. At the end, the tool will check for the realisability of the candidate solutions.
- `no-docker`: the tool won't virtualize the realisability checking and will use the local installation of Strix. 
- `random`: it will create `Max` number of individuals randomly and at the end will check for realisability. We use this flag in our comparison against random generation. 
- `GA_random_selector`: use random selector instead of best selector.
- `Max=max_num_of_individuals`
- `Gen=num_of_generations`
- `sol=THRESHOLD`: it will discard solutions with a fitness value smaller than the threshold.
- `Pop=population_size`
- `COR=crossover_rate`
- `MR=mutation_rate`: probability with which a specification is mutated.
- `geneMR=gene_mutation_rate`: probability with which a sub-formula (gene) is mutated.
- `geneNUM=num_of_genes_to_mutate`: number of mutations allowed per formula (how many sub-formulas can be mutated).
- `removeGuarantees`: it allows the GA to remove guarantees.
- `addAssumptions`: it allows the GA to add new assumptions.
- `onlyInputsA`: it restricts the GA to use just input variables in the new assumptions generated.
- `GPR=guarantee_preference_rate`: it indicates with which probability the GA will prefer mutation guarantees from assumptions (50% default. Near 0% will focus on the assumptions. Near 100% will focus on the guarantees).
- `k=bound`: bound for the model counting approach.
- `factors=STATUS_factor,MC_factor,SYN_factor`: it is useful for defining the factors of importance of each key property computed in the fitness.
- `RTO=strix_timeout`
- `GATO=GA_timeout`
- `SatTO=sat_timeout`
- `MCTO=model_count

ing_timeout`
- `ref=TLSF_reference_solution`: reference solutions to compare at the end of the analysis.
