package it.unive.lisa.tutorial;


import it.unive.lisa.analysis.heap.pointbased.FieldSensitivePointBasedHeap;
import org.junit.Test;
import it.unive.lisa.AnalysisException;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.conf.LiSAConfiguration.GraphType;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.program.Program;
import it.unive.lisa.tutorial.PairwiseInequalityDomain;

public class PairwiseInequalityDomainTest {

    @Test
    public void testPairwiseAnalysis() throws ParsingException, AnalysisException {
        // Chargement du fichier IMP à analyser
        Program program = IMPFrontend.processFile("inputs/pairwise.imp");

        // Configuration de base
        LiSAConfiguration conf = new DefaultConfiguration();

        // Dossier de sortie
        conf.workdir = "outputs/pairwise";

        // Format des graphes
        conf.analysisGraphs = GraphType.HTML;

        // Sélection du domaine abstrait avec ton domaine custom
        conf.abstractState = DefaultConfiguration.simpleState(
                new FieldSensitivePointBasedHeap(),
                new PairwiseInequalityDomain(),
                DefaultConfiguration.defaultTypeDomain());

        // Création et exécution de LiSA
        LiSA lisa = new LiSA(conf);
        lisa.run(program);
    }
}

