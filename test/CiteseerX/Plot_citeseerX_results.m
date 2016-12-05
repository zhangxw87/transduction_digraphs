clear;clc;close all;
x = 0.1:0.1:0.9;
h1 = figure(1);
load('../../citeseerX_result.mat');

nbc_acc = mean(NBC_Result.accuracy);
nlb_acc = mean(NLB_Result.accuracy);
cdrn_acc = mean(CDRN_Result.accuracy);
wvrn_acc = mean(WVRN_Result.accuracy);
ctk_acc = mean(CTK_Result.accuracy);
rctk_acc = mean(RCTK_Result.accuracy);
sgl_acc = mean(SGL_Result.accuracy);
zfl_acc = mean(ZFL_Result.accuracy);
sop_acc = mean(SOP_Result.accuracy);
bdwalk_acc = mean(bDRandomWalk_Result.accuracy);
ours_acc = mean(DiGraphARW_Result.accuracy);

% 
plot(x, nbc_acc, 'b-o', 'MarkerFaceColor', [0 0 1],...
    'MarkerEdgeColor',[0 0 1], 'Color',[0 0 1], 'MarkerSize',5,'LineWidth',2);
hold on;

plot(x, nlb_acc, '-s',  'MarkerFaceColor', [1 0 1],...
    'MarkerEdgeColor',[1 0 1], 'Color',[1 0 1], 'MarkerSize',5,'LineWidth',2);

plot(x, cdrn_acc, '-x', 'MarkerFaceColor',[0 0.5 0.5],...
    'MarkerEdgeColor',[0 0.5 0.5], 'Color',[0 0.5 0.5], 'MarkerSize',5,'LineWidth',2);

plot(x, wvrn_acc, '-s', 'MarkerFaceColor',[0.5 0.5 0],...
    'MarkerEdgeColor',[0.5 0.5 0], 'Color',[0.5 0.5 0], 'MarkerSize',5,'LineWidth',2);

plot(x, ctk_acc, '-+', 'MarkerFaceColor',[0.5 0 0],...
    'MarkerEdgeColor',[0.5 0 0], 'Color',[0.5 0 0], 'MarkerSize',5,'LineWidth',2);

plot(x, rctk_acc, 'Marker','hexagram', 'MarkerFaceColor',[0 1 1],...
    'MarkerEdgeColor',[0 1 1], 'Color',[0 1 1], 'MarkerSize',5,'LineWidth',2);

plot(x, sgl_acc, '-*', 'MarkerFaceColor',[0 0.5 0],...
    'MarkerEdgeColor',[0 0.5 0], 'Color',[0 0.5 0], 'MarkerSize',5,'LineWidth',2);

plot(x, zfl_acc, '-d', 'MarkerFaceColor',[0 0 0],...
    'MarkerEdgeColor',[0 0 0], 'Color',[0 0 0], 'MarkerSize',5,'LineWidth',2);

plot(x, sop_acc, '-^', 'MarkerFaceColor',[0.5 0 0.5],...
    'MarkerEdgeColor',[0.5 0 0.5], 'Color',[0.5 0 0.5], 'MarkerSize',5,'LineWidth',2);

plot(x, bdwalk_acc, '->', 'MarkerFaceColor',[1 0.5 0],...
    'MarkerEdgeColor',[1 0.5 0], 'Color',[1 0.5 0], 'MarkerSize',5,'LineWidth',2);

plot(x, ours_acc, 'MarkerFaceColor',[1 0 0],'MarkerEdgeColor',[1 0 0],...
    'Marker','pentagram', 'Color',[1 0 0], 'MarkerSize',5,'LineWidth',2);

legend('NBC', 'NLB', 'CDRN', 'WVRN', 'CTKd', 'RCTKd', 'SGL', 'ZFL', 'SOP', ...
    'bDWALK', 'Ours');
xlabel('r', 'FontSize', 15);
ylabel('Accuracy (AC)', 'FontSize', 15)