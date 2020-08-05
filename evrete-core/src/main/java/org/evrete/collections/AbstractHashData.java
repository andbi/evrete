package org.evrete.collections;

import org.evrete.api.BufferedInsert;
import org.evrete.api.ReIterable;
import org.evrete.api.ReIterator;
import org.evrete.util.CollectionUtils;

import java.lang.reflect.Array;
import java.util.StringJoiner;
import java.util.function.*;
import java.util.stream.Stream;

/**
 * A simple implementation of linear probing hash table without fail-fast bells and whistles and alike.
 * Unlike the stock Java's HashMap, this implementation can shrink down its bucket table when necessary
 * thus preserving the _real_ O(N) scan complexity. (Java HashMap's scan time degrades significantly at
 * low fill ratios). Compared to the Java's HashMap, this implementation is 2 times faster in scanning
 * and shows similar timing for put/remove operations
 *
 * @param <E> Entry type
 */
public abstract class AbstractHashData<E> extends UnsignedIntArray implements ReIterable<E>, BufferedInsert {
    private static final int DEFAULT_INITIAL_CAPACITY = 4;
    protected static final ToIntFunction<Object> DEFAULT_HASH = Object::hashCode;
    protected static final ToIntFunction<Object> IDENTITY_HASH = System::identityHashCode;
    protected static final BiPredicate<Object, Object> DEFAULT_EQUALS = Object::equals;
    protected static final BiPredicate<Object, Object> IDENTITY_EQUALS = (o1, o2) -> o1 == o2;
    static final int MAXIMUM_CAPACITY = 1 << 30;
    private static final int MINIMUM_CAPACITY = 2;
    protected Object[] data;
    protected boolean[] deletedIndices;
    protected int size = 0;
    protected int deletes = 0;

    protected AbstractHashData(int initialCapacity) {
        super(initialCapacity);
        int capacity = tableSizeFor(initialCapacity);
        this.data = new Object[capacity];
        this.deletedIndices = new boolean[capacity];
    }

    protected AbstractHashData() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    private static int findBinIndexFor(Object key, int hash, Object[] destination, BiPredicate<Object, Object> eqTest) {
        int mask = destination.length - 1;
        int addr = hash & mask;
        Object found;
        while ((found = destination[addr]) != null) {
            if (eqTest.test(key, found)) {
                return addr;
            } else {
                addr = (addr + 1) & mask;
            }
        }
        return addr;
    }

    private static int findEmptyBinIndex(int hash, Object[] destination) {
        int mask = destination.length - 1;
        int addr = hash & mask;
        while (destination[addr] != null) {
            addr = (addr + 1) & mask;
        }
        return addr;
    }

    @SuppressWarnings("unchecked")
    private static <K, E> int findBinIndex(K key, int hash, Object[] destination, BiPredicate<E, K> eqTest) {
        int mask = destination.length - 1;
        int addr = hash & mask;
        E found;
        while ((found = (E) destination[addr]) != null) {
            if (eqTest.test(found, key)) {
                return addr;
            } else {
                addr = (addr + 1) & mask;
            }
        }
        return addr;
    }

    private static int findBinIndexFor(int hash, Object[] destination, Predicate<Object> eqTest) {
        int mask = destination.length - 1;
        int addr = hash & mask;
        Object found;
        while ((found = destination[addr]) != null) {
            if (eqTest.test(found)) {
                return addr;
            } else {
                addr = (addr + 1) & mask;
            }
        }
        return addr;
    }

