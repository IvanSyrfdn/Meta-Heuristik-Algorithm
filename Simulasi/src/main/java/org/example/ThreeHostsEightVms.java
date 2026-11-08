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

/**
 * Simulasi CloudSim Plus: 1 datacenter, 3 host, setiap host berisi 8 VM (total 24 VM).
 *
 * Kapasitas host sengaja dibuat pas untuk 8 VM (PE, RAM, bandwidth, storage),
 * sehingga setiap host hanya bisa menampung tepat 8 VM.
 */
public class ThreeHostsEightVms {
    // Topologi
    private static final int HOSTS = 3;
    private static final int VMS_PER_HOST = 8;
    private static final int VMS = HOSTS * VMS_PER_HOST;      // 24

    // Spesifikasi satu VM
    private static final long VM_MIPS = 1000;
    private static final int VM_PES = 1;
    private static final long VM_RAM = 2048;                   // MB
    private static final long VM_BW = 1000;                    // Mbps
    private static final long VM_STORAGE = 10_000;             // MB

    // Spesifikasi satu host = 8 x spesifikasi VM
    private static final int HOST_PES = VMS_PER_HOST * VM_PES;
    private static final long HOST_RAM = VMS_PER_HOST * VM_RAM;
    private static final long HOST_BW = VMS_PER_HOST * VM_BW;
    private static final long HOST_STORAGE = VMS_PER_HOST * VM_STORAGE;

    // Beban kerja: dataset GoCJ (satu baris = panjang satu job dalam MI)
    private static final String DEFAULT_DATASET = "data/GoCJ_Dataset_100.txt";
    private static final int CLOUDLET_PES = 1;

    /** Argumen opsional: [0] path dataset GoCJ, [1] jumlah job maksimum yang dipakai. */
    public static void main(String[] args) {
        final String path = args.length > 0 ? args[0] : DEFAULT_DATASET;
        final int limit = args.length > 1 ? Integer.parseInt(args[1]) : Integer.MAX_VALUE;
        new ThreeHostsEightVms(path, limit);
    }

    private ThreeHostsEightVms(String datasetPath, int limit) {
        final CloudSim simulation = new CloudSim();

        final Datacenter datacenter = createDatacenter(simulation);
        final DatacenterBroker broker = new DatacenterBrokerSimple(simulation);

        broker.submitVmList(createVms());
        final List<Cloudlet> cloudlets = createCloudlets(loadJobLengths(datasetPath, limit));
        broker.submitCloudletList(cloudlets);

        simulation.start();

        final List<Cloudlet> finished = broker.getCloudletFinishedList();
        new CloudletsTableBuilder(finished).build();
        printSummary(datacenter, finished, cloudlets.size());
    }

    /** 1 datacenter berisi 3 host. VM dialokasikan dengan VmAllocationPolicySimple (default). */
    private Datacenter createDatacenter(CloudSim simulation) {
        final List<Host> hosts = new ArrayList<>(HOSTS);
        for (int i = 0; i < HOSTS; i++) {
            hosts.add(createHost());
        }
        return new DatacenterSimple(simulation, hosts);
    }

    private Host createHost() {
        final List<Pe> pes = new ArrayList<>(HOST_PES);
        for (int i = 0; i < HOST_PES; i++) {
            pes.add(new PeSimple(VM_MIPS));
        }
        return new HostSimple(HOST_RAM, HOST_BW, HOST_STORAGE, pes);
    }

    /** 24 VM: karena kapasitas host pas, hasilnya 8 VM per host. */
    private List<Vm> createVms() {
        final List<Vm> list = new ArrayList<>(VMS);
        for (int i = 0; i < VMS; i++) {
            final Vm vm = new VmSimple(VM_MIPS, VM_PES)
                .setRam(VM_RAM).setBw(VM_BW).setSize(VM_STORAGE)
                .setCloudletScheduler(new CloudletSchedulerSpaceShared());
            list.add(vm);
        }
        return list;
    }

    /**
     * Membaca panjang job (MI) dari file GoCJ. Yang dibaca adalah kolom pertama tiap baris,
     * baris kosong dan baris header (bukan angka) dilewati. Format enhanced GoCJ
     * (panjang, SLA, waktu kedatangan) juga terbaca, tetapi hanya kolom pertama yang dipakai.
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

    /** Satu cloudlet per job di dataset. */
    private List<Cloudlet> createCloudlets(List<Long> lengths) {
        final UtilizationModelFull full = new UtilizationModelFull();
        final List<Cloudlet> list = new ArrayList<>(lengths.size());
        for (long length : lengths) {
            list.add(new CloudletSimple(length, CLOUDLET_PES, full));
        }
        return list;
    }

    private void printSummary(Datacenter datacenter, List<Cloudlet> finished, int total) {
        System.out.println();
        System.out.println("=== Ringkasan ===");
        System.out.printf("Datacenter: 1, host: %d, VM total: %d%n", datacenter.getHostList().size(), VMS);
        for (Host host : datacenter.getHostList()) {
            System.out.printf("Host %d: %d VM%n", host.getId(), host.getVmCreatedList().size());
        }
        final double makespan = finished.stream().mapToDouble(Cloudlet::getFinishTime).max().orElse(0);
        System.out.printf("Cloudlet selesai: %d dari %d%n", finished.size(), total);
        System.out.printf("Makespan: %.2f detik%n", makespan);
    }
}