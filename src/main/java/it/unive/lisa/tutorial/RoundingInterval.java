package it.unive.lisa.tutorial;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.*;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

import java.util.List;
import java.util.Objects;

public class RoundingInterval implements BaseNonRelationalValueDomain<RoundingInterval> {

    private final DoubleOrInf low;
    private final DoubleOrInf high;

    private static final RoundingInterval BOTTOM = new RoundingInterval(DoubleOrInf.POSITIVE_INFINITY, DoubleOrInf.NEGATIVE_INFINITY);
    private static final RoundingInterval TOP = new RoundingInterval(DoubleOrInf.NEGATIVE_INFINITY, DoubleOrInf.POSITIVE_INFINITY);


    public RoundingInterval() {
        this.low = DoubleOrInf.NEGATIVE_INFINITY;
        this.high = DoubleOrInf.POSITIVE_INFINITY;
    }

    public RoundingInterval(DoubleOrInf low, DoubleOrInf high) {
        this.low = low;
        this.high = high;
    }

    @Override
    public RoundingInterval bottom() {
        return BOTTOM;
    }

    @Override
    public RoundingInterval top() {
        return TOP;
    }

    @Override
    public boolean isTop() {
        return this.low.isNegativeInfinity() && this.high.isPositiveInfinity();
    }

    @Override
    public boolean isBottom() {
        return this.low.isPositiveInfinity() || this.high.isNegativeInfinity();
    }

    @Override
    public RoundingInterval lubAux(RoundingInterval other) {

        if (this.isBottom()) {
            return other;
        }

        if (other.isBottom())
            return this;

        return new RoundingInterval(
                DoubleOrInf.min(this.low, other.low),
                DoubleOrInf.max(this.high, other.high)
        );
    }

    @Override
    public RoundingInterval glbAux(RoundingInterval other) throws SemanticException {

        DoubleOrInf newLow = DoubleOrInf.max(this.low, other.low);
        DoubleOrInf newHigh = DoubleOrInf.min(this.high, other.high);

        if (this.isBottom() || other.isBottom() || !newLow.lessOrEqual(newHigh)) {
            return bottom();
        }

        return new RoundingInterval(newLow, newHigh);

    }

    @Override
    public RoundingInterval wideningAux(RoundingInterval other) {
        if (this.isBottom()) {
            return other;
        }

        if (other.isBottom()) {
            return this;
        }

        DoubleOrInf newLow, newHigh;
        if(other.low.lessThan(this.low)) {
            newLow = DoubleOrInf.NEGATIVE_INFINITY;
        } else {
            newLow = this.low;
        }

        if(this.high.lessThan(other.high)) {
            newHigh = DoubleOrInf.POSITIVE_INFINITY;
        } else {
            newHigh = this.high;
        }

        return new RoundingInterval(newLow, newHigh);
    }

    @Override
    public boolean lessOrEqualAux(RoundingInterval other) {

        if (this.isBottom()) {
            return true;
        }

        if (other.isBottom()) {
            return false;
        }

        return other.low.lessOrEqual(this.low) && this.high.lessOrEqual(other.high);
    }

    @Override
    public RoundingInterval evalNonNullConstant(Constant constant, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        if (constant.getValue() instanceof Number) {
            double value = ((Number) constant.getValue()).doubleValue();
            return new RoundingInterval(new DoubleOrInf(value), new DoubleOrInf(value));
        }
        return top();
    }

    @Override
    public RoundingInterval evalUnaryExpression(UnaryOperator operator, RoundingInterval arg, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        if (arg.isBottom()) {
            return bottom();
        }

        if (operator instanceof NumericNegation) {
            DoubleOrInf newLow, newHigh;

            if(arg.low.isNegativeInfinity()) {
                newLow = DoubleOrInf.POSITIVE_INFINITY;
            } else if (arg.low.isPositiveInfinity()) {
                newLow = DoubleOrInf.NEGATIVE_INFINITY;
            } else {
                newLow = new DoubleOrInf(-arg.low.getValue());
            }

            if(arg.high.isNegativeInfinity()) {
                newHigh = DoubleOrInf.POSITIVE_INFINITY;
            } else if (arg.high.isPositiveInfinity()) {
                newHigh = DoubleOrInf.NEGATIVE_INFINITY;
            } else {
                newHigh = new DoubleOrInf(-arg.high.getValue());
            }

            return new RoundingInterval(newLow, newHigh);
        }

        return top();
    }

