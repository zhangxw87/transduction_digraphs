function S = bNRWR(W, Y, alpha, MaxIter)
% The Bounded Normalized Random Walk with Restart
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
P = W;
ind = (degree > 0);

P(ind, :) = bsxfun(@rdivide, P(ind,:), degree(ind));

% compute betweenness matrix
NumClass = size(Y, 2);
S = zeros(nr, NumClass);
for i = 1:NumClass
    % backward vector
    y = Y(:,i);
    S(:,i) = y;
    for j = 1:MaxIter
        S(:,i) = alpha * P' * S(:,i) + y;
    end
end

S(ind, :) = bsxfun(@rdivide, S(ind,:), degree(ind));