/**
 * Matrix.java
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
package netkit.util;

import java.util.Arrays;
import java.io.File;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Simple Matrix mathematics in support for the Harmonic function.  
 *
 * @author Sofus A. Macskassy
 */
public class Matrix
{
  private int xdim;
  private int ydim;
  private double M[][];

  public Matrix(double[][] m) {
    M = m;
    if(m==null)
      throw new IllegalArgumentException("null matrix given!");
    ydim = m.length;
    xdim = -1;
    for(int y=0;y<ydim;y++)
    {
      if(M[y]==null)
        throw new IllegalArgumentException("row["+y+"] is null!");
      if(y==0) xdim = M[y].length;
      if(xdim != M[y].length)
        throw new IllegalArgumentException("row["+y+"] is different length than previous rows ("+xdim+")");
    }
  }

  public Matrix(int xdim, int ydim) {
    this(xdim,ydim,false);
  }

  public Matrix(int xdim, int ydim, boolean identity) {
    if(xdim<1 || ydim < 1)
      throw new IllegalArgumentException("x(="+xdim+") and y(="+ydim+") must both be positive!");
    M = new double[ydim][];
    for(int y=0;y<ydim;y++)
    {
      M[y] = new double[xdim];
      Arrays.fill(M[y],0D);
      if(identity)
        M[y][y] = 1.0D;
    }
    this.xdim = xdim;
    this.ydim = ydim;
  }

  public boolean isSymmetric() {
    if(!isSquare())
      return false;

    for(int y=0;y<ydim;y++)
      for(int x=y+1;x<xdim;x++)
        if(M[x][y] != M[y][x])
          return false;
    return true;
  }


  public boolean isSquare() {
    return xdim == ydim;
  }

  public int getXdim() {
    return xdim;
  }

  public int getYdim() {
    return ydim;
  }

  public double[][] getMatrix() {
    return M;
  }

  public Matrix getDiagonal() {
    Matrix dM = new Matrix(xdim,ydim);
    double[][] D = dM.M;
    for(int y=0;y<ydim;y++)
    {
      for(int x=0;x<xdim;x++)
        D[y][y]+=M[y][x];
      if(D[y][y] == 0)
        System.err.println("WARNING - row["+y+"] has no edges!");
    }
    return dM;
  }

  public Matrix multiply(Matrix m) {
    if(xdim != m.ydim)
      throw new ArithmeticException("Cannot multiply matrices---their dimensions (["+xdim+","+ydim+"] and ["+m.xdim+","+m.ydim+"]) do not match");
    Matrix rM = new Matrix(m.xdim,ydim);
    double[][] R = rM.M;
    for(int ry=0;ry<ydim;ry++)
    {
      for(int rx=0;rx<m.xdim;rx++)
      {
        double val = 0;
        for(int i=0;i<xdim;i++)
          val+=M[ry][i]*m.M[i][rx];
        R[ry][rx] = val;
      }
    }
    return rM;
  }

  public Matrix add(Matrix m) {
    if(xdim != m.xdim || ydim != m.xdim)
      throw new ArithmeticException("Cannot add matrices---their dimensions (["+xdim+","+ydim+"] and ["+m.xdim+","+m.ydim+"]) do not match");
    Matrix rM = new Matrix(xdim,ydim);
    double[][] R = rM.M;
    for(int y=0;y<ydim;y++)
      for(int x=0;x<xdim;x++)
        R[y][x] =  M[y][x] + m.M[y][x];
    return rM;
  }

  public Matrix subtract(Matrix m) {
    if(xdim != m.xdim || ydim != m.xdim)
      throw new ArithmeticException("Cannot subtract matrices---their dimensions (["+xdim+","+ydim+"] and ["+m.xdim+","+m.ydim+"]) do not match");
    Matrix rM = new Matrix(xdim,ydim);
    double[][] R = rM.M;
    for(int y=0;y<ydim;y++)
      for(int x=0;x<xdim;x++)
        R[y][x] =  M[y][x] - m.M[y][x];
    return rM;
  }

