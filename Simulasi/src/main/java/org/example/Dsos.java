package org.example;

import java.util.Random;

/**
 * Discrete Symbiotic Organisms Search (Ezugwu dan Adewumi, 2017) untuk penjadwalan cloudlet.
 * Organisme = permutasi cloudlet. Tiga fase: mutualism, commensalism, parasitism.
 * Operator mutasi: swap 0,2, inversion 0,5, insertion 0,3 (Tabel 1 paper).
 * "Bergerak menuju xbest" di ruang permutasi dilakukan dengan menyalin sebagian urutan dari xbest (interpretasi).
 */
public final class Dsos {
    private final SchedulingProblem problem;
    private final Random rnd;
    private final int n;
    private final int ecoSize;
    private final int[][] eco;
    private final double[] fit;
    private int[] best;
    private double bestFit = Double.MAX_VALUE;

    private Dsos(SchedulingProblem problem, int ecoSize, long seed) {
        this.problem = problem;
        this.rnd = new Random(seed);
        this.n = problem.tasks();
        this.ecoSize = ecoSize;
        this.eco = new int[ecoSize][];
        this.fit = new double[ecoSize];
        for (int i = 0; i < ecoSize; i++) {
            eco[i] = shuffled();
            fit[i] = problem.fitness(problem.decode(eco[i]));
            if (fit[i] < bestFit) {
                bestFit = fit[i];
                best = eco[i];
            }
        }
    }

    public static Solution solve(SchedulingProblem problem, int ecoSize, int iterations, long seed) {
        return new Dsos(problem, ecoSize, seed).run(iterations);
    }

    private Solution run(int iterations) {
        final double[] history = new double[iterations];
        for (int it = 0; it < iterations; it++) {
            for (int i = 0; i < ecoSize; i++) {
                int j = rnd.nextInt(ecoSize);
                while (j == i) {
                    j = rnd.nextInt(ecoSize);
                }
                accept(i, mutate(pull(eco[i], best)));                              // mutualism (xi)
                accept(j, mutate(pull(eco[j], best)));                              // mutualism (xj)
                accept(i, pull(eco[i], rnd.nextBoolean() ? eco[j] : best));         // commensalism
                int[] parasite = eco[i];                                            // parasitism
                for (int r = 0; r < Math.max(2, n / 10); r++) {
                    parasite = mutate(parasite);
                }
                accept(j, parasite);
            }
            history[it] = bestFit;
        }
        return new Solution(problem.decode(best), bestFit, history);
    }

    /** Kandidat hanya diterima kalau lebih baik dari organisme yang digantikannya. */
    private void accept(int k, int[] candidate) {
        final double f = problem.fitness(problem.decode(candidate));
        if (f < fit[k]) {
            eco[k] = candidate;
            fit[k] = f;
            if (f < bestFit) {
                bestFit = f;
                best = candidate;
            }
        }
    }

    private int[] shuffled() {
        final int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = i;
        }
        for (int i = n - 1; i > 0; i--) {
            final int j = rnd.nextInt(i + 1);
            final int tmp = a[i];
            a[i] = a[j];
            a[j] = tmp;
        }
        return a;
    }

    private int[] mutate(int[] x) {
        final int[] q = x.clone();
        int i = rnd.nextInt(n);
        int j = rnd.nextInt(n - 1);
        if (j >= i) {
            j++;
        }
        if (i > j) {
            final int tmp = i;
            i = j;
            j = tmp;
        }
        final double r = rnd.nextDouble();
        if (r < 0.2) {                                   // swap
            final int tmp = q[i];
            q[i] = q[j];
            q[j] = tmp;
        } else if (r < 0.7) {                            // inversion
            for (int a = i, b = j; a < b; a++, b--) {
                final int tmp = q[a];
                q[a] = q[b];
                q[b] = tmp;
            }
        } else {                                         // insertion: pindahkan elemen i ke posisi j
            final int moved = q[i];
            System.arraycopy(q, i + 1, q, i, j - i);
            q[j] = moved;
        }
        return q;
    }

    /** Salin potongan acak dari target ke x, sisanya mengikuti urutan x (hasil tetap permutasi valid). */
    private int[] pull(int[] x, int[] target) {
        int i = rnd.nextInt(n);
        int j = rnd.nextInt(n - 1);
        if (j >= i) {
            j++;
        }
        if (i > j) {
            final int tmp = i;
            i = j;
            j = tmp;
        }
        final boolean[] inSegment = new boolean[n];
        for (int k = i; k <= j; k++) {
            inSegment[target[k]] = true;
        }
        final int[] result = new int[n];
        int pos = 0;
        final int[] rest = new int[n - (j - i + 1)];
        for (int v : x) {
            if (!inSegment[v]) {
                rest[pos++] = v;
            }
        }
        int out = 0;
        for (int k = 0; k < i; k++) {
            result[out++] = rest[k];
        }
        for (int k = i; k <= j; k++) {
            result[out++] = target[k];
        }
        for (int k = i; k < rest.length; k++) {
            result[out++] = rest[k];
        }
        return result;
    }
}
