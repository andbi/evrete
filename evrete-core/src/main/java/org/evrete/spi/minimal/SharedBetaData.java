package org.evrete.spi.minimal;

import org.evrete.api.*;
import org.evrete.runtime.ActiveField;
import org.evrete.runtime.FieldsKey;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Objects;
import java.util.function.BiPredicate;

class SharedBetaData implements SharedBetaFactStorage {
    private final ValueHandle[] reusableValueArr;
    private final FieldsFactMap deltaNewKeys = new FieldsFactMap();
    private final FieldsFactMap deltaKnownKeys = new FieldsFactMap();
    private final FieldsFactMap main = new FieldsFactMap();
    private final ActiveField[] fields;
    private final EnumMap<KeyMode, ReIterable<ValueRow>> keyIterables;

    private final BiPredicate<ValueRowImpl, Object[]> SHARED_ARRAY_EQ = new BiPredicate<ValueRowImpl, Object[]>() {
        @Override
        public boolean test(ValueRowImpl entry, Object[] values) {
            return !entry.isDeleted() && MiscUtils.sameData1(entry.data, reusableValueArr);
        }
    };

    SharedBetaData(FieldsKey typeFields) {
        this.fields = typeFields.getFields();
        this.reusableValueArr = new ValueHandle[fields.length];
        this.keyIterables = buildKeyIterables();
    }


    @Override
    public EnumMap<KeyMode, ReIterable<ValueRow>> keyIterables() {
        return keyIterables;
    }

    private EnumMap<KeyMode, ReIterable<ValueRow>> buildKeyIterables() {
        EnumMap<KeyMode, ReIterable<ValueRow>> map = new EnumMap<>(KeyMode.class);
        for (KeyMode mode : KeyMode.values()) {
            ReIterable<ValueRow> iterator;
            switch (mode) {
                case KNOWN_KNOWN:
                    iterator = main::keyIterator;
                    break;
                case KNOWN_UNKNOWN:
                    iterator = deltaKnownKeys::keyIterator;
                    break;
                case UNKNOWN_UNKNOWN:
                    iterator = deltaNewKeys::keyIterator;
                    break;
                default:
                    throw new IllegalStateException();
            }
            map.put(mode, iterator);
        }
        return map;
    }

    @Override
    public void clear() {
        deltaNewKeys.clear();
        deltaKnownKeys.clear();
        main.clear();
    }

    @ThreadUnsafe
    private int hash(FieldToValueHandle key) {
        int hash = 0;
        for (int i = 0; i < fields.length; i++) {
            hash ^= Objects.hashCode(reusableValueArr[i] = key.apply(fields[i]));
        }
        return hash;
    }

/*
    @Override
    public void delete(RuntimeFact fact) {
        assert fact.isDeleted();
        int hash = hash(fact);
        int addr = main.findBinIndex(reusableValueArr, hash, SHARED_ARRAY_EQ);
        main.remove(fact, addr);
    }
*/

    @Override
    public void commitChanges() {
        main.addAll(deltaNewKeys);
        main.addAll(deltaKnownKeys);

        deltaNewKeys.clear();
        deltaKnownKeys.clear();
    }

    private void insertInner(FactHandleVersioned fact, FieldToValueHandle values) {
        int hash = hash(values);
        int addr = main.findBinIndex(reusableValueArr, hash, SHARED_ARRAY_EQ);
        ValueRowImpl key = main.get(addr);
        if (key == null) {
            // No entry in main storage, now looking in delta
            insertTo(hash, fact, deltaNewKeys);
        } else {
            // The key is already known to the main storage, yet we need to keep a copy of inserts for RHS calls
            // see https://github.com/andbi/evrete/issues/2
            insertTo(hash, fact, deltaKnownKeys);
        }
    }

    private void insertTo(int hash, FactHandleVersioned fact, FieldsFactMap destination) {
        destination.resize();
        int addr = destination.findBinIndex(reusableValueArr, hash, SHARED_ARRAY_EQ);
        ValueRowImpl key = destination.get(addr);
        if (key == null) {
            ValueRowImpl vr = new ValueRowImpl(Arrays.copyOf(reusableValueArr, reusableValueArr.length), hash, fact);
            destination.saveDirect(vr, addr);
        } else {
            key.addFact(fact);
        }
    }

    @Override
    public void insert(FactHandleVersioned value, FieldToValueHandle key) {
        insertInner(value, key);
    }

    @Override
    public String toString() {
        return "{" +
                "main=" + main +
                ", kNew=" + deltaNewKeys +
                ", kOld=" + deltaKnownKeys +
                '}';
    }
}
