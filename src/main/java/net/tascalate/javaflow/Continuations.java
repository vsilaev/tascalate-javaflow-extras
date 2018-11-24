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
package net.tascalate.javaflow;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.javaflow.api.Continuation;
import org.apache.commons.javaflow.api.continuable;

import net.tascalate.javaflow.function.SuspendableConsumer;
import net.tascalate.javaflow.function.SuspendableFunction;
import net.tascalate.javaflow.function.SuspendableRunnable;

final public class Continuations {
    
    private Continuations() {}

    /**
     * Creates a suspended continuation, {@link SuspendableRunnable} is not started
     * @param o a continuable code block 
     * @return the continuation, suspended before code starts
     */
    public static Continuation create(SuspendableRunnable o) {
        return create(o, false);
    }
    
    /**
     * Creates a suspended continuation, {@link SuspendableRunnable} is not started
     * @param o a continuable code block 
     * @param optimized
     *      If true then continuation constructed is performance-optimized but 
     *      may be resumed only once. Otherwise "restartable" continuation is created that may 
     *      be resumed multiple times. 
     * @return the continuation, suspended before code starts
     */
    public static Continuation create(SuspendableRunnable o, boolean optimized) {
        return Continuation.startSuspendedWith(toRunnable(o), optimized);
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
        return start(o, false);
    }
    

    /**
     * Starts {@link SuspendableRunnable} code block and returns a continuation, 
     * corresponding to the first {@link Continuation#suspend()} call inside this code block 
     * (incl. nested continuable method calls), if any exists. Returns null if the code
     * is not suspended.
     * 
     * @param o a continuable code block
     * @param optimized
     *      If true then continuation constructed is performance-optimized but 
     *      may be resumed only once. Otherwise "restartable" continuation is created that may 
     *      be resumed multiple times.   
     * @return the first continuation suspended
     */
    public static Continuation start(SuspendableRunnable o, boolean optimized) {
        return start(o, null, optimized);
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
        return start(o, ctx, false);
    }
    
    /**
     * Starts {@link SuspendableRunnable} code block and returns a continuation, 
     * corresponding to the first {@link Continuation#suspend()} call inside this code block 
     * (incl. nested continuable method calls), if any exists. Returns null if the code
     * is not suspended.
     * 
     * @param o a continuable code block
     * @param ctx an initial argument for the continuable code
     * @param optimized
     *      If true then continuation constructed is performance-optimized but 
     *      may be resumed only once. Otherwise "restartable" continuation is created that may 
     *      be resumed multiple times. 
     * @return the first continuation suspended
     */
    public static Continuation start(SuspendableRunnable o, Object ctx, boolean optimized) {
        return Continuation.startWith(toRunnable(o), ctx, optimized);
    }


    /**
     * <p>Convert a <code>coroutine</code> to the {@link Iterator} of emitted values
     * 
     * <p>The current value of the coroutine is not used 
     * 
     * @param <T> a type of values
     * @param coroutine the source of emitted values
     * @return the iterator over emitted values
     */
    public static <T> CloseableIterator<T> iteratorOf(Continuation coroutine) {
        return iteratorOf(coroutine, false);
    }
    
    /**
     * <p>Convert a <code>coroutine</code> to the {@link Iterator} of emitted values
     * 
     * @param <T> a type of values
     * @param coroutine the source of emitted values
     * @param useCurrentValue should the current coroutine result be used as a first value to return 
     * @return the iterator over emitted values
     */
    public static <T> CloseableIterator<T> iteratorOf(Continuation coroutine, boolean useCurrentValue) {
        return new ContinuationIterator<>(coroutine.optimized(), useCurrentValue);
    }
    

    /**
     * <p>Convert a <code>coroutine</code> to the {@link Iterator} of emitted values
     * 
     * @param <T> a type of values
     * @param coroutine the source of emitted values
     * @return the iterator over emitted values
     */    
    public static <T> CloseableIterator<T> iteratorOf(SuspendableRunnable coroutine) {
        return iteratorOf(create(coroutine, true), false);
    }
    
    /**
     * <p>Convert a <code>coroutine</code> to the {@link Stream} of emitted values
     * 
     * <p>The current value of the coroutine is not used 
     * 
     * @param <T> a type of values
     * @param coroutine the source of emitted values
     * @return the iterator over emitted values
     */    
    public static <T> Stream<T> streamOf(Continuation coroutine) {
        return streamOf(coroutine, false);
    }
    
