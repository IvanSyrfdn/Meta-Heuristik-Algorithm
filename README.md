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
├── hasil/
│   ├── hasil.json                        # dibuat otomatis oleh program (data perbandingan)
│   ├── perbandingan.html                 # halaman visualisasi perbandingan
│   ├── ss-topologi.png                   # screenshot alokasi VM ke host
│   ├── ss-tabel.png                      # screenshot tabel perbandingan
│   ├── ss-makespan.png                   # screenshot diagram makespan
│   ├── ss-biaya.png                      # screenshot diagram biaya
│   ├── ss-energi.png                     # screenshot diagram energi
│   └── ss-konvergensi.png                # screenshot grafik konvergensi
└── src/main/
    ├── java/org/example/
    │   ├── ThreeHostsEightVms.java       # program utama: topologi CloudSim, empat skenario, tabel hasil
    │   ├── SchedulingProblem.java        # model masalah: makespan, biaya, energi, fitness
    │   ├── Dsos.java                     # algoritma DSOS
    │   ├── Smo.java                      # algoritma SMO dan Enhanced SMO
    │   ├── Comparison.java               # tabel perbandingan di konsol dan berkas hasil.json
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
| 4 | Jumlah jalan (seed berbeda) per algoritma | 5 |

Contoh memakai 100 job, 500 iterasi, dan 10 jalan per algoritma:

```
mvn -q clean compile exec:java "-Dexec.mainClass=org.example.ThreeHostsEightVms" "-Dexec.args=data/GoCJ_Dataset_100.txt 100 500 10"
```

Pada PowerShell, tanda kutip di sekitar opsi `-D` wajib ada.

## Keluaran

Program mencetak jumlah VM per host (untuk membuktikan 8 VM per host), lalu tabel perbandingan dengan kolom:

- **Fitness**: nilai fitness jadwal, dinormalisasi terhadap Round Robin (1,0 = sama dengan Round Robin, makin kecil makin baik).
- **Makespan sim.**: makespan hasil simulasi CloudSim (termasuk jeda pengiriman broker sekitar 0,1 detik), rata-rata dan simpangan baku dari semua jalan.
- **Terbaik**: makespan terkecil di antara semua jalan.
- **Biaya** dan **Energi**: perkiraan dari model sederhana (lihat bagian Model fitness).
- **Waktu algo**: lama algoritma menghitung jadwal, dalam milidetik.
- **vs RR**: persentase penurunan makespan rata-rata dibanding Round Robin.

Round Robin deterministik, jadi hanya dijalankan sekali. DSOS, SMO, dan Enhanced SMO dijalankan beberapa kali dengan seed berbeda karena hasilnya acak. Angka akan berbeda tergantung dataset, jumlah iterasi, dan seed.

## Visualisasi perbandingan

Setelah program selesai, hasilnya disimpan ke `hasil/hasil.json`. Untuk melihat grafiknya:

1. Buka `hasil/perbandingan.html` di browser.
2. Pilih berkas `hasil/hasil.json` (atau tarik berkasnya ke halaman).

Halaman menampilkan diagram batang per metrik (makespan, biaya, energi, waktu algoritma) lengkap dengan rentang minimum sampai maksimum antar jalan, grafik konvergensi fitness per iterasi, dan tabel ringkasan. Kalau halaman dibuka lewat server (misalnya GitHub Pages atau `python -m http.server` dari folder `hasil`), `hasil.json` yang berada di folder yang sama dimuat otomatis.

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

# Laporan Hasil Eksperimen

## Ringkasan

Eksperimen ini membandingkan tiga algoritma metaheuristik, yaitu **DSOS**, **SMO**, dan **Enhanced SMO**, dengan **Round Robin** sebagai pembanding. Simulasi dijalankan di CloudSim Plus 7.3.2 dengan dataset GoCJ (100 job) pada 24 VM heterogen (topologi dan model fitness: lihat bagian "Topologi simulasi" dan "Model fitness" di atas).

Temuan utama:

- Ketiga algoritma metaheuristik menurunkan makespan secara signifikan dibanding Round Robin (**59,2% sampai 74,7%**).
- **DSOS** memberi hasil terbaik dan paling stabil: makespan rata-rata **461,0 detik**, turun **74,7%** dari Round Robin, dengan simpangan baku hanya **1,9 detik**.
- **Enhanced SMO tidak lebih baik dari SMO standar** pada konfigurasi ini (743,4 vs 692,3 detik). Kemungkinan penyebabnya dibahas pada bagian Analisis.

