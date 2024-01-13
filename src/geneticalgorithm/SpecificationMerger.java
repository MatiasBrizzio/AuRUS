package geneticalgorithm;

import geneticalgorithm.SpecificationChromosome.SPEC_STATUS;
import main.Settings;
import owl.ltl.Formula;
import owl.ltl.LabelledFormula;
import owl.ltl.tlsf.Tlsf;
import tlsf.Formula_Utils;
import tlsf.TLSF_Utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class SpecificationMerger {
    public static List<Tlsf> merge(Tlsf spec0, Tlsf spec1) {
        return merge(spec0, spec1, SPEC_STATUS.UNKNOWN, SPEC_STATUS.UNKNOWN, 0);
    }
    public static List<Tlsf> merge(Tlsf spec0, Tlsf spec1, SPEC_STATUS status0, SPEC_STATUS status1) {
        List<Tlsf> merged_specifications = new LinkedList<>();
        List<Formula> assumptionConjuncts = new LinkedList<>();
        //take assumptions from spec0?
        if (Settings.RANDOM_GENERATOR.nextBoolean()) {
            for (Formula c : Formula_Utils.splitConjunction(spec0.assume()))
                if (!assumptionConjuncts.contains(c))
                    assumptionConjuncts.add(c);
        }
        //take assumptions from spec1?
        if (Settings.RANDOM_GENERATOR.nextBoolean()) {
            for (Formula c : Formula_Utils.splitConjunction(spec1.assume()))
                if (!assumptionConjuncts.contains(c))
                    assumptionConjuncts.add(c);
        }

        List<Formula> guaranteeConjuncts = new LinkedList<>();
        //take guarantees from spec0?
        if (Settings.RANDOM_GENERATOR.nextBoolean()) {
            for (Formula c : spec0.guarantee())
                if (!guaranteeConjuncts.contains(c))
                    guaranteeConjuncts.add(c);
        }
        //take guarantees from spec1?
        if (Settings.RANDOM_GENERATOR.nextBoolean()) {
            for (Formula c : spec1.guarantee())
                if (!guaranteeConjuncts.contains(c))
                    guaranteeConjuncts.add(c);
        }

        //new spec0 versions with the new assumptions and guarantees
        //Tlsf new_spec0 = TLSF_Utils.fromSpec(spec0);
		/*if (Settings.RANDOM_GENERATOR.nextBoolean())
			merged_specifications.add(TLSF_Utils.change_assume(spec0,new_assume));
		else
			merged_specifications.add(TLSF_Utils.change_guarantees(spec0,new_guarantee));

		//new spec1 versions with the new assumptions and guarantees
		//Tlsf new_spec1 = TLSF_Utils.fromSpec(spec1);
		if (Settings.RANDOM_GENERATOR.nextBoolean())
			merged_specifications.add(TLSF_Utils.change_assume(spec1,new_assume));
		else
			merged_specifications.add(TLSF_Utils.change_guarantees(spec1,new_guarantee));*/

        //new specification with the new assumptions and guarantees
        if (!guaranteeConjuncts.isEmpty()) {
            Tlsf new_spec = TLSF_Utils.change_assume(spec0, assumptionConjuncts);
            new_spec = TLSF_Utils.change_guarantees(new_spec, guaranteeConjuncts);
            merged_specifications.add(new_spec);
        }
        return merged_specifications;
    }


    // level == 0 implements random swap (no guarantee consistency);
    // level == 1 implements random merge of the assumptions and guarantees (no guarantee consistency);
    // level == 2 swaps assumptions and guarantees preserving consistency; and
    // level == 3 merges the assumptions and guarantees preserving consistency.
    public static List<Tlsf> merge(Tlsf spec0, Tlsf spec1, SPEC_STATUS status0, SPEC_STATUS status1, int level) {
        List<Tlsf> merged_specifications = new LinkedList<>();
        int turn;
        if (level == 0) {
            //create empty specification
            Tlsf new_spec = TLSF_Utils.fromSpec(spec0);
            // set assume
            turn = Settings.RANDOM_GENERATOR.nextInt(2);
            if (turn == 0)
                new_spec = TLSF_Utils.change_assume(new_spec, spec0.assume());
            else
                new_spec = TLSF_Utils.change_assume(new_spec, spec1.assume());

            // set guarantees
            turn = Settings.RANDOM_GENERATOR.nextInt(2);
            if (turn == 0)
                new_spec = TLSF_Utils.change_guarantees(new_spec, spec0.guarantee());
            else
                new_spec = TLSF_Utils.change_guarantees(new_spec, spec1.guarantee());

            merged_specifications.add(new_spec);
        } else if (level == 1) {
            //create empty specification
            Tlsf new_spec = TLSF_Utils.fromSpec(spec0);

            // set assume
            LabelledFormula assumspec0 = LabelledFormula.of(spec0.assume(), spec0.variables());
            List<LabelledFormula> assumesspec0 = Formula_Utils.splitConjunction(assumspec0);

            LabelledFormula assumspec1 = LabelledFormula.of(spec1.assume(), spec1.variables());
            List<LabelledFormula> assumesspec1 = Formula_Utils.splitConjunction(assumspec1);

            List<Formula> f = getRandomFormulas(assumesspec0);
            f.addAll(getRandomFormulas(assumesspec1));
            new_spec = TLSF_Utils.change_assume(new_spec, f);

            // set guarantee
            new_spec = TLSF_Utils.change_guarantees(new_spec, mergeGuarantess(spec0.guarantee(), spec1.guarantee()));

            merged_specifications.add(new_spec);
        } else if (level == 2 && status0.compatible(status1)) {

            //create empty specification
            Tlsf new_spec = TLSF_Utils.fromSpec(spec0);

            // set assume
            if (status0.areAssumptionsSAT() && status1.areAssumptionsSAT()) {
                turn = Settings.RANDOM_GENERATOR.nextInt(2);
                if (turn == 0)
                    new_spec = TLSF_Utils.change_assume(new_spec, spec0.assume());
                else
                    new_spec = TLSF_Utils.change_assume(new_spec, spec1.assume());
            } else if (status0.areAssumptionsSAT()) {
                new_spec = TLSF_Utils.change_assume(new_spec, spec0.assume());
            } else if (status1.areAssumptionsSAT()) {
                new_spec = TLSF_Utils.change_assume(new_spec, spec1.assume());
            }

            // set guarantees
            if (status0.areGuaranteesSAT() && status1.areGuaranteesSAT()) {
                turn = Settings.RANDOM_GENERATOR.nextInt(2);
                if (turn == 0)
                    new_spec = TLSF_Utils.change_guarantees(new_spec, spec0.guarantee());
                else
                    new_spec = TLSF_Utils.change_guarantees(new_spec, spec1.guarantee());
            } else if (status0.areGuaranteesSAT()) {
                new_spec = TLSF_Utils.change_guarantees(new_spec, spec0.guarantee());
            } else if (status1.areGuaranteesSAT()) {
                new_spec = TLSF_Utils.change_guarantees(new_spec, spec1.guarantee());
            }

            merged_specifications.add(new_spec);
        } else if (level == 3 && status0.compatible(status1)) {
            //create empty specification
            Tlsf new_spec = TLSF_Utils.fromSpec(spec0);

            // set assume
            if (status0.areAssumptionsSAT() && status1.areAssumptionsSAT()) {
                LabelledFormula assumspec0 = LabelledFormula.of(spec0.assume(), spec0.variables());
                List<LabelledFormula> assumesspec0 = Formula_Utils.splitConjunction(assumspec0);

                LabelledFormula assumspec1 = LabelledFormula.of(spec1.assume(), spec1.variables());
                List<LabelledFormula> assumesspec1 = Formula_Utils.splitConjunction(assumspec1);

                List<Formula> f = getRandomFormulas(assumesspec0);
                f.addAll(getRandomFormulas(assumesspec1));
                new_spec = TLSF_Utils.change_assume(new_spec, f);
            } else if (status0.areAssumptionsSAT()) {
                new_spec = TLSF_Utils.change_assume(new_spec, spec0.assume());
            } else if (status1.areAssumptionsSAT()) {
                new_spec = TLSF_Utils.change_assume(new_spec, spec1.assume());
            }

            // set guarantees
            if (status0.areGuaranteesSAT() && status1.areGuaranteesSAT()) {
                new_spec = TLSF_Utils.change_guarantees(new_spec, mergeGuarantess(spec0.guarantee(), spec1.guarantee()));
            } else if (status0.areGuaranteesSAT()) {
                new_spec = TLSF_Utils.change_guarantees(new_spec, spec0.guarantee());
            } else if (status1.areGuaranteesSAT()) {
                new_spec = TLSF_Utils.change_guarantees(new_spec, spec1.guarantee());
            }
            merged_specifications.add(new_spec);
        }
        return merged_specifications;
    }

    private static List<Formula> mergeGuarantess(List<Formula> guarantee, List<Formula> guarantee2) {
        List<Formula> newg = new ArrayList<>();
        List<Formula> newg2 = new ArrayList<>();
        Formula selectedFormula;
        if (!guarantee.isEmpty()) {
            int amountOfFormulas1 = Settings.RANDOM_GENERATOR.nextInt((guarantee.size())) + 1;
            for (int i = 0; i < amountOfFormulas1; i++) {
                selectedFormula = guarantee.get(Settings.RANDOM_GENERATOR.nextInt(guarantee.size()));
                if (newg.contains(selectedFormula)) continue;
                else newg.add(selectedFormula);
            }
        }
        if (!guarantee2.isEmpty()) {
            int amountOfFormulas2 = Settings.RANDOM_GENERATOR.nextInt(guarantee2.size()) + 1;
            for (int i = 0; i < amountOfFormulas2; i++) {
                selectedFormula = guarantee2.get(Settings.RANDOM_GENERATOR.nextInt(guarantee2.size()));
                if (newg.contains(selectedFormula) || newg2.contains(selectedFormula)) continue;
                else newg2.add(selectedFormula);
            }
        }

        newg.addAll(newg2);
        return newg;
    }

    private static List<Formula> getRandomFormulas(List<LabelledFormula> assumesspec0) {
        int amountOfFormulas = !assumesspec0.isEmpty() ? Settings.RANDOM_GENERATOR.nextInt(assumesspec0.size()) + 1 : 0;

        List<LabelledFormula> newAssumes = new ArrayList<>();
        LabelledFormula selectedFormula;

        for (int i = 0; i < amountOfFormulas; i++) {
            selectedFormula = assumesspec0.get(Settings.RANDOM_GENERATOR.nextInt(assumesspec0.size()));
            if (newAssumes.contains(selectedFormula)) continue;
            else newAssumes.add(selectedFormula);
        }
        return newAssumes.stream().map(LabelledFormula::formula).collect(Collectors.toList());
    }

}
