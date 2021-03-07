package org.evrete.runtime;

import org.evrete.api.IntToMemoryKey;
import org.evrete.api.MemoryKey;
import org.evrete.api.ReIterator;
import org.evrete.collections.LinkedDataRW;

class ZStore implements ZStoreI {
    private final FactType[] mapping;
    private final LinkedDataRW<MemoryKey> data = new LinkedDataRW<>();

    ZStore(FactType[] mapping) {
        this.mapping = mapping;
    }

    @Override
    public ReIterator<MemoryKey> entries() {
        return data.iterator();
    }

    @Override
    public void clear() {
        data.clear();
    }

    @Override
    public void append(ZStoreI other) {
        this.data.append(((ZStore) other).data);
    }

    @Override
    public void save(IntToMemoryKey key) {
        for (int i = 0; i < mapping.length; i++) {
            this.data.add(key.apply(i));
        }
    }

    @Override
    public String toString() {
        return data.toString();
    }
}