/**
 * ArrayIterator.java
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

/**
 * $Id: ArrayIterator.java,v 1.1 2004/12/01 05:34:37 sofmac Exp $
 * Part of the open-source Network Learning Toolkit
 *
 * User: smacskassy
 * Date: Dec 1, 2004
 * Time: 12:16:32 AM
 */
package netkit.util;

public class ArrayIterator<T> implements java.util.Iterator<T>
{
    private T[] a;
    private int idx = 0;
    private boolean incNull = true;
    public ArrayIterator(T[] entities) {
	    this(entities,true);
    }
    public ArrayIterator(T[] entities, boolean includeNull) {
	    a = entities;
        this.incNull = includeNull;
        if(!incNull)
        {
            while(idx < a.length && a[idx] == null) idx++;
        }
    }
    public T next() {
	    if(!hasNext()) throw new java.util.NoSuchElementException();
        T o = a[idx++];
        if(!incNull)
        {
            while(idx < a.length && a[idx] == null) idx++;
        }
	    return o;
    }
    public  boolean     hasNext()  {
        return (idx<a.length&&(incNull||a[idx]!=null));
    }
    public  void        remove() {
        throw new UnsupportedOperationException();
    }
}
