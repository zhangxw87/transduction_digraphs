function [alpha, MaxIter, time] = aNRL_cv(W, label, alphaSet, MaxIterSet)
% 5-fold cross-validation for bNRWR

NumFold = 5;
NumData = size(W,1);
unqLabel = unique(label);
NumClass = length(unqLabel);

% construct Y matrix
Y = zeros(NumData,NumClass);
for i=1:NumClass
    Y(:,i) = (double( label == unqLabel(i) ))';
end

idx = crossvalind('kfold', NumData, NumFold);

accMat = zeros(length(alphaSet), length(MaxIterSet));

tic;

for i = 1:NumFold
    % create data
    idx_te = (idx == i);
    label_te = label(idx_te);
                        
    Yi = Y;
    Yi(idx_te, :) = 0;
                                
    for j=1:length(alphaSet)
        alpha = alphaSet(j);
        for k=1:length(MaxIterSet)
            MaxIter = MaxIterSet(k);
            S = aNRL(W, Yi, alpha, MaxIter);
            [~, label_pred] = max(S(idx_te,:),[],2);
                                                                                                        
            accMat(j,k) = accMat(j,k) + sum(label_te == label_pred') / length(label_te);
        end
    end
end

accMat = accMat ./ NumFold;

[~,maxloc] = max(accMat(:));
[maxloc_row, maxloc_col] = ind2sub(size(accMat), maxloc);

alpha = alphaSet(maxloc_row);
MaxIter = MaxIterSet(maxloc_col);

time = toc;