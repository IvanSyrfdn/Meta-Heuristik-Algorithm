# DSOS dan SMO untuk Cloud Task Scheduling

Slide interaktif (satu file HTML) untuk demo week 10 matakuliah Strategi Komputasi Awan. Isinya menjelaskan dua algoritma metaheuristik terinspirasi alam dan cara memakainya untuk menjadwalkan task di cloud:

- **DSOS** (Discrete Symbiotic Organisms Search): meniru simbiosis antar organisme.
- **SMO** (Spider Monkey Optimization) dan **Enhanced SMO**: meniru monyet laba-laba mencari makanan.

Demo online: `(https://ivansyrfdn.github.io/Meta-Heuristik-Algorithm/)`

## Isi slide

| Slide | Bagian | Isi |
|---|---|---|
| 1 | Pembuka | Judul, pilihan mulai dari DSOS atau SMO, sumber paper |
| 2 | DSOS | Inspirasi: mutualism, commensalism, parasitism |
| 3 | DSOS | Studi kasus 8 task dan 3 VM, Gantt chart |
| 4 | DSOS | Animasi langkah demi langkah satu organisme |
| 5 | DSOS | Simulasi 40 task dan 4 VM (masalah, proses, solusi) |
| 6 | DSOS | Pemetaan TSP ke cloud, hasil paper, sumber |
| 7 | SMO | Inspirasi: local leader, global leader, fission-fusion |
| 8 | SMO | Animasi langkah demi langkah di medan 2D |
| 9 | SMO | Penjadwalan cloud: SMO vs Enhanced SMO, Gantt, tabel metrik |
| 10 | SMO | Hasil paper dan sumber |

Semua visualisasi digambar langsung oleh kode (SVG dan canvas), tanpa gambar eksternal.

## Cara memakai

- **Navigasi:** tombol panah kiri dan kanan, atau tombol Sebelumnya dan Berikutnya di bawah.
- **Loncat ke slide tertentu:** tambahkan `#nomor` di alamat, misalnya `#7` untuk slide SMO pertama.
- **Interaktif:** klik kartu inspirasi (slide 2 dan 7), tombol jadwal (slide 3), "Langkah berikutnya" atau "Otomatis" (slide 4), "Fase berikutnya" (slide 8), dan "Jalankan" (slide 5 dan 9).

## Menjalankan secara lokal

Tidak perlu instalasi. Buka `index.html` langsung di browser modern (Chrome, Edge, Firefox, Safari). Font diambil dari Google Fonts, jadi kalau offline tampilan memakai font cadangan, dan fungsinya tetap normal.

## Deploy ke GitHub Pages

1. Buat repo baru di GitHub.
2. Upload `index.html` (dan `README.md`) ke root repo.
3. Buka **Settings**, lalu **Pages**.
4. Pada **Build and deployment**, pilih **Deploy from a branch**, branch `main`, folder `/ (root)`, lalu **Save**.
5. Tunggu beberapa menit, lalu buka alamat yang muncul di halaman Pages.

## Struktur repo

```
.
├── index.html           # slide (HTML, CSS, JavaScript dalam satu file)
├── README.md            # dokumen ini
└── presenter-notes.md   # naskah presentasi (opsional diunggah)
```

## Catatan teknis dan batasan

- Kasus yang disimulasikan adalah **penjadwalan task ke VM** dengan data sintetis, bukan data dari CloudSim. Angka hasil simulasi **tidak dimaksudkan menyamai** angka di paper.
- Paper DSOS membahas Travelling Salesman Problem. Di sini ide yang sama dipakai untuk urutan task: kota menjadi task, tur menjadi urutan task, dan panjang tur menjadi makespan.
- Pseudocode kedua paper tidak selalu lengkap, jadi beberapa bagian memakai interpretasi:
  - **DSOS:** gerakan "menuju xbest" di ruang permutasi dilakukan dengan menyalin sebagian urutan dari solusi terbaik, lalu mutasi (swap, inversion, insertion dengan peluang 0,2, 0,5, 0,3).
  - **SMO:** keputusan pada local leader decision memakai perturbation rate seperti SMO standar, local limit diset kecil (nilai D×N di paper tidak akan tercapai dalam 200 iterasi), suku SUM dan SN pada Pers. 30 diartikan sebagai jumlah selisih posisi terhadap anggota kelompok dan ukuran kelompok, dan global leader tidak diacak ulang.
  - Fitness SMO memakai bobot 0,5 / 0,3 / 0,2 untuk makespan, biaya, dan energi, dinormalisasi terhadap Round Robin. Harga VM dibuat naik lebih cepat daripada kecepatannya agar ada trade-off waktu dan biaya.
  - Pada langkah demi langkah SMO, dua fase learning (global dan local leader learning) digabung menjadi satu langkah.
- Pembanding di simulasi adalah Round Robin (dan SMO standar untuk Enhanced SMO).

## Referensi

1. A. E.-S. Ezugwu and A. O. Adewumi, "Discrete symbiotic organisms search algorithm for travelling salesman problem," *Expert Systems With Applications*, vol. 87, pp. 70-78, 2017. doi:10.1016/j.eswa.2017.06.007
2. M. Kumar, K. Dubey, S. Singh, J. K. Samriya, and S. S. Gill, "Experimental performance analysis of cloud resource allocation framework using spider monkey optimization algorithm," *Concurrency and Computation: Practice and Experience*, e7469, 2022. doi:10.1002/cpe.7469

## Kelompok

| No. | Nama Mahasiswa | NRP |
| :---: | :--- | :---: |
| 1 | Ivan Syarifuddin | 5027241045 | 
| 2 | Reza Aziz Simatupang | 5027241051 | 
| 3 | Nisrina Bilqis | 5027241054 | 
| 4 | Ananda Widi Alrafi | 5027241067 | 
| 5 | Muhammad Khosyi Syehab | 5027241089 |
