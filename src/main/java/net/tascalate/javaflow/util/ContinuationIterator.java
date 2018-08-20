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
import java.util.NoSuchElementException;

import org.apache.commons.javaflow.api.Continuation;

import net.tascalate.javaflow.util.function.SuspendableRunnable;

/**
 * 
 * @author Valery Silaev
 *
 * Read-only iterator over multiple results returned by continuation passed 
 * The iterator works similar to <i>yield</i> keyword found in other languages 
 * 
 * Usage:
 * <code>
 * final Runnable code = instantiateContinuableCode();
 * for (final Iterator&lt;String&gt; i = new ContinuationIterator&lt;String&gt;(code); i.hasNext(); ) {
 *   final String line = i.next();
 *   System.out.println( line );
 * } 
 * </code>
 * 
 * Also ContinuationIterator created iterates over multiple values yielded by continuation, 
 * it's impossible to pass value back to continuation from client code; the result of 
 * {@link Continuation#suspend(Object)} in continuation will be always <i>null</i> 
 * 
 * @param <E>
 * Type of objects returned by the iterator
 */
public class ContinuationIterator<E> implements CloseableIterator<E>, Serializable {
    private static final long serialVersionUID = 1L;
    
    private boolean advance;
    private Continuation cc;

    /**
     * Iterator constructor
     * 
     * @param code
     * Continuable code that yields multiple results via call to
     * {@link Continuation#suspend(Object)}
     */
    public ContinuationIterator(Runnable code) {
        cc = Continuation.startSuspendedWith(code);
        advance = null != cc;
    }

    /**
     * Iterator constructor
     * 
     * @param code
     * {@link SuspendableRunnable} code that yields multiple results via call to
     * {@link Continuation#suspend(Object)}
     */
    public ContinuationIterator(SuspendableRunnable code) {
        this(Continuations.toRunnable(code));
    }

    /**
     * <p>Iterator constructor
     * <p>Valued returned by this iterator will be results these are yielded via call to 
     * {@link Continuation#suspend(Object)}, i.e. cc.value() is not included
     * 
     * @param cc
     * Current {@link Continuation} to start iteration from. 
     */
    public ContinuationIterator(Continuation cc) {
        this(cc, false);
    }
    
    /**
     * <p>Iterator constructor
     * <p>Valued returned by this iterator will be results these are yielded via call to 
     * {@link Continuation#suspend(Object)}, i.e. cc.value() is not included unless 
     * <code>useCurrentValue</code> is true
     *  
     * @param cc
     * Current {@link Continuation} to start iteration from.
     * 
     * @param useCurrentValue
     * Should the value of the supplied continuation be used as a first returned value 
     */
    public ContinuationIterator(Continuation cc, boolean useCurrentValue) {
        this.cc = cc;
        advance = !useCurrentValue && null != cc;
    }
    
    public boolean hasNext() {
        advanceIfNecessary();
        return cc != null;
    }

    public E next() {
        advanceIfNecessary();

        if (cc == null)
            throw new NoSuchElementException();

        @SuppressWarnings("unchecked")
        E result = (E)cc.value();
        advance = true;

        return result;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public void close() {
        try {
            if (null != cc) {
                cc.terminate();
            }
        } finally {
            cc = null;
            advance = false;
        }
    }
    
    protected void advanceIfNecessary() {
        if (advance) {
            cc = cc.resume();
        }
        advance = false;
    }
}
