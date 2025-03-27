package it.unive.lisa.tutorial;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.util.representation.MapRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

public class IntervalStrictProduct implements ValueDomain<IntervalStrictProduct>, BaseLattice<IntervalStrictProduct> {

    private final StrictUpperBounds upper;
    private final ValueEnvironment<Interval> intervals;

    public IntervalStrictProduct() {
        this(new StrictUpperBounds().top(), new ValueEnvironment<>(new Interval()).top());
    }

    private IntervalStrictProduct(StrictUpperBounds upper, ValueEnvironment<Interval> intervals) {
        this.upper = upper;
        this.intervals = intervals;
    }

    @Override public IntervalStrictProduct top() { return new IntervalStrictProduct(upper.top(), intervals.top()); }
    @Override public IntervalStrictProduct bottom() { return new IntervalStrictProduct(upper.bottom(), intervals.bottom()); }
    @Override public boolean isTop() { return upper.isTop() && intervals.isTop(); }
    @Override public boolean isBottom() { return upper.isBottom() && intervals.isBottom(); }

    @Override
    public boolean lessOrEqualAux(IntervalStrictProduct other) throws SemanticException {
        return intervals.lessOrEqual(other.intervals) && upper.lessOrEqual(other.upper);
    }

    @Override
    public IntervalStrictProduct lubAux(IntervalStrictProduct other) throws SemanticException {
        return new IntervalStrictProduct(upper.lub(other.upper), intervals.lub(other.intervals));
    }

    @Override
    public IntervalStrictProduct glbAux(IntervalStrictProduct other) throws SemanticException {
        return new IntervalStrictProduct(upper.lub(other.upper), intervals.lub(other.intervals));
    }

    @Override
    public IntervalStrictProduct wideningAux(IntervalStrictProduct other) throws SemanticException {
        return new IntervalStrictProduct(upper.widening(other.upper), intervals.widening(other.intervals));
    }

    @Override
    public IntervalStrictProduct assign(Identifier id, ValueExpression expression, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        return new IntervalStrictProduct(upper.assign(id, expression, pp, oracle), intervals.assign(id, expression, pp, oracle));
    }

    @Override
    public IntervalStrictProduct smallStepSemantics(ValueExpression expression, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        return new IntervalStrictProduct(upper.smallStepSemantics(expression, pp, oracle), intervals.smallStepSemantics(expression, pp, oracle));
    }

    @Override
    public IntervalStrictProduct assume(ValueExpression expression, ProgramPoint src, ProgramPoint dest, SemanticOracle oracle) throws SemanticException {
        return new IntervalStrictProduct(upper.assume(expression, src, dest, oracle), intervals.assume(expression, src, dest, oracle));
    }

    @Override
    public IntervalStrictProduct forgetIdentifier(Identifier id) throws SemanticException {
        return new IntervalStrictProduct(upper.forgetIdentifier(id), intervals.forgetIdentifier(id));
    }

    @Override
    public IntervalStrictProduct forgetIdentifiersIf(Predicate<Identifier> test) throws SemanticException {
        return new IntervalStrictProduct(upper.forgetIdentifiersIf(test), intervals.forgetIdentifiersIf(test));
    }

    @Override
    public Satisfiability satisfies(ValueExpression expression, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        return intervals.satisfies(expression, pp, oracle).glb(upper.satisfies(expression, pp, oracle));
    }

    @Override
    public IntervalStrictProduct pushScope(ScopeToken token) throws SemanticException {
        return new IntervalStrictProduct(upper.pushScope(token), intervals.pushScope(token));
    }

    @Override
    public IntervalStrictProduct popScope(ScopeToken token) throws SemanticException {
        return new IntervalStrictProduct(upper.popScope(token), intervals.popScope(token));
    }

    @Override
    public StructuredRepresentation representation() {
        if (isTop()) return Lattice.topRepresentation();
        if (isBottom()) return Lattice.bottomRepresentation();
        Map<StructuredRepresentation, StructuredRepresentation> map = new HashMap<>();
        for (Identifier id : intervals.getKeys())
            map.put(new StringRepresentation(id.toString()),
                    new StringRepresentation(intervals.getState(id) + " | " + upper.getState(id).toString()));
        return new MapRepresentation(map);
    }

    @Override public boolean knowsIdentifier(Identifier id) { return intervals.knowsIdentifier(id) || upper.knowsIdentifier(id); }
    @Override public int hashCode() { return Objects.hash(intervals, upper); }
    @Override public boolean equals(Object obj) {
        if (!(obj instanceof IntervalStrictProduct)) return false;
        IntervalStrictProduct o = (IntervalStrictProduct) obj;
        return Objects.equals(intervals, o.intervals) && Objects.equals(upper, o.upper);
    }
}
