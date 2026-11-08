# DSOS dan SMO untuk Cloud Task Scheduling

Slide interaktif (satu file HTML) untuk demo week 10 matakuliah Strategi Komputasi Awan. Isinya menjelaskan dua algoritma metaheuristik terinspirasi alam dan cara memakainya untuk menjadwalkan task di cloud:

- **DSOS** (Discrete Symbiotic Organisms Search): meniru simbiosis antar organisme.
- **SMO** (Spider Monkey Optimization) dan **Enhanced SMO**: meniru monyet laba-laba mencari makanan.

Demo online: `https://<username>.github.io/<nama-repo>/`

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

# Simulasi CloudSim Plus: Penjadwalan Task dengan DSOS dan SMO

Simulasi penjadwalan task di cloud menggunakan **CloudSim Plus** dengan topologi 1 datacenter, 3 host, dan 8 VM per host (24 VM). Beban kerja berasal dari dataset **GoCJ** (Google Cloud Jobs). Empat metode penjadwalan dibandingkan:

| Metode | Keterangan |
|---|---|
| Round Robin | Cloudlet dibagi bergiliran ke VM (pembanding) |
| DSOS | Discrete Symbiotic Organisms Search |
| SMO | Spider Monkey Optimization |
| Enhanced SMO | SMO dengan modifikasi local leader phase |

Tugas matakuliah Strategi Komputasi Awan.

## Topologi simulasi

| Komponen | Jumlah | Spesifikasi |
|---|---|---|
| Datacenter | 1 | Kebijakan alokasi VM default (`VmAllocationPolicySimple`) |
| Host | 3 | 8 PE, RAM 16.384 MB, bandwidth 8000 Mbps, storage 80.000 MB |
| VM | 24 (8 per host) | 1 PE, RAM 2048 MB, bandwidth 1000 Mbps, kecepatan 500, 1000, 1500, atau 2000 MIPS (bergantian) |
| Cloudlet | sesuai dataset | Satu cloudlet per job, panjang dalam MI |

Kapasitas host dibuat pas untuk 8 VM, sehingga setiap host menampung tepat 8 VM. Setiap VM memakai `CloudletSchedulerSpaceShared`: satu cloudlet diproses pada satu waktu dan sisanya antre.

## Struktur proyek

```
.
├── pom.xml
├── data/
│   └── GoCJ_Dataset_100.txt              # dataset (lihat bagian Dataset)
└── src/main/
    ├── java/org/example/
    │   ├── ThreeHostsEightVms.java       # program utama: topologi CloudSim, empat skenario, tabel hasil
    │   ├── SchedulingProblem.java        # model masalah: makespan, biaya, energi, fitness
    │   ├── Dsos.java                     # algoritma DSOS
    │   ├── Smo.java                      # algoritma SMO dan Enhanced SMO
    │   └── Solution.java                 # hasil algoritma: jadwal, fitness, riwayat konvergensi
    └── resources/
        └── logback.xml                   # level log (hanya WARN)
```

## Prasyarat

- Java 17 atau lebih baru
- Maven 3.x
- Dependensi (diunduh otomatis oleh Maven): `org.cloudsimplus:cloudsim-plus:7.3.2`

CloudSim Plus versi 8 ke atas memindahkan package ke `org.cloudsimplus.*` dan mengganti nama artifact, jadi kode ini dikunci ke versi 7.3.2.

## Dataset

Dataset **GoCJ** (Google Cloud Jobs) berisi panjang job dalam MI. Setiap file bernama `GoCJ_Dataset_XXX.txt`, dengan XXX jumlah job, dan setiap baris berisi panjang satu job.

1. Unduh dari Mendeley Data: <https://data.mendeley.com/datasets/b7bp6xhrcd/1>
2. Letakkan file yang dipakai di folder `data/`, misalnya `data/GoCJ_Dataset_100.txt`.

Program membaca kolom pertama tiap baris dan melewati baris kosong serta header, sehingga versi Enhanced GoCJ (CSV dengan kolom panjang, SLA, waktu kedatangan) juga terbaca, tetapi hanya kolom pertamanya yang dipakai.

## Cara menjalankan

Dari folder yang berisi `pom.xml`:

```
mvn -q clean compile exec:java "-Dexec.mainClass=org.example.ThreeHostsEightVms" "-Dexec.args=data/GoCJ_Dataset_100.txt"
```

Argumen program (dalam `-Dexec.args`, dipisah spasi), semuanya opsional:

| Urutan | Arti | Default |
|---|---|---|
| 1 | Path dataset GoCJ | `data/GoCJ_Dataset_100.txt` |
| 2 | Jumlah job maksimum yang dipakai | semua job di file |
| 3 | Jumlah iterasi algoritma | 300 |

Contoh memakai 100 job dan 500 iterasi:

```
mvn -q clean compile exec:java "-Dexec.mainClass=org.example.ThreeHostsEightVms" "-Dexec.args=data/GoCJ_Dataset_100.txt 100 500"
```

