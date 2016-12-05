clear; clc
addpath('../../codes/')
load('../../data/Cora.mat');

class_label = unique(Class);
NumClass = length(class_label); % number of classes

% class labels in the form of numbers
class_label_number=zeros(1,NumClass); 
for i=1:NumClass
    for j=1:length(Class)
        if strcmp(class_label(i),Class(j))
            class_label_number(j)=i;
        end
    end
end

% rename citing ids, cited ids, and paper ids
new_citing_id = zeros(length(citing_id),1);
new_cited_id = zeros(length(cited_id),1);
for i=1:length(Paper_id)
    new_citing_id(citing_id==Paper_id(i)) = i;
    new_cited_id(cited_id==Paper_id(i)) = i;
end
new_paper_id=1:length(Paper_id);

citing_id = new_citing_id;
cited_id = new_cited_id;
paper_id = new_paper_id;
clear new_citing_id new_cited_id new_paper_id Paper_id Class;

% paper ids within the same class
class_separated_ids = cell(1,NumClass);
for i=1:NumClass
    class_separated_ids{i} = paper_id(class_label_number==i);
end

% construct weight matrix
m = max(max(cited_id),max(citing_id));
W = sparse(citing_id,cited_id,1,m,m);
W = W + W';
W(W~=0) = 1;

% find the path for each node
node_path = cell(1,m);
for i=1:m
    node_path{i} = find(W(i,:)~=0);
end

% compute kernels on graph
fprintf('DiGraphARW...\n')
alpha = 0.1;
[F, DiGraphARW_Result.time0] = DiGraphARW(W, eye(m), alpha);

fprintf('CTK...\n')
eta = 0.01;
[F_CTK, CTK_Result.time0] = CTK(W, eye(m), eta);

fprintf('LLGC...\n')
alpha = 0.99;
[F_LLGC, LLGC_Result.time0] = LLGC(W, eye(m), alpha);

% experiments on different percentages of labelled data
ratio = 0.1:0.1:0.9; % percentage of labelled data
ratio_len = length(ratio);
Rept = 50; % number of repetitions of experiments

% calculate accuracy results
DiGraphARW_Result.accuracy = zeros(Rept,ratio_len);
DiGraphARW_Result.time = zeros(Rept,ratio_len);
CTK_Result = DiGraphARW_Result;
LLGC_Result = DiGraphARW_Result;

samples = cell(Rept, ratio_len);
for j=1:Rept
    for i=1:ratio_len
        fprintf('We are doing the %i-the experiment for the %i-th ratio...\n', j, i);
        % find indices of labelled data
        label_ind = [];
        for k=1:NumClass
            % data in the kth class
            nodes_class = class_separated_ids{k}; 
            len = length(nodes_class);
            % number of labelled data in the kth class
            sample_num = floor(ratio(i) * len); 
            idd = randi(len,1,sample_num);
            label_ind = [label_ind, nodes_class(idd)];
        end
        label_ind = unique(label_ind);
        samples{j,i} = label_ind;
        
        % covered nodes in the paths of labeled data
        covered_nodes = (node_path(1,label_ind));
        covered_nodes_mat = [];
        for k=1:length(covered_nodes)
            covered_nodes_mat=[covered_nodes_mat, covered_nodes{1,k}];
        end
        covered_nodes_mat = unique(covered_nodes_mat);
        
        % construct label matrix Y
        Y = zeros(m,NumClass); 
        for k=1:length(label_ind)
            Y(label_ind(k),class_label_number(label_ind(k))) = 1;
        end
        
        % DiGraphARW 
        t = tic;
        H = F * Y;
        DiGraphARW_Result.time(j,i) = DiGraphARW_Result.time0 + toc(t);
        DiGraphARW_Result.accuracy(j,i) = microAC(class_label_number', H, covered_nodes_mat);
        
        % CTK
        t = tic;
        H_CTK = F_CTK * Y;
        CTK_Result.time(j,i) = CTK_Result.time0 + toc(t);       
        CTK_Result.accuracy(j,i) = microAC(class_label_number', H_CTK, covered_nodes_mat);
        
        % LLGC
        t = tic;
        H_LLGC = F_LLGC * Y;
        LLGC_Result.time(j,i) = LLGC_Result.time0 + toc(t);       
        LLGC_Result.accuracy(j,i) = microAC(class_label_number', H_LLGC, covered_nodes_mat);
    end
end
