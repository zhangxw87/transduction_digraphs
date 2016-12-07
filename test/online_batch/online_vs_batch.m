numNodes = logspace(1,4,4); % number of nodes in the graph
Rept = 20; % number of repetitions
alpha = 0.1; % absorbing rate
tol = 10^(-6);

time_change_batch = zeros(Rept, length(numNodes));
time_change_online = zeros(Rept, length(numNodes));
time_update_batch = zeros(Rept, length(numNodes));
time_update_online = zeros(Rept, length(numNodes));

diff_change_avg = zeros(Rept, length(numNodes));
diff_change_max = zeros(Rept, length(numNodes));
diff_update_avg = zeros(Rept, length(numNodes));
diff_update_max = zeros(Rept, length(numNodes));

for j = 1:Rept
    for i = 1:length(numNodes)
        fprintf('We are doing the %i-th experiment for the %i-th graph size...\n', j, i);
        
        m = numNodes(i);
        
        % construct sparse weight matrix W
        spar = (1/m)*5;        
        numofele = m^2;
        nnzentry = ceil(numofele * spar);
        row_ind = randi(m,nnzentry,1);
        col_ind = randi(m,nnzentry,1);
        S = rand(nnzentry,1);
        W = sparse(row_ind,col_ind,S,m,m,nnzentry);
        degree = sum(W); % a row vector with in-degree
        P = W;
        ind = (degree > 0);
        P(:, ind) = bsxfun(@rdivide, P(:, ind), degree(ind));
        
        Q = alpha * P';
        
        E = (speye(m) - Q) \ speye(m);
        
        %% for perturbation of one edge
        fprintf('\t modifying one row of weights \n')
        % generate feasible weight change
        flag = true;
        while flag
            flag = false;
            ind1 = randi(m,1,1);
            ind2 = randi(m,1,1);
            delta_w = rand(1,1);
            
            % modify the weight of one edge
            W(ind1,ind2) = W(ind1,ind2) + delta_w;
            degree = sum(W);
            P = W;
            ind = (degree > 0);
            P(:, ind) = bsxfun(@rdivide, P(:, ind), degree(ind));
            
            % compute new fundamental matrix from scratch
            Q_new = alpha * P';
            
            % only the ind2 row of Q changes
            del_Q = Q_new(ind2,:) - Q(ind2,:);
            E_i = E(:,ind2);
            beta = del_Q * E_i;
            
            % Check if conditions are satisfied;
            if (1 - beta) <= tol
                fprintf('Condition Not Satisfied!! try to modify weight again.....\n');
                flag = true;
                continue;
            end
        end
                
        % batch update
        t = tic;
        E_new = (speye(m) - Q_new) \ speye(m);
        time_change_batch(j,i) = toc(t);
        
        % online update        
        t = tic;
        E_prime = E + (E_i*(del_Q*E))/(1 - beta);
        time_change_online(j,i) = toc(t);
        
        % compute difference between E_prime and E_new
        diff_change_avg(j,i) = sum(abs(E_prime(:) - E_new(:))) / numofele;
        diff_change_max(j,i) = max(abs(E_prime(:) - E_new(:)));
        
        
        %% add one new node      
        fprintf('\t adding one new node \n')
        num = m + 1;
        spar = (1/num)*5;        
        numofele = num^2;
        nnzentry = ceil(numofele * spar);
        row_ind = randi(num,nnzentry,1);
        col_ind = randi(num,nnzentry,1);
        S = rand(nnzentry,1);
        W = sparse(row_ind,col_ind,S,num,num,nnzentry);
        degree = sum(W); % a row vector with in-degree
        P = W;
        ind = (degree > 0);
        P(:, ind) = bsxfun(@rdivide, P(:, ind), degree(ind));
        
        Q_new = alpha * P';
        
        Q_new(1:end-1,1:end-1) = Q;
        
        % batch update
        t = tic;
        E_new = (speye(num) - Q_new) \ speye(num);
        time_update_batch(j,i) = toc(t);        
        
        % online update
        q = Q_new(end,end);
        temp = 1 - q - Q_new(end,1:end-1)*E*Q_new(1:end-1,end);
        if abs(temp) > tol
            gama = 1 / temp;
        else
            fprintf('the new matrix is close to singular.\n');
        end        
        u = E * Q_new(1:end-1,end);
        v = Q_new(end,1:end-1) * E;
        
        E_prime = sparse(num,num);
        t = tic;
        E_prime(1:end-1,1:end-1) = E + gama * u * v;
        E_prime(1:end-1,end) = gama*u;
        E_prime(end,1:end-1) = gama*v;
        E_prime(end,end) = gama;
        time_update_online(j,i) = toc(t);
        
        % compute difference between E_prime and E_new
        diff_update_avg(j,i) = sum(abs(E_prime(:) - E_new(:))) / numofele;
        diff_update_max(j,i) = max(abs(E_prime(:) - E_new(:)));
    end
end

save online_batch_results.mat time_* diff_* numNodes;
exit;