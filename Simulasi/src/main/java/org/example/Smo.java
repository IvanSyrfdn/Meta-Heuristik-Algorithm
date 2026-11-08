package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Spider Monkey Optimization dan Enhanced SMO (Kumar dkk., 2022) untuk penjadwalan cloudlet.
 * Monyet = vektor sepanjang jumlah cloudlet; nilai tiap dimensi pada [0, m) di-floor menjadi indeks VM.
 * Fase: local leader, global leader, leader learning, local leader decision, global leader decision
 * (fission-fusion). Pada mode enhanced, monyet yang macet memakai Pers. 30 dan 31 paper.
 */
public final class Smo {
    private static final int MAX_GROUPS = 5;
    private static final double PERTURBATION_RATE = 0.5;
    private static final int LOCAL_LIMIT = 20;
    private static final int STAGNATION = 3;

    private final SchedulingProblem problem;
    private final Random rnd;
    private final boolean enhanced;
    private final int n;           // jumlah monyet
    private final int dim;         // dimensi = jumlah cloudlet
    private final double hi;       // batas atas posisi (m, tidak termasuk)
    private final double[][] x;
    private final double[] f;
    private final int[] stagnant;
    private final int globalLimit;
    private List<int[]> groups;
    private int[] localLeader;
    private int[] localCount;
    private int globalLeader;
    private int globalCount;

