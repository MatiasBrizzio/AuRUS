package modelcounter;

import gov.nasa.ltl.trans.ParseErrorException;
import main.Settings;
import org.junit.Assert;
import org.junit.Test;
import owl.ltl.BooleanConstant;
import owl.ltl.Conjunction;
import owl.ltl.Formula;
import owl.ltl.LabelledFormula;
import owl.ltl.parser.LtlParser;
import owl.ltl.parser.TlsfParser;
import owl.ltl.rewriter.SyntacticSimplifier;
import owl.ltl.tlsf.Tlsf;

import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

public class EmersonLeiAutomatonBasedModelCountingTest {


    @Test
    public void test1() throws ParseErrorException, IOException, InterruptedException {
        List<String> vars = List.of("a", "b");

        System.out.println("========================================");
        System.out.println("Variables: " + vars);

        // Original formula
        LabelledFormula original = LtlParser.parse("G(a)", vars);
        System.out.println("Original formula : " + original);

        EmersonLeiAutomatonBasedModelCounting counter = new EmersonLeiAutomatonBasedModelCounting(original);
        BigInteger originalCount = counter.count(20);
        System.out.println("Original MC (bound=20): " + originalCount);

        System.out.println("----------------------------------------");

        // Refined formula
        LabelledFormula refined = LtlParser.parse("F(a)", vars);
        System.out.println("Refined formula  : " + refined);

        EmersonLeiAutomatonBasedModelCounting counter2 =
                new EmersonLeiAutomatonBasedModelCounting(refined);

        // BUG FIX: use counter2
        BigInteger refinedCountMC = counter2.count(20);
        System.out.println("Refined MC (bound=20): " + refinedCountMC);

        System.out.println("----------------------------------------");

        BigInteger refinedNumOfModels = countModels(refined);
        System.out.println("Refined exact models: " + refinedNumOfModels);

        if (Objects.equals(refinedNumOfModels, BigInteger.ZERO)) {
            System.out.println("Refined formula has 0 models.");
            System.out.println("Result = 0.0");
            return;
        }

        Formula originalFormula = original.formula();
        Formula wonModels = Conjunction.of(originalFormula.not(), refined.formula());

        LabelledFormula wonFormula =
                LabelledFormula.of(wonModels, original.variables());

        System.out.println("Won formula       : " + wonFormula);

        BigInteger wonCount = countModels(wonFormula);

        if (wonCount == null) {
            System.out.println("Won models count = null");
            System.out.println("Result = 0.0");
            return;
        }

        System.out.println("Won models        : " + wonCount);

        BigDecimal numOfWonModels = new BigDecimal(wonCount);
        BigDecimal numOfRefinedModels = new BigDecimal(refinedNumOfModels);

        System.out.println("----------------------------------------");
        System.out.println("Won models BD     : " + numOfWonModels);
        System.out.println("Refined models BD : " + numOfRefinedModels);

        BigDecimal ratio =
                numOfWonModels.divide(numOfRefinedModels, 6, RoundingMode.HALF_UP);

        System.out.println("Won/Refined ratio : " + ratio);

        double value = 1.0d - ratio.doubleValue();

        if (ratio.doubleValue() > 1.0d) {
            System.out.println("WARNING: increase the bound.");
        }

        System.out.println("Final value       : " + value);
        System.out.println("========================================");
    }



