function accuracy = microAC(label_true, F, covered_nodes_ind, flag)
% Compute micro-averaged accuracy for predictor F 
%
% Input: 
%     label_true: n x 1 column vector recording true labels
%     F: n x k matrix recording the predictions
%     covered_nodes_ind: the indices of covered data by the labeled data
%     flag: 0 means without class mass normalization
%           nonzero means with class mass normalization
% Output:
%     accuracy: a scalar in [0, 1]
%
% Written by Xiaowei Zhang 

if nargin < 4
    flag = 0;
end

n = length(label_true);
c = size(F, 2);
if n ~= size(F, 1)
    error('incosistent size!')
end

% compute class priors
w = ones(1,c);
if flag ~= 0
    for i=1:c
        w(i) = sum(label_true == i) / n;
    end
end

% compute class mass
d = sum(F, 2);
F = F ./ repmat(d, 1, c);
F = F .* repmat(w, n, 1);

% compute predicted labels
[~, label_pred] = max(F,[],2);

% compute prediction accuracy on labelled and unlabelled data
label_dev = (label_pred == label_true);
accuracy = sum(label_dev(covered_nodes_ind)) / length(covered_nodes_ind);
