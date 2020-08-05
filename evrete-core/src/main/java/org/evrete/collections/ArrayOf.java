package org.evrete.collections;

import org.evrete.util.CollectionUtils;

import java.util.Arrays;

/**
 * A simple array wrapper which allows for faster iteration and
 * array resize while keeping the same wrapper instance
 *
 * @param <T> type parameter
 */
public class ArrayOf<T> {
    public T[] data;

    public ArrayOf(T[] data) {
        this.data = data;
    }

    public ArrayOf(ArrayOf<T> other) {
        this.data = Arrays.copyOf(other.data, other.data.length);
    }

    public ArrayOf(Class<T> type) {
        this(CollectionUtils.array(type, 0));
    }

    public int append(T element) {
        int ret = this.data.length;
        this.data = Arrays.copyOf(this.data, ret + 1);
        this.data[ret] = element;
        return ret;
    }

    @Override
    public String toString() {
        return Arrays.toString(data);
    }
}