    /**
     * Returns a power of two size for the given target capacity.
     */
    static int tableSizeFor(int capacity) {
        int cap = Math.max(capacity, MINIMUM_CAPACITY);
        int n = -1 >>> Integer.numberOfLeadingZeros(cap - 1);
        return (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    @SuppressWarnings("unchecked")
    public E get(int addr) {
        return deletedIndices[addr] ? null : (E) data[addr];
    }

    private int findBinIndexFor(Object key, int hash, BiPredicate<Object, Object> eqTest) {
        return findBinIndexFor(key, hash, data, eqTest);
    }

    public <K> int findBinIndex(K key, int hash, BiPredicate<E, K> eqTest) {
        return findBinIndex(key, hash, data, eqTest);
    }

    protected int findBinIndexFor(int hash, Predicate<Object> eqTest) {
        return findBinIndexFor(hash, data, eqTest);
    }

    public final boolean add(E element) {
        resize();
        int hash = getHashFunction().applyAsInt(element);
        int addr = findBinIndexFor(element, hash, getEqualsPredicate());
        return saveDirect(element, addr);
    }

    public final void addNoResize(E element) {
        int hash = getHashFunction().applyAsInt(element);
        int addr = findBinIndexFor(element, hash, getEqualsPredicate());
        saveDirect(element, addr);
    }

    public final <Z extends AbstractHashData<E>> void bulkAdd(Z other) {
        resize(size + other.size);

        ToIntFunction<Object> hashFunc = getHashFunction();
        BiPredicate<Object, Object> eqPredicate = getEqualsPredicate();

        int i, idx;
        E o;
        for (i = 0; i < other.currentInsertIndex; i++) {
            idx = other.getAt(i);
            if ((o = other.get(idx)) != null) {
                int hash = hashFunc.applyAsInt(o);
                int addr = findBinIndexFor(o, hash, eqPredicate);
                saveDirect(o, addr);
            }
        }
    }


    @Override
    public final void ensureExtraCapacity(int insertCount) {
        resize(size + insertCount);
    }

    public final boolean saveDirect(E element, int addr) {
        if (data[addr] == null) {
            data[addr] = element;
            addNew(addr);
            size++;
            return true;
        } else {
            if (deletedIndices[addr]) {
                deletedIndices[addr] = false;
                deletes--;
                size++;
                data[addr] = element;
                return true;
            } else {
                return false;
            }
        }
    }

    protected abstract ToIntFunction<Object> getHashFunction();

    protected abstract BiPredicate<Object, Object> getEqualsPredicate();

    final int dataSize() {
        return data.length;
    }

    public final long size() {
        return size;
    }

    public final boolean deleteEntries(Predicate<E> predicate) {
        int initialDeletes = this.deletes;
        forEachDataEntry((e, i) -> {
            if (predicate.test(e)) {
                markDeleted(i);
            }
        });
        if (initialDeletes == this.deletes) {
            return false;
        } else {
            resize();
            return true;
        }
    }

    public void forEachDataEntry(Consumer<E> consumer) {
        int i;
        E obj;
        for (i = 0; i < currentInsertIndex; i++) {
            if ((obj = get(getAt(i))) != null) {
                consumer.accept(obj);
            }
        }
    }

    private void forEachDataEntry(ObjIntConsumer<E> consumer) {
        int i, idx;
        E obj;
        for (i = 0; i < currentInsertIndex; i++) {
            idx = getAt(i);
            if ((obj = get(idx)) != null) {
                consumer.accept(obj, idx);
            }
        }
    }

    protected void markDeleted(int addr) {
        deletedIndices[addr] = true;
        deletes++;
        size--;
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        forEachDataEntry(k -> joiner.add(k.toString()));
        return joiner.toString();
    }

    public final void clear() {
        super.clear();
        CollectionUtils.systemFill(this.data, null);
        CollectionUtils.systemFill(this.deletedIndices, false);
        this.size = 0;
        this.deletes = 0;
    }

    protected boolean containsEntry(E e) {
        int addr = findBinIndexFor(e, getHashFunction().applyAsInt(e), getEqualsPredicate());
        return data[addr] != null && !deletedIndices[addr];
    }

    protected boolean removeEntry(Object e) {
        int addr = findBinIndexFor(e, getHashFunction().applyAsInt(e), getEqualsPredicate());
        return removeEntry(addr);
    }

    protected boolean removeEntry(int addr) {
        if (data[addr] == null) {
            // Nothing to delete
            return false;
        } else {
            if (deletedIndices[addr]) {
                // Nothing to delete
                return false;
            } else {
                removeNonEmpty(addr);
                return true;
            }
        }
    }

    protected void removeNonEmpty(int addr) {
        markDeleted(addr);
        resize();
    }

    @Override
    public ReIterator<E> iterator() {
        return new It();
    }

    public <T> ReIterator<T> iterator(Function<E, T> mapper) {
        return new It1<>(mapper);
    }

    @SuppressWarnings("unchecked")
    public Stream<E> stream() {
        return intStream().filter(i -> !deletedIndices[i]).mapToObj(value -> (E) data[value]);
    }


    protected void resize() {
        assert currentInsertIndex() == this.size + this.deletes : "indices: " + currentInsertIndex() + " size: " + this.size + ", deletes: " + this.deletes;
        resize(this.size);
    }

    public void resize(int targetSize) {
        boolean expand = 2 * (targetSize + deletes) >= data.length;
        boolean shrink = deletes > 0 && targetSize < deletes;
        if (expand || shrink) {
            int newSize = tableSizeFor(Math.max(MINIMUM_CAPACITY, targetSize * 2 + 1));
            if (newSize > MAXIMUM_CAPACITY) throw new OutOfMemoryError();

            Object[] newData = (Object[]) Array.newInstance(data.getClass().getComponentType(), newSize);
            UnsignedIntArray newIndices = new UnsignedIntArray(newSize);

            if (targetSize > 0) {
                ToIntFunction<Object> hashFunction = getHashFunction();
                forEachDataEntry(e -> {
                    int addr = findEmptyBinIndex(hashFunction.applyAsInt(e), newData);
                    newData[addr] = e;
                    newIndices.addNew(addr);
                });
            }

            this.data = newData;
            this.copyFrom(newIndices);
            this.deletes = 0;
            this.deletedIndices = new boolean[newSize];
        }

    }

    void assertStructure() {
        int indices = currentInsertIndex();
        int deletes = this.deletes;
        assert indices == size + deletes : "indices: " + indices + " size: " + size + ", deletes: " + deletes;
    }


    private abstract class AbstractIterator<T> implements ReIterator<T> {
        private int pos;
        private Object next;

        protected AbstractIterator() {
            reset();
        }

        final void findNext() {
            int idx;
            next = null;

            while (next == null && pos < currentInsertIndex) {
                idx = getAt(pos++);
                next = deletedIndices[idx] ? null : data[idx];
            }
        }

        public final long reset() {
            pos = 0;
            findNext();
            return size;
        }

        protected Object nextObject() {
            Object ret = next;
            findNext();
            return ret;
        }

        public final boolean hasNext() {
            return next != null;
        }

    }

    private class It extends AbstractIterator<E> {

        private It() {
            super();
        }

        @Override
        @SuppressWarnings("unchecked")
        public E next() {
            return (E) nextObject();
        }
    }

    private class It1<T> extends AbstractIterator<T> {
        private final Function<E, T> mapper;

        private It1(Function<E, T> mapper) {
            super();
            this.mapper = mapper;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T next() {
            //if (next == null) throw new NoSuchElementException();
            E ret = (E) nextObject();
            return mapper.apply(ret);
        }
    }
}