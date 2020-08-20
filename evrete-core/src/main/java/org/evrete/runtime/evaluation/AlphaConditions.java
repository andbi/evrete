package org.evrete.runtime.evaluation;

import org.evrete.api.*;
import org.evrete.collections.ArrayOf;
import org.evrete.runtime.AbstractRuntime;

import java.util.*;
import java.util.function.Consumer;

import static org.evrete.api.LogicallyComparable.*;

public class AlphaConditions implements Copyable<AlphaConditions> {
    private static final ArrayOf<AlphaEvaluator> EMPTY = new ArrayOf<>(new AlphaEvaluator[0]);
    private final Map<Type, ArrayOf<AlphaEvaluator>> alphaPredicates;
    private final Map<Type, TypeAlphas> typeAlphas;

    private AlphaConditions(AlphaConditions other) {
        this.alphaPredicates = new HashMap<>();
        for (Map.Entry<Type, ArrayOf<AlphaEvaluator>> entry : other.alphaPredicates.entrySet()) {
            this.alphaPredicates.put(entry.getKey(), new ArrayOf<>(entry.getValue()));
        }

        this.typeAlphas = new HashMap<>();
        other.typeAlphas.forEach((fields, alphas) -> AlphaConditions.this.typeAlphas.put(fields, alphas.copyOf()));
    }

    public AlphaConditions() {
        this.typeAlphas = new HashMap<>();
        this.alphaPredicates = new HashMap<>();
    }

    @Override
    public AlphaConditions copyOf() {
        return new AlphaConditions(this);
    }

    public int size(Type type) {
        return alphaPredicates.getOrDefault(type, EMPTY).data.length;
    }

    public synchronized AlphaBucketMeta register(AbstractRuntime<?> runtime, FieldsKey betaFields, boolean beta, Set<Evaluator> typePredicates, Consumer<AlphaDelta> listener) {
        if (typePredicates.isEmpty() && betaFields.size() == 0) {
            return AlphaBucketMeta.NO_FIELDS_NO_CONDITIONS;
        }

        Collection<AlphaEvaluator> newEvaluators = new LinkedList<>();

        Type type = betaFields.getType();
        AlphaMeta candidate = createAlphaMask(runtime, type, typePredicates, newEvaluators::add);
        return typeAlphas
                .computeIfAbsent(type, TypeAlphas::new)
                .getCreate(
                        betaFields,
                        beta,
                        candidate,
                        alphaBucketMeta -> listener.accept(new AlphaDelta(betaFields, alphaBucketMeta, newEvaluators))
                );
    }

    public ArrayOf<AlphaEvaluator> getPredicates(Type t) {
        return alphaPredicates.getOrDefault(t, EMPTY);
    }

    @Override
    public String toString() {
        return "AlphaConditions{" +
                "alphaPredicates=" + alphaPredicates +
                '}';
    }

    private AlphaMeta createAlphaMask(AbstractRuntime<?> runtime, Type t, Set<Evaluator> typePredicates, Consumer<AlphaEvaluator> listener) {
        ArrayOf<AlphaEvaluator> existing = alphaPredicates.computeIfAbsent(t, k -> new ArrayOf<>(new AlphaEvaluator[0]));
        List<EvaluationSide> mapping = new LinkedList<>();

        for (Evaluator alphaPredicate : typePredicates) {
            AlphaEvaluator found = null;
            boolean foundDirect = true;

            for (AlphaEvaluator ia : existing.data) {
                int cmp = alphaPredicate.compare(ia.getDelegate());
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
                TypeField field = alphaPredicate.descriptor()[0].field();
                found = new AlphaEvaluator(existing.data.length, alphaPredicate, runtime.getCreateActiveField(field));
                existing.append(found);
                listener.accept(found);
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

    private static class EvaluationSide {
        private final AlphaEvaluator condition;
        private final boolean direct;

        EvaluationSide(AlphaEvaluator condition, boolean direct) {
            this.condition = condition;
            this.direct = direct;
        }
    }

    private static class FieldAlphas implements Copyable<FieldAlphas> {
        private final Set<AlphaBucketMeta> dataOld;
        private final ArrayOf<AlphaBucketMeta> data;

        FieldAlphas(FieldsKey fields) {
            this.dataOld = new HashSet<>();
            this.data = new ArrayOf<>(new AlphaBucketMeta[0]);
        }

        FieldAlphas(FieldAlphas other) {
            this.dataOld = new HashSet<>(other.dataOld);
            this.data = new ArrayOf<>(other.data);
        }

        AlphaBucketMeta getCreate(AlphaMeta candidate, Consumer<AlphaBucketMeta> listener) {
            AlphaBucketMeta found = null;
            for (AlphaBucketMeta mask : data.data) {
                if (mask.sameData(candidate.alphaEvaluators, candidate.requiredValues)) {
                    found = mask;
                    break;
                }
            }
            if (found == null) {
                found = AlphaBucketMeta.factory(data.data.length, candidate.alphaEvaluators, candidate.requiredValues);
                data.append(found);
                listener.accept(found);
            }

            return found;
        }

        @Override
        public FieldAlphas copyOf() {
            return new FieldAlphas(this);
        }
    }

    private static class TypeAlphas implements Copyable<TypeAlphas> {
        private final Map<FieldsKey, FieldAlphas> dataAlpha;
        private final Map<FieldsKey, FieldAlphas> dataBeta;

        TypeAlphas(Type type) {
            this.dataAlpha = new HashMap<>();
            this.dataBeta = new HashMap<>();
        }

        TypeAlphas(TypeAlphas other) {
            this.dataAlpha = new HashMap<>();
            this.dataBeta = new HashMap<>();
            this.dataAlpha.putAll(other.dataAlpha);
            this.dataBeta.putAll(other.dataBeta);
        }

        private AlphaBucketMeta getCreate(FieldsKey betaFields, boolean beta, AlphaMeta candidate, Consumer<AlphaBucketMeta> listener) {
            Map<FieldsKey, FieldAlphas> map = beta ?
                    dataBeta
                    :
                    dataAlpha;

            return map.computeIfAbsent(betaFields, FieldAlphas::new).getCreate(candidate, listener);
        }

        @Override
        public TypeAlphas copyOf() {
            return new TypeAlphas(this);
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