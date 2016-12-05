/**
 * StatUtil.java
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
 * $Id: StatUtil.java,v 1.4 2007/03/26 23:45:07 sofmac Exp $
 * Part of the open-source Network Learning Toolkit
 *
 * User: smacskassy
 * Date: Nov 30, 2004
 * Time: 11:31:18 PM
 */
package netkit.util;

import java.util.logging.Logger;

public class StatUtil
{
    private static final Logger logger = NetKitEnv.getLogger(StatUtil.class.getName());

    private static double[][] z;
    private static double[] zLUT;

    static
    {
        zLUT = new double[]{0.550,0.600,0.700,0.750,0.800,0.900,0.950, 0.975,0.988,0.990, 0.995,0.998,  0.999, 0.9995};
        z = new double[][]{
            null,
            new double[]{0.158,0.325,0.727,1.000,1.376,3.08,6.31,12.71,25.45,31.82,63.66,127.32,318.29,636.58}, //1
            new double[]{0.142,0.289,0.617,0.816,1.061,1.89,2.92, 4.30, 6.21, 6.96, 9.92, 14.09, 22.33, 31.60}, //2
            new double[]{0.137,0.277,0.584,0.765,0.978,1.64,2.35, 3.18, 4.18, 4.54, 5.84,  7.45, 10.21, 12.92}, //3
            new double[]{0.134,0.271,0.569,0.741,0.941,1.53,2.13, 2.78, 3.50, 3.75, 4.60,  5.60,  7.17,  8.61}, //4
            new double[]{0.132,0.267,0.559,0.727,0.920,1.48,2.02, 2.57, 3.16, 3.36, 4.03,  4.77,  5.89,  6.87}, //5
            new double[]{0.131,0.265,0.553,0.718,0.906,1.44,1.94, 2.45, 2.97, 3.14, 3.71,  4.32,  5.21,  5.96}, //6
            new double[]{0.130,0.263,0.549,0.711,0.896,1.41,1.89, 2.36, 2.84, 3.00, 3.50,  4.03,  4.79,  5.41}, //7
            new double[]{0.130,0.262,0.546,0.706,0.889,1.40,1.86, 2.31, 2.75, 2.90, 3.36,  3.83,  4.50,  5.04}, //8
            new double[]{0.129,0.261,0.543,0.703,0.883,1.38,1.83, 2.26, 2.69, 2.82, 3.25,  3.69,  4.30,  4.78}, //9
            new double[]{0.129,0.260,0.542,0.700,0.879,1.37,1.81, 2.23, 2.63, 2.76, 3.17,  3.58,  4.14,  4.59}, //10
            new double[]{0.129,0.260,0.540,0.697,0.876,1.36,1.80, 2.20, 2.59, 2.72, 3.11,  3.50,  4.02,  4.44}, //11
            new double[]{0.128,0.259,0.539,0.695,0.873,1.36,1.78, 2.18, 2.56, 2.68, 3.05,  3.43,  3.93,  4.32}, //12
            new double[]{0.128,0.259,0.538,0.694,0.870,1.35,1.77, 2.16, 2.53, 2.65, 3.01,  3.37,  3.85,  4.22}, //13
            new double[]{0.128,0.258,0.537,0.692,0.868,1.35,1.76, 2.14, 2.51, 2.62, 2.98,  3.33,  3.79,  4.14}, //14
            new double[]{0.128,0.258,0.536,0.691,0.866,1.34,1.75, 2.13, 2.49, 2.60, 2.95,  3.29,  3.73,  4.07}, //15
            new double[]{0.128,0.258,0.535,0.690,0.865,1.34,1.75, 2.12, 2.47, 2.58, 2.92,  3.25,  3.69,  4.01}, //16
            new double[]{0.128,0.257,0.534,0.689,0.863,1.33,1.74, 2.11, 2.46, 2.57, 2.90,  3.22,  3.65,  3.97}, //17
            new double[]{0.127,0.257,0.534,0.688,0.862,1.33,1.73, 2.10, 2.45, 2.55, 2.88,  3.20,  3.61,  3.92}, //18
            new double[]{0.127,0.257,0.533,0.688,0.861,1.33,1.73, 2.09, 2.43, 2.54, 2.86,  3.17,  3.58,  3.88}, //19
            new double[]{0.127,0.257,0.533,0.687,0.860,1.33,1.72, 2.09, 2.42, 2.53, 2.85,  3.15,  3.55,  3.85}, //20
            new double[]{0.127,0.257,0.532,0.686,0.859,1.32,1.72, 2.08, 2.41, 2.52, 2.83,  3.14,  3.53,  3.82}, //21
            new double[]{0.127,0.256,0.532,0.686,0.858,1.32,1.72, 2.07, 2.41, 2.51, 2.82,  3.12,  3.50,  3.79}, //22
            new double[]{0.127,0.256,0.532,0.685,0.858,1.32,1.71, 2.07, 2.40, 2.50, 2.81,  3.10,  3.48,  3.77}, //23
            new double[]{0.127,0.256,0.531,0.685,0.857,1.32,1.71, 2.06, 2.39, 2.49, 2.80,  3.09,  3.47,  3.75}, //24
            new double[]{0.127,0.256,0.531,0.684,0.856,1.32,1.71, 2.06, 2.38, 2.49, 2.79,  3.08,  3.45,  3.73}, //25
            new double[]{0.127,0.256,0.531,0.684,0.856,1.31,1.71, 2.06, 2.38, 2.48, 2.78,  3.07,  3.43,  3.71}, //26
            new double[]{0.127,0.256,0.531,0.684,0.855,1.31,1.70, 2.05, 2.37, 2.47, 2.77,  3.06,  3.42,  3.69}, //27
            new double[]{0.127,0.256,0.530,0.683,0.855,1.31,1.70, 2.05, 2.37, 2.47, 2.76,  3.05,  3.41,  3.67}, //28
            new double[]{0.127,0.256,0.530,0.683,0.854,1.31,1.70, 2.05, 2.36, 2.46, 2.76,  3.04,  3.40,  3.66}, //29
            new double[]{0.127,0.256,0.530,0.683,0.854,1.31,1.70, 2.04, 2.36, 2.46, 2.75,  3.03,  3.39,  3.65}, //30
            new double[]{0.126,0.255,0.529,0.681,0.851,1.30,1.68, 2.02, 2.33, 2.42, 2.70,  2.97,  3.31,  3.55}, // 40
            new double[]{0.126,0.254,0.527,0.679,0.848,1.30,1.67, 2.00, 2.30, 2.39, 2.66,  2.91,  3.23,  3.46}, // 60
            new double[]{0.126,0.254,0.526,0.677,0.845,1.29,1.66, 1.98, 2.27, 2.36, 2.62,  2.86,  3.16,  3.37}, // 120
            new double[]{0.126,0.253,0.524,0.674,0.842,1.28,1.64, 1.96, 2.24, 2.33, 2.58,  2.81,  3.09,  3.29} }; // INFINITY
    }