    /**
     * <p>Convert a <code>coroutine</code> to the {@link Stream} of emitted values
     * 
     * @param <T> a type of values
     * @param coroutine the source of emitted values
     * @param useCurrentValue should the current coroutine result be used as a first value to return 
     * @return the iterator over emitted values
     */    
    public static <T> Stream<T> streamOf(Continuation coroutine, boolean useCurrentValue) {
        CloseableIterator<T> iterator = iteratorOf(coroutine, useCurrentValue);
        return StreamSupport
               .stream(Spliterators.spliteratorUnknownSize(iterator, 0), false)
               .onClose(iterator::close);
    }

    /**
     * <p>Convert a <code>coroutine</code> to the {@link Stream} of emitted values
     * 
     * @param <T> a type of values
     * @param coroutine the source of emitted values
     * @return the iterator over emitted values
     */   
    public static <T> Stream<T> streamOf(SuspendableRunnable coroutine) {
        return streamOf(create(coroutine, true), false);
    }

    /**
     * Executes the suspended <code>coroutine</code> from the point specified till the 
     * completion and performs a non-suspendable <code>action</code> 
     * on each value emitted.
     * 
     * @param <T> a type of values  
     * @param coroutine a coroutine that yields multiple results
     * @param action a non-continuable action to perform on the values emitted
     */
    public static <T> void forEach(Continuation coroutine, Consumer<? super T> action) {
        forEach(coroutine, false, action);
    }
    
    /**
     * Executes the suspended <code>coroutine</code> from the point specified till the 
     * completion and performs a non-suspendable <code>action</code> 
     * on each value emitted.
     * 
     * @param <T> a type of values  
     * @param coroutine a coroutine that yields multiple results
     * @param useCurrentValue should the current coroutine result be used as a first value to process
     * @param action a non-continuable action to perform on the values emitted
     */
    public static <T> void forEach(Continuation coroutine, boolean useCurrentValue, Consumer<? super T> action) {
        try (CloseableIterator<T> iter = iteratorOf(coroutine, useCurrentValue)) {
            while (iter.hasNext()) {
                action.accept(iter.next());
            }
        }
    }
    
    /**
     * Fully executes the <code>coroutine</code> and performs a non-suspendable 
     * <code>action</code> on each value emitted.
     * 
     * @param <T> a type of values 
     * @param coroutine a continuable code block that yields multiple results
     * @param action a non-continuable action to perform on the values emitted
     */
    public static <T> void forEach(SuspendableRunnable coroutine, Consumer<? super T> action) {
        forEach(create(coroutine, true), action);
    }

    
    /**
     * <p>Executes the suspended <code>coroutine</code> from the point specified till the  
     * completion and performs a non-suspendable <code>action</code> 
     * on each value emitted. The reply returned from the <code>action</code> invocation is 
     * used further as a parameter to resume the suspended <code>coroutine</code> (see {@link Continuation#resume(Object)}).
     * 
     * <p>In other words, the latest value emitted from the <code>coroutine</code> is used as an input to 
     * the <code>action</code>, then the output of the <code>action</code> is used as an input to the resumed 
     * <code>coroutine</code> and so on in "ring pipe" fashion till the coroutine code is over. 
     * 
     * <p>For the first time the coroutine is resumed with <code>null</code> as argument.  
     * 
     * @param <T> a type of values  
     * @param coroutine a coroutine that yields multiple results 
     * @param action a non-continuable function that is applied to values emitted and provides argument to
     * resume coroutine.
     */      
    public static <T> void forEachReply(Continuation coroutine, Function<? super T, ?> action) {
        forEachReply(coroutine, false, action);
    }
    
    /**
     * <p>Executes the suspended <code>coroutine</code> from the point specified till the 
     * completion and performs a non-suspendable <code>action</code> 
     * on each value emitted. The reply returned from the <code>action</code> invocation is 
     * used further as a parameter to resume the suspended <code>coroutine</code> (see {@link Continuation#resume(Object)}).
     * 
     * <p>In other words, the latest value emitted from the <code>coroutine</code> is used as an input to 
     * the <code>action</code>, then output of the <code>action</code> is used as an input to the resumed 
     * <code>coroutine</code> and so on in "ring pipe" fashion till the coroutine code is over. 
     * 
     * <p>If <code>useCurrentValue</code> is false then the coroutine is resumed with <code>null</code> for the first time.  
     * 
     * @param <T> a type of values  
     * @param coroutine a coroutine that yields multiple results 
     * @param useCurrentValue should the current coroutine result be used as a first value to process
     * @param action a non-continuable function that is applied to values emitted and provides argument to
     * resume coroutine.
     */    
    public static <T> void forEachReply(Continuation coroutine, boolean useCurrentValue, Function<? super T, ?> action) {
        Continuation cc = coroutine.optimized();
        try {
            Object param = null;
            if (null != cc && useCurrentValue) {
                param = action.apply(valueOf(cc));
            } else {
                param = null;
            }
            while (null != cc) {
                cc = cc.resume(param);
                param = action.apply(valueOf(cc));
            }
        } finally {
            if (null != cc) {
                cc.terminate();
            }
        }
    }
    
