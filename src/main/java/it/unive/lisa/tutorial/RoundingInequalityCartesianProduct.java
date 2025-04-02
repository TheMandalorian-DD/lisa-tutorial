package it.unive.lisa.tutorial;

import it.unive.lisa.analysis.combination.CartesianProduct;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;

public class RoundingInequalityCartesianProduct extends CartesianProduct<RoundingInequalityCartesianProduct, PairwiseInequalityDomain, ValueEnvironment<RoundingInterval>, ValueExpression, Identifier>
        implements ValueDomain<RoundingInequalityCartesianProduct> {

    public RoundingInequalityCartesianProduct(PairwiseInequalityDomain left, ValueEnvironment<RoundingInterval> right) {
        super(left, right);
    }

    @Override
    public RoundingInequalityCartesianProduct mk(PairwiseInequalityDomain pairwiseInequalityDomain, ValueEnvironment<RoundingInterval> entries) {
        return null;
    }

    @Override
    public boolean knowsIdentifier(Identifier identifier) {
        return false;
    }
}
