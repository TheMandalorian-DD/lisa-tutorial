package it.unive.lisa.tutorial;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.symbolic.value.operator.binary.*;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class RoundingInterval implements BaseNonRelationalValueDomain<RoundingInterval> {

    // Constantes pour les modes d'arrondi
    public enum RoundingMode {
        ROUND_UP,      // Arrondi vers le haut
        ROUND_DOWN,    // Arrondi vers le bas
        ROUND_HALF_UP, // Arrondi au plus proche, avec 0.5 arrondi au-dessus
        ROUND_HALF_DOWN, // Arrondi au plus proche, avec 0.5 arrondi au-dessous
        ROUND_NEAREST   // Arrondi au point le plus proche
    }

    private static final double MIN = Double.NEGATIVE_INFINITY;
    private static final double MAX = Double.POSITIVE_INFINITY;

    private final double low;
    private final double high;
    private final RoundingMode roundingMode;
    private final int precision; // Nombre de décimales de précision

    // Constructeur par défaut (intervalle max, précision par défaut)
    public RoundingInterval() {
        this.low = MIN;
        this.high = MAX;
        this.roundingMode = RoundingMode.ROUND_HALF_UP;
        this.precision = 2; // Par défaut, 2 décimales
    }

    // Constructeur complet
    public RoundingInterval(double low, double high, RoundingMode roundingMode, int precision) {
        this.low = low;
        this.high = high;
        this.roundingMode = roundingMode;
        this.precision = precision;
    }

    @Override
    public RoundingInterval top() {
        return new RoundingInterval(MIN, MAX, RoundingMode.ROUND_HALF_UP, 2);
    }

    @Override
    public RoundingInterval bottom() {
        return new RoundingInterval(1, 0, RoundingMode.ROUND_HALF_UP, 2);
    }

    @Override
    public boolean isTop() {
        return low == MIN && high == MAX;
    }

    @Override
    public boolean isBottom() {
        return low > high;
    }

    // Méthode d'arrondi générique
    private double round(double value) {
        return switch (roundingMode) {
            case ROUND_UP -> Math.ceil(value * Math.pow(10, precision)) / Math.pow(10, precision);
            case ROUND_DOWN -> Math.floor(value * Math.pow(10, precision)) / Math.pow(10, precision);
            case ROUND_HALF_UP -> Math.round(value * Math.pow(10, precision)) / Math.pow(10, precision);
            case ROUND_HALF_DOWN ->
                    Math.signum(value) * Math.floor(Math.abs(value) * Math.pow(10, precision) + 0.5) / Math.pow(10, precision);
            case ROUND_NEAREST -> Math.rint(value * Math.pow(10, precision)) / Math.pow(10, precision);
            default -> value;
        };
    }

    @Override
    public RoundingInterval lubAux(RoundingInterval other) {

        if (this.isBottom() || other.isBottom()) {
            return bottom();
        }

        // Prendre le mode d'arrondi et la précision du premier intervalle
        return new RoundingInterval(
                Math.min(this.low, other.low),
                Math.max(this.high, other.high),
                this.roundingMode,
                this.precision
        );
    }

    @Override
    public RoundingInterval wideningAux(RoundingInterval other) {
        final double THRESHOLD = 1000.0; // Seuil ajustable
        double newLow = this.low;
        double newHigh = this.high;

        if (this.isBottom() || other.isBottom()) {
            return bottom();
        }

        if (other.low < this.low) {
            double diff = this.low - other.low;
            if (diff > THRESHOLD) {
                newLow = Math.max(other.low, this.low - THRESHOLD);
            }
        }

        if (other.high > this.high) {
            double diff = other.high - this.high;
            if (diff > THRESHOLD) {
                newHigh = Math.min(other.high, this.high + THRESHOLD);
            }
        }

        return new RoundingInterval(newLow, newHigh, this.roundingMode, this.precision);
    }

    @Override
    public boolean lessOrEqualAux(RoundingInterval other) {

        if (this.isBottom() && other.isBottom()) {
            return true;
        }
        if (this.isBottom()) {
            return true; // ⊥ ≤ tout
        }
        if (other.isBottom()) {
            return false; // rien ≤ ⊥ sauf ⊥
        }

        return other.low <= this.low && this.high <= other.high;
    }

    // Méthodes d'opérations arithmétiques avec arrondi
    public RoundingInterval add(RoundingInterval other) {
        if (this.isBottom() || other.isBottom()) {
            return bottom();
        }

        double newLow = round(this.low + other.low);
        double newHigh = round(this.high + other.high);

        return new RoundingInterval(newLow, newHigh, this.roundingMode, this.precision);
    }

    public RoundingInterval sub(RoundingInterval other) {
        if (this.isBottom() || other.isBottom()) {
            return bottom();
        }

        double newLow = round(this.low - other.high);
        double newHigh = round(this.high - other.low);
        return new RoundingInterval(newLow, newHigh, this.roundingMode, this.precision);
    }

    public RoundingInterval mul(RoundingInterval other) {
        if (this.isBottom() || other.isBottom()) {
            return bottom();
        }

        double[] results = new double[]{
                round(this.low * other.low),
                round(this.low * other.high),
                round(this.high * other.low),
                round(this.high * other.high)
        };

        double min = results[0], max = results[0];
        for (double r : results) {
            min = Math.min(min, r);
            max = Math.max(max, r);
        }

        return new RoundingInterval(min, max, this.roundingMode, this.precision);
    }

    public RoundingInterval div(RoundingInterval other) {
        if (this.isBottom() || other.isBottom()) {
            return bottom();
        }

        // Division par zéro
        if (other.low <= 0 && other.high >= 0) {
            return bottom();
        }

        // Calcul de toutes les combinaisons de division
        double[] results = new double[]{
                round(this.low / other.low),
                round(this.low / other.high),
                round(this.high / other.low),
                round(this.high / other.high)
        };

        // Trouver le minimum et le maximum des résultats
        double min = results[0];
        double max = results[0];
        for (double r : results) {
            min = Math.min(min, r);
            max = Math.max(max, r);
        }

        return new RoundingInterval(min, max, this.roundingMode, this.precision);
    }

    @Override
    public RoundingInterval evalNonNullConstant(Constant constant, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        if (constant.getValue() instanceof Number) {
            double value = ((Number) constant.getValue()).doubleValue();
            return new RoundingInterval(value, value, this.roundingMode, this.precision);
        }
        return top();
    }

    @Override
    public RoundingInterval evalUnaryExpression(UnaryOperator operator, RoundingInterval arg, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        if (arg.isBottom()) {
            return bottom();
        }

        if (operator instanceof NumericNegation) {
            double newLow = -arg.high;
            double newHigh = -arg.low;

            return new RoundingInterval(newLow, newHigh, this.roundingMode, this.precision);
        }

        return top();
    }

    @Override
    public RoundingInterval evalBinaryExpression(BinaryOperator operator, RoundingInterval left, RoundingInterval right, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        if (left.isBottom() || right.isBottom()) {
            return bottom();
        }
        if (operator instanceof AdditionOperator) {
            return add(right);
        } else if (operator instanceof SubtractionOperator) {
            return sub(right);
        } else if (operator instanceof MultiplicationOperator) {
            return mul(right);
        } else if (operator instanceof DivisionOperator) {
            return div(right);
        }
        return top();
    }

    @Override
    public Satisfiability satisfiesBinaryExpression(BinaryOperator operator,
                                                    RoundingInterval left,
                                                    RoundingInterval right,
                                                    ProgramPoint pp,
                                                    SemanticOracle oracle) {
        if (operator.equals(ComparisonLt.INSTANCE)) {
            if (left.high < right.low) {
                return Satisfiability.SATISFIED;
            }
            if (left.low >= right.high) {
                return Satisfiability.NOT_SATISFIED;
            }
            return Satisfiability.UNKNOWN;
        } else if (operator.equals(ComparisonGt.INSTANCE)) {
            if (left.low > right.high) {
                return Satisfiability.SATISFIED;
            }
            if (left.high <= right.low) {
                return Satisfiability.NOT_SATISFIED;
            }
            return Satisfiability.UNKNOWN;
        } else if (operator.equals(ComparisonEq.INSTANCE)) {
            if (left.high < right.low || left.low > right.high) {
                return Satisfiability.NOT_SATISFIED;
            }
            if (Math.abs(left.low - left.high) < Math.pow(10, -precision) &&
                    Math.abs(right.low - right.high) < Math.pow(10, -precision) &&
                    Math.abs(left.low - right.low) < Math.pow(10, -precision))
            {
                return Satisfiability.SATISFIED;
            }
            return Satisfiability.UNKNOWN;
        } else if (operator.equals(ComparisonNe.INSTANCE)) {
            if (left.high < right.low || left.low > right.high) {
                return Satisfiability.SATISFIED;
            }
            if (Math.abs(left.low - left.high) < Math.pow(10, -precision) &&
                    Math.abs(right.low - right.high) < Math.pow(10, -precision) &&
                    Math.abs(left.low - right.low) < Math.pow(10, -precision))
            {
                return Satisfiability.NOT_SATISFIED;
            }
            return Satisfiability.UNKNOWN;
        }
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
        String format = "[%." + precision + "f, %." + precision + "f]";
        return new StringRepresentation(String.format(format, low, high));
    }
}
