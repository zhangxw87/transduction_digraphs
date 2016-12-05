/**
 * UpdatablePriorityQueue
 * Copyright (C) 2008 Sofus A. Macskassy
 *
 * Part of the open-source Network Learning Toolkit
 * http://netkit-srl.sourceforge.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **/

/**
 * $Id$
 **/

package netkit.util;

import java.util.*;

public class UpdatablePriorityQueue<E> implements Iterable<E> {

    private static final int DEFAULT_INITIAL_CAPACITY = 11;

    private final class QueueElement implements Comparable<QueueElement> {
        public final E elem;
        private int index;
        QueueElement(final E elem, final int idx) {
            this.elem = elem;
            this.index = idx;
        }

        public int compareTo(QueueElement e) {
            return comparator.compare(elem,e.elem);
        }
    }

    /**
     * Priority queue represented as a balanced binary heap: the two
     * children of queue[n] are queue[2*n+1] and queue[2*(n+1)].  The
     * priority queue is ordered by comparator, or by the elements'
     * natural ordering, if comparator is null: For each node n in the
     * heap and each descendant d of n, n <= d.  The element with the
     * lowest value is in queue[0], assuming the queue is nonempty.
     */
    private transient QueueElement[] queue;

    /**
     * The number of elements in the priority queue.
     */
    private int size = 0;

    /**
     * The comparator, or null if priority queue uses elements'
     * natural ordering.
     */
    private final Comparator<? super E> comparator;

    /**
     * The number of times this priority queue has been
     * <i>structurally modified</i>.  See AbstractList for gory details.
     */
    private transient int modCount = 0;

    /**
     * Creates a {@code PriorityQueue} with the default initial
     * capacity (11) that orders its elements according to their
     * {@linkplain Comparable natural ordering}.
     */
    public UpdatablePriorityQueue() {
        this(DEFAULT_INITIAL_CAPACITY, null);
    }

    /**
     * Creates a {@code PriorityQueue} with the specified initial
     * capacity that orders its elements according to their
     * {@linkplain Comparable natural ordering}.
     *
     * @param initialCapacity the initial capacity for this priority queue
     * @throws IllegalArgumentException if {@code initialCapacity} is less
     *         than 1
     */
    public UpdatablePriorityQueue(int initialCapacity) {
        this(initialCapacity, null);
    }

    /**
     * Creates a {@code PriorityQueue} with the specified initial capacity
     * that orders its elements according to the specified comparator.
     *
     * @param  initialCapacity the initial capacity for this priority queue
     * @param  comparator the comparator that will be used to order this
     *         priority queue.  If {@code null}, the {@linkplain Comparable
     *         natural ordering} of the elements will be used.
     * @throws IllegalArgumentException if {@code initialCapacity} is
     *         less than 1
     */
    public UpdatablePriorityQueue(int initialCapacity,
                         Comparator<? super E> comparator) {
        // Note: This restriction of at least one is not actually needed,
        // but continues for 1.5 compatibility
        if (initialCapacity < 1)
            throw new IllegalArgumentException();
        // this.queue = new QueueElement[initialCapacity];
        if(comparator ==  null)
        {
            this.comparator = new Comparator<E>(){
                @SuppressWarnings("unchecked")
                public int compare(E e1, E e2) { return ((Comparable<E>)e1).compareTo(e2); }
            };
        }
        else
        {
            this.comparator = comparator;
        }
    }

    /**
     * Increases the capacity of the array.
     *
     * @param minCapacity the desired minimum capacity
     */
    private void grow(int minCapacity) {
        if (minCapacity < 0) // overflow
            throw new OutOfMemoryError();
	int oldCapacity = queue.length;
        // Double size if small; else grow by 50%
        int newCapacity = ((oldCapacity < 64)?
                           ((oldCapacity + 1) * 2):
                           ((oldCapacity / 2) * 3));
        if (newCapacity < 0) // overflow
            newCapacity = Integer.MAX_VALUE;
        if (newCapacity < minCapacity)
            newCapacity = minCapacity;
        queue = Arrays.copyOf(queue, newCapacity);
    }