Pada PowerShell, tanda kutip di sekitar opsi `-D` wajib ada.

## Keluaran

Program mencetak jumlah VM per host (untuk membuktikan 8 VM per host), lalu tabel perbandingan dengan kolom:

- **Fitness**: nilai fitness jadwal, dinormalisasi terhadap Round Robin (1,0 = sama dengan Round Robin, makin kecil makin baik).
- **Makespan est.**: makespan hasil hitungan algoritma.
- **Makespan sim.**: makespan hasil simulasi CloudSim (termasuk jeda pengiriman broker sekitar 0,1 detik).
- **Biaya** dan **Energi**: perkiraan dari model sederhana (lihat bagian Model fitness).
- **Waktu algo**: lama algoritma menghitung jadwal, dalam milidetik.
- **vs RR**: persentase penurunan makespan simulasi dibanding Round Robin.

Angka akan berbeda tergantung dataset, jumlah iterasi, dan seed.

## Konfigurasi

Konstanta di `ThreeHostsEightVms.java`:

| Konstanta | Fungsi |
|---|---|
| `VM_MIPS_LEVELS` | Kecepatan VM, bergantian per VM. Ubah menjadi `{1000}` untuk VM homogen |
| `HOST_PE_MIPS` | Kecepatan PE host, harus sama atau lebih besar dari VM tercepat |
| `WEIGHTS` | Bobot fitness `{makespan, biaya, energi}`. Default `{1, 0, 0}` (hanya makespan). Contoh multi-objektif: `{0.5, 0.3, 0.2}` |
| `POPULATION` | Ukuran populasi (organisme atau monyet), default 50 |
| `SEED` | Seed acak, supaya hasil bisa diulang |
| `PRINT_CLOUDLET_TABLE` | `true` untuk mencetak tabel cloudlet CloudSim di setiap skenario |

Hapus `src/main/resources/logback.xml` untuk melihat log INFO lengkap CloudSim Plus (misalnya alokasi VM ke host).

## Cara kerja singkat

1. Dataset dibaca dan menjadi daftar cloudlet.
2. Setiap algoritma menghitung jadwal: cloudlet ke-i dikirim ke VM ke-j.
3. Jadwal dipasang ke cloudlet dengan `cloudlet.setVm(vm)` sebelum simulasi dimulai.
4. CloudSim menjalankan simulasi, dan makespan dihitung dari waktu selesai cloudlet terakhir.

**DSOS:** organisme adalah permutasi cloudlet. Setiap permutasi diterjemahkan menjadi jadwal dengan memasukkan cloudlet sesuai urutan ke VM yang selesai paling awal. Tiga fase (mutualism, commensalism, parasitism) memperbarui populasi dengan operator swap, inversion, dan insertion; kandidat hanya diterima bila lebih baik.

**SMO dan Enhanced SMO:** monyet adalah vektor sepanjang jumlah cloudlet, dan nilai tiap dimensi dibulatkan ke bawah menjadi nomor VM. Fasenya: local leader, global leader, leader learning, local leader decision, dan global leader decision (fission-fusion). Pada Enhanced SMO, monyet yang macet memakai rumus Pers. 30 dan 31 paper.

## Model fitness

Fitness = `w0 × makespan + w1 × biaya + w2 × energi`, masing-masing dibagi nilai Round Robin.

- **Makespan**: waktu sibuk terbesar di antara VM, dengan waktu sibuk VM = jumlah (panjang cloudlet / MIPS VM).
- **Biaya**: waktu sibuk × harga VM per jam. Harga adalah asumsi buatan sendiri, `0,102 × (MIPS/1000)^1,3`, yang naik lebih cepat daripada kecepatan VM agar ada trade-off waktu dan biaya.
- **Energi**: model linear dengan daya antara 70 W (idle) dan 200 W (penuh), mengikuti bentuk Pers. 12 paper SMO.

## Catatan dan batasan

- Pseudocode paper DSOS dan SMO tidak sepenuhnya rinci, jadi beberapa bagian memakai interpretasi:
  - DSOS: gerakan "menuju xbest" di ruang permutasi dilakukan dengan menyalin sebagian urutan dari solusi terbaik, lalu mutasi.
  - SMO: perturbation rate seperti SMO standar, local limit diset 20, suku SUM dan SN pada Pers. 30 diartikan sebagai jumlah selisih posisi terhadap anggota kelompok dan ukuran kelompok, dan global leader tidak diacak ulang.
- Hasil bergantung pada seed dan dataset. Untuk kesimpulan yang lebih kuat, jalankan beberapa seed dan laporkan rata-ratanya.
- Paper DSOS membahas Travelling Salesman Problem, bukan penjadwalan cloud. Di sini ide yang sama dipakai untuk urutan cloudlet.
- Data GoCJ dan model biaya serta energi bersifat sintetis atau disederhanakan, jadi angka di sini tidak dimaksudkan menyamai angka di paper.


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