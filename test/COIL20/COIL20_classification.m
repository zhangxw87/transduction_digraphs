clear; clc
addpath('../../codes/')
load('../../data/COIL20.mat');

class_label = unique(gnd);
NumClass = length(class_label); % number of classes
m = length(gnd); % number of data

% class labels in the form of numbers
class_label_number = gnd';
clear gnd;

% paper ids within the same class
class_separated_ids = cell(1,NumClass);
for i=1:NumClass
    class_separated_ids{i} = find(class_label_number==i);
end

% construct weight matrix
options = [];
options.NeighborMode = 'KNN';
options.k = 5;
options.WeightMode = 'HeatKernel';
options.t = 1;
options.bTrueKNN = 1; % unsymmetric weight matrix
W = constructW(fea,options);

% find the path for each node
node_path = cell(1,m);
for i=1:m
    node_path{i} = find(W(i,:)~=0);
end

fprintf('SoP...\n')
theta = 1; kernelID = 2;
[W_SoP, Ksop, time_SoPW, time_SoP] = SoPCovariance(W, theta, kernelID);

% experiments on different percentages of labelled data
ratio = 0.1:0.1:0.9; % percentage of labelled data
ratio_len = length(ratio);
Rept = 50; % number of repetitions of experiments

% calculate accuracy results
DiGraphARW_Result.accuracy = zeros(Rept,ratio_len);
DiGraphARW_Result.time = zeros(Rept,ratio_len);
SGL_Result = DiGraphARW_Result;
CTK_Result = DiGraphARW_Result;
RCTK_Result = DiGraphARW_Result;
ZFL_Result = DiGraphARW_Result;
SOP_Result = DiGraphARW_Result;
bDRandomWalk_Result = DiGraphARW_Result;

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
        fprintf('DiGraphARW...\n')
        alpha = 0.1;
        [H, DiGraphARW_Result.time(j,i)] = DiGraphARW(W, Y, alpha);
        DiGraphARW_Result.accuracy(j,i) = microAC(class_label_number', H, covered_nodes_mat);
        
        % SGL
        fprintf('SGL...\n')
        alpha = 0.1; eta = 0.01;
        [H_SGL, SGL_Result.time(j,i)] = SGL(W, Y, eta, alpha);
        SGL_Result.accuracy(j,i) = microAC(class_label_number', H_SGL, covered_nodes_mat);
        
        % CTK
        fprintf('CTK...\n')
        eta = 0.01;
        [H_CTK, CTK_Result.time(j,i)] = CTK(W, Y, eta);     
        CTK_Result.accuracy(j,i) = microAC(class_label_number', H_CTK, covered_nodes_mat);
        
        % RCTK 
        fprintf('RCTK...\n')
        alpha = 0.1; eta = 0.01;
        [H_RCTK, RCTK_Result.time(j,i)] = RCTK(W, Y, eta, alpha);      
        RCTK_Result.accuracy(j,i) = microAC(class_label_number', H_RCTK, covered_nodes_mat);
        
        % ZFL 
        fprintf('ZFL...\n')
        alpha = 1;
        [H_ZFL, ZFL_Result.time(j,i) ] = ZFL(W, Y, alpha);       
        ZFL_Result.accuracy(j,i) = microAC(class_label_number', H_ZFL, covered_nodes_mat);        
    
        % SoP
        fprintf('SOP...\n')
        t = tic;
        H_SOP = Ksop * Y;
        SOP_Result.time(j,i) = toc(t) + time_SoP;
        SOP_Result.accuracy(j,i) = microAC(class_label_number', H_SOP, covered_nodes_mat);
        
        % Biased discriminant random walks
        fprintf('bDWALK...\n')
        MaxIter = 50; % maximum number of iterations
        t = tic;
        [H_bDRW, ~] = bDRandomWalk(W_SoP, Y, MaxIter);
        bDRandomWalk_Result.time(j,i) = toc(t) + time_SoPW;
        bDRandomWalk_Result.accuracy(j,i) = microAC(class_label_number', H_bDRW, covered_nodes_mat);
    end
end

%================ NetKit methods ================
[citing_id, cited_id, ~] = find(W);
paper_id = 1:m; 
edge_mat = [citing_id, cited_id];
fid = fopen('COIL20_edge.rn','w');
fprintf(fid,'%d,%d,1\n',edge_mat');
fclose(fid);

node_mat = [paper_id; class_label_number];
fid = fopen('COIL20_truth.csv','w');
fprintf(fid,'%d,%d\n',node_mat);
fclose(fid);

WVRN_Result.accuracy = zeros(Rept,ratio_len);
WVRN_Result.time = zeros(Rept,ratio_len);
CDRN_Result = WVRN_Result;
NBC_Result = WVRN_Result;
NLB_Result = WVRN_Result;

for j=1:ratio_len
   for i=1:Rept
       fprintf('We are doing the %i-the experiment for the %i-th ratio...\n', j, i);
       temp_label = node_mat(:,samples{i,j});
       fid = fopen('COIL20_temp_label_file.csv','w');
       fprintf(fid,'%d,%d\n',temp_label);
       fclose(fid);
       
       % Weighted Vote Relational Neighbor Classifier
       fprintf('WVRN...\n')
       t = tic;
       [sta,cmdout]=system('java -jar ../../codes/netkit-srl-1.4.0/lib/NetKit-1.4.0.jar -output output-file -pruneZeroKnowledge -known COIL20_temp_label_file.csv -lclassifier uniform -rclassifier wvrn -inferencemethod relaxlabel -showauc COIL20_schema.arff > COIL20_aucfile.out');
       WVRN_Result.time(i,j) = toc(t);
       fid = fopen('COIL20_aucfile.out','r');
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
       fprintf('CDRN...\n')
       t = tic;
       [sta,cmdout]=system('java -jar ../../codes/netkit-srl-1.4.0/lib/NetKit-1.4.0.jar -output output-file -pruneZeroKnowledge -known COIL20_temp_label_file.csv -lclassifier uniform -rclassifier cdrn-norm-cos -inferencemethod relaxlabel -showauc COIL20_schema.arff > COIL20_aucfile.out');
       CDRN_Result.time(i,j) = toc(t);
       fid = fopen('COIL20_aucfile.out','r');
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
       fprintf('NBC...\n')
       t = tic;
       [sta,cmdout]=system('java -jar ../../codes/netkit-srl-1.4.0/lib/NetKit-1.4.0.jar -output output-file -pruneZeroKnowledge -known COIL20_temp_label_file.csv -lclassifier uniform -rclassifier nobayes -inferencemethod relaxlabel -showauc COIL20_schema.arff > COIL20_aucfile.out');
       NBC_Result.time(i,j) = toc(t);
       fid = fopen('COIL20_aucfile.out','r');
       sum = 0;
       for k=1:NumClass
           linedata = sscanf(fgetl(fid), '%c');
           split_cell=regexp(linedata,' ','split');
           str2double(split_cell{1,2});
           sum=sum+str2double(split_cell{1,2});
       end
       NBC_Result.accuracy(i,j) = sum / NumClass;
       fclose(fid);
       
       % Network-only Link Based Classifier
       fprintf('NLB...\n')
       t = tic;
       [sta,cmdout]=system('java -jar ../../codes/netkit-srl-1.4.0/lib/NetKit-1.4.0.jar -output output-file -pruneZeroKnowledge -known COIL20_temp_label_file.csv -lclassifier uniform -rclassifier nolb-lr-count -inferencemethod iterative -showauc COIL20_schema.arff > COIL20_aucfile.out');
       NLB_Result.time(i,j) = toc(t);
       fid=fopen('COIL20_aucfile.out','r');
       sum = 0;
       for k=1:NumClass
           linedata = sscanf(fgetl(fid), '%c');
           split_cell=regexp(linedata,' ','split');
           str2double(split_cell{1,2});
           sum=sum+str2double(split_cell{1,2});
       end
       NLB_Result.accuracy(i,j) = sum / NumClass;
       fclose(fid);
   end
end

save ../results/COIL20_result.mat samples DiGraphARW_Result SGL_Result CTK_Result ...
    RCTK_Result ZFL_Result SOP_Result bDRandomWalk_Result WVRN_Result ...
    CDRN_Result NBC_Result NLB_Result;
exit