    /**
     * Inserts the specified element into this priority queue.
     *
     * @return {@code true} (as specified by {@link java.util.Collection#add})
     * @throws ClassCastException if the specified element cannot be
     *         compared with elements currently in this priority queue
     *         according to the priority queue's ordering
     * @throws NullPointerException if the specified element is null
     */
    public QueueElement add(E e) {
        if (e == null)
            throw new NullPointerException();
        modCount++;
        int i = size;
        if (i >= queue.length)
            grow(i + 1);
        size = i + 1;
        QueueElement qe = new QueueElement(e,0);
        if (i == 0)
            queue[0] = qe;
        else
            siftUp(i, qe);
        return qe;
    }

    public E peek() {
        if (size == 0)
            return null;
        return queue[0].elem;
    }

    public void update(QueueElement qe) {
        siftUp(qe.index, qe);
    }

    private int indexOf(E o) {
	if (o != null) {
            for (int i = 0; i < size; i++)
                if (o.equals(queue[i].elem))
                    return i;
        }
        return -1;
    }

    /**
     * Removes a single instance of the specified element from this queue,
     * if it is present.  More formally, removes an element {@code e} such
     * that {@code o.equals(e)}, if this queue contains one or more such
     * elements.  Returns {@code true} if and only if this queue contained
     * the specified element (or equivalently, if this queue changed as a
     * result of the call).
     *
     * @param qe element to be removed from this queue, if present
     * @return {@code true} if this queue changed as a result of the call
     */
    public boolean remove(QueueElement qe) {
	int i = qe.index;
	if (i == -1)
	    return false;
	else {
	    removeAt(i);
	    return true;
	}
    }

    /**
     * Removes a single instance of the specified element from this queue,
     * if it is present.  More formally, removes an element {@code e} such
     * that {@code o.equals(e)}, if this queue contains one or more such
     * elements.  Returns {@code true} if and only if this queue contained
     * the specified element (or equivalently, if this queue changed as a
     * result of the call).
     *
     * @param o element to be removed from this queue, if present
     * @return {@code true} if this queue changed as a result of the call
     */
    public boolean remove(E o) {
	int i = indexOf(o);
	if (i == -1)
	    return false;
	else {
	    removeAt(i);
	    return true;
	}
    }


    public boolean isEmpty() { return size==0; }

