package org.evrete.runtime.evaluation;

import org.evrete.api.*;
import org.evrete.collections.ArrayOf;
import org.evrete.runtime.AbstractRuntime;
import org.evrete.runtime.ActiveField;
import org.evrete.runtime.FieldsKey;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.evrete.api.LogicallyComparable.*;

//TODO add comments
public class AlphaConditions implements Copyable<AlphaConditions>, EvaluationListeners {
    private static final ArrayOf<AlphaEvaluator> EMPTY = new ArrayOf<>(new AlphaEvaluator[0]);
    private final Map<Type<?>, ArrayOf<AlphaEvaluator>> alphaPredicates;
    private final Map<Type<?>, TypeAlphas> typeAlphas;

    private AlphaConditions(AlphaConditions other) {
        this.alphaPredicates = new HashMap<>();
        for (Map.Entry<Type<?>, ArrayOf<AlphaEvaluator>> entry : other.alphaPredicates.entrySet()) {
            AlphaEvaluator[] alphaEvaluators = entry.getValue().data;
            AlphaEvaluator[] copy = new AlphaEvaluator[alphaEvaluators.length];
            for (int i = 0; i < copy.length; i++) {
                copy[i] = alphaEvaluators[i].copyOf();
            }
            this.alphaPredicates.put(entry.getKey(), new ArrayOf<>(copy));
        }

        this.typeAlphas = new HashMap<>();
        other.typeAlphas.forEach((type, alphas) -> AlphaConditions.this.typeAlphas.put(type, alphas.copyOf()));
    }

    public AlphaConditions() {
        this.typeAlphas = new HashMap<>();
        this.alphaPredicates = new HashMap<>();
    }

    @Override
    public void addListener(EvaluationListener listener) {
        forEachAlphaCondition(e -> e.addListener(listener));
    }

    @Override
    public void removeListener(EvaluationListener listener) {
        forEachAlphaCondition(e -> e.removeListener(listener));
    }

    private void forEachAlphaCondition(Consumer<AlphaEvaluator> consumer) {
        for (ArrayOf<AlphaEvaluator> evaluators : this.alphaPredicates.values()) {
            for (AlphaEvaluator e : evaluators.data) {
                consumer.accept(e);
            }
        }
    }

    @Override
    public AlphaConditions copyOf() {
        return new AlphaConditions(this);
    }

    public int size(Type<?> type) {
        return alphaPredicates.getOrDefault(type, EMPTY).data.length;
    }

    public synchronized AlphaBucketMeta register(AbstractRuntime<?> runtime, FieldsKey betaFields, Set<EvaluatorWrapper> typePredicates, BiConsumer<FieldsKey, AlphaBucketMeta> listener) {
        Type<?> type = betaFields.getType();
        AlphaMeta candidate = createAlphaMask(runtime, type, typePredicates);
        return typeAlphas
                .computeIfAbsent(type, TypeAlphas::new)
                .getCreate(betaFields, candidate, listener);
    }

    @Override
    public String toString() {
        return "AlphaConditions{" +
                "alphaPredicates=" + alphaPredicates +
                '}';
    }

    private static class EvaluationSide {
        private final AlphaEvaluator condition;
        private final boolean direct;

        EvaluationSide(AlphaEvaluator condition, boolean direct) {
            this.condition = condition;
            this.direct = direct;
        }
    }

