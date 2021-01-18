package org.evrete.runtime;

import org.evrete.api.Memory;
import org.evrete.api.ReIterator;
import org.evrete.api.RuntimeFact;
import org.evrete.api.SharedBetaFactStorage;
import org.evrete.collections.ArrayOf;
import org.evrete.runtime.evaluation.AlphaBucketMeta;

public class FieldsMemory implements Memory {
    private final FieldsKey typeFields;
    private final SessionMemory runtime;
    private final ArrayOf<FieldsMemoryBucket> alphaBuckets;

    FieldsMemory(SessionMemory runtime, FieldsKey typeFields) {
        this.runtime = runtime;
        this.typeFields = typeFields;
        this.alphaBuckets = new ArrayOf<>(FieldsMemoryBucket.class);
    }

    public SharedBetaFactStorage get(AlphaBucketMeta mask) {
        int bucketIndex = mask.getBucketIndex();
        if (bucketIndex >= alphaBuckets.data.length) {
            throw new IllegalArgumentException("No alpha bucket created for " + mask);
        } else {
            SharedBetaFactStorage storage = alphaBuckets.data[bucketIndex].getFieldData();
            if (storage == null) {
                throw new IllegalArgumentException("No alpha bucket created for " + mask);
            } else {
                return storage;
            }
        }
    }

    @Override
    public void commitChanges() {
        for (FieldsMemoryBucket bucket : alphaBuckets.data) {
            bucket.commitChanges();
        }
    }

    void commitDeltas() {
        for (FieldsMemoryBucket bucket : alphaBuckets.data) {
            bucket.commitChanges();
        }
    }

    FieldsMemoryBucket touchMemory(AlphaBucketMeta alphaMeta) {
        int bucketIndex = alphaMeta.getBucketIndex();
        if (alphaBuckets.isEmptyAt(bucketIndex)) {
            FieldsMemoryBucket newBucket = new FieldsMemoryBucket(runtime, typeFields, alphaMeta);
            alphaBuckets.set(bucketIndex, newBucket);
            return newBucket;
        }
        return null;
    }

    <T extends RuntimeFact> void onNewAlphaBucket(AlphaBucketMeta alphaMeta, ReIterator<T> existingFacts) {
        FieldsMemoryBucket newBucket = touchMemory(alphaMeta);
        assert newBucket != null;
        if (existingFacts.reset() > 0) {
            while (existingFacts.hasNext()) {
                RuntimeFact rto = existingFacts.next();
                if (alphaMeta.test(rto)) {
                    //TODO !!!!!!! create tests and resolve the situation
                    throw new UnsupportedOperationException();
                }
            }
        }
    }

    void clear() {
        for (FieldsMemoryBucket bucket : alphaBuckets.data) {
            bucket.clear();
        }
    }

    void insert(RuntimeFact fact) {
        for (FieldsMemoryBucket bucket : alphaBuckets.data) {
            bucket.insert(fact);
        }
    }

    void retract(RuntimeFact fact) {
        for (FieldsMemoryBucket bucket : alphaBuckets.data) {
            bucket.delete(fact);
        }
    }

    @Override
    public String toString() {
        return alphaBuckets.toString();
    }
}