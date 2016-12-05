clear;clc;close all;
load('../../results/citeseerX_result_undirected.mat');
load('../../results/citeseerX_result.mat', 'DiGraphARW_Result');

x = 0.1:0.1:0.9;
h1 = figure(2);
ctk_acc = mean(CTK_Result_undirected.accuracy);
llgc_acc = mean(LLGC_Result_undirected.accuracy);
uARW_acc = mean(DiGraphARW_Result_undirected.accuracy);
dARW_acc = mean(DiGraphARW_Result.accuracy);

% 
plot(x, llgc_acc, '-o',  'MarkerFaceColor', [0 0.5 0],...
    'MarkerEdgeColor',[0 0.5 0], 'Color',[0 0.5 0], 'MarkerSize',5,'LineWidth',2);
hold on;

plot(x, ctk_acc, '-d', 'MarkerFaceColor',[0 0 1],...
    'MarkerEdgeColor',[0 0 1], 'Color',[0 0 1], 'MarkerSize',5,'LineWidth',2);

plot(x, uARW_acc, '-+', 'MarkerFaceColor', [0 0 0],...
    'MarkerEdgeColor',[0 0 0], 'Color',[0 0 0], 'MarkerSize',5,'LineWidth',2);

plot(x, dARW_acc, 'MarkerFaceColor',[1 0 0],'MarkerEdgeColor',[1 0 0],...
    'Marker','pentagram', 'Color',[1 0 0], 'MarkerSize',5,'LineWidth',2);

legend('LLGC', 'CTKu', 'UG', 'Ours');
xlabel('r', 'FontSize', 15);
ylabel('Accuracy (AC)', 'FontSize', 15)