package it.unive.lisa.tutorial;

import java.util.*;
import java.util.function.Predicate;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;


/**
 * Represents a domain that models pairwise inequalities between variables
 * using linear inequalities. This domain is used for abstract interpretation
 * to track relationships such as less-than or equality constraints.
 *
 * The domain supports operations such as computing the least upper bound (LUB),
 * greatest lower bound (GLB), assignments, and assumptions based on expressions.
 * It also provides representations for the top and bottom elements of the
 * lattice structure.
 *
 * The behavior of the domain is controlled by a set of linear inequalities
 * represented as constraints. These constraints define relationships over
 * identifiers and constants, which can be used to approximate the program's
 * behavior for static analysis purposes.
 */
public class PairwiseInequalityDomain implements ValueDomain<PairwiseInequalityDomain> {

    private final Set<LinearInequality> constraints;
    private boolean isTop;

    public static final PairwiseInequalityDomain TOP = new PairwiseInequalityDomain(true);
    public static final PairwiseInequalityDomain BOTTOM = new PairwiseInequalityDomain(false);

    private PairwiseInequalityDomain(boolean isTop) {
        this.isTop = isTop;
        this.constraints = isTop ? Collections.emptySet() : Set.of(new LinearInequality(new HashMap<>(), 0.0));
    }

    public PairwiseInequalityDomain(Set<LinearInequality> constraints) {
        if(constraints.isEmpty()) {
            this.isTop = true;
        }
        this.constraints = new HashSet<>(compute(constraints));
    }

    public Set<LinearInequality> getConstraints() {
        return constraints;
    }

    @Override
    public PairwiseInequalityDomain top() {
        return TOP;
    }

    @Override
    public PairwiseInequalityDomain bottom() {
        return BOTTOM;
    }

    public boolean isTop() {
        return isTop;
    }

    public boolean isBottom() {
        return !isTop && constraints.size() == 1 && constraints.iterator().next().constant == 0.0;
    }

    @Override
    public boolean lessOrEqual(PairwiseInequalityDomain pairwiseInequalityDomain) throws SemanticException {
        return false;
    }

    /**
     * Computes the least upper bound (LUB) of this domain and the specified domain.
     * The LUB represents the smallest domain that contains all the information
     * from both domains.
     *
     * @param other the {@code PairwiseInequalityDomain} to compute the LUB with
     * @return a new instance of {@code PairwiseInequalityDomain} representing the LUB
     *         of this domain and {@code other}
     * @throws SemanticException if an error occurs during the computation
     */
    @Override
    public PairwiseInequalityDomain lub(PairwiseInequalityDomain other) throws SemanticException {
        if(isTop() || other.isTop()) return top();
        if(isBottom()) return other;
        if(other.isBottom()) return this;
        Set<LinearInequality> unionInequalities = new HashSet<>(constraints);
        unionInequalities.addAll(other.getConstraints());
        return new PairwiseInequalityDomain(unionInequalities);
    }

    /**
     * Computes the greatest lower bound (GLB) of this domain with the provided domain.
     * The GLB represents the intersection of the constraints from both domains.
     *
     * @param other the {@code PairwiseInequalityDomain} to compute the GLB with
     * @return a new {@code PairwiseInequalityDomain} representing the GLB of this domain and the other domain
     * @throws SemanticException if an error occurs during computation
     */
    @Override
    public PairwiseInequalityDomain glb(PairwiseInequalityDomain other) throws SemanticException {
        if (isBottom() || other.isBottom()) return bottom();
        if (isTop()) return other;
        if (other.isTop()) return this;
        Set<LinearInequality> result = new HashSet<>(constraints);
        result.addAll(other.getConstraints());
        return new PairwiseInequalityDomain(result);
    }

    public static boolean isHeapIdentifier(Identifier id) {
        return id.toString().contains("heap") || id.toString().contains("this") || id.toString().contains("&pp@");
    }

