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

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.apache.commons.javaflow.api.continuable;

import net.tascalate.javaflow.util.function.SuspendableBiFunction;
import net.tascalate.javaflow.util.function.SuspendableBinaryOperator;
import net.tascalate.javaflow.util.function.SuspendableConsumer;
import net.tascalate.javaflow.util.function.SuspendableFunction;
import net.tascalate.javaflow.util.function.SuspendablePredicate;
import net.tascalate.javaflow.util.function.SuspendableSupplier;
import net.tascalate.javaflow.util.function.SuspendableUnaryOperator;

public class SuspendableStream<T> implements AutoCloseable {
    
    protected static final SuspendableStream<Object> EMPTY = new SuspendableStream<>(new SuspendableProducer<Object>() {
        
        @Override
        public Option<Object> produce() {
            return Option.none();
        }
        
        @Override
        public void close() {}
    });
    
    protected final SuspendableProducer<T> producer;
    
    public SuspendableStream(SuspendableProducer<T> producer) {
        this.producer = producer;
    }
    
    @Override
    public void close() {
        producer.close();
    }
    
    protected <U> SuspendableStream<U> nextStage(SuspendableProducer<U> producer) {
        return new SuspendableStream<>(producer);
    }
    
    @SuppressWarnings("unchecked")
    public static <T> SuspendableStream<T> empty() {
        return (SuspendableStream<T>)EMPTY;
    }
    
    public static <T> SuspendableStream<T> of(T value) {
        return SuspendableStream.repeat(value).take(1);
    }
    
    @SafeVarargs
    public static <T> SuspendableStream<T> of(T... values) {
        return of(Stream.of(values));
    }
    
    public static <T> SuspendableStream<T> of(Iterable<? extends T> values) {
        return of(values.iterator());
    }
    
    public static <T> SuspendableStream<T> of(Stream<? extends T> values) {
        return of(values.iterator());
    }
    
    private static <T> SuspendableStream<T> of(Iterator<? extends T> values) {
        return new SuspendableStream<>(new RootProducer<T>() {
            @Override
            public Option<T> produce() {
                if (values.hasNext()) {
                    return Option.some(values.next());
                } else {
                    return Option.none();
                }
            }
        });
    }
    
    public static <T> SuspendableStream<T> repeat(T value) {
        return new SuspendableStream<>(new RootProducer<T>() {
            @Override
            public Option<T> produce() {
                return Option.some(value);
            }
        });
    }
    
    public static <T> SuspendableStream<T> generate(Supplier<? extends T> supplier) {
        return new SuspendableStream<>(new RootProducer<T>() {
            @Override
            public Option<T> produce() {
                return Option.some(supplier.get());
            }
        });
    }
    
    public static <T> SuspendableStream<T> generate$(SuspendableSupplier<? extends T> supplier) {
        return new SuspendableStream<>(new RootProducer<T>() {
            @Override
            public Option<T> produce() {
                return Option.some(supplier.get());
            }
        });
    }

    public static <T> SuspendableStream<T> iterate(T seed, UnaryOperator<T> f) {
        return new SuspendableStream<>(new RootProducer<T>() {
            Option<T> current = Option.none();
            @Override
            public Option<T> produce() {
                return current.exists() ? current.map(f) : Option.some(seed);
            }
        });
    }
    
    public static <T> SuspendableStream<T> iterate$(T seed, SuspendableUnaryOperator<T> f) {
        return new SuspendableStream<>(new RootProducer<T>() {
            Option<T> current = Option.none();
            @Override
            public Option<T> produce() {
                return current.exists() ? current.map$(f) : Option.some(seed);
            }
        });
    }
    
    
    @SafeVarargs
    public static <T> SuspendableStream<T> union(SuspendableStream<? extends T>... streams) {
        return union(Stream.of(streams));
    }
    
    public static <T> SuspendableStream<T> union(Collection<? extends SuspendableStream<? extends T>> streams) {
        return union(streams.stream());
    }
    
    public static <T> SuspendableStream<T> union(Stream<? extends SuspendableStream<? extends T>> streams) {
        return SuspendableStream.of(streams).flatMap(Function.identity());
    }
    
    public static <T, U, R> SuspendableStream<R> zip(SuspendableStream<T> a, 
                                                     SuspendableStream<U> b,
                                                     BiFunction<? super T, ? super U, ? extends R> zipper) {
        return zip(a, b, zipper, null, null);
    }
    
    public static <T, U, R> SuspendableStream<R> zip(SuspendableStream<T> a, 
                                                     SuspendableStream<U> b, 
                                                     BiFunction<? super T, ? super U, ? extends R> zipper,
                                                     Supplier<? extends T> onAMissing,
                                                     Supplier<? extends U> onBMissing) {
        return a.zip(b, zipper, onAMissing, onBMissing);
    }
    
