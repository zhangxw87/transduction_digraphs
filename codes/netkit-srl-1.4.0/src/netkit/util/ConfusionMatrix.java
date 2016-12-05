/**
 * ConfusionMatrix.java
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
 * $Id: ConfusionMatrix.java,v 1.2 2007/03/26 23:45:07 sofmac Exp $
 * Part of the open-source Network Learning Toolkit
 *
 * User: smacskassy
 * Date: Dec 1, 2004
 * Time: 11:29:35 AM
 */
package netkit.util;

import netkit.classifiers.Classification;
import netkit.classifiers.Estimate;
import netkit.graph.Node;
import netkit.graph.AttributeCategorical;

import java.text.NumberFormat;
import java.util.Arrays;
import java.io.PrintWriter;

public class ConfusionMatrix {
    private static final NumberFormat colnf = NumberFormat.getIntegerInstance();
    private static final NumberFormat accnf = NumberFormat.getNumberInstance();

    private AttributeCategorical attribute;
    private int numC;
    private int maxLn;
    private int[] count;
    private int[] predict;
    private double[] acc;
    private int totCount;
    private double totAcc;
    private int[][] matrix; // matrix[A][B] = count of class A predicted as class B

    public ConfusionMatrix(AttributeCategorical attribute) {
        this.attribute = attribute;

        numC = attribute.size();

        maxLn = 0;
        for(String token : attribute.getTokens())
            maxLn = Math.max(maxLn,token.length());
        this.matrix = new int[numC][numC];
        this.count = new int[numC];
        this.predict = new int[numC];
        this.acc = new double[numC];
        Arrays.fill(predict,0);
        Arrays.fill(count,0);
        Arrays.fill(acc,0);
        for(int i=0;i<numC;i++)
            Arrays.fill(matrix[i],0);
        totCount = 0;
        totAcc = 0;
    }

    public ConfusionMatrix(Estimate predictions, Classification truth)  {
        this(truth.getAttribute());
	    for (Node node : predictions)
        {
            int rIdx = predictions.getClassification(node);
            if(rIdx == -1)
                continue;
            int tIdx = truth.getClassValue(node);
            if(tIdx == -1)
                throw new IllegalArgumentException("Truth is not known for instance "+node.getIndex());
            matrix[tIdx][rIdx]++;
            count[tIdx]++;
            predict[rIdx]++;
        }
        for(int i=0;i<numC;i++)
        {
            totAcc += matrix[i][i];
            totCount += count[i];
            acc[i] = matrix[i][i]/(double)count[i];
        }
        totAcc /= (double)totCount;
    }

    public void add(ConfusionMatrix cf)  {
        if(!cf.attribute.equals(attribute))
            throw new IllegalArgumentException("value maps of the two matrices are not the same!");
        totAcc = 0;
        for(int i=0;i<numC;i++)
        {
            for(int j=0;j<numC;j++)
            {
                matrix[i][j]+=cf.matrix[i][j];
            }
            count[i]+=cf.count[i];
            predict[i]+=cf.predict[i];
            totAcc += matrix[i][i];

            acc[i] = matrix[i][i]/(double)count[i];
        }
        totCount += cf.totCount;
        totAcc /= (double)totCount;
    }

    public double getError(int classIndex) {
        return 1.0-acc[classIndex];
    }

    public double getErr() {
        return 1.0-totAcc;
    }

    public double getAccuracy() {
        return totAcc;
    }
    public double getAccuracy(int classIndex) {
        return acc[classIndex];
    }

    public int getCount() {
        return totCount;
    }
    public int getCount(int classIndex) {
        return count[classIndex];
    }
    public int getCount(int classIndex, int predictClass) {
        return matrix[classIndex][predictClass];
    }

    public void printMatrix(PrintWriter pw) {
        for(int i=0;i<maxLn+4;i++) pw.print(" ");
        colnf.setMinimumIntegerDigits(2);
        accnf.setMaximumFractionDigits(5);
        for(int i=0;i<numC;i++)
        {
            pw.print("     ");
            pw.print(colnf.format(i));
        }
        pw.println();
        for(int row=0;row<numC;row++)
        {
            for(int i=0;i<maxLn-attribute.getToken(row).length();i++) pw.print(" ");
            pw.print(attribute.getToken(row));
            colnf.setMinimumIntegerDigits(2);
            pw.print(" "+colnf.format(row)+":");
            colnf.setMinimumIntegerDigits(0);
            for(int col=0;col<numC;col++)
            {
                pw.print(" ");
                String num = colnf.format(matrix[row][col]);
                for(int space=0;space<6-num.length();space++)
                    pw.print(" ");
                pw.print(colnf.format(matrix[row][col]));
            }
            pw.println(" : ("+matrix[row][row]+" correct of "+count[row]+") (accuracy: "+accnf.format(acc[row])+")");
        }
        for(int i=0;i<maxLn-2;i++) pw.print(" ");
        pw.print("TOTAL:");
        int totC=0;
        for(int col=0;col<numC;col++)
        {
            pw.print(" ");
            String num = colnf.format(predict[col]);
            for(int space=0;space<6-num.length();space++)
                pw.print(" ");
            pw.print(colnf.format(predict[col]));
            totC+=matrix[col][col];
        }
        pw.println(" : ("+totC+" correct of "+totCount+") (accuracy: "+accnf.format(totAcc)+")");
    }
}