    /**
     * Version of remove using reference equality, not equals.
     * Needed by iterator.remove.
     *
     * @param o element to be removed from this queue, if present
     * @return {@code true} if removed
     */
    boolean removeEq(Object o) {
	for (int i = 0; i < size; i++) {
	    if (o == queue[i]) {
                removeAt(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if this queue contains the specified element.
     * More formally, returns {@code true} if and only if this queue contains
     * at least one element {@code e} such that {@code o.equals(e)}.
     *
     * @param o object to be checked for containment in this queue
     * @return {@code true} if this queue contains the specified element
     */
    public boolean contains(E o) {
	return indexOf(o) != -1;
    }

    /**
     * Returns an array containing all of the elements in this queue.
     * The elements are in no particular order.
     *
     * <p>The returned array will be "safe" in that no references to it are
     * maintained by this queue.  (In other words, this method must allocate
     * a new array).  The caller is thus free to modify the returned array.
     *
     * <p>This method acts as bridge between array-based and collection-based
     * APIs.
     *
     * @return an array containing all of the elements in this queue
     */
    public Object[] toArray() {
        return Arrays.copyOf(queue, size);
    }

    /**
     * Returns an array containing all of the elements in this queue; the
     * runtime type of the returned array is that of the specified array.
     * The returned array elements are in no particular order.
     * If the queue fits in the specified array, it is returned therein.
     * Otherwise, a new array is allocated with the runtime type of the
     * specified array and the size of this queue.
     *
     * <p>If the queue fits in the specified array with room to spare
     * (i.e., the array has more elements than the queue), the element in
     * the array immediately following the end of the collection is set to
     * {@code null}.
     *
     * <p>Like the {@link #toArray()} method, this method acts as bridge between
     * array-based and collection-based APIs.  Further, this method allows
     * precise control over the runtime type of the output array, and may,
     * under certain circumstances, be used to save allocation costs.
     *
     * <p>Suppose <tt>x</tt> is a queue known to contain only strings.
     * The following code can be used to dump the queue into a newly
     * allocated array of <tt>String</tt>:
     *
     * <pre>
     *     String[] y = x.toArray(new String[0]);</pre>
     *
     * Note that <tt>toArray(new Object[0])</tt> is identical in function to
     * <tt>toArray()</tt>.
     *
     * @param a the array into which the elements of the queue are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose.
     * @return an array containing all of the elements in this queue
     * @throws ArrayStoreException if the runtime type of the specified array
     *         is not a supertype of the runtime type of every element in
     *         this queue
     * @throws NullPointerException if the specified array is null
     */
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        if (a.length < size)
            // Make a new array of a's runtime type, but my contents:
            return (T[]) Arrays.copyOf(queue, size, a.getClass());
	System.arraycopy(queue, 0, a, 0, size);
        if (a.length > size)
            a[size] = null;
        return a;
    }

    /**
     * Returns an iterator over the elements in this queue. The iterator
     * does not return the elements in any particular order.
     *
     * @return an iterator over the elements in this queue
     */
    public Iterator<E> iterator() {
        return new Itr();
    }

    private final class Itr implements Iterator<E> {
        /**
         * Index (into queue array) of element to be returned by
         * subsequent call to next.
         */
        private int cursor = 0;

        /**
         * Index of element returned by most recent call to next,
         * unless that element came from the forgetMeNot list.
         * Set to -1 if element is deleted by a call to remove.
         */
        private int lastRet = -1;

        /**
         * A queue of elements that were moved from the unvisited portion of
         * the heap into the visited portion as a result of "unlucky" element
         * removals during the iteration.  (Unlucky element removals are those
         * that require a siftup instead of a siftdown.)  We must visit all of
         * the elements in this list to complete the iteration.  We do this
         * after we've completed the "normal" iteration.
         *
         * We expect that most iterations, even those involving removals,
         * will not need to store elements in this field.
         */
        private ArrayDeque<E> forgetMeNot = null;

        /**
         * Element returned by the most recent call to next iff that
         * element was drawn from the forgetMeNot list.
         */
        private E lastRetElt = null;

        /**
         * The modCount value that the iterator believes that the backing
         * Queue should have.  If this expectation is violated, the iterator
         * has detected concurrent modification.
         */
        private int expectedModCount = modCount;

        public boolean hasNext() {
            return cursor < size ||
                (forgetMeNot != null && !forgetMeNot.isEmpty());
        }

        public E next() {
            if (expectedModCount != modCount)
                throw new ConcurrentModificationException();
            if (cursor < size)
                return queue[lastRet = cursor++].elem;
            if (forgetMeNot != null) {
                lastRet = -1;
                lastRetElt = forgetMeNot.poll();
                if (lastRetElt != null)
                    return lastRetElt;
            }
            throw new NoSuchElementException();
        }

        public void remove() {
            if (expectedModCount != modCount)
                throw new ConcurrentModificationException();
            if (lastRet != -1) {
                E moved = UpdatablePriorityQueue.this.removeAt(lastRet);
                lastRet = -1;
                if (moved == null)
                    cursor--;
                else {
                    if (forgetMeNot == null)
                        forgetMeNot = new ArrayDeque<E>();
                    forgetMeNot.add(moved);
                }
            } else if (lastRetElt != null) {
                UpdatablePriorityQueue.this.removeEq(lastRetElt);
                lastRetElt = null;
            } else {
                throw new IllegalStateException();
	    }
            expectedModCount = modCount;
        }
    }

    public int size() {
        return size;
    }

    /**
     * Removes all of the elements from this priority queue.
     * The queue will be empty after this call returns.
     */
    public void clear() {
        modCount++;
        for (int i = 0; i < size; i++)
            queue[i] = null;
        size = 0;
    }

    public E poll() {
        if (size == 0)
            return null;
        int s = --size;
        modCount++;
        E result = queue[0].elem;
        QueueElement x = queue[s];
        queue[s] = null;
        if (s != 0)
            siftDown(0, x);
        return result;
    }

    /**
     * Removes the ith element from queue.
     *
     * Normally this method leaves the elements at up to i-1,
     * inclusive, untouched.  Under these circumstances, it returns
     * null.  Occasionally, in order to maintain the heap invariant,
     * it must swap a later element of the list with one earlier than
     * i.  Under these circumstances, this method returns the element
     * that was previously at the end of the list and is now at some
     * position before i. This fact is used by iterator.remove so as to
     * avoid missing traversing elements.
     */
    private E removeAt(int i) {
        assert i >= 0 && i < size;
        modCount++;
        int s = --size;
        if (s == i) // removed last element
            queue[i] = null;
        else {
            QueueElement moved = queue[s];
            queue[s] = null;
            siftDown(i, moved);
            if (queue[i] == moved) {
                siftUp(i, moved);
                if (queue[i] != moved)
                    return moved.elem;
            }
        }
        return null;
    }

    /**
     * Inserts item x at position k, maintaining heap invariant by
     * promoting x up the tree until it is greater than or equal to
     * its parent, or is the root.
     *
     * To simplify and speed up coercions and comparisons. the
     * Comparable and Comparator versions are separated into different
     * methods that are otherwise identical. (Similarly for siftDown.)
     *
     * @param k the position to fill
     * @param x the item to insert
     */
    private void siftUp(int k, QueueElement x) {
        while (k > 0) {
            int parent = (k - 1) >>> 1;
            QueueElement e = queue[parent];
            if (x.compareTo(e) >= 0)
                break;
            queue[k] = e;
            e.index = k;
            k = parent;
        }
        queue[k] = x;
        x.index = k;
    }

    /**
     * Inserts item x at position k, maintaining heap invariant by
     * demoting x down the tree repeatedly until it is less than or
     * equal to its children or is a leaf.
     *
     * @param k the position to fill
     * @param x the item to insert
     */
    private void siftDown(int k, QueueElement x) {
        int half = size >>> 1;        // loop while a non-leaf
        while (k < half) {
            int child = (k << 1) + 1; // assume left child is least
            QueueElement c = queue[child];
            int right = child + 1;
            if (right < size && c.compareTo(queue[right]) > 0)
                c = queue[child = right];
            if (x.compareTo(c) <= 0)
                break;
            queue[k] = c;
            c.index = k;
            k = child;
        }
        queue[k] = x;
        x.index = k;
    }

    /**
     * Establishes the heap invariant (described above) in the entire tree,
     * assuming nothing about the order of the elements prior to the call.
     */
    @SuppressWarnings("unused")
    private void heapify() {
        for (int i = (size >>> 1) - 1; i >= 0; i--)
            siftDown(i, queue[i]);
    }

    /**
     * Returns the comparator used to order the elements in this
     * queue, or {@code null} if this queue is sorted according to
     * the {@linkplain Comparable natural ordering} of its elements.
     *
     * @return the comparator used to order this queue, or
     *         {@code null} if this queue is sorted according to the
     *         natural ordering of its elements
     */
    public Comparator<? super E> comparator() {
        return comparator;
    }
}