    public static <T, U, R> SuspendableStream<R> zip$(SuspendableStream<T> a,
                                                      SuspendableStream<U> b,
                                                      SuspendableBiFunction<? super T, ? super U, ? extends R> zipper) {
        return zip$(a, b, zipper, null, null);
    }
    
    public static <T, U, R> SuspendableStream<R> zip$(SuspendableStream<T> a, 
                                                      SuspendableStream<U> b,
                                                      SuspendableBiFunction<? super T, ? super U, ? extends R> zipper,
                                                      SuspendableSupplier<? extends T> onAMissing,
                                                      SuspendableSupplier<? extends U> onBMissing) {
        return a.zip$(b, zipper, onAMissing, onBMissing);
    }

    
    public <R> SuspendableStream<R> map(Function<? super T, ? extends R> mapper) {
        return nextStage(new NestedStageProducer<R>() {
            @Override
            public Option<R> produce() {
                return producer.produce().map(mapper);
            }
        });
    }
    
    public <R> SuspendableStream<R> map$(SuspendableFunction<? super T, ? extends R> mapper) {
        return nextStage(new NestedStageProducer<R>() {
            @Override
            public Option<R> produce() {
                return producer.produce().map$(mapper);
            }
        });
    }

    
    public SuspendableStream<T> filter(Predicate<? super T> predicate) {
        return nextStage(new NestedStageProducer<T>() {
            @Override
            public Option<T> produce() {
                Option<T> v;
                while ((v = producer.produce()).exists()) {
                    v = v.filter(predicate);
                    if (v.exists()) {
                        return v;
                    }
                }
                return Option.none();
            }
        });
    }
    
    public SuspendableStream<T> filter$(SuspendablePredicate<? super T> predicate) {
        return nextStage(new NestedStageProducer<T>() {
            @Override
            public Option<T> produce() {
                Option<T> v;
                while ((v = producer.produce()).exists()) {
                    v = v.filter$(predicate);
                    if (v.exists()) {
                        return v;
                    }
                }
                return Option.none();
            }
        });
    }
    
    
    public <R> SuspendableStream<R> flatMap(Function<? super T, ? extends SuspendableStream<? extends R>> mapper) {
        StreamToOption<R> nextByStreamProducer = nextByStreamProducer();
        return nextStage(new NestedStageProducer<R>() {
            Option<? extends SuspendableStream<? extends R>> current = Option.none();
            
            @Override
            public Option<R> produce() {
                Option<R> r = current.flatMap$(nextByStreamProducer);
                if (r.exists()) {
                    return r;
                } else {
                    current.accept(SuspendableStream::close);
                    Option<T> v;
                    while ((v = producer.produce()).exists()) {
                        current = v.map(mapper);
                        r = current.flatMap$(nextByStreamProducer);
                        if (r.exists()) {
                            return r;
                        }
                    }
                    return Option.none();
                }
            }

            @Override
            public void close() {
                try {
                    current.accept(SuspendableStream::close);
                } finally {
                    super.close();
                }
            }
        });
    }
    
    public <R> SuspendableStream<R> flatMap$(SuspendableFunction<? super T, ? extends SuspendableStream<? extends R>> mapper) {
        StreamToOption<R> nextByStreamProducer = nextByStreamProducer();
        return nextStage(new NestedStageProducer<R>() {
            Option<? extends SuspendableStream<? extends R>> current = Option.none();
            
            @Override
            public Option<R> produce() {
                Option<R> r = current.flatMap$(nextByStreamProducer);
                if (r.exists()) {
                    return r;
                } else {
                    current.accept(SuspendableStream::close);
                    Option<T> v;
                    while ((v = producer.produce()).exists()) {
                        current = v.map$(mapper);
                        r = current.flatMap$(nextByStreamProducer);
                        if (r.exists()) {
                            return r;
                        }
                    }
                    return Option.none();
                }
            }

            @Override
            public void close() {
                try {
                    current.accept(SuspendableStream::close);
                } finally {
                    super.close();
                }
            }
        });
    }
    
    public SuspendableStream<T> union(SuspendableStream<? extends T> other) {
        return union(this, other);
    }

    public <U, R> SuspendableStream<R> zip(SuspendableStream<U> other, BiFunction<? super T, ? super U, ? extends R> zipper) {
        return zip(other, zipper, null, null);
    }
    
