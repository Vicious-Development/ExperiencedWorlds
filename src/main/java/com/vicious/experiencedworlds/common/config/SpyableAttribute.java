package com.vicious.experiencedworlds.common.config;

import com.vicious.viciouslib.persistence.storage.PersistentAttribute;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SpyableAttribute<T> extends PersistentAttribute<T> {
    public SpyableAttribute(String name, Class<T> expectedType) {
        super(name, expectedType);
    }

    public SpyableAttribute(String name, Class<T> expectedType, T value) {
        super(name, expectedType, value);
    }

    protected List<Consumer<SpyableAttribute<T>>> onUpdate = new ArrayList<>();
    public void listen(Consumer<SpyableAttribute<T>> run){
        onUpdate.add(run);
    }
    public void stopListening(Consumer<SpyableAttribute<T>> run){
        onUpdate.remove(run);
    }

    @Override
    public T set(T t) {
        T x = super.set(t);
        updateListeners();
        return x;
    }

    protected void updateListeners() {
        for (Consumer<SpyableAttribute<T>> cons : onUpdate) {
            cons.accept(this);
        }
    }
}
