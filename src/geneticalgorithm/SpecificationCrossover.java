package geneticalgorithm;

import geneticalgorithm.SpecificationChromosome.SPEC_STATUS;
import main.Settings;
import owl.ltl.BooleanConstant;
import owl.ltl.Formula;
import owl.ltl.LabelledFormula;
import owl.ltl.tlsf.Tlsf;
import tlsf.Formula_Utils;
import tlsf.TLSF_Utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SpecificationCrossover {


	public static List<Tlsf> apply(Tlsf spec0, Tlsf spec1) {
		List<Tlsf> res0 = apply(spec0, spec1, SPEC_STATUS.UNKNOWN, SPEC_STATUS.UNKNOWN, 0);
		List<Tlsf> res1 = apply(spec0, spec1, SPEC_STATUS.UNKNOWN, SPEC_STATUS.UNKNOWN, 1);
		List<Tlsf> res2 = apply(spec0, spec1, SPEC_STATUS.UNKNOWN, SPEC_STATUS.UNKNOWN, 2);
		res0.addAll(res1);
		res0.addAll(res2);
		return res0;
	}

	// level == 0 implements random swap (no guarantee consistency);
	// level == 1 implements random merge of the assumptions and guarantees (no guarantee consistency);
	// level == 2 swaps assumptions and guarantees preserving consistency; and
	// level == 3 merges the assumptions and guarantees preserving consistency.
	// level == 4 merges the formulas assumptions and guarantees preserving consistency.
	public static List<Tlsf> apply(Tlsf spec0, Tlsf spec1, SPEC_STATUS status0, SPEC_STATUS status1, int level) {
		List<Tlsf> merged_specifications = new LinkedList<>();
		List<Formula> assumptionConjuncts = new LinkedList<Formula>();
		List<Formula> guaranteeConjuncts = new LinkedList<Formula>();
		List<Formula> assumesspec0 = Formula_Utils.splitConjunction(spec0.assume());
		List<Formula> assumesspec1 = Formula_Utils.splitConjunction(spec1.assume());
		if (level == 0) {
			// set assume
			if (Settings.RANDOM_GENERATOR.nextBoolean())
				assumptionConjuncts.addAll(assumesspec0);
			else
				assumptionConjuncts.addAll(assumesspec1);

			// set guarantees
			if (Settings.RANDOM_GENERATOR.nextBoolean())
				guaranteeConjuncts.addAll(spec0.guarantee());
			else
				guaranteeConjuncts.addAll(spec1.guarantee());
		}
		else if (level == 1) {
			// set assume
			//if the assumptions can be modified
			if ( Settings.GA_GUARANTEES_PREFERENCE_FACTOR < 100) {
				assumptionConjuncts.addAll(selectRandomly(assumesspec0));
				for (Formula f : selectRandomly(assumesspec1))
					if (!assumptionConjuncts.contains(f))
						assumptionConjuncts.add(f);
			}
			else
				assumptionConjuncts.addAll(assumesspec0);
			// set guarantee
			//if the guarantees can be modified
			if (Settings.GA_GUARANTEES_PREFERENCE_FACTOR > 0) {
				guaranteeConjuncts.addAll(selectRandomly(spec0.guarantee()));
				for (Formula f : selectRandomly(spec1.guarantee()))
					if (!guaranteeConjuncts.contains(f))
						guaranteeConjuncts.add(f);
			}
			else
				guaranteeConjuncts.addAll(spec0.guarantee());

		}
//		else if (level == 2) {
//			// set assume
//			assumptionConjuncts.addAll(assumesspec0);
//			if (!assumesspec1.isEmpty()) {
//				for (int i = 0; i < assumesspec0.size(); i++) {
//					Formula new_formula =  assumesspec1.get(Settings.RANDOM_GENERATOR.nextInt(assumesspec1.size()));
//					if (Settings.RANDOM_GENERATOR.nextBoolean()) {
//						if (!assumptionConjuncts.contains(new_formula)) {
//							assumptionConjuncts.remove(i);
//							assumptionConjuncts.add(i, new_formula);
//						}
//					}
//				}
//			}
//			if (Settings.RANDOM_GENERATOR.nextBoolean() && !assumptionConjuncts.isEmpty()) {
//				int index = Settings.RANDOM_GENERATOR.nextInt(assumptionConjuncts.size());
//				Formula new_formula =  assumptionConjuncts.get(index);
//				assumptionConjuncts.remove(index);
//				assumptionConjuncts.add(index, SpecificationMutator.applyGeneralMutation(new_formula, spec0.variables()));
//			}
//
//			// set guarantee
//			guaranteeConjuncts.addAll(spec0.guarantee());
//			if (!spec1.guarantee().isEmpty()) {
//				List<Formula> g1 = spec1.guarantee();
//				for (int i = 0; i < spec0.guarantee().size(); i++) {
//					Formula new_formula =  g1.get(Settings.RANDOM_GENERATOR.nextInt(g1.size()));
//					if (Settings.RANDOM_GENERATOR.nextBoolean()) {
//						if (!guaranteeConjuncts.contains(new_formula)) {
//							guaranteeConjuncts.remove(i);
//							guaranteeConjuncts.add(i, new_formula);
//						}
//					}
//				}
//			}
//			if (Settings.RANDOM_GENERATOR.nextBoolean() && !guaranteeConjuncts.isEmpty()) {
//				int index = Settings.RANDOM_GENERATOR.nextInt(guaranteeConjuncts.size());
//				Formula new_formula =  guaranteeConjuncts.get(index);
//				guaranteeConjuncts.remove(index);
//				guaranteeConjuncts.add(index, SpecificationMutator.applyGeneralMutation(new_formula, spec0.variables()));
//			}
//
//		}
//		else if (level == 2 && status0.compatible(status1)) {
//			// set assume
//			if (status0.areAssumptionsSAT() && status1.areAssumptionsSAT()) {
//				if (Settings.RANDOM_GENERATOR.nextBoolean())
//					assumptionConjuncts.addAll(assumesspec0);
//				else
//					assumptionConjuncts.addAll(assumesspec1);
//			} else if (status0.areAssumptionsSAT()) {
//				assumptionConjuncts.addAll(assumesspec0);
//			} else if (status1.areAssumptionsSAT()) {
//				assumptionConjuncts.addAll(assumesspec1);
//			}
//
//			// set guarantees
//			if (status0.areGuaranteesSAT() && status1.areGuaranteesSAT()) {
//				if (Settings.RANDOM_GENERATOR.nextBoolean())
//					guaranteeConjuncts.addAll(spec0.guarantee());
//				else
//					guaranteeConjuncts.addAll(spec1.guarantee());
//			} else if (status0.areGuaranteesSAT()) {
//				guaranteeConjuncts.addAll(spec0.guarantee());
//			} else if (status1.areGuaranteesSAT()) {
//				guaranteeConjuncts.addAll(spec1.guarantee());
//			}
//		} else if (level == 3 && status0.compatible(status1)) {
//			// set assumes
//			if (status0.areAssumptionsSAT() && status1.areAssumptionsSAT()) {
//				assumptionConjuncts.addAll(selectRandomly(assumesspec0));
//				assumptionConjuncts.addAll(selectRandomly(assumesspec1));
//			} else if (status0.areAssumptionsSAT()) {
//				assumptionConjuncts.addAll(selectRandomly(assumesspec0));
//			} else if (status1.areAssumptionsSAT()) {
//				assumptionConjuncts.addAll(selectRandomly(assumesspec1));
//			}
//
//			// set guarantees
//			if (status0.areGuaranteesSAT() && status1.areGuaranteesSAT()) {
//				guaranteeConjuncts.addAll(selectRandomly(spec0.guarantee()));
//				guaranteeConjuncts.addAll(selectRandomly(spec1.guarantee()));
//			} else if (status0.areGuaranteesSAT()) {
//				guaranteeConjuncts.addAll(selectRandomly(spec0.guarantee()));
//			} else if (status1.areGuaranteesSAT()) {
//				guaranteeConjuncts.addAll(selectRandomly(spec1.guarantee()));
//			}
//		}
		else { //level == 4 and by default
			// set assume
			//if assumptions can be modified
			if (Settings.GA_GUARANTEES_PREFERENCE_FACTOR < 100 && assumesspec0.size()>0 && assumesspec1.size()>0) {
				assumptionConjuncts.addAll(assumesspec0);
				int size = assumptionConjuncts.size();
				if (size >= 1) {
					Formula merge_ass0 = assumptionConjuncts.remove(Settings.RANDOM_GENERATOR.nextInt(size ));
					Formula merge_ass1 = assumesspec1.get(Settings.RANDOM_GENERATOR.nextInt(assumesspec1.size()));
					// merge ass0 and ass1
					if (merge_ass0 != null && merge_ass1 != null) {
						Formula merged_assumption = null;
						if (Settings.RANDOM_GENERATOR.nextBoolean())
							merged_assumption = Formula_Utils.replaceSubformula(merge_ass0, merge_ass1);
						else {
							merged_assumption = Formula_Utils.combineSubformula(merge_ass0, merge_ass1);
						}
						if (merged_assumption != null && Formula_Utils.numOfTemporalOperators(merged_assumption) <= 2)
							assumptionConjuncts.add(merged_assumption);
					}
				}
			}
			else
				assumptionConjuncts.addAll(assumesspec0);

			// set guarantee
			//if assumptions can be modified
			if (Settings.GA_GUARANTEES_PREFERENCE_FACTOR > 0 && spec0.guarantee().size()>0 && spec1.guarantee().size()>0) {
				guaranteeConjuncts.addAll(spec0.guarantee());
				int size_g = guaranteeConjuncts.size();
				if (size_g >= 1) {
					Formula merge_g0 = guaranteeConjuncts.remove(Settings.RANDOM_GENERATOR.nextInt(size_g));
					Formula merge_g1 = spec1.guarantee().get(Settings.RANDOM_GENERATOR.nextInt(spec1.guarantee().size()));
					// merge g0 and g1
					if (merge_g0 != null && merge_g1 != null) {
						Formula merged_g = null;
						if (Settings.RANDOM_GENERATOR.nextBoolean())
							merged_g = Formula_Utils.replaceSubformula(merge_g0, merge_g1);
						else {
							merged_g = Formula_Utils.combineSubformula(merge_g0, merge_g1);
						}
						if (merged_g != null && Formula_Utils.numOfTemporalOperators(merged_g) <= 2)
							guaranteeConjuncts.add(merged_g);
					}
				}
			}
			else
				guaranteeConjuncts.addAll(spec0.guarantee());
		}
		if (!guaranteeConjuncts.isEmpty()) {
			Tlsf new_spec = TLSF_Utils.change_assume(spec0, assumptionConjuncts);
			new_spec = TLSF_Utils.change_guarantees(new_spec, guaranteeConjuncts);
			merged_specifications.add(new_spec);
		}

		return merged_specifications;
	}

	private static List<Formula> selectRandomly(List<Formula> formulas) {
		List<Formula> selectedFormulas = new LinkedList<Formula>();
		for (Formula f : formulas) {
			if (Settings.RANDOM_GENERATOR.nextBoolean() && f != BooleanConstant.TRUE && !selectedFormulas.contains(f))
				selectedFormulas.add(f);
		}
		return selectedFormulas;
	}
}