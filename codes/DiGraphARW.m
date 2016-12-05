function [F, Time] = DiGraphARW(W, Y, alpha)
% Transduction on Directed Graphs via Absorbing Random Walks
%
% Input: 
%     W: weight matrix of the graph of size n-by-n
%     Y: label matrix of size n-by-k, where k is the number of classes
%     alpha: parameter of absorbing rate
%
% Output: 
%     F: affinity matrix of size n-by-k
%     Time: CPU time
%
% Reference:
%     J. De, X. Zhang, L. Cheng, and F. Lin. Transduction on Directed 
%         Graphs via Absorbing Random Walks.
%
% Written by Xiaowei Zhang 

if nargin < 3
    alpha = 0.1;
end

t = tic;

% construct transision probability matrix
n = size(W, 1);
degree = sum(W); % a row vector with in-degree
P = W;
ind = (degree > 0);
% P(:, ind) = P(:, ind) ./ repmat(degree(ind), n, 1);
P(:, ind) = bsxfun(@rdivide, P(:, ind), degree(ind));

% F = (speye(n) - alpha * P')^-1 * Y;
F = (speye(n) - alpha * P') \ Y;

Time = toc(t);
end