    @Test
    public void testSemanticSimilarityPaperVsImplementation()
            throws ParseErrorException, IOException, InterruptedException {

        List<String> vars = List.of("a", "b");

        LabelledFormula original = LtlParser.parse("F(a)", vars);
        LabelledFormula refined = LtlParser.parse("G(a)", vars);

        System.out.println("========================================");
        System.out.println("Original : " + original);
        System.out.println("Refined  : " + refined);
        System.out.println("========================================");


        // ============================================================
        // Model counts
        // ============================================================

        BigInteger originalModels = countModels(original);
        BigInteger refinedModels = countModels(refined);


        System.out.println("#(S)  = " + originalModels);
        System.out.println("#(S') = " + refinedModels);


        // ============================================================
        // Common models: #(S AND S')
        // This is the formula used by the paper
        // ============================================================

        Formula commonFormula =
                Conjunction.of(
                        original.formula(),
                        refined.formula()
                );

        LabelledFormula common =  LabelledFormula.of(commonFormula, vars);
        BigInteger commonModels = countModels(common);


        System.out.println("----------------------------------------");
        System.out.println("#(S AND S') = " + commonModels);



        // ============================================================
        // PAPER FORMULA
        //
        // semSim(S,S') =
        //
        // 0.5 * (
        //     #(S AND S')/#(S)
        //     +
        //     #(S AND S')/#(S')
        // )
        //
        // ============================================================

        double paperLostSide =
                new BigDecimal(commonModels)
                        .divide(
                                new BigDecimal(originalModels),
                                10,
                                RoundingMode.HALF_UP
                        )
                        .doubleValue();


        double paperWonSide =
                new BigDecimal(commonModels)
                        .divide(
                                new BigDecimal(refinedModels),
                                10,
                                RoundingMode.HALF_UP
                        )
                        .doubleValue();


        double paperSemanticSimilarity =
                0.5 * (paperLostSide + paperWonSide);


        System.out.println("----------------------------------------");
        System.out.println("PAPER:");
        System.out.println("#(S AND S')/#(S)  = " + paperLostSide);
        System.out.println("#(S AND S')/#(S') = " + paperWonSide);
        System.out.println("semSim paper      = " + paperSemanticSimilarity);



        // ============================================================
        // IMPLEMENTATION FORM
        //
        // lost:
        //
        // 1 - #(S AND !S')/#(S)
        //
        // won:
        //
        // 1 - #(!S AND S')/#(S')
        //
        // ============================================================


        Formula lostFormula =
                Conjunction.of(
                        original.formula(),
                        refined.formula().not()
                );

        LabelledFormula lost =
                LabelledFormula.of(lostFormula, vars);


        BigInteger lostModels = countModels(lost);



        Formula wonFormula =
                Conjunction.of(
                        original.formula().not(),
                        refined.formula()
                );

        LabelledFormula won =
                LabelledFormula.of(wonFormula, vars);


        BigInteger wonModels = countModels(won);



        System.out.println("----------------------------------------");
        System.out.println("IMPLEMENTATION COUNTS:");
        System.out.println("#(S AND !S') = " + lostModels);
        System.out.println("#(!S AND S') = " + wonModels);



        double lostFitness =
                1.0 -
                        new BigDecimal(lostModels)
                                .divide(
                                        new BigDecimal(originalModels),
                                        10,
                                        RoundingMode.HALF_UP
                                )
                                .doubleValue();


        double wonFitness =
                1.0 -
                        new BigDecimal(wonModels)
                                .divide(
                                        new BigDecimal(refinedModels),
                                        10,
                                        RoundingMode.HALF_UP
                                )
                                .doubleValue();



        double implementationSemanticSimilarity =
                0.5 * (lostFitness + wonFitness);



        System.out.println("----------------------------------------");
        System.out.println("IMPLEMENTATION:");
        System.out.println("1 - #(S AND !S')/#(S)  = " + lostFitness);
        System.out.println("1 - #(!S AND S')/#(S') = " + wonFitness);
        System.out.println("semSim implementation  = "
                + implementationSemanticSimilarity);



        // ============================================================
        // FINAL CHECK
        // ============================================================

        double difference =
                Math.abs(
                        paperSemanticSimilarity
                                -
                                implementationSemanticSimilarity
                );


        System.out.println("----------------------------------------");
        System.out.println("Difference = " + difference);

        Assert.assertEquals(
                paperSemanticSimilarity,
                implementationSemanticSimilarity,
                0.000001
        );

        System.out.println("========================================");
    }



