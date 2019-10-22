package geneticalgorithm;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import geneticalgorithm.SpecificationChromosome.SPEC_STATUS;
import owl.ltl.BooleanConstant;
import owl.ltl.Conjunction;
import owl.ltl.Disjunction;
import owl.ltl.Formula;
import owl.ltl.LabelledFormula;
import owl.ltl.rewriter.NormalForms;
import owl.ltl.tlsf.Tlsf;
import tlsf.Formula_Utils;
import tlsf.TLSF_Utils;

public class SpecificationMerger {
	
	
	public static List<Tlsf> merge(Tlsf spec0, Tlsf spec1) {
		return merge(spec0, spec1, SPEC_STATUS.UNKNOWN, SPEC_STATUS.UNKNOWN, 0);
	}

	public static List<Tlsf> merge(Tlsf spec0, Tlsf spec1, SPEC_STATUS status0, SPEC_STATUS status1) {
		List<Tlsf> merged_specifications = new LinkedList<>();
		
		
//		if (Settings.RANDOM_GENERATOR.nextBoolean()) {
		// crossover assume
		//create empty specification
			Tlsf new_spec = TLSF_Utils.fromSpec(spec0);
			int option = Settings.RANDOM_GENERATOR.nextInt(3);
			//arbitrary merge of assumptions
			if (option == 0) { 
				LabelledFormula assumespec0 =  LabelledFormula.of(spec0.assume(), spec0.variables());
				List<LabelledFormula> assumesspec0 = Formula_Utils.splitConjunction(assumespec0);
				
				LabelledFormula assumspec1 =  LabelledFormula.of(spec1.assume(), spec1.variables());
				List<LabelledFormula> assumesspec1 = Formula_Utils.splitConjunction(assumspec1);
				
				List<Formula> f = getRandomFormulas(assumesspec0);
				for (Formula s : getRandomFormulas(assumesspec1))
					if (!f.contains(s))
						f.add(s);		
				new_spec = TLSF_Utils.change_assume(new_spec,f);
			}
			else if (option == 1){
				//if (status0 != SPEC_STATUS.UNREALIZABLE && status1 != SPEC_STATUS.UNREALIZABLE) {
				
				// weaken assumptions
				List<Formula> conjuncts0 = new LinkedList<Formula>();
				for (Set<Formula> clause : NormalForms.toCnf(spec0.assume())){
					Formula f = Disjunction.of(clause);
					if (!conjuncts0.contains(f))
						conjuncts0.add(f);
				}
				
				List<Formula> conjuncts1 = new LinkedList<Formula>();
				for (Set<Formula> clause : NormalForms.toCnf(spec1.assume())){
					Formula f = Disjunction.of(clause);
					if (!conjuncts1.contains(f))
						conjuncts1.add(f);
				}
				
				List<Formula> listOfConjuncts = new LinkedList<Formula>();
				for (Formula c : conjuncts0) 
					if (conjuncts1.contains(c))
						listOfConjuncts.add(c);
				conjuncts0.removeAll(listOfConjuncts);
				conjuncts1.removeAll(listOfConjuncts);
				Formula assume0 = BooleanConstant.TRUE;
				if (!conjuncts0.isEmpty())
					assume0 = Conjunction.of(conjuncts0);
				Formula assume1 = BooleanConstant.TRUE;
				if (!conjuncts1.isEmpty())
					assume1 = Conjunction.of(conjuncts1);
				if(assume0 != BooleanConstant.TRUE && assume1 != BooleanConstant.TRUE)
					listOfConjuncts.add(Disjunction.of(assume0,assume1));
				if (listOfConjuncts.isEmpty())
					listOfConjuncts.add(BooleanConstant.TRUE);
				new_spec = TLSF_Utils.change_assume(new_spec, listOfConjuncts);
			}
			else{ // (option ==2)
				// strengthen assumptions
				List<Formula> conjuncts = new LinkedList<Formula>();
				for (Set<Formula> clause :NormalForms.toCnf(spec0.assume())){
					Formula f = Disjunction.of(clause);
					if (!conjuncts.contains(f))
						conjuncts.add(f);
				}
				for (Set<Formula> clause :NormalForms.toCnf(spec1.assume())) {
					Formula f = Disjunction.of(clause);
					if (!conjuncts.contains(f))
						conjuncts.add(f);
				}
				
				new_spec = TLSF_Utils.change_assume(new_spec, conjuncts);
			}
			merged_specifications.add(new_spec);
			
//		}
//		else {	
		// crossover guarantees
			new_spec = TLSF_Utils.fromSpec(spec0);
			option = Settings.RANDOM_GENERATOR.nextInt(3);
			//arbitrary merge of assumptions
			if (option == 0) { 
				new_spec = TLSF_Utils.change_guarantees(new_spec, mergeGuarantess(spec0.guarantee(),spec1.guarantee()));		
			}
			else if (option == 1){
	//			if (status0 != SPEC_STATUS.REALIZABLE && status1 != SPEC_STATUS.REALIZABLE) {
				// weaken guarantees
				List<Formula> conjuncts0 = new LinkedList<Formula>();
				for (Formula f : spec0.guarantee()){
					if (!conjuncts0.contains(f))
						conjuncts0.add(f);
				}
				
				List<Formula> conjuncts1 = new LinkedList<Formula>();
				for (Formula f : spec1.guarantee()){
					if (!conjuncts1.contains(f))
						conjuncts1.add(f);
				}
				
				List<Formula> listOfConjuncts = new LinkedList<Formula>();
				for (Formula c : conjuncts0) 
					if (conjuncts1.contains(c))
						listOfConjuncts.add(c);
				conjuncts0.removeAll(listOfConjuncts);
				conjuncts1.removeAll(listOfConjuncts);
				Formula guarantee0 = BooleanConstant.TRUE;
				if (!conjuncts0.isEmpty())
					guarantee0 = Conjunction.of(conjuncts0);
				Formula guarantee1 = BooleanConstant.TRUE;
				if (!conjuncts1.isEmpty())
					guarantee1 = Conjunction.of(conjuncts1);
				if(guarantee0 != BooleanConstant.TRUE && guarantee1 != BooleanConstant.TRUE)
					listOfConjuncts.add(Disjunction.of(guarantee0,guarantee1));
				if (listOfConjuncts.isEmpty())
					listOfConjuncts.add(BooleanConstant.TRUE);
				new_spec = TLSF_Utils.change_guarantees(new_spec, listOfConjuncts);
			}
			else{
				// strengthen guarantees to look for finer grained solutions
				List<Formula> conjuncts = new LinkedList<Formula>();
				for (Formula s : spec0.guarantee())
					if (!conjuncts.contains(s))
						conjuncts.add(s);
				for (Formula s : spec1.guarantee())
					if (!conjuncts.contains(s))
						conjuncts.add(s);
				new_spec = TLSF_Utils.change_guarantees(new_spec, conjuncts);
			}
			merged_specifications.add(new_spec);
//		}
		
			
		return merged_specifications;
		
	}
	
