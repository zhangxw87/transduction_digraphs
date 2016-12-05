# Transduction on Directed Graphs via Absorbing Random Walks

This is a collection of MATLAB codes that implement our algorithm and comparison methods in the above paper.


## About
In this collection, we include implementations of 11 algorithms, among which we implemented the following algorithms:
+ Our own algorithm proposed in [1],
+ Digraph variant of the Commute Time Kernel classifier (CTKd) [2],
+ Regularized Commute Time Kernel classifier (RCTKd) [2],
+ Symmetrized Graph Laplacian (SGL) [3],
+ Zero-mode Free Laplacian (ZFL) [4],
+ Biased Discriminative random Walks (bDWALK) [5].

For the following methods:
+ Network-only Bayes Classifier (NBC) [6],
+ Network-only Link Based classifier (NLB) [7],
+ Class Distribution Relational Neighbor classifier (CDRN) [8],
+ Weighted Vote Relational Neighbor classifier (WVRN) [8],

we adopted the implementation by NetKit [8] in Java. Finally, the implementation of 
+ Sum Over Path covariance kernel (SOP) [9]

is borrowed from the authors of [9].

## Folders
+ `codes/`: contains all the source codes implementing the above 11 algorithms
+ `data/`: default path to local files storing datasets
+ `results/`: stores the computed solutions and total running time
+ `test/`: contains basic test codes. There are several subfolders, each of which corresponds to an individual dataset.


## Example
Take CiteseerX dataset for example. Proceed to folder `test/CiteseerX/`, and execute `run_citeseerX.m` in the command window of MATLAB. 

## Reference
[1] Jaydeep De, Xiaowei Zhang, Feng Lin, and Li Cheng, [Transduction on Directed Graphs via Absorbing Random Walks](), submitted, 2015.

[2] F. Fouss, K. Francoisse, L. Yen, A. Pirotte, and M. Saerens, [An
experimental investigation of kernels on graphs for collaborative recommendation and semisupervised classification](http://www.sciencedirect.com/science/article/pii/S0893608012000822), Neural Network, vol. 31, pp. 53 -- 72, 2012.

[3] D. Zhou, J. Huang, and B. Sch ̈olkopf, [Learning from labeled and
unlabeled data on a directed graph](http://research.microsoft.com/en-us/um/people/denzho/papers/LLUD.pdf), ICML, 2005.

[4] H. Wang, C. Ding, and H. Huang, [Directed graph learning via high-order co-linkage analysis](http://inside.mines.edu/~huawang/Papers/Conference/2010ecmlpkdd_directed_graph_high_order.pdf), ECML, 2010.

[5] A. Mantrach, N. van Zeebroeck, P. Francq, M. Shimbo, H. Bersini, and
M. Saerens, [Semi-supervised classification and betweenness computa-
tion on large, sparse, directed graphs](http://www.sciencedirect.com/science/article/pii/S0031320310005467), Pattern Recognition, vol. 44, no. 6, pp. 1212 -- 1224, 2011.

[6] S. Chakrabarti, B. Dom, and P. Indyk, [Enhanced hypertext categorization using hyperlinks](http://dl.acm.org/citation.cfm?id=276332), SIGMOD, 1998.

[7] Q. Lu and L. Getoor, [Link-based classification](http://www.umiacs.umd.edu/~getoor/Publications/icml03.pdf), ICML, 2003.

[8] S. Macskassy and F. Provost, [Classification in networked data: A toolkit and a univariate case study](http://www.jmlr.org/papers/volume8/macskassy07a/macskassy07a.pdf), JMLR, vol. 8, pp. 935 -- 983, 2007.

[9] A. Mantrach, L. Yen, J. Callut, K. Franc ̧oisse, M. Shimbo, and M. Saerens, [The sum-over-paths covariance kernel: A novel covariance measure between nodes of a directed graph](http://ieeexplore.ieee.org/abstract/document/4815265/), IEEE Trans. PAMI, vol. 32, no. 6, pp. 1112 -- 1126, 2010.

## Ackownledgement
+ All datasets used in this collection are publicly available. All credibility of those datasets shall go to the respective authors. We have acknowledged this in the paper.
