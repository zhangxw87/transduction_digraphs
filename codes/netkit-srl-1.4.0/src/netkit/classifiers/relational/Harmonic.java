/**
 * Harmonic.java
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
package netkit.classifiers.relational;

import netkit.classifiers.*;
import netkit.graph.*;
import netkit.util.*;

import java.util.Arrays;

/**
 * The Harmonic Function classifier from Zhu (2003)
 *
 * <B>Reference:</B>
 * <UL>
 * <LI> Zhu, X., Ghahramani, Z., & Lafferty, J. (2003).<BR>
 *      Semi-supervised learning using Gaussian fields and harmonic functions.<BR>
 *      The 20th International Conference on Machine Learning (ICML).
 * </UL>
 * 
 * This takes no parameters and does not use collective inference.  It uses only
 * known labels and the graph.
 *
 * @author Sofus A. Macskassy
 */

public class Harmonic extends NetworkClassifierImp 
{
	private Matrix fuM;
	private Matrix Iuu;
	private int[] rumap;
	private Node[] fumap; 
	private double[] ERM = null; // computed ERMs based on last induced model
	private double[] acc = null; // computed accuracies based on last induced model 

	private Estimate predictions = null;
	private String[] classes = null;

	public String getShortName() {
		return "Harmonic";
	}
	public String getName() {
		return "Harmonic function with Gaussian Random Field";
	}
	public String getDescription() {
		return "See reference: Zhu, X., Ghahramani, Z., and Lafferty, J. (2003),  \"Semi-supervised learning using Gaussian fields and harmonic functions,\" The 20th International Conference on Machine Learning (ICML).'";
	}



	/**
	 * This initializes Harmonic function for the next collective inference iteration by
	 * doing absolutely nothing.
	 *
	 * @param currPrior The current priors for all nodes in the graph
	 * @param unknowns The list of nodes whose labels are unknown
	 */
	public void initializeRun(Estimate currPrior, Node[] unknowns) {}

	/**
	 * Returns the prediction computer in the induceModel call.
	 * @param node
	 * @param estimation
	 * @return true, if the node was part of the original test set, false otherwise.
	 */
	public boolean doEstimate(Node node, double[] estimation) {
		double[] val = predictions.getEstimate(node);
		if(val != null)
		{
			logger.finest(getName()+" doEstimate("+node.getName()+") = "+ArrayUtil.asString(val));
			System.arraycopy(val, 0, estimation, 0, estimation.length);
			return true;
		}
		else
		{
			logger.finest(getName()+" doEstimate("+node.getName()+") = NULL!");
		}
		return false;
	}



	public double getERM(Node n) {
		return getERM(n,null)[0];
	}

