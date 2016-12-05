/**
 * ArrayUtil.java
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
 * $Id: ArrayUtil.java,v 1.3 2005/02/07 16:31:51 sofmac Exp $
 * Part of the open-source Network Learning Toolkit
 *
 * User: smacskassy
 * Date: Nov 30, 2004
 * Time: 11:45:18 PM
 */
package netkit.util;

import java.text.NumberFormat;

public final class ArrayUtil {
    private static final NumberFormat nf = NumberFormat.getInstance();

    public static String asString(double[][] count) {
        StringBuilder sb = new StringBuilder();
        nf.setMaximumFractionDigits(0);
        nf.setMinimumIntegerDigits(2);

        sb.append("  ");
        for(int c=0;c<count.length;c++)
        {
            sb.append("      ");
            sb.append(nf.format(c));
        }
        sb.append(NetKitEnv.newline);

        for(int i=0;i<count.length;i++)
        {
            nf.setMaximumFractionDigits(0);
            nf.setMinimumFractionDigits(0);
            nf.setMaximumIntegerDigits(2);
            nf.setMinimumIntegerDigits(2);
            sb.append("  ").append(nf.format(i)).append(' ');
            nf.setMaximumFractionDigits(4);
            nf.setMinimumFractionDigits(4);
            nf.setMaximumIntegerDigits(1);
            nf.setMinimumIntegerDigits(1);
            for(int col=0;col<count[i].length;col++)
            {
                sb.append("  ");
                sb.append(nf.format(count[i][col]));
            }
            sb.append(NetKitEnv.newline);
        }
        return sb.toString();
    }

    public static String asString(double[] array) {
        return asString(array,0,((array==null)?-1:array.length));
    }
    public static String asString(double[] array, int from, int to) {
        if(array == null)
            return "null";
        final StringBuilder sb = new StringBuilder("(");
        if(from<to)
            sb.append(array[from++]);
        for(int i=from;i<to;i++)
            sb.append(',').append(array[i]);
        sb.append(')');
        return sb.toString();
    }

    public static String asString(int[] array) {
        return asString(array,0,((array==null)?-1:array.length));
    }
    public static String asString(int[] array, int from, int to) {
        if(array == null)
            return "null";
        final StringBuilder sb = new StringBuilder("(");
        if(from<to)
            sb.append(array[from++]);
        for(int i=from;i<to;i++)
            sb.append(',').append(array[i]);
        sb.append(')');
        return sb.toString();
    }

    public static <T> String asString(T[] array) {
        return asString(array,0,((array==null)?-1:array.length));
    }
    public static <T> String asString(T[] array, int from, int to) {
        if(array == null)
            return "null";
        final StringBuilder sb = new StringBuilder("(");
        if(from<to)
            sb.append(array[from++]);
        for(int i=from;i<to;i++)
            sb.append(',').append(array[i]);
        sb.append(')');
        return sb.toString();
    }

}
