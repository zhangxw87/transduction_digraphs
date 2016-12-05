function [F, Time] = ZFL(W, Y, alpha)
% Zero-mode Free Laplacian
%
% Input: 
%     W: weight matrix of the graph of size n-by-n
%     Y: label matrix of size n-by-k, where k is the number of classes
%     alpha: parameter of controlling singularity
% Output: 
%     F: affinity matrix of size n-by-k
%     Time: CPU time
%
% Reference:
%     H. Wang, C. Ding, and H. Huang. Directed Graph Learning via High-order
%       Co-linkage Analysis. ECML 2010.
%
% Written by Xiaowei Zhang 

if nargin < 3
    alpha = 0.99;
end
t = tic;

% link normalization
n = size(W, 1);
InDegree = sum(W); % a row vector with in-degree
OutDegree = sum(W, 2);
ind = (InDegree > 0);
% W(:, ind) = W(:, ind) ./ repmat(sqrt(InDegree(ind)), n, 1);
W(:, ind) = bsxfun(@rdivide, W(:, ind), sqrt(InDegree(ind)));
ind = (OutDegree > 0);
% W(ind, :) = W(ind, :) ./ repmat(sqrt(OutDegree(ind)), 1, n);
W(ind, :) = bsxfun(@rdivide, W(ind, :), sqrt(OutDegree(ind)));

% second-order similarity
W2 = W * W' + W' * W;

% third-order similarity
W3 = W * (W + W') * W' + W' * (W + W') * W;

% second-order similarity
W4 = W * (W^2 + (W^2)' + W * W') * W' + W' * (W^2 + (W^2)' + W' * W) * W;

alpha1 = sum(W2(:)) - trace(W2);
beta = sum(W3(:)) - trace(W3);
gamma = sum(W4(:)) - trace(W4);

% construct high-order weight matrix
W1 = W2 + (alpha1 / beta) * W3 + (alpha1 / gamma) * W4;

% compute zero-mode free Laplacian
D = diag(sum(W1));
L = D - alpha * W1 + (sum(W1(:)) / n) * ones(n);
L = .5 * (L' + L); % make L symmetric

% F = L^-1 * Y;
F = L \ Y;

Time = toc(t);
end
