/**
 * ﻿Copyright 2013-2022 Valery Silaev (http://vsilaev.com)
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:

 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.

 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.tascalate.javaflow;

import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.commons.javaflow.api.continuable;

import net.tascalate.javaflow.function.SuspendableBiFunction;
import net.tascalate.javaflow.function.SuspendableConsumer;
import net.tascalate.javaflow.function.SuspendableFunction;
import net.tascalate.javaflow.function.SuspendablePredicate;
import net.tascalate.javaflow.function.SuspendableSupplier;

public abstract class Option<T> {
    // Package visible to restrict inheritance
    Option() {}
    
    abstract public boolean exists();
    abstract public T get();
    
    public final Option<T> orElseNull() {
        return orElse(useNull());
    }
    
    public final Option<T> orElse(Option<T> alt) {
        return orElse(() -> alt);
    }
    
    abstract public Option<T> orElse(Supplier<? extends Option<T>> alt);
    abstract public @continuable Option<T> orElse$(SuspendableSupplier<? extends Option<? extends T>> alt);
    
    abstract public <R> Option<R> map(Function<? super T, ? extends R> mapper);
    abstract public @continuable <R> Option<R> map$(SuspendableFunction<? super T, ? extends R> mapper);
    
    abstract public <R> Option<R> flatMap(Function<? super T, ? extends Option<? extends R>> mapper);
    abstract public @continuable <R> Option<R> flatMap$(SuspendableFunction<? super T, ? extends Option<? extends R>> mapper);
    
    abstract public Option<T> filter(Predicate<? super T> predicate);
    abstract public @continuable Option<T> filter$(SuspendablePredicate<? super T> predicate);
    
    abstract public <U, R> Option<R> combine(Option<U> other, BiFunction<? super T, ? super U, ? extends R> zipper);
    abstract public @continuable <U, R> Option<R> combine$(Option<U> other, SuspendableBiFunction<? super T, ? super U, ? extends R> zipper);
    
    abstract public void accept(Consumer<? super T> action);
    abstract public @continuable void accept$(SuspendableConsumer<? super T> action);
    
    public static class Some<T> extends Option<T> {
        private final T value;
        Some(T value) {
            this.value = value;
        }
        
        @Override
        public boolean exists() {
            return true;
        }
        
        @Override
        public T get() {
            return value;
        }
        
        @Override
        public Option<T> orElse(Supplier<? extends Option<T>> alt) {
            return this;
        }
        
        @Override
        public Option<T> orElse$(SuspendableSupplier<? extends Option<? extends T>> alt) {
            return this;
        }
        
        @Override
        public <R> Option<R> map(Function<? super T, ? extends R> mapper) {
            return some(mapper.apply(value));
        }
        
        @Override
        public <R> Option<R> map$(SuspendableFunction<? super T, ? extends R> mapper) {
            return some(mapper.apply(value));
        }
        
        @Override
        public Option<T> filter(Predicate<? super T> predicate) {
            return predicate.test(value) ? this : none();
        }
        
        @Override
        public Option<T> filter$(SuspendablePredicate<? super T> predicate) {
            return predicate.test(value) ? this : none();
        }
        
        @Override
        public <R> Option<R> flatMap(Function<? super T, ? extends Option<? extends R>> mapper) {
            return narrow(mapper.apply(value));
        }
        
        @Override
        public <R> Option<R> flatMap$(SuspendableFunction<? super T, ? extends Option<? extends R>> mapper) {
            return narrow(mapper.apply(value));
        }
        
        @Override
        public <U, R> Option<R> combine(Option<U> other, BiFunction<? super T, ? super U, ? extends R> zipper) {
            if (other.exists()) {
                Some<U> someOther = (Some<U>)other;
                return some(zipper.apply(value, someOther.value));
            } else {
                return none();
            }
        }
        
        @Override
        public <U, R> Option<R> combine$(Option<U> other, SuspendableBiFunction<? super T, ? super U, ? extends R> zipper) {
            if (other.exists()) {
                Some<U> someOther = (Some<U>)other;
                return some(zipper.apply(value, someOther.value));
            } else {
                return none();
            }            
        }
        
        @Override
        public void accept(Consumer<? super T> action) {
            action.accept(value);
        }
        
        @Override
        public void accept$(SuspendableConsumer<? super T> action) {
            action.accept(value);
        }
    }
    
    public static class None<T> extends Option<T> {
        private None() {}
        
        @Override
        public boolean exists() {
            return false;
        }
        
        @Override
        public T get() {
            throw new NoSuchElementException();
        }
        
        @Override
        public Option<T> orElse(Supplier<? extends Option<T>> alt) {
            return alt.get();
        }
        
        @Override
        public Option<T> orElse$(SuspendableSupplier<? extends Option<? extends T>> alt) {
            return narrow(alt.get());
        }

        
        @Override
        public <R> Option<R> map(Function<? super T, ? extends R> mapper) {
            return none();
        }
        
        @Override
        public <R> Option<R> map$(SuspendableFunction<? super T, ? extends R> mapper) {
            return none();
        }
        
        @Override
        public Option<T> filter(Predicate<? super T> predicate) {
            return this;
        }
        
        @Override
        public Option<T> filter$(SuspendablePredicate<? super T> predicate) {
            return this;
        }

        @Override
        public <R> Option<R> flatMap(Function<? super T, ? extends Option<? extends R>> mapper) {
            return none();
        }
        
        @Override
        public <R> Option<R> flatMap$(SuspendableFunction<? super T, ? extends Option<? extends R>> mapper) {
            return none();
        }
        
        @Override
        public <U, R> Option<R> combine(Option<U> other, BiFunction<? super T, ? super U, ? extends R> zipper) {
            return none();
        }
        
        @Override
        public <U, R> Option<R> combine$(Option<U> other, SuspendableBiFunction<? super T, ? super U, ? extends R> zipper) {
            return none();
        }
        
        @Override
        public void accept(Consumer<? super T> action) {
            
        }
        
        @Override
        public void accept$(SuspendableConsumer<? super T> action) {
            
        }
        
        static final Option<?> INSTANCE = new None<>();
    }
    
    public static <T> Option<T> some(T value) {
        return new Some<>(value);
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Option<T> none() {
        return (Option<T>)None.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    private static <T> Supplier<Option<T>> useNull() {
        return (Supplier<Option<T>>)USE_NULL;
    }
    
    @SuppressWarnings("unchecked")
    static <T> Option<T> narrow(Option<? extends T> v) {
        return (Option<T>)v;
    }
    
    private static final Option<?> NULL = new Some<>(null);
    private static final Supplier<?> USE_NULL = () -> NULL;
}