    public <U, R> SuspendableStream<R> zip(SuspendableStream<U> other, 
                                           BiFunction<? super T, ? super U, ? extends R> zipper,
                                           Supplier<? extends T> onLeftMissing, 
                                           Supplier<? extends U> onRightMissing) {
        
        Supplier<Option<T>> aMissing = toValueSupplier(onLeftMissing); 
        Supplier<Option<U>> bMissing = toValueSupplier(onRightMissing);
        return nextStage(new NestedStageProducer<R>() {
            @Override
            public Option<R> produce() {
                Option<T> a = producer.produce();
                Option<U> b = other.producer.produce();
                if (a.exists() || b.exists()) {
                    // Try to combine if at least one exist
                    return a.orElse(aMissing).combine(b.orElse(bMissing), zipper);                    
                } else {
                    return Option.none();
                }
            }

            @Override
            public void close() {
                try {
                    other.close();
                } finally {
                    super.close();
                }
            }
        });        
    }
    
    public <U, R> SuspendableStream<R> zip$(SuspendableStream<U> other, SuspendableBiFunction<? super T, ? super U, ? extends R> zipper) {
        return zip$(other, zipper, null, null);
    }
    
    public <U, R> SuspendableStream<R> zip$(SuspendableStream<U> other, 
                                            SuspendableBiFunction<? super T, ? super U, ? extends R> zipper,
                                            SuspendableSupplier<? extends T> onLeftMissing,
                                            SuspendableSupplier<? extends U> onRightMissing) {
        SuspendableSupplier<Option<T>> aMissing = toValueSupplier(onLeftMissing); 
        SuspendableSupplier<Option<U>> bMissing = toValueSupplier(onRightMissing);
        return nextStage(new NestedStageProducer<R>() {
            @Override
            public Option<R> produce() {
                Option<T> a = producer.produce();
                Option<U> b = other.producer.produce();
                if (a.exists() || b.exists()) {
                    // Try to combine if at least one exist
                    return a.orElse$(aMissing).combine$(b.orElse$(bMissing), zipper);                    
                } else {
                    return Option.none();
                }
            }

            @Override
            public void close() {
                try {
                    other.close();
                } finally {
                    super.close();
                }
            }
        });     
    }
    
    public SuspendableStream<T> peek(Consumer<? super T> action) {
        return nextStage(new NestedStageProducer<T>() {
            @Override
            public Option<T> produce() {
                Option<T> result = producer.produce();
                result.accept(action);
                return result;
            }
        });
    }
    
    public SuspendableStream<T> peek$(SuspendableConsumer<? super T> action) {
        return nextStage(new NestedStageProducer<T>() {
            @Override
            public Option<T> produce() {
                Option<T> result = producer.produce();
                result.accept$(action);
                return result;
            }
        });
    }
    
    public SuspendableStream<T> ignoreErrors() {
        return nextStage(new NestedStageProducer<T>() {
            @Override
            public Option<T> produce() {
                while (true) {
                    try {
                        return producer.produce();
                    } catch (Exception ex) {
                        // Ignore regular exceptions
                    }
                }
            }
        });
    }
    
    public SuspendableStream<T> stopOnError() {
        return nextStage(new NestedStageProducer<T>() {
            @Override
            public Option<T> produce() {
                try {
                    return producer.produce();
                } catch (Exception ex) {
                    return Option.none();
                }
            }
        });
    }    
    
    public SuspendableStream<T> recover(T value) {
        return recover(ex -> value);
    }    
    
    public SuspendableStream<T> recover(Function<? super Throwable, ? extends T> recover) {
        return nextStage(new NestedStageProducer<T>() {
            @Override
            public Option<T> produce() {
                try {
                    return producer.produce();
                } catch (Exception ex) {
                    return Option.some(recover.apply(ex));
                }
            }
        });
    }
    
    public SuspendableStream<T> recover$(SuspendableFunction<? super Throwable, ? extends T> recover) {
        return nextStage(new NestedStageProducer<T>() {
            @Override
            public Option<T> produce() {
                try {
                    return producer.produce();
                } catch (Exception ex) {
                    return Option.some(recover.apply(ex));
                }
            }
        });
    }
    
    public SuspendableStream<T> drop(long count) {
        return nextStage(new NestedStageProducer<T>() {
            long idx = 0;
            
            @Override
            public Option<T> produce() {
                for (; idx < count; idx++) {
                    // Forward
                    Option<T> v = producer.produce();
                    if (!v.exists()) {
                        idx = count;
                        return Option.none();
                    }
                }
                return producer.produce();
            }
        });
    }
    
    public SuspendableStream<T> take(long maxSize) {
        return nextStage(new NestedStageProducer<T>() {
            long idx = 0;
            
            @Override
            public Option<T> produce() {
                if (idx < maxSize) {
                    idx++;
                    Option<T> v = producer.produce();
                    if (!v.exists()) {
                        idx = maxSize;
                        return Option.none();
                    } else {
                        return v;
                    }
                } else {
                    // Close as long as we potentially terminating preliminary
                    close();
                    return Option.none();
                }
            }
        });
    }
    