	// level == 0 implements random swap (no guarantee consistency); 
	// level == 1 implements random merge of the assumptions and guarantees (no guarantee consistency);
	// level == 2 swaps assumptions and guarantees preserving consistency; and
	// level == 3 merges the assumptions and guarantees preserving consistency.
	public static List<Tlsf> merge(Tlsf spec0, Tlsf spec1, SPEC_STATUS status0, SPEC_STATUS status1, int level) {
		List<Tlsf> merged_specifications = new LinkedList<>();
		if (level == 0) {
			//create empty specification
			Tlsf new_spec = TLSF_Utils.fromSpec(spec0);
			
//			// set initially
			int turn = Settings.RANDOM_GENERATOR.nextInt(2);
//			if (turn == 0)
//				new_spec = TLSF_Utils.change_initially(new_spec, spec0.initially());
//			else 
//				new_spec = TLSF_Utils.change_initially(new_spec, spec1.initially());
//			
//			// set preset
//			turn = Settings.RANDOM_GENERATOR.nextInt(2);
//			if (turn == 0)
//				new_spec = TLSF_Utils.change_preset(new_spec, spec0.preset());
//			else 
//				new_spec = TLSF_Utils.change_preset(new_spec, spec1.preset());
//			
//			// set require
//			turn = Settings.RANDOM_GENERATOR.nextInt(2);
//			if (turn == 0)
//				new_spec = TLSF_Utils.change_require(new_spec, spec0.require());
//			else 
//				new_spec = TLSF_Utils.change_require(new_spec, spec1.require());
//			
//			// set assert
//			turn = Settings.RANDOM_GENERATOR.nextInt(2);
//			if (turn == 0)
//				new_spec = TLSF_Utils.change_assert(new_spec, spec0.assert_());
//			else 
//				new_spec = TLSF_Utils.change_assert(new_spec, spec1.assert_());
			
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
		}
		else if (level == 1) {
			//create empty specification
			Tlsf new_spec = TLSF_Utils.fromSpec(spec0);
			
//			// set initially
//			LabelledFormula init0 =  LabelledFormula.of(spec0.initially(), spec0.variables());
//			List<LabelledFormula> contraints_init0 = Formula_Utils.splitConjunction(init0);
//			
//			LabelledFormula init1 =  LabelledFormula.of(spec1.initially(), spec1.variables());
//			List<LabelledFormula> contraints_init1 = Formula_Utils.splitConjunction(init1);
//			
//			List<Formula> new_init = getRandomFormulas(contraints_init0);
//			new_init.addAll(getRandomFormulas(contraints_init1));		
//			new_spec = TLSF_Utils.change_initially(new_spec,Conjunction.of(new_init));
//			
//			// set preset
//			LabelledFormula preset0 =  LabelledFormula.of(spec0.preset(), spec0.variables());
//			List<LabelledFormula> contraints_preset0 = Formula_Utils.splitConjunction(preset0);
//			
//			LabelledFormula preset1 =  LabelledFormula.of(spec1.preset(), spec1.variables());
//			List<LabelledFormula> contraints_preset1 = Formula_Utils.splitConjunction(preset1);
//			
//			List<Formula> new_preset = getRandomFormulas(contraints_preset0);
//			new_preset.addAll(getRandomFormulas(contraints_preset1));		
//			new_spec = TLSF_Utils.change_preset(new_spec,Conjunction.of(new_preset));
//			
//			// set require
//			LabelledFormula require0 =  LabelledFormula.of(spec0.require(), spec0.variables());
//			List<LabelledFormula> contraints_require0 = Formula_Utils.splitConjunction(require0);
//			
//			LabelledFormula require1 =  LabelledFormula.of(spec1.require(), spec1.variables());
//			List<LabelledFormula> contraints_require1 = Formula_Utils.splitConjunction(require1);
//			
//			List<Formula> new_require = getRandomFormulas(contraints_require0);
//			new_require.addAll(getRandomFormulas(contraints_require1));		
//			new_spec = TLSF_Utils.change_require(new_spec,Conjunction.of(new_require));
//			
//			// set assert
//			List<LabelledFormula> contraints_assert0 = new LinkedList<LabelledFormula>();
//			for (Formula a : spec0.assert_())
//				contraints_assert0.add(LabelledFormula.of(a, spec0.variables()));
//			
//			List<LabelledFormula> contraints_assert1 = new LinkedList<LabelledFormula>();
//			for (Formula a : spec1.assert_())
//				contraints_assert1.add(LabelledFormula.of(a, spec1.variables()));
//			
//			List<Formula> new_assert = getRandomFormulas(contraints_assert0);
//			new_assert.addAll(getRandomFormulas(contraints_assert1));		
//			new_spec = TLSF_Utils.change_assert(new_spec,new_assert);
			
			// set assume
			LabelledFormula assumspec0 =  LabelledFormula.of(spec0.assume(), spec0.variables());
			List<LabelledFormula> assumesspec0 = Formula_Utils.splitConjunction(assumspec0);
			
			LabelledFormula assumspec1 =  LabelledFormula.of(spec1.assume(), spec1.variables());
			List<LabelledFormula> assumesspec1 = Formula_Utils.splitConjunction(assumspec1);
			
			List<Formula> f = getRandomFormulas(assumesspec0);
			f.addAll(getRandomFormulas(assumesspec1));		
			new_spec = TLSF_Utils.change_assume(new_spec,f);
			
			// set guarantee
			new_spec = TLSF_Utils.change_guarantees(new_spec, mergeGuarantess(spec0.guarantee(),spec1.guarantee()));		
			
			merged_specifications.add(new_spec);
		}
		else if (level == 2 && status0.compatible(status1)) {
			
			//create empty specification
			Tlsf new_spec = TLSF_Utils.fromSpec(spec0);
			int turn = 0;
			
//			// set initially
//			if (status0.areAssumptionsSAT() && status1.areAssumptionsSAT()) {
//				turn = Settings.RANDOM_GENERATOR.nextInt(2);
//				if (turn == 0)
//					new_spec = TLSF_Utils.change_initially(new_spec, spec0.initially());
//				else 
//					new_spec = TLSF_Utils.change_initially(new_spec, spec1.initially());
//			}
//			else if (status0.areAssumptionsSAT()) {
//					new_spec = TLSF_Utils.change_initially(new_spec, spec0.initially());
//			}
//			else if (status1.areAssumptionsSAT()) {
//				new_spec = TLSF_Utils.change_initially(new_spec, spec1.initially());
//			}
//			
//			// set preset
//			if (status0.areGuaranteesSAT() && status1.areGuaranteesSAT()) {
//				turn = Settings.RANDOM_GENERATOR.nextInt(2);
//				if (turn == 0)
//					new_spec = TLSF_Utils.change_preset(new_spec, spec0.preset());
//				else 
//					new_spec = TLSF_Utils.change_preset(new_spec, spec1.preset());
//			}
//			else if (status0.areGuaranteesSAT()) {
//					new_spec = TLSF_Utils.change_preset(new_spec, spec0.preset());
//			}
//			else if (status1.areGuaranteesSAT()) {
//				new_spec = TLSF_Utils.change_preset(new_spec, spec1.preset());
//			}
//			
//			// set require
//			if (status0.areAssumptionsSAT() && status1.areAssumptionsSAT()) {
//				turn = Settings.RANDOM_GENERATOR.nextInt(2);
//				if (turn == 0)
//					new_spec = TLSF_Utils.change_require(new_spec, spec0.require());
//				else 
//					new_spec = TLSF_Utils.change_require(new_spec, spec1.require());
//			}
//			else if (status0.areAssumptionsSAT()) {
//				new_spec = TLSF_Utils.change_require(new_spec, spec0.require());
//			}
//			else if (status1.areAssumptionsSAT()) {
//				new_spec = TLSF_Utils.change_require(new_spec, spec1.require());
//			}
//			
//			// set assert
//			if (status0.areGuaranteesSAT() && status1.areGuaranteesSAT()) {
//				turn = Settings.RANDOM_GENERATOR.nextInt(2);
//				if (turn == 0)
//					new_spec = TLSF_Utils.change_assert(new_spec, spec0.assert_());
//				else 
//					new_spec = TLSF_Utils.change_assert(new_spec, spec1.assert_());
//			}
//			else if (status0.areGuaranteesSAT()) {
//					new_spec = TLSF_Utils.change_assert(new_spec, spec0.assert_());
//			}
//			else if (status1.areGuaranteesSAT()) {
//				new_spec = TLSF_Utils.change_assert(new_spec, spec1.assert_());
//			}
			
			// set assume
			if (status0.areAssumptionsSAT() && status1.areAssumptionsSAT()) {
				turn = Settings.RANDOM_GENERATOR.nextInt(2);
				if (turn == 0)
					new_spec = TLSF_Utils.change_assume(new_spec, spec0.assume());
				else 
					new_spec = TLSF_Utils.change_assume(new_spec, spec1.assume());
			}
			else if (status0.areAssumptionsSAT()) {
					new_spec = TLSF_Utils.change_assume(new_spec, spec0.assume());
			}
			else if (status1.areAssumptionsSAT()) {
				new_spec = TLSF_Utils.change_assume(new_spec, spec1.assume());
			}
			
			// set guarantees
			if (status0.areGuaranteesSAT() && status1.areGuaranteesSAT()) {
				turn = Settings.RANDOM_GENERATOR.nextInt(2);
				if (turn == 0)
					new_spec = TLSF_Utils.change_guarantees(new_spec, spec0.guarantee());
				else 
					new_spec = TLSF_Utils.change_guarantees(new_spec, spec1.guarantee());
			}
			else if (status0.areGuaranteesSAT()) {
					new_spec = TLSF_Utils.change_guarantees(new_spec, spec0.guarantee());
			}
			else if (status1.areGuaranteesSAT()) {
				new_spec = TLSF_Utils.change_guarantees(new_spec, spec1.guarantee());
			}
			
			merged_specifications.add(new_spec);
		}
		else if (level == 3 && status0.compatible(status1)) {
			//create empty specification
			Tlsf new_spec = TLSF_Utils.fromSpec(spec0);
			
//			// set initially
//			if (status0.areAssumptionsSAT() && status1.areAssumptionsSAT()) {
//				LabelledFormula init0 =  LabelledFormula.of(spec0.initially(), spec0.variables());
//				List<LabelledFormula> contraints_init0 = Formula_Utils.splitConjunction(init0);
//				
//				LabelledFormula init1 =  LabelledFormula.of(spec1.initially(), spec1.variables());
//				List<LabelledFormula> contraints_init1 = Formula_Utils.splitConjunction(init1);
//				
//				List<Formula> new_init = getRandomFormulas(contraints_init0);
//				new_init.addAll(getRandomFormulas(contraints_init1));
//				
//				new_spec = TLSF_Utils.change_initially(new_spec,Conjunction.of(new_init));
//			}
//			else if (status0.areAssumptionsSAT()) {
//					new_spec = TLSF_Utils.change_initially(new_spec, spec0.initially());
//			}
//			else if (status1.areAssumptionsSAT()) {
//				new_spec = TLSF_Utils.change_initially(new_spec, spec1.initially());
//			}
//			
//			// set preset
//			if (status0.areGuaranteesSAT() && status1.areGuaranteesSAT()) {
//				LabelledFormula preset0 =  LabelledFormula.of(spec0.preset(), spec0.variables());
//				List<LabelledFormula> contraints_preset0 = Formula_Utils.splitConjunction(preset0);
//				
//				LabelledFormula preset1 =  LabelledFormula.of(spec1.preset(), spec1.variables());
//				List<LabelledFormula> contraints_preset1 = Formula_Utils.splitConjunction(preset1);
//				
//				List<Formula> new_preset = getRandomFormulas(contraints_preset0);
//				new_preset.addAll(getRandomFormulas(contraints_preset1));		
//				new_spec = TLSF_Utils.change_preset(new_spec,Conjunction.of(new_preset));
//			}
//			else if (status0.areGuaranteesSAT()) {
//					new_spec = TLSF_Utils.change_preset(new_spec, spec0.preset());
//			}
//			else if (status1.areGuaranteesSAT()) {
//				new_spec = TLSF_Utils.change_preset(new_spec, spec1.preset());
//			}
//			
//			// set require
//			if (status0.areAssumptionsSAT() && status1.areAssumptionsSAT()) {
//				LabelledFormula require0 =  LabelledFormula.of(spec0.require(), spec0.variables());
//				List<LabelledFormula> contraints_require0 = Formula_Utils.splitConjunction(require0);
//				
//				LabelledFormula require1 =  LabelledFormula.of(spec1.require(), spec1.variables());
//				List<LabelledFormula> contraints_require1 = Formula_Utils.splitConjunction(require1);
//				
//				List<Formula> new_require = getRandomFormulas(contraints_require0);
//				new_require.addAll(getRandomFormulas(contraints_require1));		
//				new_spec = TLSF_Utils.change_require(new_spec,Conjunction.of(new_require));
//			}
//			else if (status0.areAssumptionsSAT()) {
//				new_spec = TLSF_Utils.change_require(new_spec, spec0.require());
//			}
//			else if (status1.areAssumptionsSAT()) {
//				new_spec = TLSF_Utils.change_require(new_spec, spec1.require());
//			}
//			
//			// set assert
//			if (status0.areGuaranteesSAT() && status1.areGuaranteesSAT()) {
//				List<LabelledFormula> contraints_assert0 = new LinkedList<LabelledFormula>();
//				for (Formula a : spec0.assert_())
//					contraints_assert0.add(LabelledFormula.of(a, spec0.variables()));
//				
//				List<LabelledFormula> contraints_assert1 = new LinkedList<LabelledFormula>();
//				for (Formula a : spec1.assert_())
//					contraints_assert1.add(LabelledFormula.of(a, spec1.variables()));
//				
//				List<Formula> new_assert = getRandomFormulas(contraints_assert0);
//				new_assert.addAll(getRandomFormulas(contraints_assert1));		
//				new_spec = TLSF_Utils.change_assert(new_spec,new_assert);
//			}
//			else if (status0.areGuaranteesSAT()) {
//					new_spec = TLSF_Utils.change_assert(new_spec, spec0.assert_());
//			}
//			else if (status1.areGuaranteesSAT()) {
//				new_spec = TLSF_Utils.change_assert(new_spec, spec1.assert_());
//			}
			
			// set assume
			if (status0.areAssumptionsSAT() && status1.areAssumptionsSAT()) {
				LabelledFormula assumspec0 =  LabelledFormula.of(spec0.assume(), spec0.variables());
				List<LabelledFormula> assumesspec0 = Formula_Utils.splitConjunction(assumspec0);
				
				LabelledFormula assumspec1 =  LabelledFormula.of(spec1.assume(), spec1.variables());
				List<LabelledFormula> assumesspec1 = Formula_Utils.splitConjunction(assumspec1);
				
				List<Formula> f = getRandomFormulas(assumesspec0);
				f.addAll(getRandomFormulas(assumesspec1));	
				new_spec = TLSF_Utils.change_assume(new_spec,f);
			}
			else if (status0.areAssumptionsSAT()) {
					new_spec = TLSF_Utils.change_assume(new_spec, spec0.assume());
			}
			else if (status1.areAssumptionsSAT()) {
				new_spec = TLSF_Utils.change_assume(new_spec, spec1.assume());
			}
			
			// set guarantees
			if (status0.areGuaranteesSAT() && status1.areGuaranteesSAT()) {
				new_spec = TLSF_Utils.change_guarantees(new_spec, mergeGuarantess(spec0.guarantee(),spec1.guarantee()));		
			}
			else if (status0.areGuaranteesSAT()) {
					new_spec = TLSF_Utils.change_guarantees(new_spec, spec0.guarantee());
			}
			else if (status1.areGuaranteesSAT()) {
				new_spec = TLSF_Utils.change_guarantees(new_spec, spec1.guarantee());
			}
			merged_specifications.add(new_spec);
		}
		return merged_specifications;
	}

