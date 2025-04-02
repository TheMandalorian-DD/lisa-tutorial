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

    private Set<LinearInequality> constraints;
    private boolean isTop = false;

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
        Set<LinearInequality> unionInequalities = new HashSet<>(this.constraints);
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
        Set<LinearInequality> result = new HashSet<>(this.constraints);
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
        if(isHeapIdentifier(identifier))
            return this;

        if(valueExpression instanceof Identifier id){
            Map<Identifier, Double> coefficients = new HashMap<>();
            coefficients.put(identifier, 1.0);
            coefficients.put(id, -1.0);
            LinearInequality inequality = new LinearInequality(coefficients, 0.0);
            Set<LinearInequality> res = new HashSet<>(this.constraints);
            res.add(inequality);
            return new PairwiseInequalityDomain(res);
        }
        if (valueExpression instanceof BinaryExpression binaryExpression) {
            if (binaryExpression.getOperator() instanceof AdditionOperator && binaryExpression.getLeft() instanceof Identifier leftIdentifier && binaryExpression.getRight() instanceof Constant constant) {
                Map<Identifier, Double> coefficients = new HashMap<>();
                coefficients.put(identifier, 1.0);
                coefficients.put(leftIdentifier, -1.0);
                LinearInequality inequality = new LinearInequality(coefficients, (Integer) (constant.getValue()));
                Set<LinearInequality> res = new HashSet<>(this.constraints);
                res.add(inequality);
                return new PairwiseInequalityDomain(res);
            }
            if (binaryExpression.getOperator() instanceof AdditionOperator && binaryExpression.getRight() instanceof Constant rightConstant) {
                if ((binaryExpression.getLeft() instanceof BinaryExpression leftExpr)) {
                    if ((leftExpr.getOperator() instanceof MultiplicationOperator) && leftExpr.getLeft() instanceof Constant constant && leftExpr.getRight() instanceof Identifier rightIdentifier) {
                        Map<Identifier, Double> coefficients = new HashMap<>();
                        coefficients.put(identifier, 1.0);
                        coefficients.put(rightIdentifier, -(Double) constant.getValue());
                        LinearInequality inequality = new LinearInequality(coefficients, (Integer) (rightConstant.getValue()));
                        Set<LinearInequality> res = new HashSet<>(this.constraints);
                        res.add(inequality);
                        return new PairwiseInequalityDomain(res);
                    }
                }
            }
            if (binaryExpression.getOperator() instanceof SubtractionOperator && binaryExpression.getLeft() instanceof Identifier leftIdentifier && binaryExpression.getRight() instanceof Constant constant) {
                Map<Identifier, Double> coefficients = new HashMap<>();
                coefficients.put(identifier, 1.0);
                coefficients.put(leftIdentifier, -1.0);
                LinearInequality inequality = new LinearInequality(coefficients, -(Integer) (constant.getValue()));
                Set<LinearInequality> res = new HashSet<>(this.constraints);
                res.add(inequality);
                return new PairwiseInequalityDomain(res);
            }
            if (binaryExpression.getOperator() instanceof SubtractionOperator && binaryExpression.getRight() instanceof Constant rightConstant) {
                if ((binaryExpression.getLeft() instanceof Identifier leftIdentifier)) {
                    Map<Identifier, Double> coefficients = new HashMap<>();
                    coefficients.put(identifier, 1.0);
                    coefficients.put(leftIdentifier, -1.0);
                    LinearInequality inequality = new LinearInequality(coefficients, -(Integer) (rightConstant.getValue()));
                    Set<LinearInequality> res = new HashSet<>(this.constraints);
                    res.add(inequality);
                    return new PairwiseInequalityDomain(res);
                }
            }
        }
        return this;
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

            if(constraintMap.containsKey(key)){
                LinearInequality existingConstraint = constraintMap.get(key);
                if(currentConstraint.lessOrEqual && existingConstraint.constant > currentConstraint.constant){
                    constraintMap.put(key, currentConstraint);
                }
            } else {
                constraintMap.put(key, currentConstraint);
            }
        }
        constraints.clear();
        constraints.addAll(constraintMap.values());
        return constraints;
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

        if(!(binaryExpression.getOperator() instanceof ComparisonLe)) {
            return this;
        }

        SymbolicExpression left = binaryExpression.getLeft();
        SymbolicExpression right = binaryExpression.getRight();

        Map<Identifier, Double> coefficients = new HashMap<>();
        if(left instanceof Identifier leftIdentifier && right instanceof Identifier rightIdentifier){
            coefficients.put(leftIdentifier, 1.0);
            coefficients.put(rightIdentifier, -1.0);
            LinearInequality inequality = new LinearInequality(coefficients, 0.0);
            Set<LinearInequality> res = new HashSet<>(this.constraints);
            res.add(inequality);
            return new PairwiseInequalityDomain(res);
        }
        if (left instanceof BinaryExpression leftExpr && right instanceof Constant rightConstant) {
            Integer constant = (Integer) rightConstant.getValue();
            if (leftExpr.getOperator() instanceof AdditionOperator && leftExpr.getLeft() instanceof BinaryExpression && leftExpr.getRight() instanceof BinaryExpression) {
                SymbolicExpression ax = leftExpr.getLeft();
                SymbolicExpression by = leftExpr.getRight();
                BinaryExpression axExpr = (BinaryExpression) ax;
                BinaryExpression byExpr = (BinaryExpression) by;
                if(axExpr.getOperator() instanceof MultiplicationOperator && byExpr.getOperator() instanceof MultiplicationOperator && axExpr.getLeft() instanceof Constant && axExpr.getRight() instanceof Identifier) {
                    SymbolicExpression a = axExpr.getLeft();
                    SymbolicExpression x = axExpr.getRight();
                    SymbolicExpression b = byExpr.getLeft();
                    SymbolicExpression y = byExpr.getRight();
                    coefficients.put((Identifier) y, ((Integer)((Constant) b).getValue()).doubleValue());
                    coefficients.put((Identifier) x, ((Integer)((Constant) a).getValue()).doubleValue());
                    LinearInequality inequality = new LinearInequality(coefficients, constant);
                    Set<LinearInequality> res = new HashSet<>(this.constraints);
                    res.add(inequality);
                    return new PairwiseInequalityDomain(res);
                }


            }
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
        if(isTop())
            return new StringRepresentation(Lattice.topRepresentation());
        if(isBottom())
            return new StringRepresentation(Lattice.bottomRepresentation());
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
        private double constant;

        public LinearInequality(Map<Identifier, Double> coefficients, double constant) {
            this.coefficients = new HashMap<>(coefficients);
            this.constant = constant;
        }
        public void setLessOrEqual(boolean lessOrEqual) {
            this.lessOrEqual = lessOrEqual;
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