    private Smo(SchedulingProblem problem, int monkeys, boolean enhanced, long seed) {
        this.problem = problem;
        this.rnd = new Random(seed);
        this.enhanced = enhanced;
        this.n = monkeys;
        this.dim = problem.tasks();
        this.hi = problem.vms() - 1e-9;
        this.x = new double[n][dim];
        this.f = new double[n];
        this.stagnant = new int[n];
        this.globalLimit = n / 2;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < dim; j++) {
                x[i][j] = rnd.nextDouble() * hi;
            }
            f[i] = evaluate(x[i]);
        }
        this.groups = split(1);
        this.localLeader = leaders();
        this.localCount = new int[groups.size()];
        this.globalLeader = bestOf(allIndices());
    }

    public static Solution solve(SchedulingProblem problem, int monkeys, int iterations,
                                 boolean enhanced, long seed) {
        return new Smo(problem, monkeys, enhanced, seed).run(iterations);
    }

    private Solution run(int iterations) {
        final double[] history = new double[iterations];
        for (int it = 0; it < iterations; it++) {
            localLeaderPhase();
            globalLeaderPhase();
            leaderLearning();
            localLeaderDecision();
            globalLeaderDecision();
            history[it] = f[globalLeader];
        }
        return new Solution(toAssignment(x[globalLeader]), f[globalLeader], history);
    }

    private void localLeaderPhase() {
        for (int k = 0; k < groups.size(); k++) {
            final int[] g = groups.get(k);
            final double[] leader = x[localLeader[k]];
            for (int i : g) {
                final double[] cur = x[i];
                final double[] y = cur.clone();
                for (int j = 0; j < dim; j++) {
                    if (rnd.nextDouble() < PERTURBATION_RATE) {
                        continue;
                    }
                    if (enhanced && stagnant[i] >= STAGNATION) {
                        final double esm = (cur[j] + rnd.nextDouble() * hi) / 2;     // rata-rata posisi sekarang dan acak
                        double sum = 0;
                        for (int q : g) {
                            sum += cur[j] - x[q][j];                                 // SUM (Pers. 31)
                        }
                        y[j] = clip(cur[j] + rnd.nextDouble() * (leader[j] - esm)
                            + rnd.nextDouble() * sum / g.length);                    // gamma (Pers. 30)
                    } else {
                        final int r = pick(g, i);
                        y[j] = clip(cur[j] + rnd.nextDouble() * (leader[j] - cur[j])
                            + (2 * rnd.nextDouble() - 1) * (x[r][j] - cur[j]));      // Pers. 25
                    }
                }
                greedy(i, y);
            }
        }
    }

    private void globalLeaderPhase() {
        double fitMax = 0;
        for (double v : f) {
            fitMax = Math.max(fitMax, 1 / (1 + v));
        }
        for (int i = 0; i < n; i++) {
            final double prob = 0.9 * (1 / (1 + f[i])) / fitMax + 0.1;               // Pers. 27
            if (rnd.nextDouble() < prob) {
                final int[] g = groupOf(i);
                final int j = rnd.nextInt(dim);
                final int r = pick(g, i);
                final double[] y = x[i].clone();
                y[j] = clip(x[i][j] + rnd.nextDouble() * (x[globalLeader][j] - x[i][j])
                    + (2 * rnd.nextDouble() - 1) * (x[r][j] - x[i][j]));             // Pers. 26
                greedy(i, y);
            }
        }
    }

    private void leaderLearning() {
        final int candidate = bestOf(allIndices());
        if (f[candidate] < f[globalLeader]) {
            globalLeader = candidate;
            globalCount = 0;
        } else {
            globalCount++;
        }
        for (int k = 0; k < groups.size(); k++) {
            final int c = bestOf(groups.get(k));
            if (f[c] < f[localLeader[k]]) {
                localLeader[k] = c;
                localCount[k] = 0;
            } else {
                localCount[k]++;
            }
        }
    }

    private void localLeaderDecision() {
        for (int k = 0; k < groups.size(); k++) {
            if (localCount[k] <= LOCAL_LIMIT) {
                continue;
            }
            localCount[k] = 0;
            for (int i : groups.get(k)) {
                if (i == globalLeader) {
                    continue;                                                        // pemimpin global tidak diacak
                }
                final double[] y = new double[dim];
                for (int j = 0; j < dim; j++) {
                    if (rnd.nextDouble() >= PERTURBATION_RATE) {
                        y[j] = rnd.nextDouble() * hi;
                    } else {
                        y[j] = clip(x[i][j] + rnd.nextDouble() * (x[globalLeader][j] - x[i][j])
                            + rnd.nextDouble() * (x[i][j] - x[localLeader[k]][j]));  // Pers. 28 dan 29
                    }
                }
                x[i] = y;
                f[i] = evaluate(y);
            }
        }
    }

    /** Fission: kelompok dipecah hingga maksimum; setelah itu fusion: semua digabung lagi. */
    private void globalLeaderDecision() {
        if (globalCount > globalLimit) {
            globalCount = 0;
            groups = split(groups.size() < MAX_GROUPS ? groups.size() + 1 : 1);
            localLeader = leaders();
            localCount = new int[groups.size()];
        }
    }

    private void greedy(int i, double[] y) {
        final double fy = evaluate(y);
        if (fy < f[i]) {
            x[i] = y;
            f[i] = fy;
            stagnant[i] = 0;
        } else {
            stagnant[i]++;
        }
    }

    private double evaluate(double[] position) {
        return problem.fitness(toAssignment(position));
    }

    private int[] toAssignment(double[] position) {
        final int[] a = new int[dim];
        for (int j = 0; j < dim; j++) {
            a[j] = Math.min(problem.vms() - 1, (int) position[j]);
        }
        return a;
    }

    private double clip(double v) {
        return Math.min(hi, Math.max(0, v));
    }

    private int pick(int[] g, int i) {
        if (g.length == 1) {
            return i;
        }
        int r = g[rnd.nextInt(g.length)];
        while (r == i) {
            r = g[rnd.nextInt(g.length)];
        }
        return r;
    }

    private int[] allIndices() {
        final int[] all = new int[n];
        for (int i = 0; i < n; i++) {
            all[i] = i;
        }
        return all;
    }

    private List<int[]> split(int count) {
        final List<int[]> result = new ArrayList<>();
        for (int k = 0; k < count; k++) {
            final List<Integer> members = new ArrayList<>();
            for (int i = k; i < n; i += count) {
                members.add(i);
            }
            result.add(members.stream().mapToInt(Integer::intValue).toArray());
        }
        return result;
    }

    private int[] groupOf(int i) {
        return groups.get(i % groups.size());
    }

    private int[] leaders() {
        final int[] l = new int[groups.size()];
        for (int k = 0; k < l.length; k++) {
            l[k] = bestOf(groups.get(k));
        }
        return l;
    }

    private int bestOf(int[] indices) {
        int best = indices[0];
        for (int i : indices) {
            if (f[i] < f[best]) {
                best = i;
            }
        }
        return best;
    }
}