    @Test
    public void testSemanticSimilarity() throws ParseErrorException, IOException, InterruptedException {

        List<String> vars = List.of("a", "b");

        LabelledFormula original = LtlParser.parse("(a)", vars);
        LabelledFormula refined = LtlParser.parse("(!a)", vars);

        System.out.println("========================================");
        System.out.println("Original : " + original);
        System.out.println("Refined  : " + refined);
        System.out.println("========================================");

        BigInteger originalModels = countModels(original);
        BigInteger refinedModels = countModels(refined);

        System.out.println("#(S)     = " + originalModels);
        System.out.println("#(S')    = " + refinedModels);


        // S AND S'
        Formula commonFormula =
                Conjunction.of(original.formula(), refined.formula());

        LabelledFormula common =
                LabelledFormula.of(commonFormula, vars);

        BigInteger commonModels = countModels(common);


        // S AND !S'
        Formula lostFormula =
                Conjunction.of(original.formula(), refined.formula().not());

        LabelledFormula lost =
                LabelledFormula.of(lostFormula, vars);

        BigInteger lostModels = countModels(lost);


        // !S AND S'
        Formula wonFormula =
                Conjunction.of(original.formula().not(), refined.formula());

        LabelledFormula won =
                LabelledFormula.of(wonFormula, vars);

        BigInteger wonModels = countModels(won);


        System.out.println("----------------------------------------");
        System.out.println("#(S AND S')       = " + commonModels);
        System.out.println("#(S AND !S')      = " + lostModels);
        System.out.println("#(!S AND S')      = " + wonModels);


        BigDecimal S =
                new BigDecimal(originalModels);

        BigDecimal Sp =
                new BigDecimal(refinedModels);


        double lostFitness =
                1.0 -
                        new BigDecimal(lostModels)
                                .divide(S, 6, RoundingMode.HALF_UP)
                                .doubleValue();


        double wonFitness =
                1.0 -
                        new BigDecimal(wonModels)
                                .divide(Sp, 6, RoundingMode.HALF_UP)
                                .doubleValue();


        double semanticSimilarity =
                0.5 * lostFitness +
                        0.5 * wonFitness;


        System.out.println("----------------------------------------");
        System.out.println("Lost models fitness = " + lostFitness);
        System.out.println("Won models fitness  = " + wonFitness);

        System.out.println("----------------------------------------");

        double paperFormula =
                0.5 *
                        (
                                new BigDecimal(commonModels)
                                        .divide(S, 6, RoundingMode.HALF_UP)
                                        .doubleValue()
                                        +
                                        new BigDecimal(commonModels)
                                                .divide(Sp, 6, RoundingMode.HALF_UP)
                                                .doubleValue()
                        );


        System.out.println("semSim implementation = " + semanticSimilarity);
        System.out.println("paper equation        = " + paperFormula);

        System.out.println("Difference            = "
                + Math.abs(semanticSimilarity - paperFormula));

        System.out.println("========================================");
    }


    private BigInteger countModels(LabelledFormula formula) {
        SyntacticSimplifier simp = new SyntacticSimplifier();
        Formula simplified = formula.formula().accept(simp);
        if (simplified == BooleanConstant.FALSE)
            return BigInteger.ZERO;
        LabelledFormula simp_formula = LabelledFormula.of(simplified, formula.variables());
        EmersonLeiAutomatonBasedModelCounting counter = new EmersonLeiAutomatonBasedModelCounting<>(simp_formula);
        return counter.count(Settings.MC_BOUND);
    }

    @Test
    public void test2() throws ParseErrorException, IOException, InterruptedException {
        List<String> vars = List.of("a", "b");
        LabelledFormula formula = LtlParser.parse("G(a -> X(b))", vars);
        EmersonLeiAutomatonBasedModelCounting counter = new EmersonLeiAutomatonBasedModelCounting(formula);
        BigInteger d = counter.count(4);
        System.out.println(d);
    }