    private AlphaMeta createAlphaMask(AbstractRuntime<?> runtime, Type<?> t, Set<EvaluatorWrapper> typePredicates) {
        ArrayOf<AlphaEvaluator> existing = alphaPredicates.computeIfAbsent(t, k -> new ArrayOf<>(new AlphaEvaluator[0]));
        List<EvaluationSide> mapping = new LinkedList<>();

        for (EvaluatorWrapper alphaPredicate : typePredicates) {
            AlphaEvaluator found = null;
            boolean foundDirect = true;

            for (AlphaEvaluator ia : existing.data) {
                int cmp = alphaPredicate.compare(ia);
                switch (cmp) {
                    case RELATION_EQUALS:
                        found = ia;
                        foundDirect = true;
                        break;
                    case RELATION_INVERSE:
                        found = ia;
                        foundDirect = false;
                        break;
                    case RELATION_NONE:
                        break;
                    default:
                        throw new IllegalStateException();
                }
            }

            if (found == null) {
                //Unknown condition
                FieldReference[] descriptor = alphaPredicate.descriptor();

                ActiveField[] activeDescriptor = new ActiveField[descriptor.length];
                for (int i = 0; i < descriptor.length; i++) {
                    FieldReference ref = descriptor[i];
                    ActiveField af = runtime.getCreateActiveField(ref.field());
                    assert af.getDelegate().equals(ref.field());
                    activeDescriptor[i] = af;
                }

                found = new AlphaEvaluator(existing.data.length, alphaPredicate, activeDescriptor);
                existing.append(found);
            }

            mapping.add(new EvaluationSide(found, foundDirect));
        }

        boolean[] validValues = new boolean[existing.data.length];
        AlphaEvaluator[] alphaEvaluators = new AlphaEvaluator[mapping.size()];

        int mappingIdx = 0;
        for (EvaluationSide h : mapping) {
            int alphaId = h.condition.getUniqueId();
            alphaEvaluators[mappingIdx] = h.condition;
            validValues[alphaId] = h.direct;
            mappingIdx++;
        }

        Arrays.sort(alphaEvaluators, Comparator.comparingInt(AlphaEvaluator::getUniqueId));
        return new AlphaMeta(validValues, alphaEvaluators);
    }


    private static class TypeAlphas implements Copyable<TypeAlphas> {
        private final Map<FieldsKey, FieldAlphas> data;

        TypeAlphas(Type<?> type) {
            this.data = new HashMap<>();
        }

        TypeAlphas(TypeAlphas other) {
            this.data = new HashMap<>(other.data);
        }

        private AlphaBucketMeta getCreate(FieldsKey betaFields, AlphaMeta candidate, BiConsumer<FieldsKey, AlphaBucketMeta> listener) {
            return data.computeIfAbsent(betaFields, FieldAlphas::new).getCreate(candidate, listener);
        }

        @Override
        public TypeAlphas copyOf() {
            return new TypeAlphas(this);
        }

        private static class FieldAlphas implements Copyable<FieldAlphas> {
            private final ArrayOf<AlphaBucketMeta> data;
            private final FieldsKey fields;

            FieldAlphas(FieldsKey fields) {
                this.fields = fields;
                this.data = new ArrayOf<>(new AlphaBucketMeta[0]);
            }

            FieldAlphas(FieldAlphas other) {
                this.data = new ArrayOf<>(other.data);
                this.fields = other.fields;
            }

            AlphaBucketMeta getCreate(AlphaMeta candidate, BiConsumer<FieldsKey, AlphaBucketMeta> listener) {
                for (AlphaBucketMeta mask : data.data) {
                    if (mask.sameData(candidate.alphaEvaluators, candidate.requiredValues)) {
                        return mask;
                    }
                }
                AlphaBucketMeta found = AlphaBucketMeta.factory(data.data.length, candidate.alphaEvaluators, candidate.requiredValues);
                data.append(found);
                listener.accept(fields, found);

                return found;
            }

            @Override
            public FieldAlphas copyOf() {
                return new FieldAlphas(this);
            }
        }

    }

    private static class AlphaMeta {
        private final AlphaEvaluator[] alphaEvaluators;
        private final boolean[] requiredValues;

        AlphaMeta(boolean[] requiredValues, AlphaEvaluator[] alphaEvaluators) {
            this.requiredValues = requiredValues;
            this.alphaEvaluators = alphaEvaluators;
        }
    }
}
