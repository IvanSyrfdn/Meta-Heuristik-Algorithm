package org.example;

/**
 * Model masalah penjadwalan: n cloudlet (panjang dalam MI) ke m VM (kecepatan dalam MIPS).
 * Setiap VM menjalankan cloudlet-nya satu per satu (sesuai CloudletSchedulerSpaceShared),
 * jadi waktu sibuk VM = jumlah (panjang / MIPS) cloudlet yang ditugaskan padanya.
 *
 * Fitness = w0*makespan + w1*biaya + w2*energi, masing-masing dinormalisasi terhadap
 * Round Robin (nilai 1,0 = sama dengan Round Robin; makin kecil makin baik).
 */
public final class SchedulingProblem {
    public record Metrics(double makespan, double cost, double energy) {
    }

    private final double[] lengths;
    private final double[] mips;
    private final double[] price;      // $/jam, naik lebih cepat daripada MIPS (ada trade-off waktu vs biaya)
    private final double[] weights;    // {makespan, biaya, energi}
    private final Metrics base;

    public SchedulingProblem(double[] lengths, double[] mips, double[] weights) {
        this.lengths = lengths;
        this.mips = mips;
        this.weights = weights;
        this.price = new double[mips.length];
        for (int v = 0; v < mips.length; v++) {
            price[v] = 0.102 * Math.pow(mips[v] / 1000.0, 1.3);
        }
        this.base = metrics(roundRobin());
    }

    public int tasks() {
        return lengths.length;
    }

    public int vms() {
        return mips.length;
    }

    public int[] roundRobin() {
        final int[] a = new int[tasks()];
        for (int t = 0; t < a.length; t++) {
            a[t] = t % vms();
        }
        return a;
    }

    public Metrics metrics(int[] assignment) {
        final double[] busy = new double[vms()];
        for (int t = 0; t < assignment.length; t++) {
            busy[assignment[t]] += lengths[t] / mips[assignment[t]];
        }
        double makespan = 0;
        for (double b : busy) {
            makespan = Math.max(makespan, b);
        }
        double cost = 0;
        double energy = 0;
        for (int v = 0; v < busy.length; v++) {
            cost += busy[v] / 3600.0 * price[v];
            energy += (70 + 130 * (makespan > 0 ? busy[v] / makespan : 0)) * makespan / 3600.0;   // Wh
        }
        return new Metrics(makespan, cost, energy);
    }

    public double fitness(int[] assignment) {
        final Metrics m = metrics(assignment);
        return weights[0] * m.makespan() / base.makespan()
            + weights[1] * m.cost() / base.cost()
            + weights[2] * m.energy() / base.energy();
    }

    /** Dekoder DSOS: task diambil sesuai urutan permutasi, tiap task ke VM yang selesai paling awal. */
    public int[] decode(int[] permutation) {
        final double[] finish = new double[vms()];
        final int[] assignment = new int[tasks()];
        for (int t : permutation) {
            int best = 0;
            double bestFinish = Double.MAX_VALUE;
            for (int v = 0; v < vms(); v++) {
                final double f = finish[v] + lengths[t] / mips[v];
                if (f < bestFinish) {
                    bestFinish = f;
                    best = v;
                }
            }
            finish[best] = bestFinish;
            assignment[t] = best;
        }
        return assignment;
    }
}
