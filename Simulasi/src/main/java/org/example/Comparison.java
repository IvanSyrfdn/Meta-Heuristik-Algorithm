package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

/** Ringkasan hasil beberapa metode: tabel di konsol dan berkas JSON untuk halaman visualisasi. */
public final class Comparison {
    private Comparison() {
    }

    /** Satu kali jalan sebuah metode: nilai hasil hitungan algoritma dan hasil simulasi CloudSim. */
    public record Run(double fitness, double estimatedMakespan, double simulatedMakespan,
                      double cost, double energy, long algorithmMs, double[] history) {
    }

    public record Series(String name, List<Run> runs) {
        public double mean(ToDoubleFunction<Run> f) {
            return runs.stream().mapToDouble(f).average().orElse(0);
        }

        public double std(ToDoubleFunction<Run> f) {
            final double m = mean(f);
            return Math.sqrt(runs.stream().mapToDouble(r -> Math.pow(f.applyAsDouble(r) - m, 2)).average().orElse(0));
        }

        public double min(ToDoubleFunction<Run> f) {
            return runs.stream().mapToDouble(f).min().orElse(0);
        }
    }

    public static void print(List<Series> series) {
        System.out.println("=== Perbandingan (rata-rata dari beberapa kali jalan) ===");
        System.out.printf("%-14s %6s %24s %12s %10s %12s %12s %9s%n", "Metode", "Jalan",
            "Makespan sim. (s)", "Terbaik (s)", "Biaya ($)", "Energi (Wh)", "Waktu algo", "vs RR");
        final double baseline = series.get(0).mean(Run::simulatedMakespan);
        for (Series s : series) {
            final double mean = s.mean(Run::simulatedMakespan);
            System.out.printf("%-14s %6d %15.1f +- %6.1f %12.1f %10.3f %12.1f %9.0f ms %7.1f%%%n", s.name(),
                s.runs().size(), mean, s.std(Run::simulatedMakespan), s.min(Run::simulatedMakespan),
                s.mean(Run::cost), s.mean(Run::energy), s.mean(r -> (double) r.algorithmMs()),
                (1 - mean / baseline) * 100);
        }
        System.out.println("(makespan sim. = hasil CloudSim, termasuk jeda pengiriman broker sekitar 0,1 detik)");
    }

    public static String toJson(String dataset, int jobs, int vms, int iterations, List<Series> series) {
        final String algorithms = series.stream().map(s -> "{\"name\":\"" + s.name() + "\",\"runs\":["
            + s.runs().stream().map(Comparison::runJson).collect(Collectors.joining(",")) + "]}")
            .collect(Collectors.joining(","));
        return "{\"dataset\":\"" + dataset.replace("\\", "/").replace("\"", "'") + "\",\"jobs\":" + jobs
            + ",\"vms\":" + vms + ",\"iterations\":" + iterations + ",\"algorithms\":[" + algorithms + "]}";
    }

    private static String runJson(Run r) {
        final StringBuilder history = new StringBuilder("[");
        for (int i = 0; i < r.history().length; i++) {
            history.append(i > 0 ? "," : "").append(num(r.history()[i]));
        }
        history.append("]");
        return "{\"fitness\":" + num(r.fitness()) + ",\"est\":" + num(r.estimatedMakespan())
            + ",\"sim\":" + num(r.simulatedMakespan()) + ",\"cost\":" + num(r.cost())
            + ",\"energy\":" + num(r.energy()) + ",\"ms\":" + r.algorithmMs() + ",\"history\":" + history + "}";
    }

    /** Titik desimal selalu "." (bukan koma) supaya JSON valid di semua locale. */
    private static String num(double v) {
        return String.format(Locale.ROOT, "%.6f", v);
    }

    public static void write(Path file, String json) {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.writeString(file, json);
            System.out.println("Hasil disimpan ke: " + file.toAbsolutePath());
        } catch (IOException e) {
            System.out.println("PERINGATAN: gagal menyimpan " + file + " (" + e.getMessage() + ")");
        }
    }
}