    @Override
    public RoundingInterval evalBinaryExpression(BinaryOperator operator, RoundingInterval left, RoundingInterval right, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        if (left.isBottom() || right.isBottom()) {
            return bottom();
        }
        if (operator instanceof AdditionOperator) {
            DoubleOrInf newLow = DoubleOrInf.add(left.low, right.high);
            DoubleOrInf newHigh = DoubleOrInf.add(left.high, right.high);
            return new RoundingInterval(newLow, newHigh);
        } else if (operator instanceof SubtractionOperator) {
            DoubleOrInf newLow = DoubleOrInf.subtract(left.low, right.high);
            DoubleOrInf newHigh = DoubleOrInf.subtract(left.high, right.low);
            return new RoundingInterval(newLow, newHigh);
        } else if (operator instanceof MultiplicationOperator) {
            List<DoubleOrInf> bounds = List.of(
                    DoubleOrInf.multiply(left.low, right.low),
                    DoubleOrInf.multiply(left.low, right.high),
                    DoubleOrInf.multiply(left.high, right.low),
                    DoubleOrInf.multiply(left.high, right.high)
            );
            DoubleOrInf low = bounds.stream().reduce(DoubleOrInf::min).orElse(DoubleOrInf.NEGATIVE_INFINITY);
            DoubleOrInf high = bounds.stream().reduce(DoubleOrInf::max).orElse(DoubleOrInf.POSITIVE_INFINITY);
            return new RoundingInterval(low, high);
        } else if (operator instanceof DivisionOperator) {
            // TODO division
        }
        return top();
    }

    @Override
    public ValueEnvironment<RoundingInterval> assumeBinaryExpression(
            ValueEnvironment<RoundingInterval> environment,
            BinaryOperator operator,
            ValueExpression left,
            ValueExpression right,
            ProgramPoint src,
            ProgramPoint dest,
            SemanticOracle oracle) throws SemanticException {

        // Validation initiale
        if (environment == null || operator == null || left == null || right == null) {
            return environment;
        }

        // Identification de l'identifiant et évaluation
        Identifier id = null;
        RoundingInterval eval;
        boolean rightIsExpr;

        if (left instanceof Identifier) {
            eval = eval(right, environment, src, oracle);
            id = (Identifier) left;
            rightIsExpr = true;
        } else if (right instanceof Identifier) {
            eval = eval(left, environment, src, oracle);
            id = (Identifier) right;
            rightIsExpr = false;
        } else {
            return environment; // pas d'identifiant
        }

        // Récupération de l'intervalle existant
        RoundingInterval starting = environment.getState(id);
        if (eval.isBottom() || starting.isBottom()) {
            return environment.bottom();
        }

        // raffinement
        RoundingInterval update = null;
        boolean lowIsMinusInfinity = eval.low.isNegativeInfinity();

        RoundingInterval lowToInf = new RoundingInterval(
                eval.low,
                DoubleOrInf.POSITIVE_INFINITY
        );

        RoundingInterval lowPlusOneToInf = new RoundingInterval(
                new DoubleOrInf(eval.low.isInfinite() ? eval.low.getValue() : eval.low.getValue() + 1),
                DoubleOrInf.POSITIVE_INFINITY
        );

        RoundingInterval negInfToHigh = new RoundingInterval(
                DoubleOrInf.NEGATIVE_INFINITY,
                eval.high
        );

        RoundingInterval negInfToHighMinusOne = new RoundingInterval(
                DoubleOrInf.NEGATIVE_INFINITY,
                new DoubleOrInf(eval.high.isInfinite() ? eval.high.getValue() : eval.high.getValue() - 1)
        );

        if (operator instanceof ComparisonEq) {
            update = eval;
        } else if (operator instanceof ComparisonGe) {
            update = rightIsExpr
                    ? (lowIsMinusInfinity ? null : starting.glb(lowToInf))
                    : starting.glb(negInfToHigh);
        } else if (operator instanceof ComparisonGt) {
            update = rightIsExpr
                    ? (lowIsMinusInfinity ? null : starting.glb(lowPlusOneToInf))
                    : (!eval.isTop() && lowIsMinusInfinity ? eval : starting.glb(negInfToHighMinusOne));
        } else if (operator instanceof ComparisonLe) {
            update = rightIsExpr
                    ? starting.glb(negInfToHigh)
                    : (lowIsMinusInfinity ? null : starting.glb(lowToInf));
        } else if (operator instanceof ComparisonLt) {
            update = rightIsExpr
                    ? (!eval.isTop() && lowIsMinusInfinity ? eval : starting.glb(negInfToHighMinusOne))
                    : (lowIsMinusInfinity ? null : starting.glb(lowPlusOneToInf));
        }

        // Mise à jour de l'environnement
        if (update == null) {
            return environment; // Pas de raffinement possible
        } else if (update.isBottom()) {
            return environment.bottom(); // Condition contradictoire
        } else {
            return environment.putState(id, update);
        }
    }

    // TODO
    @Override
    public Satisfiability satisfiesBinaryExpression(BinaryOperator operator,
                                                    RoundingInterval left,
                                                    RoundingInterval right,
                                                    ProgramPoint pp,
                                                    SemanticOracle oracle) {
        return Satisfiability.UNKNOWN;
    }