    /**
     * Assigns a value to the given identifier and updates the current domain's constraints
     * accordingly. This method handles different types of value expressions, including identifiers,
     * binary expressions with arithmetic operators, and constants. It creates and adds new linear
     * inequalities to the domain based on the assignment operation.
     *
     * @param identifier the {@code Identifier} to which a value is being assigned
     * @param valueExpression the {@code ValueExpression} representing the value being assigned
     * @param pp the program point {@code ProgramPoint} where the assignment occurs
     * @param oracle the {@code SemanticOracle} providing additional semantic information
     * @return a new {@code PairwiseInequalityDomain} instance representing the updated constraints
     *         after the assignment
     * @throws SemanticException if a semantic error occurs during the assignment process
     */
    @Override
    public PairwiseInequalityDomain assign(Identifier identifier, ValueExpression valueExpression, ProgramPoint pp,
                                           SemanticOracle oracle) throws SemanticException {
        if (isHeapIdentifier(identifier)) {
            return this;
        }

        Map<Identifier, Double> coefficients = new HashMap<>();
        Set<LinearInequality> updatedConstraints = new HashSet<>(constraints);

        if (valueExpression instanceof Identifier id) {
            return createSimpleInequality(updatedConstraints, coefficients, identifier, id, 0.0);
        }

        if (valueExpression instanceof BinaryExpression binaryExpression) {
            if (binaryExpression.getOperator() instanceof AdditionOperator) {
                if (binaryExpression.getLeft() instanceof Identifier leftIdentifier &&
                        binaryExpression.getRight() instanceof Constant constant) {
                    return createSimpleInequality(updatedConstraints, coefficients, identifier, leftIdentifier,
                            (Integer) constant.getValue());
                }
                if (binaryExpression.getLeft() instanceof BinaryExpression leftExpr &&
                        leftExpr.getOperator() instanceof MultiplicationOperator &&
                        leftExpr.getLeft() instanceof Constant constant &&
                        leftExpr.getRight() instanceof Identifier rightIdentifier &&
                        binaryExpression.getRight() instanceof Constant rightConstant) {
                    return createComplexInequality(updatedConstraints, coefficients, identifier, rightIdentifier,
                            -(Double) constant.getValue(),
                            (Integer) rightConstant.getValue());
                }
            }
            if (binaryExpression.getOperator() instanceof SubtractionOperator &&
                    binaryExpression.getLeft() instanceof Identifier leftIdentifier &&
                    binaryExpression.getRight() instanceof Constant constant) {
                return createSimpleInequality(updatedConstraints, coefficients, identifier, leftIdentifier,
                        -(Integer) constant.getValue());
            }
        }

        return this;
    }

    private PairwiseInequalityDomain createSimpleInequality(Set<LinearInequality> updatedConstraints,
                                                            Map<Identifier, Double> coefficients,
                                                            Identifier target, Identifier source, double constant) {
        coefficients.put(target, 1.0);
        coefficients.put(source, -1.0);
        updatedConstraints.add(new LinearInequality(coefficients, constant));
        return new PairwiseInequalityDomain(updatedConstraints);
    }

    private PairwiseInequalityDomain createComplexInequality(Set<LinearInequality> updatedConstraints,
                                                             Map<Identifier, Double> coefficients,
                                                             Identifier target, Identifier source,
                                                             double multiplier, double constant) {
        coefficients.put(target, 1.0);
        coefficients.put(source, multiplier);
        updatedConstraints.add(new LinearInequality(coefficients, constant));
        return new PairwiseInequalityDomain(updatedConstraints);
    }


