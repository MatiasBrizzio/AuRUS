<h3 align="center">Automated Repair of Unrealisable LTL Specifications</h3>

<p align="center">
  <a href="https://github.com/MatiasBrizzio/AuRUS/actions/workflows/build.yml"><img src="https://github.com/MatiasBrizzio/AuRUS/actions/workflows/build.yml/badge.svg" alt="Build"/></a>
  <a href="https://matiasbrizzio.github.io/AuRUS/"><img src="https://img.shields.io/badge/docs-Javadoc-blue" alt="Docs"/></a>
  <a href="https://github.com/MatiasBrizzio/AuRUS/releases"><img src="https://img.shields.io/github/v/release/MatiasBrizzio/AuRUS" alt="Release"/></a>
  <a href="https://dl.acm.org/doi/10.1145/3583131.3590454"><img src="https://img.shields.io/badge/GECCO%202023-10.1145%2F3583131.3590454-orange" alt="Paper"/></a>
  <a href="https://arxiv.org/abs/2105.12595"><img src="https://img.shields.io/badge/arXiv-2105.12595-b31b1b.svg" alt="arXiv"/></a>
  <a href="#requirements"><img src="https://img.shields.io/badge/Java-11%2B-red?logo=openjdk" alt="Java"/></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPLv3-blue.svg" alt="License: GPL v3"/></a>
</p>

**AuRUS** (**Au**tomated **R**epair of **U**nrealisable **S**pecifications) is a search-based tool that automatically repairs *unrealisable* Linear-Time Temporal Logic (LTL) specifications.

