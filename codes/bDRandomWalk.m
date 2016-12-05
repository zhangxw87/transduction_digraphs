function [F, Time] = bDRandomWalk(W, Y, MaxIter)
% Biased Discriminant Random Walks
%
% Input: 
%     W: weight matrix of the directed graph containing non-negative weights
%     Y: label matrix of size n-by-k, where k is the number of classes
%     MaxIter: maximum number of itersations
%
% Output: 
%     F: affinity matrix of size n-by-k
%     Time: CPU time
%
% Reference:
%     A. Mantrach, N. van Zeebroeck, P. Francq, M. Shimbo, H. Bersini, and
%     M. Saerens. Semi-supervised classification and betweenness
%     computation on large, sparse, directed graphs. Pattern Recognition,
%     2011.
%
% Written by Xiaowei Zhang 
if nargin < 3
    MaxIter = 50;
end
 
[nr,nc] = size(W);   
if (nr ~= nc)
    fprintf('ERROR: The adjacency matrix is not square !\n');
    return;
end;

t = tic;

% compute betweenness matrix
NumClass = size(Y, 2);
F = zeros(nr, NumClass);
for i = 1:NumClass
    % backward vector
    yc = Y(:,i);
    beta = zeros(nr, MaxIter+1);
    beta(:,end) = yc;
    for j = MaxIter:-1:1
        beta(:,j) = W * beta(:,j+1) + yc;
    end
    
    % forward vector
    alpha = zeros(nr, MaxIter);
    alpha(:,1) = W' * yc;
    for j = 1:MaxIter-1
        alpha(:,j+1) = W' * alpha(:,j);
    end
    
    F(:,i) = sum(alpha .* beta(:, 2:end), 2) / (yc' * beta(:, 1));
end

Time = toc(t);
end