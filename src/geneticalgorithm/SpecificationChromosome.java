package geneticalgorithm;

import java.util.List;

import org.hamcrest.core.IsInstanceOf;

import com.lagodiuk.ga.Chromosome;

import owl.ltl.BooleanConstant;
import owl.ltl.Formula;
import owl.ltl.LabelledFormula;
import owl.ltl.parser.TlsfParser;
import owl.ltl.tlsf.Tlsf;

public class SpecificationChromosome implements Chromosome<SpecificationChromosome>, Cloneable {
	
	Tlsf spec = null;
	
	public SpecificationChromosome() {
		// TODO Auto-generated constructor stub
	}
	
	public SpecificationChromosome(Tlsf spec) {
		this.spec = TlsfParser.parse(spec.toString());
	}

	@Override
	public List<SpecificationChromosome> crossover(SpecificationChromosome anotherChromosome) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SpecificationChromosome mutate() {
		// TODO Auto-generated method stub
		return null;
	}

	
	
}