A reactive specification is **unrealisable** when no controller can satisfy it under every possible environment behaviour: the fault lies in the specification itself, not in any implementation. Synthesis tools such as [Strix](https://strix.model.in.tum.de/) can *detect* unrealisability, but they don't tell you *how to fix it*. AuRUS closes that gap: it searches for **realisable variants of the specification that remain as semantically and syntactically close as possible to the original intent**.

> 📌 **AuRUS is the first reference implementation** of the model-counting-guided repair approach introduced in our GECCO 2023 paper *[Automated Repair of Unrealisable LTL Specifications Guided by Model Counting](https://dl.acm.org/doi/10.1145/3583131.3590454)* ([arXiv](https://arxiv.org/pdf/2105.12595.pdf)). If you build on this approach or reimplement any part of it, please [cite the paper](#-citing-aurus).

---

## ✨ How it works

AuRUS is built around a **genetic algorithm** (GA): a search procedure inspired by natural selection. A GA maintains a *population* of candidate solutions and improves it over successive *generations* — the fittest candidates survive and reproduce, their traits are recombined and randomly perturbed, and quality gradually increases. GAs need only two ingredients — a representation of candidates and a fitness function to score them — which makes them a great fit for the space of LTL formulas: discrete, tree-structured, and hard to navigate with directed search. Each classic GA ingredient maps to AuRUS as follows:

- **Individual** — one candidate solution. *In AuRUS:* a complete assume-guarantee specification `(A', G')`, represented by the syntax trees of its formulas.
- **Population** — the set of candidates alive in a given generation. *In AuRUS:* seeded from the input specification by adding patterned assumptions over the input variables (e.g. `G F x`, `G ¬(x0 ∧ … ∧ xn)`).
- **Fitness** — a score measuring how good a candidate is; it decides who survives. *In AuRUS:* a weighted combination of **realisability status** (checked with Strix, on a graded scale so partially fixed candidates still receive gradient), **semantic similarity** to the original (via bounded model counting — [details below](#-semantic-similarity-via-approximate-model-counting)), and **syntactic similarity** (overlap between the sub-formula sets).
- **Selection** — the fittest individuals become the parents of the next generation. *In AuRUS:* a best-selector keeps the top-*N* candidates, which also carry over unchanged (elitism), so the best fitness never decreases.
- **Crossover** — two parents exchange sub-structures, combining traits discovered independently. *In AuRUS:* a sub-formula of one parent is transplanted into the other, or sub-formulas from each parent are merged under a binary connective (`∨, ∧, U, R, W`).
- **Mutation** — small random changes that inject the diversity crossover cannot provide. *In AuRUS:* a random sub-formula of an assumption or guarantee is rewritten (see the modes below).
- **Termination** — the loop stops when a budget is exhausted. *In AuRUS:* generations (`-Gen`), individuals (`-Max`), or wall-clock time (`-GATO`); every realisable candidate found along the way is reported as a repair, ranked by fitness.

**Mutation in more detail.** The operator picks an assumption or a guarantee (biased by `-GPR`), selects a random *sub-formula* inside it, and applies one of three modes: a **general** syntactic mutation (flip/swap atoms, exchange or stack operators), a **weakening** (`φ ⊨ φ_w` — no original model is lost), or a **strengthening** (`φ_s ⊨ φ` — no new model is added). Weakening an assumption relaxes the environment; strengthening a guarantee tightens the system's obligations. The principal directed rules:

| Formula | Weakening | Strengthening |
| --- | --- | --- |
| `p` | `true`, `p ∨ q`, `F p` | `false`, `p ∧ q`, `G p` |
| `X φ` | `true`, `F φ` | `false`, `G φ` |
| `F φ` | `true`, `F G φ`, `q W φ` | `false`, `φ`, `G φ`, `G F φ` |
| `G φ` | `true`, `φ`, `F φ`, `G F φ` | `false`, `G φ1 ∨ G φ2` (when `φ = φ1 ∨ φ2`) |
| `φ1 ∧ φ2` | `true`, `φ1`, `φ1 ∨ φ2`, `F(φ1 ∧ φ2)` | `false`, `φ1 ∧ φ2 ∧ q`, `G(φ1 ∧ φ2)` |
| `φ1 ∨ φ2` | `true`, `φ1 ∨ φ2 ∨ q` | `false`, `φ1 ∧ φ2`, drop a disjunct |
| `φ1 U φ2` | `true`, `φ1 W φ2`, `F φ2` | `false`, `φ2`, `φ1 ∧ ¬φ2 ∧ X(φ1 U φ2)` |
| `φ1 W φ2` | `true`, `F φ1 ∨ (φ1 U φ2)`, `G φ1 ∨ F φ2` | `false`, `G φ1`, `φ1 U φ2` |

(`q` denotes a fresh literal; one outcome per cell is selected uniformly at random.)

---

## 🔢 Semantic similarity via approximate model counting

The central technical contribution of our [GECCO 2023 paper](https://dl.acm.org/doi/10.1145/3583131.3590454) is a way to *quantify* how much of the original specification's meaning a candidate repair preserves — without solving an intractable exact model-counting problem. AuRUS approximates the number of **models** (satisfying lasso traces of bounded length *k*) of an LTL formula as follows:

```
 LTL φ  ──OWL──▶  Büchi automaton B_φ  ──▶  word automaton A_φ  ──▶  transfer matrix T_φ
                                                                          │
                                              approx. count  #̂(φ, k) = I · T_φ^k · F
```

1. **From formulas to automata.** φ is translated into a Büchi automaton, and from it a *finite*-word automaton `A_φ` accepting exactly the finite prefixes (bases) extendable into a satisfying lasso trace.
2. **Weighted transition matrix.** `A_φ` is encoded as a matrix `T_φ` where entry `T[i][j]` is the number of propositional valuations carrying state `i` to state `j`.
3. **Counting by matrix exponentiation.** The number of accepted bases of length *k* is `I · T_φ^k · F`, with `I`/`F` the indicator vectors of initial and accepting states — one matrix build per formula, then each bound is just a matrix power.
4. **Comparing specifications.** The semantic similarity between the original `S` and a candidate `S'` averages the two containment ratios `#(S ∧ S', k)/#(S, k)` and `#(S ∧ S', k)/#(S', k)` — capturing the behaviours of `S` the repair **preserves** (*lost models*) and the **new** behaviours it introduces (*won models*).

**Worked example.** For `φ = G(p → X q)` over `{p, q}`, the word automaton has three live states — *start*, *obligation pending* (`p` just read, `q` due next), *no obligation* — plus a sink. Counting the valuations along each edge:

```
        ⎛ 0  2  2 ⎞
T_φ  =  ⎜ 0  1  1 ⎟        I = (1 0 0),   F = (0 1 1)ᵀ,      I · T_φ⁴ · F = 108
        ⎝ 0  2  2 ⎠
```

The matrix counts *bases*, not lassos, so it can under- or over-count — but the fitness function only needs the **relative ordering** of candidates, which the approximation preserves (9 out of 10 benchmark sets match the exact ranking) while running two to three orders of magnitude faster. This is what makes semantic guidance feasible inside a search evaluating thousands of candidates per run. The implementation computes the two containment ratios through an equivalent complement formulation (counting the *lost* and *won* models directly); see [FITNESS.md](FITNESS.md) for the equivalence proof and its verification.

> This model-counting-based semantic distance, including the weighted-transition-matrix construction and the conjunction-based lost/won-models comparison, was introduced by our paper. If you reuse or reimplement this technique, please [cite it](#-citing-aurus).

📘 **Want the full picture?** For a much more detailed treatment of everything above — how genetic algorithms work, the design and rationale of each operator, the formal definition of *k*-word models, and why *approximate* model counting is the right trade-off inside a search loop — see **Chapter 4 of my PhD thesis**: *link coming soon (thesis under publication)*. <!-- TODO: replace with thesis URL once published -->

🦉 **Just want the model counter?** The transfer-matrix approach lives on as a standalone tool, **[EstiMate](https://github.com/MatiasBrizzio/EstiMate)** — a fast, accurate model counter that estimates the number of models of LTL formulas using transfer matrices. Use it directly if you need bounded LTL model counting outside the repair setting.

---

## 📖 Citing AuRUS

If you use AuRUS, the techniques it implements, or any derivative/reimplementation of this approach in your research, please cite:

> Matías Brizzio, Maxime Cordy, Mike Papadakis, César Sánchez, Nazareno Aguirre, and Renzo Degiovanni. 2023. **Automated Repair of Unrealisable LTL Specifications Guided by Model Counting.** In *Proceedings of the Genetic and Evolutionary Computation Conference (GECCO '23)*, Lisbon, Portugal. ACM, 1499–1507.

```bibtex
@inproceedings{10.1145/3583131.3590454,
    author    = {Brizzio, Mat\'{\i}as and Cordy, Maxime and Papadakis, Mike and
               S\'{a}nchez, C\'{e}sar and Aguirre, Nazareno and Degiovanni, Renzo},
    title     = {Automated Repair of Unrealisable LTL Specifications Guided by Model Counting},
    year      = {2023},
    isbn      = {9798400701191},
    publisher = {Association for Computing Machinery},
    address   = {New York, NY, USA},
    url       = {https://doi.org/10.1145/3583131.3590454},
    doi       = {10.1145/3583131.3590454},
    booktitle = {Proceedings of the Genetic and Evolutionary Computation Conference},
    pages     = {1499--1507},
    numpages  = {9},
    keywords  = {search-based software engineering, LTL-synthesis, model counting},
    location  = {Lisbon, Portugal},
    series    = {GECCO '23}
}
```

A `CITATION.cff` file is included, so GitHub's *"Cite this repository"* button works out of the box.

---

## 🛠️ Installation

### Requirements

- **Java 11** or later (set `JAVA_HOME`).
- [Apache Ant](https://ant.apache.org/) (a `build.xml` is provided; required libraries ship in `lib/`).
- The [**Strix**](https://strix.model.in.tum.de/) reactive synthesis tool — installed natively, **or** via our Docker image (recommended for macOS, or for Linux users who prefer not to build Strix's dependencies).

### Build

```bash
git clone https://github.com/MatiasBrizzio/AuRUS.git
cd AuRUS
./setup.sh          # checks Java/Ant/Docker and the vendored native tools,
                     # and warns about known OS-specific gotchas (see below)
ant compile
```

By default AuRUS invokes the **native Strix binary** (`lib/new_strix/strix`) directly — the formula and input/output signal lists are prepared in Java, with no shell wrapper involved. To use the provided Docker image instead — recommended on macOS — pass `-docker` at runtime.

> ⚠️ **Linux users:** the committed `lib/new_strix/strix` binary is currently macOS-only. Without `-docker`, realizability checks fail silently on Linux and the search will not find any repairs. `./setup.sh` detects this and warns you. Two fixes, neither requiring you to own a Linux machine: **`-docker`** (the bundled `Dockerfile` compiles Strix for Linux inside the container — Docker Desktop already runs a Linux VM under the hood, even on macOS), or **`-synth=ltlsynt`**, a Docker-free alternative described below.

Besides TLSF, AuRUS also accepts [Spectra](https://smlab.cs.tau.ac.il/syntech/spectra/) specifications: pass a `.spectra` file with the `-use-spectra` flag (realisability is then checked via the image in `docker-spectra/`).

### Docker image for Strix (optional)

```bash
cd lib
docker build -t strix_image .
docker-machine create default
docker-machine env --shell cmd default
```

---

## 🚀 Quickstart

Repair the classic (unrealisable) arbiter example:

```bash
./unreal-repair.sh case-studies/arbiter/arbiter.tlsf
```

The arbiter must grant each client infinitely often, but nothing forces clients to keep requesting — so no implementation exists. AuRUS finds repairs such as adding the missing fairness assumptions, recovering the standard fix while staying close to the original specification.

A configuration close to the one used in our experimental evaluation:

```bash
./unreal-repair.sh -Max=1000 -Gen=1000 -Pop=100 -k=20 -GATO=7200 -addA \
    -out=result/arbiter/ case-studies/arbiter/arbiter.tlsf
```

This caps generation at 1000 individuals, uses a population of 100, allows the GA to add assumptions, and writes the realisable repairs to `result/arbiter/` (default: next to the input specification).

Reference (genuine) solutions can be supplied so AuRUS assesses the quality of the learnt repairs at the end of the run:

```bash
./unreal-repair.sh \
    -ref=case-studies/arbiter/genuine/arbiter_fixed0.tlsf \
    -ref=case-studies/arbiter/genuine/arbiter_fixed1.tlsf \
    case-studies/arbiter/arbiter.tlsf
```

---

## ⚙️ Configuration reference

Usage: `./unreal-repair.sh [flags] input-file.{tlsf|spectra}`

### Search budget & population

| Flag | Default | Meaning |
| --- | --- | --- |
| `-Gen=N` | 10 | Number of generations |
| `-Pop=N` | 100 | Population size per generation |
| `-Max=N` | ∞ | Maximum number of individuals to generate |
| `-GATO=s` | none | Overall GA timeout (seconds) |
| `-sol=T` | 0.0 | Discard solutions with fitness below threshold `T` |

### Genetic operators

| Flag | Default | Meaning |
| --- | --- | --- |
| `-COR=r` | 10 | Percentage of the population selected for crossover |
| `-MR=r` | 100 | Probability (%) with which a specification is mutated |
| `-geneMR=r` | 1/\|formula\| | Probability (%) with which each sub-formula (gene) is mutated (`0` = the default `1/size` rule) |
| `-geneNUM=n` | unbounded | Maximum number of sub-formulas mutated per formula (`0` = no limit) |
| `-GPR=r` | 50 | Probability (%) of mutating guarantees rather than assumptions — near 0 focuses on assumptions, near 100 on guarantees |
| `-addA` | off | Allow the GA to add new assumptions (long form `-addAssumptions` also accepted) |
| `-removeG` | off | Allow the GA to remove guarantees (long form `-removeGuarantees` also accepted) |
| `-onlyInputsA` | off | Restrict newly generated assumptions to input variables only |
| `-GA_random_selector` | off | Replace the best-selector with a random selector (for ablation studies) |

### Fitness function

| Flag | Default | Meaning |
| --- | --- | --- |
| `-factors=S,SYN,SEM` | `0.7,0.1,0.2` | Weights of realisability status, syntactic distance, and semantic distance (the semantic weight is split evenly between the lost-models and won-models directions) |
| `-k=N` | 10 | Bound for the model-counting approach |
| `-onlySAT` | off | Disable realisability checking inside the fitness; realisability is verified only on the final candidates |
| `-strongSAT` | off | Additionally check *strong satisfiability* of candidates during fitness evaluation |
| `-precise` | off | Use the exact bounded model counter instead of the automata/matrix approximation — exact counts, orders of magnitude slower |
| `-random` | off | Baseline mode: generate `Max` random mutants and check realisability only at the end |

### External solvers & timeouts

| Flag | Default | Meaning |
| --- | --- | --- |
| `-RTO=s` | 20 | Strix (realisability) timeout per query |
| `-SatTO=s` | 30 | LTL SAT-solving timeout per query |
| `-MCTO=s` | 180 | Model-counting timeout per query |
| `-docker` | off | Run Strix through the Docker image (recommended on macOS) |
| `-no-docker` | default | Use the local Strix installation (`lib/strix_tlsf.sh`) |
| `-use-spectra` | off | Treat the input as a Spectra specification |
| `-synth=NAME` | `strix` | Realisability tool to use: `strix` (native binary or Docker, see above) or `ltlsynt` — a **Docker-free** alternative from the [Spot](https://spot.lre.epita.fr/) library, installable via `brew install spot` (macOS) or `conda install -c conda-forge spot` (Linux/macOS); see [spot.lre.epita.fr/install.html](https://spot.lre.epita.fr/install.html) for Debian/Ubuntu packages |
| `-synth-bin=PATH` | tool default | Override the synthesiser binary path/name |
| `-ref=file.tlsf` | — | Reference (genuine) solution for the end-of-run quality analysis (repeatable) |
| `-out=dir` | input dir | Output directory for the generated repairs |

---

## 📊 Reproducing the paper's experiments

The specifications for every case study used in the paper live in `case-studies/`, each with its genuine reference solutions. Evaluation scripts:

| Script | Purpose |
| --- | --- |
| `run-literature.sh` | Case studies from the literature (default configuration) |
| `run-syntech.sh` | Case studies from the SynTech benchmark |
| `run-syntcomp.sh` | Case studies from the SYNTCOMP benchmark |
| `run-benchmarks.sh` | Runs each case study 10 times (also usable for the random-generation baseline) |
| `run-sensitivity-analysis.sh` | Sensitivity analysis (configure `run-all-sensitivity.sh` / `run-all-sensitivity-syntcomp.sh` first to enable/disable fitness components) |

### Reading the results

Use `read-results.sh` to summarise the runs:

```bash
./read-results.sh result/result-70-10-20/arbiter/arbiter-genuine
```

This shows the 10-run results for the arbiter under weights 0.7 (realisability), 0.1 (syntactic), 0.2 (semantic).

---

## 👩‍💻 Maintainers

AuRUS is implemented and maintained by:

- [Matías Brizzio](mailto:matias.brizzio@imdea.org?subject=%5BGitHub%5D%20AuRUS)

Questions, bug reports, and contributions are welcome — please open an [issue](https://github.com/MatiasBrizzio/AuRUS/issues) or a pull request, contact me by email or by skype maty.brizzio.

## 📄 License

AuRUS is released under the [GNU GPL v3](LICENSE).