    @Test
    public void test3() throws ParseErrorException, IOException, InterruptedException {
        List<String> vars = List.of("a", "b");
        LabelledFormula formula = LtlParser.parse("F (a && b)", vars);
        EmersonLeiAutomatonBasedModelCounting counter = new EmersonLeiAutomatonBasedModelCounting(formula);
        BigInteger d = counter.count(3);
        System.out.println(d);
    }

    @Test
    public void testMinepump() throws ParseErrorException, IOException, InterruptedException {

        FileReader f = new FileReader("examples/minepump.tlsf");
        Tlsf spec = TlsfParser.parse(f);

        FileReader f2 = new FileReader("examples/minepump-3.tlsf");
        Tlsf spec2 = TlsfParser.parse(f2);

//		Formula cnf = Conjunction.of(spec.toFormula().formula().not(),spec2.toFormula().formula().not());
//		SyntacticSimplifier simp = new SyntacticSimplifier();
//	    Formula simplified = cnf.accept(simp);
//		LabelledFormula formula = LabelledFormula.of(simplified, spec.variables());
        LabelledFormula formula = LtlParser.parse(spec.toFormula().not().toString() + " && " + spec2.toFormula().not().toString(), spec.variables());
        System.out.println(formula);
        EmersonLeiAutomatonBasedModelCounting counter = new EmersonLeiAutomatonBasedModelCounting(formula);
        BigInteger d = counter.count(5);
        System.out.println(d);
    }

    @Test
    public void testMinePumpBrokenMC() throws ParseErrorException, IOException, InterruptedException {
        List<String> vars = List.of("methane", "high_water", "pump_on");
//		LabelledFormula formula =  LtlParser.parse("((F(X!p2&p1)|F(Xp2&p0))&F(X!p2&p1)&G(X(X!p1|!p1)|!p2|!p1))",vars);
//		LabelledFormula formula =  LtlParser.parse("((F((methane & X(pump_on))) | F((high_water & X(!pump_on)))) & (F((methane & X(pump_on))) | F((!methane & high_water & X(!pump_on)))) & G((!high_water | !pump_on | X((!high_water | X(!high_water))))))", vars);
        LabelledFormula formula = LtlParser.parse("((F((methane & X(pump_on))) | F((high_water & X(!pump_on)))) & (F((methane & X(pump_on))) | F((!methane & high_water & X(!pump_on)))) & G((!high_water | !pump_on | X((!high_water | X(!high_water))))))", vars);

        EmersonLeiAutomatonBasedModelCounting counter = new EmersonLeiAutomatonBasedModelCounting(formula);
        BigInteger d = counter.count(5);
        System.out.println(d);
    }

    @Test
    public void testMinePumpBroken() throws ParseErrorException, IOException, InterruptedException {
        List<String> vars = List.of("methane", "high_water", "pump_on");
        LabelledFormula formula = LtlParser.parse("(((G((!methane | X(!pump_on))) & G((!high_water | X(pump_on)))) | F((high_water & pump_on & X((high_water & X(high_water)))))) & ((G((!methane | X(!pump_on))) & G((methane | !high_water | X(pump_on)))) | F((high_water & pump_on & X((high_water & X(high_water)))))))", vars);

        EmersonLeiAutomatonBasedModelCounting counter = new EmersonLeiAutomatonBasedModelCounting(formula);
        BigInteger d = counter.count(10);
        System.out.println(d);
    }

    @Test
    public void testDetector() throws ParseErrorException, IOException, InterruptedException {

        FileReader f = new FileReader("examples/syntcomp2019/unreal/9158508/detector_unreal_4_basic.tlsf");
        Tlsf spec = TlsfParser.parse(f);

        Formula cnf = spec.toFormula().formula();
        SyntacticSimplifier simp = new SyntacticSimplifier();
        Formula simplified = cnf.accept(simp);
        System.out.println(simplified);
        LabelledFormula formula = LabelledFormula.of(simplified, spec.variables());
        EmersonLeiAutomatonBasedModelCounting counter = new EmersonLeiAutomatonBasedModelCounting(formula);
        BigInteger d = counter.count(5);
        System.out.println(d);
    }

}