    private static double[] getRow(int size)
    {
        if(size<1)   return z[0];
        if(size<31)  return z[size];
        if(size<41)  return z[31];
        if(size<61)  return z[32];
        if(size<121) return z[33];
        return z[34];
   }

    public static double getSignificance(int size, double stdDevDiff)
    {
        double[] vals = getRow(size);
        if(vals == null)
        {
            logger.warning("getSignificance("+size+","+stdDevDiff+") = -1 --- no size found!");
            return -1;
        }
        for(int i=vals.length-1;i>=0;i--)
        {
            if(vals[i] < stdDevDiff)
                return zLUT[i];
        }
        return 0;
    }

    public static double getOneSidedZ(int size,double pvalue)
    {
        if(pvalue > 0.45)
            return 0;
        double[] vals = getRow(size);
        if(vals == null)
        {
            logger.warning("getOneSidedZ("+size+","+pvalue+") = -1 --- no size found!");
            return -1;
        }
        double p = 1-pvalue;
        for(int i=0;i<zLUT.length;i++)
        {
            if(zLUT[i]>=p)
            {
                return vals[i];
            }
        }
        return 0;
    }

    public static double getTwoSidedZ(int size,double pvalue)
    {
        return getOneSidedZ(size,(pvalue/2));
    }

    public static double getMean(double[] values)
    {
        if(values==null)
            return 0;
        double avg = 0;
	for (double d : values)
            avg += d;
        return (avg/values.length);
    }

