/**
 * InferenceMethodListener.java
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
 * $Id: InferenceMethodListener.java,v 1.1 2004/12/05 02:36:50 sofmac Exp $
 * Part of the open-source Network Learning Toolkit
 *
 * User: smacskassy
 * Date: Dec 1, 2004
 * Time: 11:00:04 AM
 */
package netkit.inference;

import netkit.graph.Graph;
import netkit.classifiers.Classification;
import netkit.classifiers.Estimate;

public interface InferenceMethodListener {
    public void estimate(Estimate e, int[] unknown);
    public void classify(Classification c,int[] unknown);
    public void iterate(Graph g,int[] unknown);
}
