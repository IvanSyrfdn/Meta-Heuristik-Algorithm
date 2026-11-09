package org.example;

import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongFunction;

/**
 * Simulasi CloudSim Plus: 1 datacenter, 3 host, setiap host berisi 8 VM (total 24 VM),
 * beban kerja dari dataset GoCJ. Jadwal cloudlet ke VM dibandingkan untuk empat metode:
 * Round Robin, DSOS, SMO, dan Enhanced SMO. Setiap metode dijalankan pada simulasi baru.
 *
 * Argumen opsional: [0] path dataset GoCJ, [1] jumlah job maksimum, [2] jumlah iterasi algoritma,
 * [3] jumlah jalan (seed) per algoritma. Hasil disimpan ke hasil/hasil.json untuk halaman visualisasi.
 */
public class ThreeHostsEightVms {
    // Topologi
    private static final int HOSTS = 3;
    private static final int VMS_PER_HOST = 8;
    private static final int VMS = HOSTS * VMS_PER_HOST;       // 24

    // VM heterogen: kecepatan bergantian per VM. Ganti menjadi {1000} untuk VM homogen.
    private static final double[] VM_MIPS_LEVELS = {500, 1000, 1500, 2000};
    private static final double HOST_PE_MIPS = 2000;           // harus >= MIPS VM tercepat
    private static final int VM_PES = 1;
    private static final long VM_RAM = 2048;                   // MB
    private static final long VM_BW = 1000;                    // Mbps
    private static final long VM_STORAGE = 10_000;             // MB

    // Host dibuat pas untuk 8 VM (PE, RAM, bandwidth, storage)
    private static final int HOST_PES = VMS_PER_HOST * VM_PES;
    private static final long HOST_RAM = VMS_PER_HOST * VM_RAM;
    private static final long HOST_BW = VMS_PER_HOST * VM_BW;
    private static final long HOST_STORAGE = VMS_PER_HOST * VM_STORAGE;

    // Beban kerja dan algoritma
    private static final String DEFAULT_DATASET = "data/GoCJ_Dataset_100.txt";
    private static final int CLOUDLET_PES = 1;
    private static final double[] WEIGHTS = {1.0, 0.0, 0.0};   // {makespan, biaya, energi}; contoh multi-objektif: {0.5, 0.3, 0.2}
    private static final int POPULATION = 50;
    private static final int DEFAULT_ITERATIONS = 300;
    private static final int DEFAULT_RUNS = 5;                  // jumlah seed per algoritma
    private static final long SEED = 42;
    private static final String OUTPUT_FILE = "hasil/hasil.json";
    private static final boolean PRINT_CLOUDLET_TABLE = false;

    public static void main(String[] args) {
        final String path = args.length > 0 ? args[0] : DEFAULT_DATASET;
        final int limit = args.length > 1 ? Integer.parseInt(args[1]) : Integer.MAX_VALUE;
        final int iterations = args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_ITERATIONS;
        final int runs = args.length > 3 ? Integer.parseInt(args[3]) : DEFAULT_RUNS;

        final List<Long> lengths = loadJobLengths(path, limit);
        final double[] mips = vmMips();
        final double[] lengthArray = lengths.stream().mapToDouble(Long::doubleValue).toArray();
        final SchedulingProblem problem = new SchedulingProblem(lengthArray, mips, WEIGHTS);

        System.out.printf("Dataset: %s (%d job), %d VM, iterasi: %d, jalan per algoritma: %d%n%n",
            path, lengths.size(), VMS, iterations, runs);

        final List<Comparison.Series> series = new ArrayList<>();
        series.add(new Comparison.Series("Round Robin",
            List.of(simulate(problem, lengths, mips, problem.roundRobin(), 0, new double[0], true))));
        series.add(runAlgorithm("DSOS", runs, seed -> Dsos.solve(problem, POPULATION, iterations, seed),
            problem, lengths, mips));
        series.add(runAlgorithm("SMO", runs, seed -> Smo.solve(problem, POPULATION, iterations, false, seed),
            problem, lengths, mips));
        series.add(runAlgorithm("Enhanced SMO", runs, seed -> Smo.solve(problem, POPULATION, iterations, true, seed),
            problem, lengths, mips));

        Comparison.print(series);
        Comparison.write(Paths.get(OUTPUT_FILE),
            Comparison.toJson(path, lengths.size(), VMS, iterations, series));
    }

    /** Menjalankan satu algoritma beberapa kali (seed berbeda); tiap jadwal disimulasikan di CloudSim. */
    private static Comparison.Series runAlgorithm(String name, int runs, LongFunction<Solution> solver,
                                                  SchedulingProblem problem, List<Long> lengths, double[] mips) {
        final List<Comparison.Run> list = new ArrayList<>();
        for (int k = 0; k < runs; k++) {
            final long start = System.nanoTime();
            final Solution solution = solver.apply(SEED + k);
            list.add(simulate(problem, lengths, mips, solution.assignment(), elapsedMs(start),
                solution.history(), false));
        }
        return new Comparison.Series(name, list);
    }

