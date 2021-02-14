package org.evrete.spi.minimal;

import org.evrete.api.FactHandle;
import org.evrete.api.FactStorage;
import org.evrete.api.Type;
import org.evrete.collections.AbstractLinearHash;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.ToIntFunction;

class DefaultFactStorage<T> implements FactStorage<T> {
    private final TupleCollection<T> collection;
    private final Function<Tuple<T>, T> ITERATOR_MAPPER = t -> t.object;

    DefaultFactStorage(Type<?> type, BiPredicate<T, T> identityFunction) {
        this.collection = new TupleCollection<>(type, identityFunction);
    }

    @Override
    public FactHandle insert(T fact) {
        return collection.insert(fact);
    }

    @Override
    public void delete(FactHandle handle) {
        this.collection.delete(handle);
    }

    @Override
    public void update(FactHandle handle, T newInstance) {
        this.collection.add(new Tuple<>((FactHandleImpl) handle, newInstance));
    }

    @Override
    public T getFact(FactHandle handle) {
        return this.collection.getFact(handle);
    }

    @Override
    public void clear() {
        this.collection.clear();
    }

    @Override
    public Iterator<T> iterator() {
        return collection.iterator(ITERATOR_MAPPER);
    }

    static class TupleCollection<T> extends AbstractLinearHash<DefaultFactStorage.Tuple<T>> {
        private static final ToIntFunction<Object> HASH_FUNCTION = Object::hashCode;
        private final BiPredicate<T, T> identityFunction;
        private final AtomicLong handleId = new AtomicLong(0);

        private final BiPredicate<Object, Object> EQ_PREDICATE = new BiPredicate<Object, Object>() {
            @Override
            @SuppressWarnings("unchecked")
            public boolean test(Object o1, Object o2) {
                Tuple<T> t1 = (Tuple<T>) o1;
                Tuple<T> t2 = (Tuple<T>) o2;
                return identityFunction.test(t1.object, t2.object);
            }
        };

        private final BiPredicate<Tuple<T>, FactHandleImpl> searchByHandle = (tuple, o) -> tuple.handle.id == o.id;
        private final BiPredicate<Tuple<T>, T> searchByFact = new BiPredicate<Tuple<T>, T>() {
            @Override
            public boolean test(Tuple<T> tuple, T o) {
                return identityFunction.test(tuple.object, o);
            }
        };

        private final Type<?> type;

        TupleCollection(Type<?> type, BiPredicate<T, T> identityFunction) {
            this.identityFunction = identityFunction;
            this.type = type;
        }

        @Override
        protected ToIntFunction<Object> getHashFunction() {
            return HASH_FUNCTION;
        }

        @Override
        protected BiPredicate<Object, Object> getEqualsPredicate() {
            return EQ_PREDICATE;
        }

        public FactHandleImpl insert(T fact) {
            resize();
            int hash = HASH_FUNCTION.applyAsInt(fact);
            int addr = findBinIndex(fact, hash, searchByFact);
            Tuple<T> tuple = get(addr);
            if (tuple == null) {
                // Object id unknown, creating new handle...
                FactHandleImpl handle = new FactHandleImpl(handleId.getAndIncrement(), hash, this.type.getId());
                tuple = new Tuple<>(handle, fact);
                saveDirect(tuple, addr);
                return handle;
            } else {
                return null;
            }
        }

        public void delete(FactHandle handle) {
            FactHandleImpl impl = (FactHandleImpl) handle;
            int addr = findBinIndex(impl, impl.hash, searchByHandle);
            if (get(addr) != null) {
                markDeleted(addr);
            }
        }

        public T getFact(FactHandle handle) {
            FactHandleImpl impl = (FactHandleImpl) handle;
            int addr = findBinIndex(impl, impl.hash, searchByHandle);
            Tuple<T> t = get(addr);
            return t == null ? null : t.object;
        }
    }

    static class Tuple<Z> {
        private final FactHandleImpl handle;
        private final Z object;

        Tuple(FactHandleImpl handle, Z object) {
            this.handle = handle;
            this.object = object;
        }

        public FactHandle getHandle() {
            return handle;
        }

        public Object getFact() {
            return object;
        }

        @Override
        public int hashCode() {
            return handle.hash;
        }

        @Override
        public String toString() {
            return "Tuple{" +
                    "handle=" + handle +
                    ", object=" + object +
                    ", hash=" + hashCode() +
                    '}';
        }

    }

}