    /**
     * Processes a set of linear inequalities, identifying and keeping only the most restrictive
     * inequality for each unique coefficient combination. This operation helps simplify the
     * given set of constraints by reducing redundancy while preserving essential information.
     *
     * @param constraints the set of {@code LinearInequality} objects to be processed. Each
     *                    inequality consists of coefficients, a constant, and a comparison operator.
     * @return the updated set of {@code LinearInequality} objects containing the most restrictive
     *         inequalities for each unique set of coefficients.
     */
    public Set<LinearInequality> compute(Set<LinearInequality> constraints) {

        Map<String, LinearInequality> constraintMap = new HashMap<>();

        for (LinearInequality currentConstraint : constraints) {
            String key = currentConstraint.coefficients.toString();

            constraintMap.putIfAbsent(key, currentConstraint);

            LinearInequality existingConstraint = constraintMap.get(key);
            if (currentConstraint.lessOrEqual && existingConstraint.constant > currentConstraint.constant) {
                constraintMap.put(key, currentConstraint);
            }
        }

        // Transitivity check and adding derived inequalities
        Set<LinearInequality> closure = new HashSet<>(constraintMap.values());
        for (LinearInequality c1 : closure) {
            for (LinearInequality c2 : closure) {
                if (c1 != c2) {
                    for (Map.Entry<Identifier, Double> entry : c1.coefficients.entrySet()) {
                        Identifier sharedIdentifier = entry.getKey();
                        Double coeff1 = entry.getValue();

                        if (c2.coefficients.containsKey(sharedIdentifier)) {
                            Double coeff2 = c2.coefficients.get(sharedIdentifier);
                            if (coeff1 * coeff2 < 0
                                    && c1.coefficients.size() == 2
                                    && c2.coefficients.size() == 2) { // Ensure both inequalities involve exactly two variables
                                Map<Identifier, Double> newCoefficients = new HashMap<>(c1.coefficients);
                                c2.coefficients.forEach((key, value) ->
                                        newCoefficients.merge(key, value, Double::sum));
                                newCoefficients.remove(sharedIdentifier);

                                // Filter out derived inequalities without variables or trivial cases
                                newCoefficients.values().removeIf(val -> val == 0);

                                if (!newCoefficients.isEmpty()) {
                                    double newConstant = c1.constant + c2.constant;
                                    // Ensure non-trivial inequality
                                    LinearInequality derived = new LinearInequality(newCoefficients, newConstant);
                                    constraintMap.putIfAbsent(derived.coefficients.toString(), derived);
                                }
                            }
                        }
                    }
                }
            }
        }

        return new HashSet<>(constraintMap.values());
    }

