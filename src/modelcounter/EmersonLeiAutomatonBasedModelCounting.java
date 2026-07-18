package modelcounter;


import it.unimi.dsi.fastutil.ints.IntArrayList;
import jhoafparser.ast.AtomAcceptance;
import jhoafparser.ast.BooleanExpression;
import main.Settings;
import org.apache.commons.math3.fraction.BigFraction;
import org.apache.commons.math3.linear.Array2DRowFieldMatrix;
import org.apache.commons.math3.linear.FieldMatrix;
import owl.automaton.Automaton;
import owl.automaton.acceptance.EmersonLeiAcceptance;
import owl.automaton.edge.Edge;
import owl.ltl.LabelledFormula;
import owl.run.DefaultEnvironment;
import owl.translations.delag.DelagBuilder;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.IntConsumer;

/**
 * AuRUS's <b>approximate bounded LTL model counter</b> (the <i>ApMC</i> of the
 * paper): estimates the number of satisfying traces of bounded length of an
 * LTL formula by a single linear-algebra computation over the formula's
 * automaton.
 *
 * <p>The pipeline has three steps:</p>
 * <ol>
 *   <li><b>LTL &rarr; automaton.</b> The formula is translated, with OWL's
 *       DELAG construction ({@link DelagBuilder}), into a deterministic
 *       automaton with an {@link EmersonLeiAcceptance} condition — an
 *       arbitrary boolean combination of {@code Inf}/{@code Fin} atoms over
 *       acceptance sets.</li>
 *   <li><b>Automaton &rarr; transfer matrix.</b> The automaton is encoded as
 *       an {@code n x n} matrix {@code T} where entry {@code T[i][j]} is the
 *       <i>number of propositional valuations</i> (letters of the alphabet
 *       {@code 2^AP}) that move state {@code s_i} to state {@code s_j}
 *       (see {@link #buildTransferMatrix()}).</li>
 *   <li><b>Counting by matrix exponentiation.</b> The number of accepted
 *       words of length {@code k} is
 *       <pre>   #&#770;(&phi;, k)  =  u &middot; T^k &middot; v</pre>
 *       where {@code u} is the indicator row vector of the initial states and
 *       {@code v} the indicator column vector of the accepting states
 *       (see {@link #count(int)}).</li>
 * </ol>
 *
 * <p><b>What is being approximated.</b> The computation counts the accepted
 * <i>finite</i> words of length {@code k} — the bases of lasso traces —
 * rather than the lasso traces themselves. A single base may close its loop
 * at several positions (under-counting), and a base may reach an "accepting"
 * state although no loop actually satisfies the infinitary acceptance
 * condition (over-counting; see {@link #buildFinalStates()}). The
 * approximation is nevertheless sufficient for the fitness function, which
 * only needs the <i>relative ordering</i> of candidates — empirically
 * preserved in 9 out of 10 benchmark sets at a small fraction of the cost of
 * exact counting.</p>
 *
 * <p>Arithmetic is exact: matrix entries are {@link BigFraction}s and the
 * result a {@link BigInteger}, so the astronomically large counts that arise
 * at higher bounds neither overflow nor lose precision, as floating point
 * would. Both the automaton translation and the counting run in worker
 * threads guarded by timeouts ({@code Settings.PARSING_TIMEOUT} and
 * {@code Settings.MC_TIMEOUT}); on timeout the counter degrades gracefully by
 * returning {@code null}, which callers treat as a failed count.</p>
 *
 * <p>This counter is the central technical contribution of: <i>Brizzio,
 * Cordy, Papadakis, S&aacute;nchez, Aguirre, Degiovanni. "Automated Repair of
 * Unrealisable LTL Specifications Guided by Model Counting", GECCO 2023
 * (<a href="https://doi.org/10.1145/3583131.3590454">doi:10.1145/3583131.3590454</a>).</i>
 * Please cite the paper if you reuse or reimplement the technique. A
 * standalone evolution of this counter is maintained as
 * <a href="https://github.com/MatiasBrizzio/EstiMate">EstiMate</a>.</p>
 *
 * @param <S> the state type of the underlying automaton
 * @author Mat&iacute;as Brizzio
 * @see geneticalgorithm.AutomataBasedModelCountingSpecificationFitness
 */
