/**
 * ﻿Copyright 2013-2018 Valery Silaev (http://vsilaev.com)
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
package net.tascalate.javaflow.util;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.javaflow.api.Continuation;
import org.apache.commons.javaflow.api.continuable;

import net.tascalate.javaflow.util.function.SuspendableConsumer;
import net.tascalate.javaflow.util.function.SuspendableRunnable;

final public class Continuations {
    
    private Continuations() {}

    /**
     * Creates a suspended continuation, {@link SuspendableRunnable} is not started
     * @param o a continuable code block 
     * @return the continuation, suspended before code starts
     */
    public static Continuation create(SuspendableRunnable o) {
        return Continuation.startSuspendedWith(toRunnable(o));
    }

    /**
     * Starts {@link SuspendableRunnable} code block and returns a continuation, 
     * corresponding to the first {@link Continuation#suspend()} call inside this code block 
     * (incl. nested continuable method calls), if any exists. Returns null if the code
     * is not suspended.
     * 
     * @param o a continuable code block
     * @param ctx an initial argument for the continuable code
     * @return the first continuation suspended
     */
    public static Continuation start(SuspendableRunnable o, Object ctx) {
        return Continuation.startWith(toRunnable(o), ctx);
    }

    /**
     * Starts {@link SuspendableRunnable} code block and returns a continuation, 
     * corresponding to the first {@link Continuation#suspend()} call inside this code block 
     * (incl. nested continuable method calls), if any exists. Returns null if the code
     * is not suspended.
     * 
     * @param o a continuable code block
     * @return the first continuation suspended
     */
    public static Continuation start(SuspendableRunnable o) {
        return Continuation.startWith(toRunnable(o));
    }

    public static <T> CloseableIterator<T> iterate(Continuation continuation) {
        return iterate(continuation, false);
    }
    
    public static <T> CloseableIterator<T> iterate(Continuation continuation, boolean useCurrentValue) {
        return new ContinuationIterator<>(continuation, useCurrentValue);
    }
    
    
    public static <T> CloseableIterator<T> iterate(SuspendableRunnable generator) {
        return iterate(create(generator), false);
    }
    
    public static <T> Stream<T> stream(Continuation continuation) {
        return stream(continuation, false);
    }
    
    public static <T> Stream<T> stream(Continuation continuation, boolean useCurrentValue) {
        CloseableIterator<T> iterator = iterate(continuation, useCurrentValue);
        return StreamSupport
               .stream(Spliterators.spliteratorUnknownSize(iterator, 0), false)
               .onClose(iterator::close);
    }

    public static <T> Stream<T> stream(SuspendableRunnable generator) {
        return stream(create(generator), false);
    }

    /**
     * Executes the suspended continuation from the point specified till the end 
     * of the corresponding code block and performs a non-suspendable action 
     * on each value yielded.
     * 
     * @param <T> a type of values  
     * @param continuation a continuation to resume a code block that yields multiple results
     * @param action a non-continuable action to perform on the values yielded
     */
    public static <T> void execute(Continuation continuation, Consumer<? super T> action) {
        execute(continuation, false, action);
    }
    
    /**
     * Executes the suspended continuation from the point specified till the end 
     * of the corresponding code block and performs a non-suspendable action 
     * on each value yielded.
     * 
     * @param <T> a type of values  
     * @param continuation a continuation to resume a code block that yields multiple results
     * @param useCurrentValue should the value of the supplied continuation be used as a first value to process
     * @param action a non-continuable action to perform on the values yielded
     */
    public static <T> void execute(Continuation continuation, boolean useCurrentValue, Consumer<? super T> action) {
        try (CloseableIterator<T> iter = iterate(continuation, useCurrentValue)) {
            while (iter.hasNext()) {
                action.accept(iter.next());
            }
        }
    }
    
    /**
     * Fully executes the continuable code block and performs a non-suspendable 
     * action on each value yielded.
     * 
     * @param <T> a type of values 
     * @param generator a continuable code block that yields multiple results
     * @param action a non-continuable action to perform on the values yielded
     */
    public static <T> void execute(SuspendableRunnable generator, Consumer<? super T> action) {
        execute(create(generator), action);
    }

    
    /**
     * Executes the suspended continuation from the point specified till the end 
     * of the corresponding code block and performs a potentially suspendable action 
     * on each value yielded.
     * 
     * @param <T> a type of values  
     * @param continuation a continuation to resume a code block that yields multiple results
     * @param action a continuable action to perform on the values yielded
     */
    public @continuable static <T> void execute$(Continuation continuation, SuspendableConsumer<? super T> action) {
        execute$(continuation, false, action);
    }
    
    /**
     * Fully executes the continuable code block and performs a potentially suspendable 
     * action on each value yielded.
     * 
     * @param <T> a type of values 
     * @param generator a continuable code block that yields multiple results
     * @param action a continuable action to perform on the values yielded
     */
    public @continuable static <T> void execute$(SuspendableRunnable generator, SuspendableConsumer<? super T> action) {
        execute$(create(generator), false, action);
    }
    
    /**
     * Executes the suspended continuation from the point specified till the end 
     * of the corresponding code block and performs a potentially suspendable action 
     * on each value yielded.
     * 
     * @param <T> a type of values  
     * @param continuation a continuation to resume a code block that yields multiple results 
     * @param useCurrentValue should the value of the supplied continuation be used as a first value to process
     * @param action a continuable action to perform on the values yielded
     */
    public @continuable static <T> void execute$(Continuation continuation, boolean useCurrentValue, SuspendableConsumer<? super T> action) {
        try (CloseableIterator<T> iter = iterate(continuation, useCurrentValue)) {
            forEach$(iter, action);
        }
    }

    /**
     * Performs an continuable action for each element of the {@link Stream} supplied.
     *
     * <p>This is a terminal operation that should be used instead of 
     * {@link Stream#forEach(java.util.function.Consumer)} with continuable code.
     * 
     * @param <T> a type of elements 
     * @param stream the stream to perform an action on
     * @param action a continuable action to perform on the elements
     */    
    public @continuable static <T> void forEach$(Stream<T> stream, SuspendableConsumer<? super T> action) {
        forEach$(stream.iterator(), action);
    }

    /**
     * Performs an continuable action for each element of the {@link Iterable} supplied.
     *
     * <p>This is a convenient functional replacement for the Java 7 For-Each Loop
     * over {@link Iterable}.
     * 
     * @param <T> a type of elements 
     * @param iterable the iterable to perform an action on
     * @param action a continuable action to perform on the elements
     */   
    public @continuable static <T> void forEach$(Iterable<T> iterable, SuspendableConsumer<? super T> action) {
        Iterator<T> iter = iterable.iterator();
        try (CloseableIterator<T> closeable = asCloseable(iter)) {
            forEach$(iter, action);
        }
    }

    /**
     * Performs an continuable action for each element of the {@link Iterator} supplied.
     *
     * <p>This is a convenient functional replacement for the classic Java While Loop 
     * over {@link Iterator}.
     * 
     * @param <T> a type of elements
     * @param iterator the iterator to perform an action on
     * @param action a continuable action to perform on the elements
     */ 
    public @continuable static <T> void forEach$(Iterator<T> iterator, SuspendableConsumer<? super T> action) {
        while (iterator.hasNext()) {
            action.accept(iterator.next());
        }
    }
    
    static Runnable toRunnable(SuspendableRunnable code) {
        @SuppressWarnings("serial")
        abstract class ContinuableRunnableAdapter implements Runnable, Serializable {
        }

        return new ContinuableRunnableAdapter() {
            private static final long serialVersionUID = 0L;

            @Override
            public @continuable void run() {
                code.run();
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static <E> CloseableIterator<E> asCloseable(Object o) {
        return o instanceof CloseableIterator ? (CloseableIterator<E>)o : null;
    }

}
