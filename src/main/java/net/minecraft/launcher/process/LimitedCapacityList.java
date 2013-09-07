package net.minecraft.launcher.process;

import java.lang.reflect.Array;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LimitedCapacityList<T> {
    private final T[] items;
    private final Class<? extends T> clazz;
    private final ReadWriteLock locks = new ReentrantReadWriteLock();
    private int size;
    private int head;

    public LimitedCapacityList(final Class<? extends T> clazz, final int maxSize) {
        this.clazz = clazz;
        items = (T[]) Array.newInstance(clazz, maxSize);
    }

    public T add(final T value) {
        locks.writeLock().lock();

        items[head] = value;
        head = (head + 1) % getMaxSize();
        if(size < getMaxSize())
            size += 1;

        locks.writeLock().unlock();
        return value;
    }

    public T[] getItems() {
        final T[] result = (T[]) Array.newInstance(clazz, size);

        locks.readLock().lock();
        for(int i = 0; i < size; i++) {
            int pos = (head - size + i) % getMaxSize();
            if(pos < 0)
                pos += getMaxSize();
            result[i] = items[pos];
        }
        locks.readLock().unlock();

        return result;
    }

    public int getMaxSize() {
        locks.readLock().lock();
        final int result = items.length;
        locks.readLock().unlock();
        return result;
    }

    public int getSize() {
        locks.readLock().lock();
        final int result = size;
        locks.readLock().unlock();
        return result;
    }
}