	private double computeERM(Node n, double[] uuK, double[] probK) {
		if(!Double.isNaN(ERM[n.getIndex()]))
			return ERM[n.getIndex()];

		logger.fine(getName()+" computeERM "+n.getName());
		// precompute pre_maxfplus for node k
		// matlab code for the whole array:
		// for c=1:nC
		//   if (c==1)
		//     pre_maxfplus = repmat(f(:,c), 1, u) + repmat(-f(:,c)', u, 1).*(invDeltaU ./ repmat(diag(invDeltaU)', u, 1));
		//   else
		//     pre_maxfplus = max(pre_maxfplus, repmat(f(:,c), 1, u) + repmat(-f(:,c)', u, 1).*(invDeltaU ./ repmat(diag(invDeltaU)', u, 1)));
		//   end
		// end
		// risk = zeros(1, u); % the risk of querying point k
		// for yk=1:nC
		//   risk = risk + sum(1- max(repmat(f(:,c), 1, u) + repmat(1-f(:,c)', u, 1).*(invDeltaU ./ repmat(diag(invDeltaU)', u, 1)) , pre_maxfplus)) .* f(:,c)';
		// end
		//
		// where nC = number classes, f=the learned function = predictions in my case

		// I've decomposed it into a per-node computation to save space + computation (smaller constant in front)

		double[][] fum = fuM.getMatrix();
		double[] premaxfplus = new double[fum.length];
		Arrays.fill(premaxfplus,Double.NEGATIVE_INFINITY);
		StringBuffer sb;
		int k = rumap[n.getIndex()];

		for(int c=0;c<classes.length;c++)
		{
			sb = new StringBuffer(fumap[k].getName()+": premax["+c+"]=");
			for(int i=0;i<fum.length;i++)
			{
				double[] fcp = predictions.getEstimate(fumap[i]);
				if(fcp==null)
				{
					logger.warning("Could not get predictions[2] for node="+fumap[i].getName());
					continue;
				}
				double val = fcp[c] - (probK[c] * uuK[i]);
				premaxfplus[i] = Math.max(premaxfplus[i], val);
				sb.append("["+fcp[c]+"+(-"+probK[c]+"*"+uuK[i]+")="+val+"]");
			}
			logger.finest(sb.toString());
		}

		sb = new StringBuffer(fumap[k].getName()+": premax=");
		for(int i=0;i<fum.length;i++)
		{
			sb.append("["+premaxfplus[i]+"]");
		}
		logger.finest(sb.toString());

		double risk = 0;
		for(int c=0;c<classes.length;c++)
		{
			double fsum = 0;
			sb = new StringBuffer(fumap[k].getName()+": neg["+c+"]=");
			for(int j=0;j<fum.length;j++)
			{
				double[] fcp = predictions.getEstimate(fumap[j]);
				if(fcp==null)
				{
					logger.warning("Could not get predictions [3] for node="+fumap[j].getName());
					continue;
				}
				double neg = fcp[c] + (1.0D - probK[c]) * uuK[j];
				sb.append("["+neg+"]");
				fsum += Math.max(premaxfplus[j], neg);
			}
			sb.append(" tot="+(fum.length - fsum));
			logger.finest(sb.toString());
			risk += probK[c] * (fum.length - fsum);
		}
		logger.finest(fumap[k].getName()+" risk="+risk);
		ERM[n.getIndex()] = risk;
		return risk;
	}

	private double computeAccuracy(Node n, Classification truth, double[] uuK, double[] probK) {
		if(!Double.isNaN(acc[n.getIndex()]))
			return acc[n.getIndex()];
		if(truth==null)
			return Double.NaN;

		if(truth.isUnknown(n))
		{
			logger.warning("Cannot get true class for node "+n.getName());
			return Double.NaN;
		}

		double numC = 0;
		double numP = 0;      
		int trueC = truth.getClassValue(n);
		double[][] fum = fuM.getMatrix();

		for(int i=0;i<fum.length;i++)
		{
			double[] fcp = predictions.getEstimate(fumap[i]);
			if(fcp==null)
			{
				logger.warning("Could not get predictions[2] for node="+fumap[i].getName());
				continue;
			}
			if(truth.isUnknown(fumap[i]))
			{
				logger.warning("Could not get true class[2] for node="+fumap[i].getName());
				continue;
			}
			int truePred = truth.getClassValue(fumap[i]); 
			double max = Double.NEGATIVE_INFINITY;
			double maxC = -1;
			for(int c=0;c<classes.length;c++)
			{
				double delta = ((c==trueC) ? 1.0D : 0 ) - probK[c]; 
				double val = fcp[c] + delta * uuK[i];
				if(val > max)
				{
					maxC = c;
					max = val;
				}
			}
			numP++;
			if(maxC == truePred) numC++;
		}   
		acc[n.getIndex()] = numC/numP;
		return acc[n.getIndex()];
	}