	private static List<Formula> mergeGuarantess(List<Formula> guarantee, List<Formula> guarantee2) {
		List<Formula> newg = new ArrayList<Formula>();
		List<Formula> newg2 = new ArrayList<Formula>();
		Formula selectedFormula;
		if (!guarantee.isEmpty()) {
			int amountOfFormulas1 = Settings.RANDOM_GENERATOR.nextInt((guarantee.size()))+1;
			for(int i = 0 ; i < amountOfFormulas1; i++) {
				selectedFormula = guarantee.get(Settings.RANDOM_GENERATOR.nextInt(guarantee.size()));
				if (newg.contains(selectedFormula)) continue;
				else newg.add(selectedFormula);
			}
		}
		if (!guarantee2.isEmpty()) {
			int amountOfFormulas2 = Settings.RANDOM_GENERATOR.nextInt(guarantee2.size())+1;
			for(int i = 0 ; i < amountOfFormulas2; i++) {
				selectedFormula = guarantee2.get(Settings.RANDOM_GENERATOR.nextInt(guarantee2.size()));
				if (newg.contains(selectedFormula) || newg2.contains(selectedFormula)) continue;
				else newg2.add(selectedFormula);
			}
		}

		newg.addAll(newg2);
		return newg;
	}

	private static List<Formula> getRandomFormulas(List<LabelledFormula> assumesspec0) {
		int amountOfFormulas = !assumesspec0.isEmpty()? Settings.RANDOM_GENERATOR.nextInt(assumesspec0.size())+1 : 0;
		
		List<LabelledFormula> newAssumes = new ArrayList<LabelledFormula>();
		LabelledFormula selectedFormula;
		
		for(int i = 0 ; i < amountOfFormulas; i++) {
			selectedFormula = assumesspec0.get(Settings.RANDOM_GENERATOR.nextInt(assumesspec0.size()));
			if (newAssumes.contains(selectedFormula)) continue;
			else newAssumes.add(selectedFormula);
		}
		List<Formula> newAssm = new ArrayList<Formula>();
		for (LabelledFormula lf : newAssumes) {
			newAssm.add(lf.formula());
		}
		return newAssm;
	}
	}