public class EmersonLeiAutomatonBasedModelCounting<S> {

    /** The labelled LTL formula whose bounded models are counted. */
    private final LabelledFormula formula;

    /** The bound {@code k} of the current {@link #count(int)} invocation. */
    int BOUND = 0;

    /** Scratch accumulator used while counting the valuations of one matrix entry. */
    long transitions = 0;

    /** The transfer matrix {@code T}, built lazily by {@link #countModels()}. */
    private FieldMatrix<BigFraction> T = null;

    /** The Emerson-Lei automaton of {@link #formula}, or {@code null} if translation timed out. */
    private Automaton<S, EmersonLeiAcceptance> automaton = null;

    /** The automaton's states in a fixed order — the index space of the matrix and vectors. */
    private Object[] states = null;


    /**
     * Creates a counter for the given formula and immediately translates it
     * into its Emerson-Lei automaton.
     *
     * <p>The translation runs in a separate worker thread and is abandoned
     * after {@code Settings.PARSING_TIMEOUT} seconds; in that case the
     * automaton stays unbuilt and every subsequent {@link #count(int)}
     * returns {@code null}. The translation is done once here so that
     * repeated counts at different bounds pay it only once.</p>
     *
     * @param formula the labelled LTL formula to count models of
     */
    public EmersonLeiAutomatonBasedModelCounting(LabelledFormula formula) {
        this.formula = formula;
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        // Do the call in a separate thread, get a Future back
        Future<String> future = executorService.submit(this::parse);
        try {
            // Wait for at most TIMEOUT seconds until the result is returned
            future.get(Settings.PARSING_TIMEOUT, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            System.out.println("EmersonLeiAutomatonBasedModelCounting: TIMEOUT parsing.");
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("EmersonLeiAutomatonBasedModelCounting: ERROR while parsing. " + e.getMessage());
        }
    }

    /**
     * Translates {@link #formula} into a deterministic Emerson-Lei automaton
     * with OWL's DELAG builder and fixes the state ordering used as the index
     * space of the transfer matrix.
     *
     * @return the literal {@code "OK"} (the value is irrelevant; the method
     *         runs inside a {@link Future} and only its completion matters)
     */
    private String parse() {
        // Convert the ltl formula to an automaton with OWL
        DelagBuilder translator = new DelagBuilder(DefaultEnvironment.standard());
        automaton = (Automaton<S, EmersonLeiAcceptance>) translator.apply(formula);
        states = automaton.states().toArray();
        return "OK";
    }

    /**
     * Computes the approximate bounded model count
     * {@code #&#770;(formula, bound) = u * T^bound * v}.
     *
     * <p>The computation runs in a worker thread guarded by
     * {@code Settings.MC_TIMEOUT} seconds. It returns {@code null} — rather
     * than throwing — when the automaton was never built (translation
     * timeout), when the count itself times out, or when the worker fails;
     * callers such as the fitness function treat {@code null} as a failed
     * count and fall back to a neutral score.</p>
     *
     * @param bound the trace-length bound {@code k}
     * @return the number of accepted words of length {@code bound}, or
     *         {@code null} if the count could not be completed
     */
    public BigInteger count(int bound) {
        //We compute uTkv, where u is the row vector such that ui = 1 if and only if i is the start state and 0 otherwise,
        // and v is the column vector where vi = 1 if and only if i is an accepting state and 0 otherwise.
        if (states == null)
            return null;
        BOUND = bound;
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        // Do the call in a separate thread, get a Future back
        Future<BigInteger> future = executorService.submit(this::countModels);
        try {
            // Wait for at most TIMEOUT seconds until the result is returned
            return future.get(Settings.MC_TIMEOUT, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            System.out.println("EmersonLeiAutomatonBasedModelCounting::count TIMEOUT.");
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("EmersonLeiAutomatonBasedModelCounting::count ERROR. " + e.getMessage());
        }
        return null;
    }

    /**
     * The actual counting computation: builds the transfer matrix {@code T}
     * and the indicator vectors {@code u} (initial states) and {@code v}
     * (accepting states), raises {@code T} to the power {@link #BOUND}, and
     * evaluates the product {@code u * T^BOUND * v}. The scalar result is a
     * {@link BigFraction} with unit denominator (all entries are integral),
     * so the count is its numerator.
     *
     * @return the approximate number of accepted words of length {@link #BOUND}
     */
    private BigInteger countModels() {
        T = buildTransferMatrix();
//		printMatrix(T);
        int n = T.getRowDimension();

        //set initial states
        FieldMatrix u = buildInitialStates();

        //set final states
        FieldMatrix v = buildFinalStates();

        // count models
        FieldMatrix T_res = T.power(BOUND);
//		printMatrix(T_res);
        FieldMatrix reachable = u.multiply(T_res);
//		System.out.println("reachable: " + reachable.toString());
        FieldMatrix result = reachable.multiply(v);
//		System.out.println("result: " + result.toString());
        BigFraction value = (BigFraction) result.getEntry(0, 0);
        BigInteger count = value.getNumerator();
        return count;
    }

    /**
     * Builds the weighted transfer matrix {@code T} of the automaton: entry
     * {@code T[i][j]} is the number of propositional valuations
     * {@code sigma in 2^AP} whose edge carries state {@code s_i} to state
     * {@code s_j}. Weighting each transition by its valuation count is what
     * lets a single matrix power count <i>words</i> rather than mere paths.
     *
     * <p>Cost: for each of the {@code n^2} state pairs the full valuation
     * universe is enumerated, giving {@code O(n^2 * 2^|AP|)} — this
     * construction, done once per formula, dominates the counter's cost,
     * while each additional bound afterwards is only a matrix power.</p>
     *
     * @return the {@code n x n} transfer matrix with exact
     *         {@link BigFraction} entries
     */
    public FieldMatrix buildTransferMatrix() {

        int n = automaton.size();
        BigFraction[][] pData = new BigFraction[n][n];
        for (int i = 0; i < n; i++) {
            S si = (S) states[i];
            for (int j = 0; j < n; j++) {
                S sj = (S) states[j];
                transitions = 0;
                automaton.factory().universe().forEach(valuation -> {
                    Set<Edge<S>> edges = automaton.edges(si, valuation);
                    for (Edge<S> edge : edges) {
                        if (edge.successor().equals(sj))
                            transitions++;
                    }
                });
                BigFraction v = new BigFraction(transitions);
                pData[i][j] = v;
            }
        }
        return new Array2DRowFieldMatrix<BigFraction>(pData, false);
    }

    /**
     * Builds the indicator row vector {@code u} of the initial states:
     * {@code u[0][j] = 1} iff {@code states[j]} is an initial state of the
     * automaton, {@code 0} otherwise.
     *
     * @return the {@code 1 x n} initial-state vector
     */
    @SuppressWarnings("SuspiciousMethodCalls")
    public FieldMatrix buildInitialStates() {
        int n = T.getRowDimension();
        //set initial states
        FieldMatrix u = createMatrix(1, n);
        Set<S> initial_states = automaton.initialStates();
        for (int j = 0; j < n; j++) {
            if (initial_states.contains(states[j])) {
                u.addToEntry(0, j, new BigFraction(1));
            }
        }
        return u;
    }

    /**
     * Builds the indicator column vector {@code v} of the accepting states —
     * and this is where the finite-word abstraction (and hence the
     * approximation) lives.
     *
     * <p>An Emerson-Lei automaton accepts <i>infinite</i> runs through a
     * condition over the acceptance sets its edges visit infinitely often;
     * finite prefixes have no acceptance of their own. This method derives a
     * finite-word notion of "accepting state" from the accepting
     * <i>edges</i>: a state is marked final iff it is the successor of some
     * edge whose acceptance-set membership satisfies the automaton's boolean
     * acceptance condition (evaluated by
     * {@link #accConditionIsSatisfied(BooleanExpression, IntArrayList)}).
     * Intuitively, a base ending in such a state can plausibly be extended
     * into an accepted lasso. This is precisely the over-approximation
     * discussed in the paper: reaching such a state does not guarantee that a
     * valid loop actually exists from it.</p>
     *
     * @return the {@code n x 1} accepting-state vector
     */
    @SuppressWarnings("SuspiciousMethodCalls")
    public FieldMatrix buildFinalStates() {
        int n = T.getRowDimension();
        //set final states
        Set<S> final_states = new HashSet<>();
        for (S s : automaton.states()) {
            Set<Edge<S>> edges = automaton.edges(s);
            for (Edge<S> edge : edges) {
                //check if it is an acceptance transition
                IntArrayList acceptanceSets = new IntArrayList();
                if (edge.acceptanceSetIterator().hasNext())
                    edge.acceptanceSetIterator().forEachRemaining((IntConsumer) acceptanceSets::add);
                if (accConditionIsSatisfied(automaton.acceptance().booleanExpression(), acceptanceSets)) {
                    final_states.add(edge.successor());
                }
            }
        }

        FieldMatrix v = createMatrix(n, 1);
        for (int i = 0; i < n; i++) {
            if (final_states.contains(states[i])) {
                v.addToEntry(i, 0, new BigFraction(1));
            }
        }
        return v;
    }

    /**
     * Creates a zero matrix of the given dimensions with exact
     * {@link BigFraction} entries.
     *
     * @param row    number of rows
     * @param column number of columns
     * @return a {@code row x column} matrix filled with zeros
     */
    public FieldMatrix createMatrix(int row, int column) {
        BigFraction[][] pData = new BigFraction[row][column];
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < column; j++) {
                pData[i][j] = new BigFraction(0);
            }
        }
        return new Array2DRowFieldMatrix<>(pData, false);
    }

