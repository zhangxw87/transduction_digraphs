function H = aNRL(W, Y, alpha, MaxIter)
% Approximate Normalized, Regularized Laplacian
%
% W is the weight matrix (affinity, adjacency) of the directed graph containing affinities >= 0
% Y is the label matrix
% alpha is a scalar in (0, 1) denoting the probability that the random walker continues his walk.
% MaxIter is the maximum number of itersations
%
% Reference:
%     A. Mantrach, N. van Zeebroeck, P. Francq, M. Shimbo, H. Bersini, and
%     M. Saerens. Semi-supervised classification and betweenness
%     computation on large, sparse, directed graphs. Pattern Recognition,
%     2011.

% t = tic; 
[nr,nc] = size(W);   
if (nr ~= nc)
    fprintf('ERROR: The adjacency matrix is not square !\n');
    return;
end;

degree = sum(W, 2);
L = W;
ind = (degree > 0);

% normalized Laplacian matrix
L(ind, :) = bsxfun(@rdivide, L(ind,:), sqrt(degree(ind)));
L(:, ind) = bsxfun(@rdivide, L(:,ind), sqrt(degree(ind))');

% compute betweenness matrix
NumClass = size(Y, 2);
H = zeros(nr, NumClass);
for i = 1:NumClass
    y = Y(:,i);
    H(:,i) = y;
    for j = 1:MaxIter
        H(:,i) = -alpha * L * H(:,i) + y;
    end
end