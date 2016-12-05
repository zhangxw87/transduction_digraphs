/**
 * ROC.java
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
 * $Id: ROC.java,v 1.1 2004/12/17 16:00:53 sofmac Exp $
 * Part of the open-source Network Learning Toolkit
 *
 * User: smacskassy
 * Date: Dec 15, 2004
 * Time: 4:55:47 PM
 */
package netkit.util;

import netkit.classifiers.Estimate;
import netkit.classifiers.Classification;
import netkit.graph.Node;

import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.util.zip.GZIPOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.awt.geom.Point2D;

public class ROC {
  private double AUC = 0;
  private Point2D[] points = null;
  private int numPos;
  private int numNeg;

  private final static class Prediction implements Comparable<Prediction> {
    final double score;
    final boolean positive;
    public Prediction(final double score, final boolean positive) {
      this.score = score;
      this.positive = positive;
    }
    public int compareTo(final Prediction prediction) {
      double diff = score - prediction.score;
      return ( (diff<0) ? 1 : ( (diff > 0) ? -1 : 0 ));
    }
  }
  public ROC(Estimate predictions, Classification truth, int posClass) {
    Prediction[] scores = getScores(predictions,truth,posClass);
    if(scores == null)
      points = new Point2D[0];
    else
    {
      points = getPoints(scores);
      calcAUC();
    }
  }
  
  private Prediction[] getScores(Estimate predictions, Classification truth, int posClass) {
    numPos = 0;
    numNeg = 0;
    
    if(predictions.size() < 1)
      return null;
    
    ArrayList<Prediction> lpred = new ArrayList<Prediction>(predictions.size());
    for(Node n : predictions)
    {
      double score = predictions.getScore(n,posClass);
      if(Double.isNaN(score))
        continue;
      
      int tIdx = truth.getClassValue(n);
      if(tIdx == posClass)
        numPos++;
      else
        numNeg++;
      lpred.add(new Prediction(score,(tIdx==posClass)));
    }
    
    if(numPos == 0 || numNeg == 0)
      return null;
    
    return lpred.toArray(new Prediction[0]);
  }
  
  private Point2D[] getPoints(Prediction[] scores) {
    Arrays.sort(scores);
    int[] cat = new int[]{0,0};
    int[] pcat = null;
    Prediction prev = scores[0];
    ArrayList<int[]> list = new ArrayList<int[]>();
    for(Prediction p : scores)
    {
      if(p.score == prev.score)
      {
      }
      else if(pcat == null || (cat[0] == 0 && pcat[0] != 0) || (cat[1] == 0 && pcat[1] != 0))
      {
        list.add(cat);
        pcat = cat;
        cat = new int[]{0,0};
      }
      else
      {
        pcat[0] += cat[0];
        pcat[1] += cat[1];
        cat[0] = cat[1] = 0;
      }
      if(p.positive)
        cat[0]++;
      else
        cat[1]++;
      prev = p;
    }
    if(pcat == null || (cat[0] == 0 && pcat[0] != 0) || (cat[1] == 0 && pcat[1] != 0))
    {
      list.add(cat);
    }
    else
    {
      pcat[0] += cat[0];
      pcat[1] += cat[1];
    }

    ArrayList<Point2D> pts = new ArrayList<Point2D>();
    pts.add(new Point2D.Double(0,0));
    int cumP = 0;
    int cumN = 0;
    for(int[] c : list)
    {
      cumP += c[0];
      cumN += c[1];
      pts.add(new Point2D.Double((double)cumN/(double)numNeg,(double)cumP/(double)numPos));
    }
    return pts.toArray(new Point2D[0]);
  }

  private void calcAUC() {
    if (points == null || points.length == 0)
      return;

    AUC = (points[0].getY() / 2.0) * (points[0].getX());
    for (int i = 1; i < points.length; i++)
    {
      if (points[i - 1].getX() != points[i].getX())
        AUC += ((points[i].getY() + points[i - 1].getY()) / 2.0) * (points[i].getX() - points[i - 1].getX());
    }
    if (points[points.length - 1].getX() != 1)
      AUC += ((1 + points[points.length - 1].getY()) / 2.0) * (1 - points[points.length - 1].getX());
  }

  public Point2D[] getPoints() {
    return points;
  }

  public double getAUC() {
    return AUC;
  }

  public int getNumPos() {
    return numPos;
  }

  public int getNumNeg() {
    return numNeg;
  }

  public void save(java.io.File file) {
    try
    {
      PrintWriter pw;
      if (file.getName().endsWith(".gz"))
        pw = new PrintWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(file))));
      else
        pw = new java.io.PrintWriter(new java.io.FileWriter(file));
      save(pw);
      pw.close();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

  }

  public void save(PrintWriter pw) {
    try
    {
      pw.println("# AUC: " + getAUC());
      for (Point2D point : points)
      {
        pw.print(point.getX());
        pw.print(" ");
        pw.print(point.getY());
        pw.println();
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}