## Parameter eksperimen

| Parameter | Nilai |
|---|---|
| Dataset | `GoCJ_Dataset_100.txt` (100 job) |
| Bobot fitness (`WEIGHTS`) | `{1, 0, 0}` (hanya makespan dioptimasi; biaya dan energi tetap dihitung dan dilaporkan) |
| Ukuran populasi | 50 |
| Jumlah iterasi | 300 |
| Jumlah jalan per algoritma (seed berbeda) | 5 |
| Round Robin | 1 jalan (deterministik) |
| SMO: local limit | 20 |

## Verifikasi topologi

Log awal simulasi membuktikan 24 VM terbagi merata ke 3 host, masing-masing 8 VM.

<img width="886" height="708" alt="image" src="https://github.com/user-attachments/assets/779ec0f4-bcdc-40c2-a3dd-65916c617cc3" />

*Gambar 1. Log alokasi VM. VM 0 sampai 23 terbagi bergantian ke Host 0, 1, dan 2.*

## Hasil

### Tabel perbandingan

| Metode | Jalan | Makespan sim. (s) | Terbaik (s) | Biaya ($) | Energi (Wh) | Waktu algo | vs RR |
|---|---|---|---|---|---|---|---|
| Round Robin | 1 | 1823,4 ± 0,0 | 1823,4 | 0,404 | 1336,2 | 0 ms | 0,0% |
| **DSOS** | 5 | **461,0 ± 1,9** | **458,6** | 0,427 | **599,9** | 606 ms | **74,7%** |
| SMO | 5 | 692,3 ± 20,9 | 656,0 | 0,429 | 704,7 | 118 ms | 62,0% |
| Enhanced SMO | 5 | 743,4 ± 40,3 | 712,8 | 0,423 | 750,0 | 114 ms | 59,2% |

*Makespan sim. adalah hasil CloudSim, termasuk jeda pengiriman broker sekitar 0,1 detik. Nilai adalah rata-rata ± simpangan baku dari seluruh jalan.*

<img width="904" height="367" alt="image" src="https://github.com/user-attachments/assets/71758efd-337e-4189-82d4-c93ec53080e8" />

*Gambar 2. Tabel perbandingan dari `perbandingan.html`.*

### Visualisasi

<img width="1491" height="771" alt="image" src="https://github.com/user-attachments/assets/fa2e02c5-2240-456d-8612-deeffde033c5" />

*Gambar 3. Makespan rata-rata per metode. Garis tipis menunjukkan nilai terkecil sampai terbesar antar jalan.*

<img width="738" height="383" alt="image" src="https://github.com/user-attachments/assets/b688624c-c667-43ae-880e-2f34b7792300" />

*Gambar 4. Biaya per metode.*

<img width="737" height="385" alt="image" src="https://github.com/user-attachments/assets/e93e3ddf-0604-4d63-b368-e8541ceab518" />

*Gambar 5. Energi per metode.*

<img width="1465" height="661" alt="image" src="https://github.com/user-attachments/assets/430e4063-4a8e-48e1-a471-fd55dc9832ac" />

*Gambar 6. Konvergensi fitness per iterasi (rata-rata semua jalan). Hijau: DSOS, kuning: SMO, ungu: Enhanced SMO, garis putus-putus merah: Round Robin (1,0). Makin kecil makin baik.*

Grafik konvergensi menunjukkan perbedaan perilaku yang jelas. DSOS mulai dari fitness yang sudah rendah (sekitar 0,30), turun ke sekitar 0,26 dalam beberapa iterasi pertama, lalu praktis datar sampai iterasi ke-300, sehingga DSOS konvergen sangat cepat. SMO dan Enhanced SMO mulai dari fitness sekitar 0,57 dan turun bertahap. Keduanya mirip di awal, tetapi Enhanced SMO tertahan di plateau sekitar 0,47 pada kira-kira iterasi 50 sampai 130, sementara SMO terus turun dan berakhir di sekitar 0,39 (Enhanced SMO sekitar 0,41). Nilai akhir ini konsisten dengan makespan di tabel. Kedua varian SMO masih menurun pelan di iterasi ke-300, artinya belum sepenuhnya konvergen. Menambah iterasi (misalnya 500 atau 1000) kemungkinan mempersempit selisih dengan DSOS, tetapi ini perlu dibuktikan lewat run lanjutan.

