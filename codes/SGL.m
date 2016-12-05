function [F, Time] = SGL(W, Y, eta, alpha)
% Symmetrized Graph Laplacian with Teleporting Random Walk
%
% Input: 
%     W: weight matrix of the graph of size n-by-n
%     Y: label matrix of size n-by-k, where k is the number of classes
%    eta: parameter of controlling teleporting
%     alpha: parameter of absorbing rate
%
% Output: 
%     F: affinity matrix of size n-by-k
%     Time: CPU time
%
% Reference:
%     D. Zhou, J. Huang, and B. Scholkopf. Learning from Labeled and Unlabeled
%         Data on a Directed Graph. ICML 2005.
%
% Written by Xiaowei Zhang 

if nargin < 3
    eta = 0.01;
end

if nargin < 4
    alpha = 0.1;
end

t = tic;
% construct transision probability matrix
n = size(W, 1);
degree = sum(W,2); % outdegree
P = W;
ind = (degree > 0);
% P(ind, :) = P(ind, :) ./ repmat(degree(ind), 1, n);
P(ind, :) = bsxfun(@rdivide, P(ind, :), degree(ind));
P(~ind, :) = 1 / n;

% add teleportation
P = eta * P + (1 - eta) * ones(n) / n;

% compute stationary distribution
[v, ~] = eigs(P', 1); % eigenvector corresponding to the largest eigenvalue
if all(sign(v) == -1)
    v = -v;
end
v = v / sum(v);

PI_half = diag(sqrt(v));
PI_half_inv = diag(1 ./ sqrt(v));

Theta = PI_half * P * PI_half_inv;
Theta = 0.5 * (Theta + Theta');

% F = (eye(n) - alpha * Theta)^-1 * Y;
F = (eye(n) - alpha * Theta) \ Y;

Time = toc(t);
end
