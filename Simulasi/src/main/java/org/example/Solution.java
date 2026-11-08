package org.example;

/** Hasil satu algoritma: jadwal (indeks cloudlet -> indeks VM), nilai fitness, dan riwayat konvergensi. */
public record Solution(int[] assignment, double fitness, double[] history) {
}