    @Override
    public StructuredRepresentation representation() {
        if (isBottom()) {
            return Lattice.bottomRepresentation();
        }
        if (isTop()) {
            return Lattice.topRepresentation();
        }
        return new StringRepresentation("[" + low + ", " + high + "]");
    }

    public static class DoubleOrInf {
        private final boolean isInfinite;
        private final boolean isNegative;

        private final Double value;

        public static final DoubleOrInf NEGATIVE_INFINITY = new DoubleOrInf(true);
        public static final DoubleOrInf POSITIVE_INFINITY = new DoubleOrInf(false);

        // Constructor for finite value
        public DoubleOrInf(Double value) {
            this.isInfinite = false;
            this.isNegative = false;
            this.value = value;
        }

        // Constructor for infinity
        private DoubleOrInf(boolean isNegative) {
            this.isInfinite = true;
            this.isNegative = isNegative;
            this.value = isNegative ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        }

        public Double getValue() {
            return value;
        }

        public boolean isInfinite() {
            return isInfinite;
        }

        public boolean isNegativeInfinity() {
            return isInfinite && isNegative;
        }

        public boolean isPositiveInfinity() {
            return isInfinite && !isNegative;
        }

        public static DoubleOrInf min(DoubleOrInf a, DoubleOrInf b) {
            if (a.isNegativeInfinity() || b.isNegativeInfinity())
                return NEGATIVE_INFINITY;

            if (a.isPositiveInfinity())
                return b;

            if (b.isPositiveInfinity())
                return a;

            return new DoubleOrInf(Math.min(a.value, b.value));
        }

        public static DoubleOrInf max(DoubleOrInf a, DoubleOrInf b) {
            if (a.isPositiveInfinity() || b.isPositiveInfinity()) return POSITIVE_INFINITY;
            if (a.isNegativeInfinity()) return b;
            if (b.isNegativeInfinity()) return a;
            return new DoubleOrInf(Math.max(a.value, b.value));
        }

        public static DoubleOrInf add(DoubleOrInf a, DoubleOrInf b) {
            if (a.isInfinite() || b.isInfinite()) {
                if (a.isNegativeInfinity() || b.isNegativeInfinity()) return NEGATIVE_INFINITY;
                return POSITIVE_INFINITY;
            }
            return new DoubleOrInf(a.value + b.value);
        }

        public static DoubleOrInf subtract(DoubleOrInf a, DoubleOrInf b) {
            if (a.isInfinite() || b.isInfinite()) {
                if (a.isNegativeInfinity() || b.isPositiveInfinity()) return NEGATIVE_INFINITY;
                if (a.isPositiveInfinity() || b.isNegativeInfinity()) return POSITIVE_INFINITY;
            }
            return new DoubleOrInf(a.value - b.value);
        }

        public static DoubleOrInf multiply(DoubleOrInf a, DoubleOrInf b) {
            if (a.isInfinite() || b.isInfinite()) {
                if ((a.isNegativeInfinity() && b.value < 0) || (b.isNegativeInfinity() && a.value < 0))
                    return POSITIVE_INFINITY;
                if ((a.isPositiveInfinity() && b.value < 0) || (b.isPositiveInfinity() && a.value < 0))
                    return NEGATIVE_INFINITY;
                return POSITIVE_INFINITY;
            }
            return new DoubleOrInf(a.value * b.value);
        }

        public boolean lessThan(DoubleOrInf other) {
            if (this.isNegativeInfinity()) return !other.isNegativeInfinity();
            if (this.isPositiveInfinity()) return false;
            if (other.isNegativeInfinity()) return false;
            if (other.isPositiveInfinity()) return true;
            return this.value < other.value;
        }

        public boolean lessOrEqual(DoubleOrInf other) {
            if (this.isNegativeInfinity()) return true;
            if (this.isPositiveInfinity()) return other.isPositiveInfinity();
            if (other.isNegativeInfinity()) return false;
            if (other.isPositiveInfinity()) return true;
            return this.value <= other.value;
        }

        @Override
        public String toString() {
            if (isNegativeInfinity()) {
                return "-inf";
            }
            if (isPositiveInfinity()) {
                return "+inf";
            }
            return String.valueOf(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (!(obj instanceof DoubleOrInf other)) {
                return false;
            }

            if (this.isInfinite && other.isInfinite) {
                return this.isNegative == other.isNegative;
            }

            if (this.isInfinite || other.isInfinite) {
                return false;
            }

            return Objects.equals(this.value, other.value);
        }

        @Override
        public int hashCode() {
            return isInfinite ? (isNegative ? -1 : 1) : Objects.hashCode(value);
        }
    }


}
