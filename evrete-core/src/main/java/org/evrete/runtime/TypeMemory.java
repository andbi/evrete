package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.collections.ArrayOf;
import org.evrete.runtime.evaluation.AlphaBucketMeta;
import org.evrete.runtime.evaluation.AlphaConditions;
import org.evrete.runtime.evaluation.AlphaDelta;
import org.evrete.runtime.evaluation.AlphaEvaluator;
import org.evrete.util.ActionQueue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class TypeMemory extends TypeMemoryBase {
    private static final Logger LOGGER = Logger.getLogger(TypeMemory.class.getName());
    private final AlphaConditions alphaConditions;
    private final Map<FieldsKey, FieldsMemory> betaMemories = new HashMap<>();
    private final ArrayOf<TypeMemoryBucket> alphaBuckets;
    private final ActionQueue<Object> inputBuffer = new ActionQueue<>();

    TypeMemory(SessionMemory runtime, Type<?> type) {
        super(runtime, type);
        this.alphaConditions = runtime.getAlphaConditions();
        this.alphaBuckets = new ArrayOf<>(new TypeMemoryBucket[]{new TypeMemoryBucket(runtime, AlphaBucketMeta.NO_FIELDS_NO_CONDITIONS)});
    }

    public final Set<FieldsKey> knownFieldSets() {
        return Collections.unmodifiableSet(betaMemories.keySet());
    }

    void processInsertBuffer() {
        ReIterator<Object> inserts = inputBuffer.get(Action.INSERT);
        while (inserts.hasNext()) {
            Object o = inserts.next();
            RuntimeFactImpl fact = create(o);
            for (TypeMemoryBucket bucket : alphaBuckets.data) {
                bucket.insert(fact);
            }

            for (FieldsMemory fm : betaMemories.values()) {
                fm.insert(fact);
            }
        }
        inputBuffer.clear(Action.INSERT);
    }


    boolean bufferContains(Action... actions) {
        for (Action action : actions) {
            if (inputBuffer.hasData(action)) {
                return true;
            }
        }
        return false;
    }

    private RuntimeFactImpl find(Object o) {
        RuntimeFact fact = main0().find(o);
        if (fact == null) {
            fact = delta0().find(o);
        }
        return (RuntimeFactImpl) fact;
    }

    void clear() {
        for (TypeMemoryBucket bucket : alphaBuckets.data) {
            bucket.clear();
        }

        for (FieldsMemory fm : betaMemories.values()) {
            fm.clear();
        }
        inputBuffer.clear();
    }

    public final FieldsMemory get(FieldsKey fields) {
        FieldsMemory fm = betaMemories.get(fields);
        if (fm == null) {
            throw new IllegalArgumentException("No key memory exists for " + fields);
        } else {
            return fm;
        }
    }

    void commitDeltas() {
        for (TypeMemoryBucket bucket : this.alphaBuckets.data) {
            bucket.commitChanges();
        }

        for (FieldsMemory fm : betaMemories.values()) {
            fm.commitDeltas();
        }
    }

    void processDeleteBuffer() {
        ReIterator<Object> it = inputBuffer.get(Action.RETRACT);

        // Step 1: Marking facts as deleted
        RuntimeFactImpl impl;
        while (it.hasNext()) {
            Object o = it.next();
            impl = find(o);
            if (impl == null) {
                LOGGER.warning("Unknown object " + o + ", DELETE operation skipped");
            } else {
                impl.setDeleted(true);
                // Step 2: clear alpha storage
                // Step 3: clear beta storage
                for (FieldsMemory fm : betaMemories.values()) {
                    fm.retract(impl);
                }
            }
        }
        // Step 3: clear the delete buffer
        inputBuffer.clear(Action.RETRACT);
    }

    public PlainMemory get(AlphaBucketMeta alphaMask) {
        return alphaBuckets.getChecked(alphaMask.getBucketIndex());
    }

    void touchMemory(FieldsKey key, AlphaBucketMeta alphaMeta) {
        if (key.size() == 0) {
            touchAlphaMemory(alphaMeta);
        } else {
            betaMemories
                    .computeIfAbsent(key, k -> new FieldsMemory(getRuntime(), key))
                    .touchMemory(alphaMeta);
        }
    }

    private TypeMemoryBucket touchAlphaMemory(AlphaBucketMeta alphaMeta) {
        // Alpha storage
        if (!alphaMeta.isEmpty()) {
            int bucketIndex = alphaMeta.getBucketIndex();
            if (alphaBuckets.isEmptyAt(bucketIndex)) {
                TypeMemoryBucket newBucket = new TypeMemoryBucket(getRuntime(), alphaMeta);
                alphaBuckets.set(bucketIndex, newBucket);
                return newBucket;
            }
        }
        return null;
    }

    void onNewAlphaBucket(AlphaDelta delta) {
        if (inputBuffer.get(Action.INSERT).reset() > 0) {
            //TODO develop a strategy
            throw new UnsupportedOperationException("A new condition was created in an uncommitted memory.");
        }

        ReIterator<RuntimeFact> existingFacts = main0().iterator();
        // 1. Update all the facts by applying new alpha flags
        AlphaEvaluator[] newEvaluators = delta.getNewEvaluators();
        if (newEvaluators.length > 0 && existingFacts.reset() > 0) {
            while (existingFacts.hasNext()) {
                RuntimeFactImpl fact = (RuntimeFactImpl) existingFacts.next();

                fact.appendAlphaTest(newEvaluators);
            }
        }


        // 2. Create and fill buckets
        FieldsKey key = delta.getKey();
        AlphaBucketMeta alphaMeta = delta.getNewAlphaMeta();
        if (key.size() == 0) {
            // 3. Create new alpha data bucket
            TypeMemoryBucket newBucket = touchAlphaMemory(alphaMeta);
            assert newBucket != null;
            // Fill data
            newBucket.fillMainStorage(existingFacts);
        } else {
            // 3. Process keyed/beta-memory
            betaMemories
                    .computeIfAbsent(key, k -> new FieldsMemory(getRuntime(), key))
                    .onNewAlphaBucket(alphaMeta, existingFacts);
        }

        this.cachedAlphaEvaluators = alphaConditions.getPredicates(type).data;
    }

    final <T> void forEachMemoryObject(Consumer<T> consumer) {
        main0().iterator().forEachRemaining(fact -> {
            if (!fact.isDeleted()) {
                consumer.accept(fact.getDelegate());
            }
        });
    }

    final void forEachObjectUnchecked(Consumer<Object> consumer) {
        main0().iterator().forEachRemaining(fact -> {
            if (!fact.isDeleted()) {
                consumer.accept(fact.getDelegate());
            }
        });
    }


    private SharedPlainFactStorage main0() {
        return alphaBuckets.data[0].getData();
    }

    private SharedPlainFactStorage delta0() {
        return alphaBuckets.data[0].getDelta();
    }

    private RuntimeFactImpl create(Object o) {
        // Read values
        Object[] values = new Object[cachedActiveFields.length];
        for (int i = 0; i < cachedActiveFields.length; i++) {
            values[i] = cachedActiveFields[i].readValue(o);
        }

        // Evaluate alpha conditions if necessary
        if (cachedAlphaEvaluators.length > 0) {
            boolean[] alphaTests = new boolean[cachedAlphaEvaluators.length];
            for (AlphaEvaluator alpha : cachedAlphaEvaluators) {
                alphaTests[alpha.getUniqueId()] = alpha.test(values);
            }
            return RuntimeFactImpl.factory(o, values, alphaTests);
        } else {
            return RuntimeFactImpl.factory(o, values);
        }
    }

    /**
     * <p>
     * Modifies existing facts by appending value of the newly
     * created field
     * </p>
     *
     * @param newField newly created field
     */
    final void onNewActiveField(ActiveField newField) {
        for (SharedPlainFactStorage storage : new SharedPlainFactStorage[]{main0(), delta0()}) {
            ReIterator<RuntimeFact> it = storage.iterator();
            while (it.hasNext()) {
                RuntimeFactImpl rto = (RuntimeFactImpl) it.next();
                Object fieldValue = newField.readValue(rto.getDelegate());
                rto.appendValue(newField, fieldValue);
            }

        }
        this.cachedActiveFields = getRuntime().getActiveFields(type);
    }

    void memoryAction(Action action, Object o) {
        inputBuffer.add(action, o);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(1024);
        for (TypeMemoryBucket b : alphaBuckets.data) {


            s.append(b.getAlphaMask()).append("\n");
            s.append("\tM:").append(b.getData()).append('\n');
            s.append("\tD:").append(b.getDelta()).append('\n');
        }

        return s.toString();
/*
        return "TypeMemory{" +
                "alphaBuckets=" + alphaBuckets +
                '}';
*/
    }
}
