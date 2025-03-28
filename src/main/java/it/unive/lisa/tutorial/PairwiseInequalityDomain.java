package it.unive.lisa.tutorial;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.lattices.SetLattice;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;

/**
 * PairwiseInequalityDomain
 * Domain representing inequalities of the form : αx + βy ≤ γ
 */
public class PairwiseInequalityDomain extends FunctionalLattice<PairwiseInequalityDomain, Identifier, PairwiseInequalityDomain.SetOfInequalities> implements ValueDomain<PairwiseInequalityDomain> {

    // ===================================
    // Constructeurs
    // ===================================

    public PairwiseInequalityDomain() {
        super(new SetOfInequalities(Collections.emptySet(), true));
    }

    public PairwiseInequalityDomain(SetOfInequalities lattice) {
        super(lattice);
    }

    public PairwiseInequalityDomain(SetOfInequalities lattice, Map<Identifier, SetOfInequalities> map) {
        super(lattice, map);
    }

    @Override
    public SetOfInequalities stateOfUnknown(Identifier id) {
        return new SetOfInequalities(Collections.emptySet(), true);
    }

    @Override
    public PairwiseInequalityDomain mk(SetOfInequalities lattice, Map<Identifier, SetOfInequalities> map) {
        return new PairwiseInequalityDomain(lattice, map);
    }

    // ===================================
    // Obligatoires pour ValueDomain
    // ===================================

    @Override
    public PairwiseInequalityDomain top() {
        return new PairwiseInequalityDomain(new SetOfInequalities(Collections.emptySet(), true));
    }

    @Override
    public PairwiseInequalityDomain bottom() {
        return new PairwiseInequalityDomain(new SetOfInequalities(Collections.emptySet(), false));
    }

    @Override
    public PairwiseInequalityDomain assign(Identifier id, ValueExpression expr, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        return this;
    }

    @Override
    public PairwiseInequalityDomain smallStepSemantics(ValueExpression expr, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        return this;
    }

    @Override
    public PairwiseInequalityDomain assume(ValueExpression expr, ProgramPoint src, ProgramPoint dest, SemanticOracle oracle) throws SemanticException {
        return this;
    }

    @Override
    public boolean knowsIdentifier(Identifier id) {
        return false;
    }

    @Override
    public PairwiseInequalityDomain forgetIdentifier(Identifier id) throws SemanticException {
        return this;
    }

    @Override
    public PairwiseInequalityDomain forgetIdentifiersIf(Predicate<Identifier> pred) throws SemanticException {
        return this;
    }

    @Override
    public Satisfiability satisfies(ValueExpression expr, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        return Satisfiability.UNKNOWN;
    }

    @Override
    public PairwiseInequalityDomain pushScope(ScopeToken scopeToken) throws SemanticException {
        return this;
    }

    @Override
    public PairwiseInequalityDomain popScope(ScopeToken scopeToken) throws SemanticException {
        return this;
    }

    // ===================================
    // Inner classes
    // ===================================

    /**
     * Represents a single inequality of the form : αx + βy ≤ γ
     */
    public static class LinearInequality {
        private final Identifier leftVar;
        private final double leftCoeff;
        private final Identifier rightVar;
        private final double rightCoeff;
        private final double rhs;

        public LinearInequality(Identifier leftVar, double leftCoeff, Identifier rightVar, double rightCoeff, double rhs) {
            this.leftVar = leftVar;
            this.leftCoeff = leftCoeff;
            this.rightVar = rightVar;
            this.rightCoeff = rightCoeff;
            this.rhs = rhs;
        }

        public Set<Identifier> variables() {
            Set<Identifier> vars = new HashSet<>();
            if (leftVar != null && leftCoeff != 0)
                vars.add(leftVar);
            if (rightVar != null && rightCoeff != 0)
                vars.add(rightVar);
            return vars;
        }

        @Override
        public String toString() {
            StringBuilder res = new StringBuilder();
            boolean started = false;

            if (leftVar != null && leftCoeff != 0) {
                if (leftCoeff != 1) res.append(leftCoeff);
                res.append(leftVar);
                started = true;
            }
            if (rightVar != null && rightCoeff != 0) {
                if (started) res.append(rightCoeff > 0 ? " + " : " - ");
                if (Math.abs(rightCoeff) != 1) res.append(Math.abs(rightCoeff));
                res.append(rightVar);
            }
            res.append(" ≤ ").append(rhs);
            return res.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof LinearInequality other))
                return false;
            return Objects.equals(leftVar, other.leftVar)
                    && Objects.equals(rightVar, other.rightVar)
                    && Double.compare(leftCoeff, other.leftCoeff) == 0
                    && Double.compare(rightCoeff, other.rightCoeff) == 0
                    && Double.compare(rhs, other.rhs) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(leftVar, rightVar, leftCoeff, rightCoeff, rhs);
        }
    }

    /**
     * Ensemble d'inégalités
     */
    public static class SetOfInequalities extends SetLattice<SetOfInequalities, LinearInequality> {
        public SetOfInequalities(Set<LinearInequality> elements, boolean isTop) {
            super(elements, isTop);
        }

        @Override
        public SetOfInequalities mk(Set<LinearInequality> elements) {
            return new SetOfInequalities(elements, elements.isEmpty());
        }

        @Override
        public SetOfInequalities top() {
            return new SetOfInequalities(Collections.emptySet(), true);
        }

        @Override
        public SetOfInequalities bottom() {
            return new SetOfInequalities(Collections.emptySet(), false);
        }
    }
}
