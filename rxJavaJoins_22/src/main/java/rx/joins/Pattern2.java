/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.joins;

import rx.Observable;
import rx.functions.Func2;

/**
 * Represents a join pattern over observable sequences.
 */
public final class Pattern2<T1, T2> implements Pattern {
    private final Observable<T1> o1;
    private final Observable<T2> o2;

    public Pattern2(Observable<T1> o1, Observable<T2> o2) {
        this.o1 = o1;
        this.o2 = o2;
    }

    Observable<T1> o1() {
        return o1;
    }

    Observable<T2> o2() {
        return o2;
    }

    /**
     * Creates a pattern that matches when all three observable sequences have an available element.
     * 
     * @param other
     *            Observable sequence to match with the two previous sequences.
     * @return Pattern object that matches when all observable sequences have an available element.
     */
    public <T3> Pattern3<T1, T2, T3> and(Observable<T3> other) {
        if (other == null) {
            throw new NullPointerException();
        }
        return new Pattern3<T1, T2, T3>(o1, o2, other);
    }

    /**
     * Matches when all observable sequences have an available
     * element and projects the elements by invoking the selector function.
     * 
     * @param selector
     *            the function that will be invoked for elements in the source sequences.
     * @return the plan for the matching
     * @throws NullPointerException
     *             if selector is null
     */
    public <R> Plan0<R> then(Func2<T1, T2, R> selector) {
        if (selector == null) {
            throw new NullPointerException();
        }
        return new Plan2<T1, T2, R>(this, selector);
    }
}
