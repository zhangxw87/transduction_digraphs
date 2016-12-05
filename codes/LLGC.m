function [F, Time] = LLGC(W, Y, alpha)
% Learning with Local and Global Consistency
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
%     D. Zhou, O. Bousquet, and T. Navin Lal, J. Weston and B. Scholkopf. 
%          Learning with Local and Global Consistency, NIPS 2013.
%
%
% Written by Xiaowei Zhang 

if nargin < 3
    alpha = 0.99;
end

t = tic;

% construct transision probability matrix
n = size(W, 1);
degree = sum(W); % a row vector with in-degree
ind = (degree > 0);

% S = D^(-1/2) * W * D^(-1/2)
W(:, ind) = bsxfun(@rdivide, W(:, ind), sqrt(degree(ind)));
W(ind, :) = bsxfun(@rdivide, W(ind, :), sqrt(degree(ind))');

% F = (speye(n) - alpha * W')^-1 * Y;
F = (speye(n) - alpha * W) \ Y;

Time = toc(t);
end