    public @continuable void forEach(Consumer<? super T> action) {
        Option<T> v;
        while ((v = producer.produce()).exists()) {
            v.accept(action);
        }
    }
    
    public @continuable void forEach$(SuspendableConsumer<? super T> action) {
        Option<T> v;
        while ((v = producer.produce()).exists()) {
            v.accept$(action);
        }
    }
    
    public @continuable Optional<T> reduce(BinaryOperator<T> accumulator) {
        Option<T> result = Option.none();
        Option<T> v;
        while ((v = producer.produce()).exists()) {
            if (result.exists()) {
                result = result.combine(v, accumulator);
            } else {
                result = v;                
            }
        }
        return toOptional(result);
    }
    
    public @continuable Optional<T> reduce$(SuspendableBinaryOperator<T> accumulator) {
        Option<T> result = Option.none();
        Option<T> v;
        while ((v = producer.produce()).exists()) {
            if (result.exists()) {
                result = result.combine$(v, accumulator);
            } else {
                result = v;                
            }
        }
        return toOptional(result);
    }
    
    public @continuable <U> U fold(U identity, BiFunction<U, ? super T, U> accumulator) {
        Option<U> result = Option.some(identity);
        Option<T> v;
        while ((v = producer.produce()).exists()) {
            result = result.combine(v, accumulator);
        }
        return result.get();
    }
    
    public @continuable <U> U fold$(U identity, SuspendableBiFunction<U, ? super T, U> accumulator) {
        Option<U> result = Option.some(identity);
        Option<T> v;
        while ((v = producer.produce()).exists()) {
            result = result.combine$(v, accumulator);
        }
        return result.get();
    }
    
    public <R> R as(Function<? super SuspendableStream<T>, R> converter) {
        return converter.apply(this);
    }
    
    public <R> R convert(Function<? super SuspendableProducer<T>, R> converter) {
        return converter.apply(producer);
    }
    
    public SuspendableIterator<T> iterator() {
        return new StreamIterator();
    }

    abstract class NestedStageProducer<U> implements SuspendableProducer<U> {
        @Override
        public void close() {
            SuspendableStream.this.close();
        }
    }
    
    abstract static class RootProducer<U> implements SuspendableProducer<U> {
        @Override
        public void close() {
        }
    }
    
    final class StreamIterator implements SuspendableIterator<T> {
        private boolean advance  = true;
        private Option<T> current = Option.none();
        
        @Override
        public boolean hasNext() {
            advanceIfNecessary();
            return current.exists();
        }

        @Override
        public T next() {
            advanceIfNecessary();
            T result = current.get();
            advance = true;
            return result;
        }

        @Override
        public void close() {
            current = Option.none();
            advance = false;
            SuspendableStream.this.close();
        }
        
        protected @continuable void advanceIfNecessary() {
            if (advance) {
                current = producer.produce();
            }
            advance = false;
        }

        @Override
        public String toString() {
            return String.format("%s[owner=%s, current=%s]", getClass().getSimpleName(), SuspendableStream.this, current);
        }
    }
    
    @SuppressWarnings("unchecked")
    private static <T> Supplier<Option<T>> toValueSupplier(Supplier<? extends T> s) {
        return s != null ? () -> Option.some(s.get()) : (Supplier<Option<T>>)NONE_SUPPLIER_R;
    }
    
    @SuppressWarnings("unchecked")
    private static <T> SuspendableSupplier<Option<T>> toValueSupplier(SuspendableSupplier<? extends T> s) {
        return s != null ? () -> Option.some(s.get()) : (SuspendableSupplier<Option<T>>)NONE_SUPPLIER_C;
    }

    private static <T> Optional<T> toOptional(Option<T> maybeValue) {
        return maybeValue.exists() ? Optional.ofNullable( maybeValue.get() ) : Optional.empty();
    }

    private static <R> StreamToOption<R> nextByStreamProducer() {
        return new StreamToOption<R>() {
            @Override
            public Option<? extends R> apply(SuspendableStream<? extends R> stream) {
                return stream.producer.produce();
            }
        };
    }
    
    // To capture insane generic signature
    private static interface StreamToOption<T> 
        extends SuspendableFunction<SuspendableStream<? extends T>, Option<? extends T>> {}
    
    private static final Supplier<?> NONE_SUPPLIER_R = () -> Option.none();
    private static final SuspendableSupplier<?> NONE_SUPPLIER_C = () -> Option.none();
}