    public static double getCorrelation(double[] valueSet1, double[] valueSet2)
    {
        return getCovariance(valueSet1,valueSet2)/(getStdDev(valueSet1,getMean(valueSet1))*getStdDev(valueSet2,getMean(valueSet2)));
    }

    public static double getCovariance(double[] valueSet1, double[] valueSet2)
    {
        if(valueSet1.length != valueSet2.length)
            throw new IllegalArgumentException("the two value sets have different sizes! ("+valueSet1.length+","+valueSet2.length+")");
        double s1m = getMean(valueSet1);
        double s2m = getMean(valueSet1);
        double cov = 0;
        for(int i=0;i<valueSet1.length;i++)
            cov += (valueSet1[i]-s1m)*(valueSet2[i]-s2m);
        return cov/(double)(valueSet1.length-1);
    }

    public static double getStdDev(double[] values, double mean)
    {
        return Math.sqrt(getVariance(values,mean));
    }

    public static double getVariance(double[] values, double mean)
    {
        if(values==null || values.length<2)
            return 0;

        double sigma = 0;
        double y;
	for (double d : values)
        {
            y = d - mean;
            sigma += y*y;
        }
        return sigma/(double)(values.length-1);
    }

    public static double getBinomialConfidenceInterval(double p, int size, double confidence)
    {
        /**
         * variance: p*(1-p)
         * standard deviation: p*(1-p)/|N|
         * z based on normal distribution
         *
         * p + z * sqrt(p(1-p)/n)
         *
         * where z = +/- z(1-a/2) for the two-sided 1-a confidence interval
         * where z = -z1-a for the 1-a confidence lower bound
         * where z = +z1-a for the 1-a confidence upper bound
         *
         **/
        double z = getTwoSidedZ(size,1-confidence);
        return z*Math.sqrt(p*(1-p)/(double)size);
    }
    /**
     * Return: 1-sided confidence that err1 < err2 (0 if not significant)
     * @param err1
     * @param size1
     * @param err2
     * @param size2
     * @return 1-sided confidence that err1 < err2 (0 if not significant)
     */
    public static double getSignificanceDifference(double err1, int size1, double err2, int size2)
    {
        // normalize errors to lie in [0:1]
        // ASSUMPTON: err{1,2} are either in 0:100 or 0:1
        if(err1>1||err2>1)
        {
            err1/=100.0;
            err2/=100.0;
        }

        // find standard deviations
        double sigmaDsq = (err1*(1.0-err1))/size1 + (err2*(1-err2))/size2;
        double sigmaD   = Math.sqrt(sigmaDsq);

        if(sigmaD == 0)
            return 0;

        // find diffs
        double diff = (err2-err1)/sigmaD;
        int sz = ((size1 > size2)?size2:size1);

        return ( (diff>0) ? getSignificance(sz,diff) : -getSignificance(sz,-diff) );
    }

    public static double pairedTTest(double[] errors1, double[] errors2)
    {
        if(errors1.length != errors2.length)
            throw new IllegalArgumentException("value sets have different sizes! ("+errors1.length+") and ("+errors2.length+")");

        double avg = 0;
        for(int i=0;i<errors1.length;i++)
        {
            avg += errors1[i] - errors2[i];
        }
        avg /= (double)errors1.length;

        // Now, let's find the standard deviation
        double sigmaDsq = 0;
        for(int i=0;i<errors1.length;i++) {
            double delta = (errors1[i] - errors2[i]) - avg;
            sigmaDsq += delta*delta;
        }
        double sigmaD = Math.sqrt( ( (1/(errors1.length*(errors1.length-1))) * sigmaDsq ) );

        // If $err1 < $err2, then there is no confidence on err1>err2
        if(sigmaD==0)
            return 0;
        avg /= sigmaD;

        return ( (avg>0) ? getSignificance(errors1.length, avg) : -getSignificance(errors1.length, -avg) );
    }
}
