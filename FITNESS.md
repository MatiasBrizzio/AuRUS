# The Semantic Similarity Fitness: Paper Formulation vs. Implementation

This note documents how the semantic-similarity component of AuRUS's fitness
function is implemented, and why the implementation's formulas look different
from — but are mathematically equivalent to — the ones in the
[GECCO 2023 paper](https://dl.acm.org/doi/10.1145/3583131.3590454).

## The two formulations

The paper defines the semantic similarity between the original specification
`S` and a candidate repair `S'` through the models **shared** by both
(all counts are bounded model counts `#(·, k)`; the bound `k` is left implicit
below):

$$semSim(S, S') = \tfrac{1}{2}\left(\frac{\#(S \land S')}{\#(S)} + \frac{\#(S \land S')}{\#(S')}\right)$$

The implementation
([`AutomataBasedModelCountingSpecificationFitness`](../src/geneticalgorithm/AutomataBasedModelCountingSpecificationFitness.java))
computes the same two quantities through their **complements**:

$$lost(S, S') = 1 - \frac{\#(S \land \lnot S')}{\#(S)} \qquad\qquad won(S, S') = 1 - \frac{\#(\lnot S \land S')}{\#(S')}$$

with `semSim = 0.5 · lost + 0.5 · won`.

## The intuition: grading an exam

Before the algebra, the picture. Think of the candidate `S'` as a student being
graded on how well it preserved the original specification. There are two ways
to grade an exam of 10 questions where the student got 2 wrong: count the
correct answers (`8/10 = 0.8`), or count the mistakes and subtract them from
the total (`1 − 2/10 = 0.8`). Same grade, opposite bookkeeping.

Now replace questions by models. Suppose `S` has 10 bounded models, of which
the candidate preserves 8 and drops 2:

```
the 10 models of S:   🟩 🟩 🟩 🟩 🟩 🟩 🟩 🟩 🟥 🟥

🟩 preserved — models of  S ∧ S'     (the paper counts these)
🟥 lost      — models of  S ∧ ¬S'    (the implementation counts these)
```

- **Paper** (count what survived): `#(S ∧ S')/#(S) = 8/10 = 0.8`
- **Implementation** (count what was dropped, flip it): `1 − #(S ∧ ¬S')/#(S) = 1 − 2/10 = 0.8`

The `1 −` performs no deep mathematics: it converts *"20% was lost"* into
*"80% was kept"*. The division by `#(S)` normalises a raw count into a
proportion; the `1 −` inverts a penalty (higher = worse) into a reward
(higher = better), so that every fitness component points in the same
maximise-me direction the genetic search expects.

Why does the implementation count the red squares instead of the green ones?
Because the red set has a name in logic: `S ∧ ¬S'` reads, literally,
*"behaviours that S allows **and** S' no longer allows"* — the lost models of
the approach's narrative. The code counts exactly the set the story talks
about, and lets the `1 −` turn the damage report into a grade. The won-models
direction is the mirror image: `¬S ∧ S'` names the *new* behaviours the
candidate smuggled in, and `1 − #(¬S ∧ S')/#(S')` grades how few of them there
are.

## Why they are the same

Every model of `S` is either **preserved** by the candidate (a model of
`S ∧ S'`) or **lost** (a model of `S ∧ ¬S'`), and these two sets are disjoint.
Hence:

$$\#(S) = \#(S \land S') + \#(S \land \lnot S') \quad\Longrightarrow\quad \frac{\#(S \land S')}{\#(S)} = 1 - \frac{\#(S \land \lnot S')}{\#(S)}$$

Symmetrically, every model of `S'` is either already a model of `S` or a
**new** behaviour (a model of `¬S ∧ S'`), giving:

$$\frac{\#(S \land S')}{\#(S')} = 1 - \frac{\#(\lnot S \land S')}{\#(S')}$$

The paper asks *"how many behaviours survived?"*; the implementation asks
*"how many were lost/added, subtracted from the total"*. Same value, different
viewpoint.

## Worked examples

Both examples use `AP = {a, b}` and bound `k = 10`, computed with AuRUS's own
approximate counter (see the regression test referenced below).

**Weakening** — `S = G(a)`, `S' = F(a)` (every model of `G(a)` satisfies
`F(a)`, but `F(a)` admits many more):

| Quantity | Value |
| --- | --- |
| `#(S)` | 1024 |
| `#(S')` | 1047552 |
| `#(S ∧ S')` | 1024 |
| `#(S ∧ ¬S')` | 0 |
| `#(¬S ∧ S')` | 1046528 |
| paper: `#(S∧S')/#(S)` | **1.0** |
| implementation: `1 − #(S∧¬S')/#(S)` | **1.0** |
| paper: `#(S∧S')/#(S')` | **0.000977…** |
| implementation: `1 − #(¬S∧S')/#(S')` | **0.000977…** |
| `semSim` (both) | **0.50048875855** |

The metric behaves exactly as designed: nothing was lost (`lost = 1`), but a
huge amount of new behaviour was admitted (`won ≈ 0.001`), so the candidate
scores a mediocre ≈ 0.5 — the fate a "do whatever you want" weakening
deserves. Note also that the partition identity holds *exactly* even under the
approximate counter: `1024 + 1046528 = 1047552 = #(S')`.

**Strengthening** — the mirror case `S = F(a)`, `S' = G(a)` yields the mirrored
table: `#(¬S ∧ S') = 0`, hence `won = 1.0` exactly (no new behaviour), while
`lost ≈ 0.001` (almost everything was cut), and again `semSim = 0.50048875855`
under both formulations, with `Difference = 0.0`.

## Why the complement form?

Three properties motivate implementing the complement rather than the paper's
positive form:

1. **Conceptual directness.** The approach's narrative is phrased in terms of
   *lost models* and *won models*; the implementation counts literally those
   sets, under those names. The positive form is the presentation-friendly
   algebraic rewrite.
2. **Independent components.** The experimental ablation study
   activates/deactivates the lost and won directions independently
   (`LOST_MODELS_FACTOR`, `WON_MODELS_FACTOR`). In the complement form each
   direction is a self-contained computation.
3. **Exactness at the boundary, by construction.** When the candidate is a
   genuine weakening (`S ⇒ S'`), the formula `S ∧ ¬S'` is unsatisfiable: the
   syntactic simplifier collapses it to `false` and the count is `0` without
   ever building an automaton, so `lost = 1.0` **exactly**, free of
   approximation noise. Symmetrically, genuine strengthenings yield
   `won = 1.0` exactly. The positive form enjoys no such unconditional
   guarantee (it relies on the automaton pipeline assigning equal approximate
   counts to semantically equivalent formulas — empirically robust, but not
   guaranteed).

A trade-off worth recording: the complement form performs three model counts
per candidate (`#(S∧¬S')`, `#(S')`, `#(¬S∧S')`; `#(S)` is cached) versus two
for the positive form (`#(S∧S')`, `#(S')`). Since the dominant cost of a
fitness evaluation is the realisability check rather than the model count,
this overhead is negligible in practice — but if model counting ever becomes
the bottleneck, switching to the positive form is the available lever, at the
price of point 3.

## Regression test

The equivalence is protected by
`SemanticSimilarityEquivalenceTest`, which computes both formulations with the
actual counter on the examples above and asserts that they agree to 1e-6.

---

*If you build on this fitness design, please cite: Brizzio, Cordy, Papadakis,
Sánchez, Aguirre, Degiovanni. "Automated Repair of Unrealisable LTL
Specifications Guided by Model Counting", GECCO 2023
([doi:10.1145/3583131.3590454](https://doi.org/10.1145/3583131.3590454)).*