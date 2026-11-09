package modelcounter;

import java.util.List;

/** The automaton behind a count, as returned by {@link ModelCounter#dump}. */
public final class AutomatonDump {

    /** Largest alphabet that can be dumped, in variables. */
    public static final int MAX_VARIABLES = 10;

    /** Successor of a state that has no transition for a letter. */
    public static final int NO_SUCCESSOR = -1;

    /** One transition; {@code letter} is a bit mask over {@link #variables()}. */
    public static final class Transition {
        public final int state;
        public final int letter;
        public final int successor;
        public final boolean accepting;

        Transition(int state, int letter, int successor, boolean accepting) {
            this.state = state;
            this.letter = letter;
            this.successor = successor;
            this.accepting = accepting;
        }
    }

    private final List<String> variables;
    private final int stateCount;
    private final List<Integer> initialStates;
    private final List<Integer> finalStates;
    private final List<Transition> transitions;

    AutomatonDump(List<String> variables, int stateCount, List<Integer> initialStates,
                  List<Integer> finalStates, List<Transition> transitions) {
        this.variables = List.copyOf(variables);
        this.stateCount = stateCount;
        this.initialStates = List.copyOf(initialStates);
        this.finalStates = List.copyOf(finalStates);
        this.transitions = List.copyOf(transitions);
    }

    public List<String> variables() {
        return variables;
    }

    public int stateCount() {
        return stateCount;
    }

    public List<Integer> initialStates() {
        return initialStates;
    }

    public List<Integer> finalStates() {
        return finalStates;
    }

    public List<Transition> transitions() {
        return transitions;
    }
}