	public double[] getERM(Node n, Classification truth) {
		double[] result = new double[]{ERM[n.getIndex()],acc[n.getIndex()]};

		// I here follow Zhu's matlab code from:
		// http://www.cs.cmu.edu/~zhuxj/pub/semisupervisedcode/active_learning/active_learning.m
		if(fuM == null)
			throw new IllegalStateException(getName()+" has not yet been induced!");
		int k = rumap[n.getIndex()];

		logger.finer(getName()+": getting ERM score for node "+n.getName());

		if(k==-1)
		{
			logger.warning("getERM("+n.getName()+") - this is not an unlabeled node?!");
			return result;
		}

		double[] probK = predictions.getEstimate(fumap[k]);
		if(probK==null)
		{
			logger.warning("Could not get predictions[1] for node="+fumap[k].getName());
			return result;
		}

		// compute nG for column k, where nG(i,k) = G(i,k)/G(i,i), where G = invDelta = Iuu matrix
		double[] uuK = new double[Iuu.getYdim()];
		for(int i=0;i<uuK.length;i++)
			uuK[i] = Iuu.getMatrix()[i][k];
		VectorMath.divide(uuK, uuK[k]);

		result[1] = computeAccuracy(n, truth, uuK, probK);
		result[0] = computeERM(n, uuK, probK);

		logger.fine(getName()+": ERM score for node "+n.getName()+" = "+ArrayUtil.asString(result));

		return result;
	}


	/**
	 * Harmonic has no model per se as its learning consists of computing the
	 * harmonic function which results in the predictions.  Thus, this call
	 * creates the predictions for all the instances in the split test set.
	 *
	 * @param graph The graph to induce a model over
	 * @param split The data split identifying which nodes have known and unknown labels
	 *
	 * @see NetworkClassifierImp#induceModel(netkit.graph.Graph, netkit.classifiers.DataSplit)
	 */
	public void induceModel(Graph graph, DataSplit split) {
		super.induceModel(graph, split);
		ERM = new double[graph.numNodes(split.getView().getNodeType())];
		acc = new double[graph.numNodes(split.getView().getNodeType())];
		Arrays.fill(ERM,Double.NaN);
		Arrays.fill(acc,Double.NaN);

		classes = split.getView().getAttribute().getTokens();
		label(split);
		for(Node n : predictions)
			logger.finest("induceModel->predict["+n.getName()+"] : "+ArrayUtil.asString(predictions.getEstimate(n)));
	}


