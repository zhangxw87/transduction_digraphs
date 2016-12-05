/**
 * DistanceL2.java
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
 * $Id: DistanceL2.java,v 1.2 2004/12/05 02:55:59 sofmac Exp $
 * Part of the open-source Network Learning Toolkit
 *
 * User: smacskassy
 * Date: Nov 30, 2004
 * Time: 11:56:41 PM
 */
package netkit.util;

public class DistanceL2 implements DistanceMeasure
{
    public double distance(double[] vec1, double[] vec2) {
        if(vec1.length != vec2.length)
            throw new IllegalArgumentException("vectors 1 and 2 are not of same length! ("+vec1.length+" and "+vec2.length+" respectively).");
        double dist = 0;
        for(int i=0;i<vec1.length;i++)
            dist += Math.pow(vec1[i]-vec2[i],2.0);
	return Math.sqrt(dist);
    }
}