    /**
     * <p>Fully executes the coroutine and performs a non-suspendable <code>action</code> 
     * on each value emitted. The reply returned from the <code>action</code> invocation is 
     * used further as a parameter to resume the suspended <code>coroutine</code> (see {@link Continuation#resume(Object)}).
     * 
     * <p>In other words, the latest value emitted from the <code>coroutine</code> is used as an input to 
     * the <code>action</code>, then output of the <code>action</code> is used as an input to the resumed 
     * <code>coroutine</code> and so on in "ring pipe" fashion till the coroutine is finished. 
     * 
     * <p>For the first time the coroutine is resumed with <code>null</code> as argument.  
     * 
     * @param <T> a type of values  
     * @param coroutine a coroutine that yields multiple results 
     * @param action a non-continuable function that is applied to values emitted and provides argument to
     * resume continuation.
     */       
    public static <T> void forEachReply(SuspendableRunnable coroutine, Function<? super T, ?> action) {
        forEachReply(create(coroutine, true), action);
    }
    
    /**
     * Executes the suspended <code>coroutine</code> from the point specified till the 
     * completion and performs a potentially <b>suspendable</b> <code>action</code> 
     * on each value emitted.
     * 
     * @param <T> a type of values  
     * @param coroutine a coroutine that yields multiple results
     * @param action a continuable action to perform on the values emitted
     */
    public static @continuable <T> void forEach$(Continuation coroutine, SuspendableConsumer<? super T> action) {
        forEach$(coroutine, false, action);
    }
    
    /**
     * Executes the suspended <code>coroutine</code> from the point specified till the 
     * completion and performs a potentially <b>suspendable</b> <code>action</code> 
     * on each value emitted.
     * 
     * @param <T> a type of values  
     * @param coroutine a coroutine  that yields multiple results 
     * @param useCurrentValue should the current coroutine result be used as a first value to process
     * @param action a continuable action to perform on the values emitted
     */
    public static @continuable <T> void forEach$(Continuation coroutine, boolean useCurrentValue, SuspendableConsumer<? super T> action) {
        try (CloseableIterator<T> iter = iteratorOf(coroutine, useCurrentValue)) {
            forEach$(iter, action);
        }
    }
    
    /**
     * Fully executes the coroutine and performs a potentially <b>suspendable</b> 
     * <code>action</code> on each value emitted.
     * 
     * @param <T> a type of values 
     * @param coroutine a coroutine that yields multiple results
     * @param action a continuable action to perform on the values emitted
     */
    public static @continuable <T> void forEach$(SuspendableRunnable coroutine, SuspendableConsumer<? super T> action) {
        forEach$(create(coroutine, true), false, action);
    }
    
    /**
     * <p>Executes the suspended <code>coroutine</code> from the point specified till the 
     * completion and performs a potentially <b>suspendable</b> <code>action</code> 
     * on each value emitted. The reply returned from the <code>action</code> invocation is 
     * used further as a parameter to resume the suspended <code>coroutine</code> (see {@link Continuation#resume(Object)}).
     * 
     * <p>In other words, the latest value emitted from the <code>coroutine</code> is used as an input to 
     * the <code>action</code>, then the output of the <code>action</code> is used as an input to the resumed 
     * <code>coroutine</code> and so on in "ring pipe" fashion till the coroutine is finished. 
     * 
     * <p>For the first time the coroutine is resumed with <code>null</code> as argument.  
     * 
     * @param <T> a type of values  
     * @param coroutine a coroutine that yields multiple results 
     * @param action a non-continuable function that is applied to values emitted and provides argument to
     * resume coroutine.
     */      
    public static @continuable <T> void forEachReply$(Continuation coroutine, SuspendableFunction<? super T, ?> action) {
        forEachReply$(coroutine, false, action);
    }
    
