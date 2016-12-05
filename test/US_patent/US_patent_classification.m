clear; clc
addpath('../../codes/')
load('../../data/US_patent.mat');

NumClass = length(class_label); % number of classes

% paper ids within the same class
class_separated_ids = cell(1,NumClass);
for i=1:NumClass
    class_separated_ids{i} = new_paper_id(class_label_number==i);
end

% construct weight matrix
m = max(max(cited_id),max(citing_id));
W = sparse(citing_id,cited_id,1,m,m);

% find the path for each node
% node_path = cell(1,m);
[rID, cID] = find(W);
node_path = accumarray(rID,cID, [], @(v) {sort(v).'});
node_path = node_path';

% experiments on different percentages of labelled data
ratio = 0.1:0.1:0.9; % percentage of labelled data
ratio_len = length(ratio);
Rept = 10; % number of repetitions of experiments

% calculate accuracy results
DiGraphARW_Result.accuracy = zeros(Rept,ratio_len);
DiGraphARW_Result.time = zeros(Rept,ratio_len);

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
                       
        % construct label matrix Y
        Y = zeros(m,NumClass); 
        for k=1:length(label_ind)
            Y(label_ind(k),class_label_number(label_ind(k))) = 1;
        end
        
        % covered nodes in the paths of labeled data
        covered_nodes = (node_path(1,label_ind));
        covered_nodes_mat = [];
        for k=1:length(covered_nodes)
            covered_nodes_mat=[covered_nodes_mat, covered_nodes{1,k}];
        end
        covered_nodes_mat = unique(covered_nodes_mat);
        
        % DiGraphARW
        alpha = 0.1;
        [H, DiGraphARW_Result.time(j,i)] = DiGraphARW(W, Y, alpha);  
        DiGraphARW_Result.accuracy(j,i) = microAC(class_label_number', H, covered_nodes_mat);    
    end
end

save ../../results/US_patent_result.mat samples DiGraphARW_Result;

%================ NetKit methods ================
[citing_id, cited_id, ~] = find(W);
paper_id = 1:m;
edge_mat = [citing_id, cited_id];
fid = fopen('US_patent_edge.rn','w');
fprintf(fid,'%d,%d,1\n',edge_mat');
fclose(fid);

node_mat = [paper_id; class_label_number];
fid = fopen('US_patent_truth.csv','w');
fprintf(fid,'%d,%d\n',node_mat);
fclose(fid);

WVRN_Result.accuracy = zeros(Rept,ratio_len);
WVRN_Result.time = zeros(Rept,ratio_len);
CDRN_Result = WVRN_Result;
NBC_Result = WVRN_Result;

for j=1:Rept
    for i=1:ratio_len
        fprintf('We are doing the %i-the experiment for the %i-th ratio...\n', j, i);
        temp_label = node_mat(:,samples{i,j});
        fid = fopen('US_patent_temp_label_file.csv','w');
        fprintf(fid,'%d,%d\n',temp_label);
        fclose(fid);
        
        % Weighted Vote Relational Neighbor Classifier
        t = tic;
        [sta,cmdout]=system('java -jar ../../codes/netkit-srl-1.4.0/lib/NetKit-1.4.0.jar -output output-file -pruneZeroKnowledge -known US_patent_temp_label_file.csv -lclassifier uniform -rclassifier wvrn -inferencemethod relaxlabel -showauc US_patent_schema.arff > US_patent_aucfile.out');
        WVRN_Result.time(i,j) = toc(t);
        fid = fopen('US_patent_aucfile.out','r');
        sum=0;
        for k=1:NumClass
            linedata = sscanf(fgetl(fid), '%c');
            split_cell = regexp(linedata,' ','split');
            str2double(split_cell{1,2});
            sum = sum + str2double(split_cell{1,2});
        end
        WVRN_Result.accuracy(i,j) = sum / NumClass;
        fclose(fid);
        
        % Class Distribution Relational Neighbor Classifier
        t = tic;
        [sta,cmdout]=system('java -jar ../../codes/netkit-srl-1.4.0/lib/NetKit-1.4.0.jar -output output-file -pruneZeroKnowledge -known US_patent_temp_label_file.csv -lclassifier uniform -rclassifier cdrn-norm-cos -inferencemethod relaxlabel -showauc US_patent_schema.arff > US_patent_aucfile.out');
        CDRN_Result.time(i,j) = toc(t);
        fid = fopen('US_patent_aucfile.out','r');
        sum = 0;
        for k=1:NumClass
            linedata = sscanf(fgetl(fid), '%c');
            split_cell = regexp(linedata,' ','split');
            str2double(split_cell{1,2});
            sum = sum + str2double(split_cell{1,2});
        end
        CDRN_Result.accuracy(i,j) = sum / NumClass;
        fclose(fid);
        
        % Network-only Bayes Classifier
        t = tic;
        [sta,cmdout]=system('java -jar ../../codes/netkit-srl-1.4.0/lib/NetKit-1.4.0.jar -output output-file -pruneZeroKnowledge -known US_patent_temp_label_file.csv -lclassifier uniform -rclassifier nobayes -inferencemethod relaxlabel -showauc US_patent_schema.arff > US_patent_aucfile.out');
        NBC_Result.time(i,j) = toc(t);
        fid = fopen('US_patent_aucfile.out','r');
        sum = 0;
        for k=1:NumClass
            linedata = sscanf(fgetl(fid), '%c');
            split_cell=regexp(linedata,' ','split');
            str2double(split_cell{1,2});
            sum=sum+str2double(split_cell{1,2});
        end
        NBC_Result.accuracy(i,j) = sum / NumClass;
        fclose(fid);
    end
end

save ../../results/US_patent_result.mat -append WVRN_Result CDRN_Result NBC_Result;

exit