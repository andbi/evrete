package org.evrete.runtime.evaluation;

import org.evrete.api.*;
import org.evrete.runtime.BetaEvaluationState;
import org.evrete.runtime.BetaFieldReference;
import org.evrete.runtime.FactType;

import java.util.function.Function;

public class BetaEvaluator extends EvaluatorWrapper implements Copyable<BetaEvaluator> {
    public static final BetaEvaluator[] ZERO_ARRAY = new BetaEvaluator[0];
    private final BetaFieldReference[] descriptor;
    //private IntToValueHandle stateValues2;
    private IntToValue stateValues1;
    private BetaEvaluationState values;
    private ValueResolver valueResolver = null;

    public BetaEvaluator(EvaluatorWrapper delegate, Function<NamedType, FactType> typeFunction) {
        super(delegate);
        FieldReference[] evaluatorDescriptor = delegate.descriptor();
        this.descriptor = new BetaFieldReference[evaluatorDescriptor.length];
        for (int i = 0; i < this.descriptor.length; i++) {
            FieldReference fieldReference = evaluatorDescriptor[i];
            FactType factType = typeFunction.apply(fieldReference.type());
            TypeField field = fieldReference.field();
            this.descriptor[i] = new BetaFieldReference(factType, field);
        }
    }

    private BetaEvaluator(BetaEvaluator other) {
        super(other);
        this.descriptor = other.descriptor;
        this.stateValues1 = other.stateValues1;
        //this.stateValues2 = other.stateValues2;
        this.values = other.values;
        this.valueResolver = null;
    }

    public BetaFieldReference[] betaDescriptor() {
        return descriptor;
    }

    @Override
    public BetaEvaluator copyOf() {
        return new BetaEvaluator(this);
    }

    void setEvaluationState(final ValueResolver resolver, BetaEvaluationState values) {
        final Argument[] arguments = new Argument[descriptor.length];
        for (int i = 0; i < arguments.length; i++) {
            BetaFieldReference ref = descriptor[i];
            arguments[i] = new Argument(values, ref.getFactType(), ref.getFieldIndex());
        }

        //this.stateValues = value -> arguments[value].getValue1();
        this.stateValues1 = new IntToValue() {
            @Override
            public Object apply(int value) {
                ValueHandle vh = arguments[value].getValue1();
                return resolver.getValue(vh);
            }
        };
    }

    public boolean test() {
        return test(this.stateValues1);
    }

    private static class Argument {
        private final BetaEvaluationState values;
        private final FactType factType;
        private final int index;

        Argument(BetaEvaluationState values, FactType factType, int index) {
            this.values = values;
            this.factType = factType;
            this.index = index;
        }

        ValueHandle getValue1() {
            return values.apply1(factType, index);
        }
    }
}
