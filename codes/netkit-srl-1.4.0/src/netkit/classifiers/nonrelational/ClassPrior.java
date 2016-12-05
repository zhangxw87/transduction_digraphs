/**
 * ClassPrior.java
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

package netkit.classifiers.nonrelational;

import netkit.graph.Node;
import netkit.util.NetKitEnv;
import netkit.classifiers.ClassifierImp;

/**
 * This classifier is static and always returns the class marginals.
 *
 * @author Sofus A. Macskassy (sofmac@gmail.com)
 */
public final class ClassPrior extends ClassifierImp
{
    /**
     * @return 'ClassPrior'
     */
    public String getShortName() {
        return "ClassPrior";
    }

    /**
     * @return 'ClassPrior'
     */
    public String getName() {
        return "ClassPrior";
    }

    /**
     * @return 'This classifier is static and always returns the class marginals.'
     */
    public String getDescription() {
        return "This classifier is static and always returns the class marginals.";
    }

    /**
     * Copies the class marginals into the 'result' array.
     * @param node Ignored
     * @param result Filled with the class marginals
     * @return true
     */
    public boolean estimate(Node node, double[] result) {
        System.arraycopy(classPrior,0,result,0,classPrior.length);
        return true;
    }
    
    public String toString() {
    	StringBuffer sb = new StringBuffer();
    	sb.append(getName()+" (Non-Relational Classifier)").append(NetKitEnv.newline);
    	sb.append("-------------------------------------").append(NetKitEnv.newline);
    	for(int i=0;i<classPrior.length;i++) {
    		sb.append(attribute.getToken(i)).append(": ").append(classPrior[i]).append(NetKitEnv.newline);
    	}
    	return sb.toString();
    }
}