  public Matrix invert() {
    Matrix rM = new Matrix(xdim,ydim,true);
    double[][] R = rM.M;
    Matrix cM = (Matrix)clone();
    double[][] C = cM.M;
    //PrintWriter pw = new PrintWriter(System.err);
    //pw.println("Inverting matrix");
    for(int y=0;y<ydim;y++)
    {
      //pw.println("Row "+y);
      //cM.print(pw);
      //pw.println("------------------------------------");
      //rM.print(pw);

      // pivot?
      if(C[y][y] == 0) {
        int p = -1;
        for(int i=y+1;i<ydim&&p==-1;i++)
          if(C[i][y] != 0) p=i;
        if(p==-1) {
          cM.print(new PrintWriter(System.err));
          throw new ArithmeticException("Cannot invert matrix on row "+y);
        }
        //pw.println("Pivot row: "+p);

        double[] r = C[y];
        C[y] = C[p];
        C[p] = r;

        r = R[y];
        R[y] = R[p];
        R[p] = r;

        //cM.print(pw);
        //pw.println("------------------------------------");
        //rM.print(pw);
      }

      // normalize to make C[y][y] == 1
      if(C[y][y] != 1.0) {
        double div = C[y][y];
        for(int x=0;x<xdim;x++)
        {
          C[y][x] /= div;
          R[y][x] /= div;
        }
      }

      // make column [y] 0 in all other rows
      for(int yp=0;yp<ydim;yp++)
      {
        if(y==yp || C[yp][y]==0)
          continue;
        double mul=-C[yp][y];
        for(int x=0;x<y;x++)
          R[yp][x] += mul*R[y][x];
        for(int x=y;x<xdim;x++)
        {
          C[yp][x] += mul*C[y][x];
          R[yp][x] += mul*R[y][x];
        }
        //if(C[y][y] == 0.0) pw.println("WARNING - Row "+y+" has 0 diagonal");
      }
    }
    //pw.println("Final");
    //cM.print(pw);
    //pw.println("------------------------------------");
    //rM.print(pw);
    //pw.close();
    return rM;
  }

  public Matrix solve() {
    if(xdim!=(ydim+1))
      throw new ArithmeticException("Cannot solve linear system---maxtrix must be an Nx(N+1) matrix and not an "+xdim+" by "+ydim+" matrix");

    Matrix cM = (Matrix)clone();
    double[][] C = cM.M;
    for(int y=0;y<ydim;y++)
    {
      if(C[y][y] == 0) {
        int p = -1;
        for(int i=y+1;i<ydim&&p==-1;i++)
          if(C[i][y] != 0) p=i;
        if(p==-1) {
          cM.print(new PrintWriter(System.err));
          throw new ArithmeticException("Cannot invert matrix on row "+y);
        }
        double[] r = C[y];
        C[y] = C[p];
        C[p] = r;
      }

      // normalize to make C[y][y] == 1
      if(C[y][y] != 1.0) {
        double div = C[y][y];
        for(int x=0;x<xdim;x++)
          C[y][x] /= div;
      }

      // make column [y] 0 in all other rows
      for(int yp=0;yp<ydim;yp++)
      {
        if(y==yp || C[yp][y]==0)
          continue;
        double mul=-C[yp][y];
        for(int x=y;x<xdim;x++)
          C[yp][x] += mul*C[y][x];
      }
    }
    return cM;
  }

  public Object clone() {
    Matrix c = new Matrix(xdim,ydim);
    double[][] m = c.M;
    for(int i=0;i<ydim;i++)
      System.arraycopy(M[i],0,m[i],0,xdim);
    return c;
  }

  public Matrix submatrix(int startRow,int numRow,int startCol,int numCol) {
    Matrix rM = new Matrix(numCol,numRow);
    double[][] r = rM.M;
    for(int y=startRow,ry=0;ry<numRow;y++,ry++)
      System.arraycopy(M[y],startCol,r[ry],0,numCol);
    return rM;
  }