    /**
     * Debug helper: prints a matrix to standard output, one row per line.
     *
     * @param M the matrix to print
     */
    public void printMatrix(FieldMatrix<BigFraction> M) {
        int row = M.getRowDimension();
        int column = M.getColumnDimension();
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < column; j++) {
                System.out.print(M.getEntry(i, j) + " ");
            }
            System.out.println();
        }
    }


    /**
     * Recursively evaluates the automaton's Emerson-Lei acceptance condition
     * — an arbitrary boolean expression in HOA format over
     * {@code Inf}/{@code Fin} atoms — against the acceptance sets visited by
     * a single edge.
     *
     * <p>An {@code Inf(i)} atom is satisfied iff the edge belongs to
     * acceptance set {@code i}; a {@code Fin(i)} atom iff it does not;
     * {@code AND}/{@code OR}/{@code NOT} compose recursively (the operand of
     * the unary {@code NOT} is stored as the right child, following the
     * jhoafparser AST convention). Evaluating the infinitary condition on a
     * single edge is the per-edge heuristic behind the finite-word
     * abstraction of {@link #buildFinalStates()}.</p>
     *
     * @param acceptanceCondition the boolean acceptance expression of the automaton
     * @param acceptanceSets      the acceptance sets the edge belongs to
     * @return {@code true} iff the edge satisfies the condition
     */
    public boolean accConditionIsSatisfied(BooleanExpression<AtomAcceptance> acceptanceCondition, IntArrayList acceptanceSets) {
        boolean accConditionSatisfied = false;
        switch (acceptanceCondition.getType()) {
            case EXP_TRUE: {
                accConditionSatisfied = true;
                break;
            }
            case EXP_FALSE:
                break;
            case EXP_ATOM: {
                if (acceptanceCondition.getAtom().getType() == AtomAcceptance.Type.TEMPORAL_INF)
                    accConditionSatisfied = (acceptanceSets.contains(acceptanceCondition.getAtom().getAcceptanceSet()));
                else if (acceptanceCondition.getAtom().getType() == AtomAcceptance.Type.TEMPORAL_FIN) {
                    accConditionSatisfied = !(acceptanceSets.contains(acceptanceCondition.getAtom().getAcceptanceSet()));
                }
                break;
            }
            case EXP_AND: {
                if (accConditionIsSatisfied(acceptanceCondition.getLeft(), acceptanceSets))
                    accConditionSatisfied = accConditionIsSatisfied(acceptanceCondition.getRight(), acceptanceSets);
                break;
            }
            case EXP_OR: {
                if (accConditionIsSatisfied(acceptanceCondition.getLeft(), acceptanceSets))
                    accConditionSatisfied = true;
                else
                    accConditionSatisfied = accConditionIsSatisfied(acceptanceCondition.getRight(), acceptanceSets);
                break;
            }
            case EXP_NOT: {
                accConditionSatisfied = !accConditionIsSatisfied(acceptanceCondition.getRight(), acceptanceSets);
                break;
            }
        }

        return accConditionSatisfied;
    }


}