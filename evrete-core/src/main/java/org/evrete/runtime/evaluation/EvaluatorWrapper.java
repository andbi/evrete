package org.evrete.runtime.evaluation;

import org.evrete.api.*;
import org.evrete.runtime.builder.FieldReference;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class EvaluatorWrapper implements Evaluator, Copyable<EvaluatorWrapper>, EvaluationListenerHolder {
    private final Evaluator delegate;
    private final Set<EvaluationListener> listeners = new HashSet<>();
    private final Predicate<IntToValue> verboseUnmapped = new Predicate<IntToValue>() {
        @Override
        public boolean test(IntToValue intToValue) {
            boolean b = delegate.test(intToValue);
            for (EvaluationListener listener : listeners) {
                listener.fire(delegate, intToValue, b);
            }
            return b;
        }
    };
    private final Set<NamedType> types = new HashSet<>();
    private Predicate<IntToValue> active;
    private int[] indexMapper;
    private final Predicate<IntToValue> verboseMapped = new Predicate<IntToValue>() {
        @Override
        public boolean test(IntToValue intToValue) {
            IntToValue mapped = intToValue.remap(indexMapper);
            boolean b = delegate.test(mapped);
            for (EvaluationListener listener : listeners) {
                listener.fire(delegate, mapped, b);
            }
            return b;
        }
    };
    private final Predicate<IntToValue> muteMapped = new Predicate<IntToValue>() {
        @Override
        public boolean test(IntToValue intToValue) {
            IntToValue mapped = intToValue.remap(indexMapper);
            return delegate.test(mapped);
        }
    };

    public EvaluatorWrapper(Evaluator delegate) {
        this.delegate = unwrap(delegate);
        for (FieldReference ref : delegate.descriptor()) {
            this.types.add(ref.type());
        }
        updateActiveEvaluator();
    }

    private EvaluatorWrapper(EvaluatorWrapper other) {
        this.delegate = unwrap(other.delegate);
        this.listeners.addAll(other.listeners);
        this.types.addAll(other.types);
        this.indexMapper = other.indexMapper;
        updateActiveEvaluator();
    }

    private static Evaluator unwrap(Evaluator e) {
        if (e instanceof EvaluatorWrapper) {
            EvaluatorWrapper wrapper = (EvaluatorWrapper) e;
            return unwrap(wrapper.delegate);
        } else {
            return e;
        }
    }

    public void remap(int[] indexMapper) {
        this.indexMapper = indexMapper;
        updateActiveEvaluator();
    }

    @Override
    public void addListener(EvaluationListener listener) {
        this.listeners.add(listener);
        updateActiveEvaluator();
    }

    @Override
    public void removeListener(EvaluationListener listener) {
        this.listeners.remove(listener);
        updateActiveEvaluator();
    }

    private void updateActiveEvaluator() {
        if (listeners.isEmpty()) {
            if (indexMapper == null) {
                this.active = delegate;
            } else {
                this.active = muteMapped;
            }
        } else {
            if (indexMapper == null) {
                this.active = verboseUnmapped;
            } else {
                this.active = verboseMapped;
            }
        }
    }

    @Override
    public EvaluatorWrapper copyOf() {
        return new EvaluatorWrapper(this);
    }

    @Override
    public boolean test(IntToValue intToValue) {
        return active.test(intToValue);
    }

    @Override
    public double getComplexity() {
        return delegate.getComplexity();
    }

    @Override
    public FieldReference[] descriptor() {
        return delegate.descriptor();
    }

    @Override
    public int compare(LogicallyComparable other) {
        return delegate.compare(other);
    }
}