    @Override
    public PairwiseInequalityDomain smallStepSemantics(ValueExpression expr, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        return this;
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;

        for(LinearInequality constraint : constraints) {
            if(!first)
                sb.append(", ");
            sb.append(constraint.toString());
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }


    /**
     * Updates the current domain with new constraints derived from the provided expression.
     * This operation assumes the expression to be true and updates the set of linear
     * inequalities accordingly. The method handles both simple and compound inequality
     * expressions, extracting coefficients and constants to form linear inequalities.
     *
     * @param expression the value expression that is assumed to be true
     * @param src the source program point where the assumption originates
     * @param dest the destination program point where the assumption is applied
     * @param oracle the semantic oracle used for the analysis and verification process
     * @return a new instance of {@code PairwiseInequalityDomain} with the updated constraints
     * @throws SemanticException if any semantic errors occur during the assumption operation
     */
    @Override
    public PairwiseInequalityDomain assume(
            ValueExpression expression,
            ProgramPoint src,
            ProgramPoint dest,
            SemanticOracle oracle) throws SemanticException {

        if (!(expression instanceof BinaryExpression binaryExpression)) {
            return this;
        }

        if (!(binaryExpression.getOperator() instanceof ComparisonLe)) {
            return this;
        }

        SymbolicExpression left = binaryExpression.getLeft();
        SymbolicExpression right = binaryExpression.getRight();

        // Initialize result constraints and coefficient map
        Map<Identifier, Double> coefficients = new HashMap<>();
        Set<LinearInequality> updatedConstraints = new HashSet<>(this.constraints);

        if (left instanceof Identifier leftIdentifier && right instanceof Identifier rightIdentifier) {
            return handleSimpleIdentifiers(updatedConstraints, coefficients, leftIdentifier, rightIdentifier);
        }

        if (left instanceof BinaryExpression lhsExpression && right instanceof Constant rhsConstant) {
            return handleComplexExpression(updatedConstraints, coefficients, lhsExpression, rhsConstant);
        }

        return this;
    }

    private PairwiseInequalityDomain handleSimpleIdentifiers(
            Set<LinearInequality> updatedConstraints,
            Map<Identifier, Double> coefficients,
            Identifier leftIdentifier,
            Identifier rightIdentifier) {

        coefficients.put(leftIdentifier, 1.0);
        coefficients.put(rightIdentifier, -1.0);

        LinearInequality inequality = new LinearInequality(coefficients, 0.0);
        updatedConstraints.add(inequality);

        return new PairwiseInequalityDomain(updatedConstraints);
    }

    private PairwiseInequalityDomain handleComplexExpression(
            Set<LinearInequality> updatedConstraints,
            Map<Identifier, Double> coefficients,
            BinaryExpression lhsExpression,
            Constant rhsConstant) throws SemanticException {

        if (!(rhsConstant.getValue() instanceof Integer constantValue)) {
            return this; // Ignore non-integer constants
        }

        if (lhsExpression.getOperator() instanceof AdditionOperator &&
                lhsExpression.getLeft() instanceof BinaryExpression axExpression &&
                lhsExpression.getRight() instanceof BinaryExpression byExpression) {

            return handleAdditionOperator(updatedConstraints, coefficients, axExpression, byExpression, constantValue);
        }

        return this;
    }

    private PairwiseInequalityDomain handleAdditionOperator(
            Set<LinearInequality> updatedConstraints,
            Map<Identifier, Double> coefficients,
            BinaryExpression axExpression,
            BinaryExpression byExpression,
            int constantValue) throws SemanticException {

        if (axExpression.getOperator() instanceof MultiplicationOperator &&
                byExpression.getOperator() instanceof MultiplicationOperator &&
                axExpression.getLeft() instanceof Constant a &&
                axExpression.getRight() instanceof Identifier x &&
                byExpression.getLeft() instanceof Constant b &&
                byExpression.getRight() instanceof Identifier y) {

            coefficients.put(y, ((Integer) b.getValue()).doubleValue());
            coefficients.put(x, ((Integer) a.getValue()).doubleValue());

            LinearInequality inequality = new LinearInequality(coefficients, (double) constantValue);
            updatedConstraints.add(inequality);

            return new PairwiseInequalityDomain(updatedConstraints);
        }

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

    @Override
    public StructuredRepresentation representation() {
        if(isTop()) return Lattice.topRepresentation();
        if(isBottom()) return Lattice.bottomRepresentation();
        return new StringRepresentation(toString());
    }

    // ===================================
    // Inner classes
    // ===================================

    /**
     * Represents a single inequality of the form : αx + βy ≤ γ
     */
    /**
     * Classe représentant une inégalité linéaire ax + by ≤ c
     */
    public static class LinearInequality {
        // Coefficients des variables (ax + by)
        public Map<Identifier, Double> coefficients;
        public boolean lessOrEqual = true;
        // Constante c dans ax + by ≤ c
        private final Double constant;

        public LinearInequality(Map<Identifier, Double> coefficients, Double constant) {
            this.coefficients = new HashMap<>(coefficients);
            this.constant = constant;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other)
                return true;
            if (other == null || getClass() != other.getClass())
                return false;
            LinearInequality that = (LinearInequality) other;
            return Double.compare(that.constant, constant) == 0 && coefficients.equals(that.coefficients);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            boolean first = true;

            for (Map.Entry<Identifier, Double> entry : coefficients.entrySet()) {
                double coef = entry.getValue();

                if (!first) {
                    sb.append(coef >= 0 ? " + " : " - ");
                } else if (coef < 0) {
                    sb.append("-");
                }

                if (Math.abs(coef) != 1.0) {
                    sb.append(Math.abs(coef)).append("*");
                }

                sb.append(entry.getKey().getName());
                first = false;
            }

            sb.append(lessOrEqual ? " <= " : " < ");
            sb.append(constant);
            return sb.toString();
        }
    }
}
