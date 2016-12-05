/**
 * DistanceCosine.java
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
 * $Id: DistanceCosine.java,v 1.2 2004/12/05 02:55:59 sofmac Exp $
 * Part of the open-source Network Learning Toolkit
 *
 * User: smacskassy
 * Date: Nov 30, 2004
 * Time: 11:53:29 PM
 */
package netkit.util;

public class DistanceCosine implements DistanceMeasure
{
    public double distance(double[] vec1, double[] vec2) {
	double dist = VectorMath.dotproduct(vec1,vec2);
        double v1S = VectorMath.l2_length(vec1);
        double v2S = VectorMath.l2_length(vec2);
	if(v1S == 0 || v2S == 0)
	    return Double.NaN;
	return ( dist / (v1S*v2S) );
    }
}
