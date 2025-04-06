package it.unive.lisa.tutorial;

import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.program.Program;
import org.junit.Test;
import it.unive.lisa.interprocedural.ReturnTopPolicy;

import java.util.HashSet;


public class RoundingInequalityCartesianProductTest {

    @Test
    public void testRoundingInequalityCartesianProduct() throws ParsingException {
        // Nous allons d'abord parser le programme
        Program program = IMPFrontend.processFile("inputs/cartesianproduct.imp");

        // Création de la configuration pour l'analyse
        LiSAConfiguration conf = new DefaultConfiguration();

        // Spécification du répertoire où les fichiers d'analyse seront générés
        conf.workdir = "outputs/cartesianproduct";

        // Format des résultats d'analyse
        conf.analysisGraphs = LiSAConfiguration.GraphType.HTML;

        // Définir l'état abstrait de l'analyse
        conf.abstractState = DefaultConfiguration.simpleState(
                DefaultConfiguration.defaultHeapDomain(),
                new RoundingInequalityCartesianProduct(
                        new PairwiseInequalityDomain(new HashSet<>()),
                        new ValueEnvironment<>(new RoundingInterval())
                ),
                DefaultConfiguration.defaultTypeDomain()
        );

        conf.openCallPolicy = ReturnTopPolicy.INSTANCE;

        // we instantiate LiSA with our configuration
        LiSA lisa = new LiSA(conf);

        // finally, we tell LiSA to analyze the program
        lisa.run(program);
    }
}