    /** Satu simulasi penuh dengan jadwal tertentu: plan[i] = indeks VM untuk cloudlet ke-i. */
    private static Comparison.Run simulate(SchedulingProblem problem, List<Long> lengths, double[] mips,
                                           int[] plan, long algorithmMs, double[] history, boolean showHosts) {
        final CloudSim simulation = new CloudSim();
        final Datacenter datacenter = createDatacenter(simulation);
        final DatacenterBroker broker = new DatacenterBrokerSimple(simulation);

        final List<Vm> vms = createVms(mips);
        final List<Cloudlet> cloudlets = createCloudlets(lengths);
        for (int i = 0; i < cloudlets.size(); i++) {
            cloudlets.get(i).setVm(vms.get(plan[i]));          // memasang jadwal hasil algoritma
        }
        broker.submitVmList(vms);
        broker.submitCloudletList(cloudlets);

        simulation.start();

        final List<Cloudlet> finished = broker.getCloudletFinishedList();
        if (finished.size() != cloudlets.size()) {
            System.out.printf("PERINGATAN: hanya %d dari %d cloudlet selesai%n", finished.size(), cloudlets.size());
        }
        if (PRINT_CLOUDLET_TABLE) {
            new CloudletsTableBuilder(finished).build();
        }
        if (showHosts) {
            for (Host host : datacenter.getHostList()) {
                System.out.printf("Host %d: %d VM%n", host.getId(), host.getVmCreatedList().size());
            }
            System.out.println();
        }
        final double simulated = finished.stream().mapToDouble(Cloudlet::getFinishTime).max().orElse(0);
        final SchedulingProblem.Metrics m = problem.metrics(plan);
        return new Comparison.Run(problem.fitness(plan), m.makespan(), simulated, m.cost(), m.energy(),
            algorithmMs, history);
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private static double[] vmMips() {
        final double[] mips = new double[VMS];
        for (int i = 0; i < VMS; i++) {
            mips[i] = VM_MIPS_LEVELS[i % VM_MIPS_LEVELS.length];
        }
        return mips;
    }

    /** 1 datacenter berisi 3 host. VM dialokasikan dengan VmAllocationPolicySimple (default). */
    private static Datacenter createDatacenter(CloudSim simulation) {
        final List<Host> hosts = new ArrayList<>(HOSTS);
        for (int i = 0; i < HOSTS; i++) {
            hosts.add(createHost());
        }
        return new DatacenterSimple(simulation, hosts);
    }

    private static Host createHost() {
        final List<Pe> pes = new ArrayList<>(HOST_PES);
        for (int i = 0; i < HOST_PES; i++) {
            pes.add(new PeSimple(HOST_PE_MIPS));
        }
        return new HostSimple(HOST_RAM, HOST_BW, HOST_STORAGE, pes);
    }

    /** 24 VM: karena kapasitas host pas, hasilnya 8 VM per host. */
    private static List<Vm> createVms(double[] mips) {
        final List<Vm> list = new ArrayList<>(VMS);
        for (int i = 0; i < VMS; i++) {
            final Vm vm = new VmSimple(mips[i], VM_PES)
                .setRam(VM_RAM).setBw(VM_BW).setSize(VM_STORAGE)
                .setCloudletScheduler(new CloudletSchedulerSpaceShared());
            list.add(vm);
        }
        return list;
    }

    /** Satu cloudlet per job di dataset. */
    private static List<Cloudlet> createCloudlets(List<Long> lengths) {
        final UtilizationModelFull full = new UtilizationModelFull();
        final List<Cloudlet> list = new ArrayList<>(lengths.size());
        for (long length : lengths) {
            list.add(new CloudletSimple(length, CLOUDLET_PES, full));
        }
        return list;
    }

    /**
     * Membaca panjang job (MI) dari file GoCJ. Yang dibaca adalah kolom pertama tiap baris,
     * baris kosong dan baris header (bukan angka) dilewati.
     */
    private static List<Long> loadJobLengths(String file, int limit) {
        final Path path = Paths.get(file);
        final List<Long> lengths = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(path)) {
                final String text = line.trim();
                if (text.isEmpty()) {
                    continue;
                }
                try {
                    lengths.add((long) Double.parseDouble(text.split("[\\s,;]+")[0]));
                } catch (NumberFormatException e) {
                    continue;                                   // lewati header
                }
                if (lengths.size() >= limit) {
                    break;
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Dataset tidak bisa dibaca: " + path.toAbsolutePath(), e);
        }
        if (lengths.isEmpty()) {
            throw new IllegalStateException("Dataset kosong: " + path.toAbsolutePath());
        }
        return lengths;
    }
}