	private void printMatrix(Matrix m, String fn) {
		if(!logger.isLoggable(java.util.logging.Level.FINEST))
			return;

		try
		{
			java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.File(fn));
			m.print(pw);
			pw.close();
		}
		catch(java.io.IOException ioe) {}
	}

	private void label(DataSplit split) {
		logger.fine(getName()+" label() called");
		Graph g = split.getView().getGraph();
		GraphMetrics gm = g.getMetrics();
		int nc = gm.getNumComponents();
		int[] inc = new int[nc+1];
		Arrays.fill(inc,-1);

		int graphSize = g.numNodes(split.getView().getNodeType());

		Node[] flmap = new Node[split.getTrainSetSize()];
		int[] rlmap = new int[graphSize];
		Arrays.fill(flmap,null);
		Arrays.fill(rlmap,-1);

		logger.finer(getName()+" - label() - creating flmap (num components="+nc+") (trainsize="+split.getTrainSetSize()+") (totsize="+graphSize+")");
		int numLabeled = 0;
		for(Node n : split.getTrainSet())
		{
			int cnum = gm.getComponent(n);
			if(cnum==-1)
			{
				logger.warning("Training node["+n.getName()+"] does not belong to a cluster!");
				continue;
			}
			if(n.getWeightedDegree()==0)
			{
				logger.warning("Training node["+n.getName()+"] does not have any edges!");
				continue;
			}
			inc[cnum]++;
			int i = n.getIndex();
			rlmap[i] = numLabeled;
			flmap[numLabeled] = n;
			logger.finest("Adding "+n.getName()+" to flmap");
			numLabeled++;
		}

		// let's count the number of unlabeled
		// nodes that are part of active components
		fumap = new Node[split.getUnknownSetSize()];
		rumap = new int[graphSize];
		Arrays.fill(fumap,null);
		Arrays.fill(rumap,-1);

		logger.finer(getName()+" - label() - creating fumap");
		int numPredict = 0;
		for(Node n : split.getUnknownSet())
		{
			int cnum = gm.getComponent(n);
			if(cnum==-1)
			{
				logger.warning("Test node["+n.getName()+"] does not belong to a cluster!");
				continue;
			}
			// if this belongs to a component that is not 'active', then ignore it
			if(inc[cnum] == -1)
			{
				logger.finest("Pruning "+n.getName()+" from fumap - not in active cluster");
				continue;
			}

			int i = n.getIndex();
			rumap[i] = numPredict;
			fumap[numPredict] = n;
			logger.finest("Adding "+n.getName()+" to fumap");

			numPredict++;
		}
		int numNodes = numLabeled+numPredict;

		Matrix W = new Matrix(numNodes,numNodes);
		double[][] w = W.getMatrix();

		logger.finer(getName()+" - label() - creating W [numLabeled="+numLabeled+"][numPredict="+numPredict+"]");
		for(String edgeName : g.getEdgeTypeNames(nodeType, nodeType))
		{
			for(Edge e : g.getEdges(edgeName))
			{
				int i1 = e.getSource().getIndex();
				int i2 = e.getDest().getIndex();
				i1 = ( (rlmap[i1]!=-1) ? rlmap[i1] : ((rumap[i1]==-1)?-1:(rumap[i1]+numLabeled) ) );
				i2 = ( (rlmap[i2]!=-1) ? rlmap[i2] : ((rumap[i2]==-1)?-1:(rumap[i2]+numLabeled) ) );
				if(i1 == -1 || i2 == -1)
					continue;
				w[i1][i2] += e.getWeight();
				w[i2][i1] += e.getWeight();
			}
		}
		logger.finer(getName()+" - label(): initialized W");
		printMatrix(W,"zhu-W-matrix");

		Matrix D = W.getDiagonal();
		logger.finer(getName()+" - label(): Created D");
		printMatrix(D,"zhu-D-matrix");

		Matrix L = D.subtract(W);
		logger.finer(getName()+" - label(): Created L = D-W");
		printMatrix(L,"zhu-L-matrix");

		Matrix Luu = L.submatrix(numLabeled,numPredict,numLabeled,numPredict);
		printMatrix(Luu,"zhu-Luu-matrix");

		Matrix Wul = W.submatrix(numLabeled,numPredict,0,numLabeled);
		logger.finer(getName()+" - label(): Created Luu and Lul");
		printMatrix(Wul,"zhu-Wul-matrix");

		Iuu = Luu.invert();
		logger.finer(getName()+" - label(): Created Luu^{-1} dim="+Iuu.getXdim()+"x"+Iuu.getYdim());
		printMatrix(Iuu,"zhu-Iuu-matrix");

		Matrix flM = new Matrix(classes.length,numLabeled);
		double[][] flm = flM.getMatrix();
		Classification truth = split.getView().getTruth();
		for(int i=0;i<flm.length;i++)
		{
			flm[i][truth.getClassValue(flmap[i])] = 1.0D;
		}
		logger.finer(getName()+" - label(): Created fl");
		printMatrix(flM,"zhu-fl-matrix");

		fuM = Iuu.multiply(Wul).multiply(flM);
		logger.finer(getName()+" - label(): Created fu");
		printMatrix(fuM,"zhu-fu-matrix");

		double[][] fum = fuM.getMatrix();
		predictions = new Estimate(g,nodeType,split.getView().getAttribute());
		for(int i=0;i<fum.length;i++)
		{
			if(fumap[i] == null)
			{
				logger.info("No prediction fumap["+i+"] (out of "+fum.length+")");
				continue;
			}
			logger.info("Adding prediction for node="+fumap[i].getName()+" rumap["+i+"] (out of "+fum.length+")");
			predictions.estimate(fumap[i], fum[i]);
		}
	}

	public String toString() {
    	StringBuffer sb = new StringBuffer();
    	sb.append(getName()+" (Relational Classifier)").append(NetKitEnv.newline);
    	sb.append("-------------------------------------").append(NetKitEnv.newline);
    	sb.append("[[ MATRIX ]]").append(NetKitEnv.newline);
    	return sb.toString();
	}
}