### Perbandingan terhadap Round Robin

| Metode | Makespan | Energi | Biaya |
|---|---|---|---|
| DSOS | −74,7% | −55,1% | +5,7% |
| SMO | −62,0% | −47,3% | +6,2% |
| Enhanced SMO | −59,2% | −43,9% | +4,7% |

## Analisis

**DSOS paling unggul dan paling stabil.** Makespan rata-rata DSOS (461,0 s) jauh di bawah SMO (692,3 s) dan Enhanced SMO (743,4 s). Simpangan bakunya hanya 1,9 s, sedangkan SMO 20,9 s dan Enhanced SMO 40,3 s. Artinya DSOS hampir selalu menemukan solusi dengan kualitas sama di seed berbeda, sedangkan hasil SMO lebih bergantung pada seed. Salah satu penjelasan yang masuk akal adalah representasi permutasi pada DSOS dengan dekoder "taruh task ke VM yang selesai paling awal", yang sudah membawa heuristik penyeimbang beban. Representasi SMO (vektor bernilai kontinu yang dibulatkan) lebih lemah dalam hal ini, walaupun ini hipotesis yang belum diuji secara terpisah.

**Waktu komputasi DSOS lebih lama.** DSOS butuh sekitar 606 ms, sekitar 5 kali lipat SMO (118 ms) dan Enhanced SMO (114 ms), tetapi untuk 100 job selisih ini masih tidak signifikan dibanding penurunan makespan yang didapat.

**Enhanced SMO tidak lebih baik dari SMO standar.** Pada konfigurasi ini Enhanced SMO lebih lambat sekitar 51 detik dan lebih tidak stabil. Beberapa kemungkinan penyebabnya:

1. Bobot `{1, 0, 0}` hanya menilai makespan, padahal paper SMO mengevaluasi fitness multi-objektif.
2. Pseudocode paper tidak lengkap, sehingga suku SUM dan SN pada Pers. 30 diartikan sendiri.
3. Hanya 5 jalan dan 300 iterasi, jumlah yang kecil untuk menarik kesimpulan tegas. Grafik konvergensi juga menunjukkan kedua varian SMO belum datar di iterasi ke-300.

**Trade-off biaya.** Biaya semua metaheuristik sedikit di atas Round Robin (naik 4,7% sampai 6,2%) walaupun makespan dan energi turun drastis. Ini wajar karena yang dioptimasi hanya makespan: task panjang diarahkan ke VM cepat yang harganya naik lebih cepat daripada kecepatannya. Untuk menekan biaya, bobot `w1` perlu dinaikkan.

**Energi turun jauh.** Energi turun 43,9% sampai 55,1% karena VM tidak lama menganggur menunggu VM lain selesai.

## Keterbatasan eksperimen

- Hanya satu dataset dan satu ukuran (100 job). Skalabilitas ke ratusan atau ribuan job belum diuji.
- Hanya 5 jalan per algoritma dan belum ada uji signifikansi statistik (misalnya Wilcoxon).
- Pembandingnya hanya Round Robin, yang merupakan baseline lemah. Belum ada pembanding seperti Min-Min, Max-Min, MCT, GA, atau PSO.
- Iterasi, bukan jumlah evaluasi fitness, yang disamakan antar algoritma, sehingga perbandingan belum sepenuhnya adil dari sisi budget komputasi.
- Skenario multi-objektif (`WEIGHTS = {0.5, 0.3, 0.2}`) dan iterasi lebih banyak belum dijalankan.
- Model biaya dan energi disederhanakan dan sintetis, sehingga angkanya tidak dimaksudkan menyamai angka di paper.

## Kesimpulan

Pada skenario 100 job dengan 24 VM heterogen, DSOS menghasilkan jadwal terbaik: makespan turun 74,7% dibanding Round Robin dengan variasi antar-seed sangat kecil. SMO dan Enhanced SMO juga jauh lebih baik dari Round Robin (62,0% dan 59,2%) tetapi kurang stabil, dan perbaikan Enhanced SMO belum terlihat pada konfigurasi bobot makespan-saja. Eksperimen lanjutan yang disarankan: skenario multi-objektif, iterasi lebih banyak, dataset lebih besar, lebih banyak seed dengan uji statistik, dan pembanding algoritma yang lebih kuat.

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
