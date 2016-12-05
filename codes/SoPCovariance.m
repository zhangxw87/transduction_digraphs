function [W, Ksop, time1, time2] = SoPCovariance(A0,theta,kernelID);
% This code is the implementation of the SoP covariance kernel introduced in
% "The Sum-Over-Paths Covariance Kernel : a novel covariance measure between
% nodes of a directed graph" (Amin Mantrach, Luh Yen, Jerome Callut, Kevin Francoisse,
% Masashi Shimbo and Marco Saerens)
% submitted to IEEE Transactions on Pattern Analysis and Machine Intelligence
%
% INPUT:
% A0 is the adjacency matrix of the directed graph containing affinities >= 0
% theta > 0 is the paramter to tune the entropy spread into the graph (0,+10)
% kernelID (integer) to specify the type of output kernel
% 0 : Standard covariance
% 1 : Centered covariance
% 2 : Standard correlation (= standard option)
% 3 : Centered then normalized (correlation)
%
% OUTPUT:
% Ksop is the Sum-over-Paths kernel matrix
 
myMax    = realmax;
eps      = 0.00000001;
 
[nr,nc] = size(A0);
Kcov    = zeros(nr,nc);
Kcor    = zeros(nr,nc);
   
if (nr ~= nc)
    fprintf('ERROR: The adjacency matrix is not square !\n');
    return;
end;
 
if (theta < eps) || (theta > 10.0)
    fprintf('ERROR: The value of theta is out of the admissible range [%g,10.0] !\n',eps);
    return;
end;
 
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    t = tic;
    % Computation of the cost matrix C (inverse of affinities)
    C  = A0;
    C(A0 >= eps) = 1./(A0(A0 >= eps));
    C(C < eps)   = myMax;
    A0(A0 < eps) = 0;
   
    e   = ones(nr,1);
    I   = eye(nr);
    H   = (I - (ones(nr,nc)/nr)); % Centering matrix
   
    % Computation of P, the reference transition probabilities matrix (natural random walk)
    P   = zeros(nr,nc);
    s   = sum(A0,2);
	sumprod = s*e';
    div = find(sumprod);
    P(div)=A0(div)./sumprod(div);
 
	%%%%%%%%%%%%%%%%%%%%%%  
    W   = exp(-theta * C) .* P;
    time1 = toc(t);  % time of computing weight matrix 
    
    t = tic;
	%Z = cholinv(I-W);
    Z   = (I - W)\I; %% = inv(I - W); the fundamental matrix
    zc  = sum(Z,1);  %% row vector: sum of columns, z.k - denoted as zr instead in the paper !
    zr  = sum(Z,2);  %% column vector: sum of rows, zk. - denoted as zc instead in the paper !
    z   = sum(zr) - nr;
   
    for k = 1:nr
        for l = k:nc
            Kcov(k,l) = ( (zc(k) - 1)*zr(k)*I(k,l) + zr(k)*(zc(l) - 1)*(Z(l,k) - I(l,k)) + zr(l)*(zc(k) - 1)*(Z(k,l) - I(k,l)) ) / z;
            Kcov(k,l) = Kcov(k,l) - ( zr(k)*zr(l)*(zc(k) - 1)*(zc(l) - 1) ) / (z^2);
            Kcov(l,k) = Kcov(k,l);
        end;
    end;
    switch kernelID
        case 0 %% Standard covariance
        Ksop = Kcov;
        case 1 %% Centered covariance
        Ksop = H * Kcov * H;
        case 2 %% Standard correlation
        D    = diag(diag(Kcov));
        Ksop = D^(-0.5) * Kcov * D^(-0.5);
        case 3 %% Centered then normalized (correlation)
        Kcov = H * Kcov * H;
        D    = diag(diag(Kcov));
        Ksop = D^(-0.5) * Kcov * D^(-0.5);
    end
    
    time2 = time1 + toc(t); % time of the whole SoP algorithm
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