    /**
     * <p>Executes the suspended <code>coroutine</code> from the point specified till the 
     * completion and performs a potentially <b>suspendable</b> <code>action</code> 
     * on each value emitted. The reply returned from the <code>action</code> invocation is 
     * used further as a parameter to resume the suspended <code>coroutine</code> (see {@link Continuation#resume(Object)}).
     * 
     * <p>In other words, the latest value emitted from the <code>coroutine</code> is used as an input to 
     * the <code>action</code>, then output of the <code>action</code> is used as an input to the resumed 
     * <code>coroutine</code> and so on in "ring pipe" fashion till the coroutine is finished. 
     * 
     * <p>If <code>useCurrentValue</code> is false then the coroutine is resumed with <code>null</code> for the first time.  
     * 
     * @param <T> a type of values  
     * @param coroutine a coroutine that yields multiple results 
     * @param useCurrentValue should the current coroutine result be used as a first value to process
     * @param action a non-continuable function that is applied to values emitted and provides argument to
     * resume coroutine.
     */    
    public static @continuable <T> void forEachReply$(Continuation coroutine, boolean useCurrentValue, SuspendableFunction<? super T, ?> action) {
        Continuation cc = coroutine.optimized();
        try {
            Object param = null;
            if (null != cc && useCurrentValue) {
                param = action.apply(valueOf(cc));
            } else {
                param = null;
            }
            while (null != cc) {
                cc = cc.resume(param);
                param = action.apply(valueOf(cc));
            }
        } finally {
            if (null != cc) {
                cc.terminate();
            }
        }
    }
    
    /**
     * <p>Fully executes the coroutine and performs a potentially <b>suspendable</b> <code>action</code> 
     * on each value emitted. The reply returned from the <code>action</code> invocation is 
     * used further as a parameter to resume the suspended <code>coroutine</code> (see {@link Continuation#resume(Object)}).
     * 
     * <p>In other words, the latest value emitted from the <code>coroutine</code> is used as an input to 
     * the <code>action</code>, then output of the <code>action</code> is used as an input to the resumed 
     * <code>coroutine</code> and so on in "ring pipe" fashion till the coroutine is finished. 
     * 
     * <p>For the first time the coroutine is resumed with <code>null</code> as argument.  
     * 
     * @param <T> a type of values  
     * @param coroutine a corotine that yields multiple results 
     * @param action a non-continuable function that is applied to values emitted and provides argument to
     * resume coroutine.
     */       
    public static @continuable <T> void forEachReply$(SuspendableRunnable coroutine, SuspendableFunction<? super T, ?> action) {
        forEachReply$(create(coroutine, true), action);
    }    

    /**
     * Performs the <b>continuable</b> <code>action</code> for each element of the {@link Stream} supplied.
     *
     * <p>This is a terminal operation that should be used instead of 
     * {@link Stream#forEach(java.util.function.Consumer)} with continuable code.
     * 
     * @param <T> a type of elements 
     * @param stream the stream to perform an action on
     * @param action a continuable action to perform on the elements
     */    
    public static @continuable <T> void forEach$(Stream<T> stream, SuspendableConsumer<? super T> action) {
        forEach$(stream.iterator(), action);
    }

    /**
     * Performs the <b>continuable</b> <code>action</code> for each element of the {@link Iterable} supplied.
     *
     * <p>This is a convenient functional replacement for the Java 7 For-Each Loop
     * over {@link Iterable}.
     * 
     * @param <T> a type of elements 
     * @param iterable the iterable to perform an action on
     * @param action a continuable action to perform on the elements
     */   
    public static @continuable <T> void forEach$(Iterable<T> iterable, SuspendableConsumer<? super T> action) {
        Iterator<T> iter = iterable.iterator();
        try (CloseableIterator<T> closeable = asCloseable(iter)) {
            forEach$(iter, action);
        }
    }

    /**
     * Performs the <b>continuable</b> <code>action</code> for each element of the {@link Iterator} supplied.
     *
     * <p>This is a convenient functional replacement for the classic Java While Loop 
     * over {@link Iterator}.
     * 
     * @param <T> a type of elements
     * @param iterator the iterator to perform an action on
     * @param action a continuable action to perform on the elements
     */ 
    private static @continuable <T> void forEach$(Iterator<T> iterator, SuspendableConsumer<? super T> action) {
        while (iterator.hasNext()) {
            action.accept(iterator.next());
        }
    }
    
    /**
     * <p>A keyword-like syntactic sugar over {@link Continuation#suspend(Object)}.
     * <p>It allows to write generators in the following fashion:
     * <pre>
     * Stream&lt;String&gt; strings = Continuations.streamOf(() -&gt; {
     *     yield("A");
     *     yield("B");
     *     yield("C");
     * });
     * </pre>
     *
     * @param <T> a type of elements 
     * @param value a value to yield
     * @return an argument passed to resume coroutine
     */
    public static @continuable <T> Object yield(T value) {
        return Continuation.suspend(value);
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

    @SuppressWarnings("unchecked")
    private static <T> T valueOf(Continuation continuation) {
        return (T)continuation.value();
    }
}