  private static NumberFormat nf;
  static {
    nf = NumberFormat.getNumberInstance();	
    nf.setMinimumFractionDigits(3);
    nf.setMaximumFractionDigits(3);
  }

  private static Matrix readMatrix(File f) {
    int xdim=0;
    int ydim=0;
    ArrayList<double[]> rows = new ArrayList<double[]>();
    int line=0;
    double[][] m = new double[0][0];
    Pattern p = Pattern.compile("\\s+");
    try
    {
      BufferedReader br = new BufferedReader(new FileReader(f));
      for(String s=br.readLine(); s!=null; s=br.readLine())
      {
        line++;
        String[] vars = p.split(s.trim());
        if(xdim == 0)
          xdim = vars.length;
        if(xdim!=vars.length)
          throw new RuntimeException(f+":"+line+" - ["+s+"] does not match number of entries with previous lines");
        double[] x = new double[vars.length];
        for(int i=0;i<vars.length;i++)
          x[i] = Double.parseDouble(vars[i]);
        rows.add(x);
      }
      br.close();
      ydim=rows.size();
      m = new double[ydim][];
      for(int i=0;i<ydim;i++)
        m[i] = (double[])rows.get(i);
    }
    catch(IOException ioe)
    {
      throw new RuntimeException(f+":"+line+" - "+ioe.getMessage(),ioe);
    }
    return new Matrix(m);
  }

  public void print(PrintWriter pw) {
    int maxx = 0;
    for(int y=0;y<ydim;y++)
      for(int x=0;x<xdim;x++)
      {
        int val = Math.abs((int)M[y][x]);
        if(val > maxx)
          maxx = val;
      }
    int ispace=1;
    for(int i=10;i<10000000 && maxx>=i;i*=10)
      ispace++;
    ispace++;

    for(int y=0;y<ydim;y++)
    {
      for(int x=0;x<xdim;x++)
      {
        int val = Math.abs((int)M[y][x]);
        int pad = ispace;
        if(M[y][x]<0) pad--;
        for(int i=10;i<10000000 && val>=i;i*=10)
          pad--;
        for(int i=0;i<pad;i++)
          pw.print(" ");
        pw.print(nf.format(M[y][x]));
      }
      pw.println();
    }
  }

  private static void usage(String msg) {
    System.out.println("usage: Matrix <command> file1 [file2]");
    System.out.println("command is one of:");
    System.out.println("   print     - print matrix in file1");
    System.out.println("   invert    - invert matrix in file1");
    System.out.println("   solve     - solve linear equations in matrix in file1");
    System.out.println("   add       - add matrices in file1 and file2");
    System.out.println("   subtract  - subtract matrix in file2 from matrix in file1");
    System.out.println("   multiply  - multiply matrices in file1 and file2");
    if(msg != null) {
      System.out.println();
      System.out.println(msg);
    }
    System.exit(0);
  }
  public static void main(String[] args) {
    if(args.length<2)
      usage(null);
    Matrix m1 = readMatrix(new File(args[1]));
    Matrix m2 = ((args.length>2)?readMatrix(new File(args[2])):null);
    PrintWriter pw = new PrintWriter(System.out,true);
    if(args[0].equalsIgnoreCase("print"))
      m1.print(pw);
    else if(args[0].equalsIgnoreCase("invert"))
      m1.invert().print(pw);
    else if(args[0].equalsIgnoreCase("solve"))
      m1.solve().print(pw);
    else if(args[0].equalsIgnoreCase("add")) {
      if(m2==null) usage("command "+args[0]+" needs two matrices");
      m1.add(m2).print(pw);
    }
    else if(args[0].equalsIgnoreCase("subtract")) {
      if(m2==null) usage("command "+args[0]+" needs two matrices");
      m1.subtract(m2).print(pw);
    }
    else if(args[0].equalsIgnoreCase("multiply")) {
      if(m2==null) usage("command "+args[0]+" needs two matrices");
      m1.multiply(m2).print(pw);
    }
    pw.close();
  }
}
