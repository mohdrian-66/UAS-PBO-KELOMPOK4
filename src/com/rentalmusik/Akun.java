package com.rentalmusik;

import java.time.LocalDate;
import java.util.ArrayList;

/**
 * Kelas abstrak dasar untuk entitas akun pengguna dalam sistem rental.
 * Menerapkan prinsip Abstraction dan Encapsulation.
 *
 * @author Kelompok 4
 */
public abstract class Akun {

    private String id;
    private String username;
    private String password;
    private String nama;

    protected Akun() {
    }

    public Akun(String id, String username, String password, String nama) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.nama = nama;
    }

    /** Memverifikasi kredensial login pengguna secara case-sensitive. */
    public boolean login(String username, String password) {
        if (this.username == null || this.password == null) {
            return false;
        }
        return this.username.equals(username) && this.password.equals(password);
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getNama() {
        return nama;
    }

    public abstract String getPeran();
}

/**
 * Kelas representasi akun Administrator sistem rental.
 *
 * @author Kelompok 4
 */
class Admin extends Akun {

    private DaftarBarang daftarBarang;
    private DaftarTransaksi daftarTransaksi;

    public Admin(String id, String username, String password, String nama,
                 DaftarBarang daftarBarang, DaftarTransaksi daftarTransaksi) {
        super(id, username, password, nama);
        this.daftarBarang = daftarBarang;
        this.daftarTransaksi = daftarTransaksi;
    }

    @Override
    public String getPeran() {
        return "Admin";
    }

    public void setDaftarBarang(DaftarBarang daftarBarang) {
        this.daftarBarang = daftarBarang;
    }

    public void setDaftarTransaksi(DaftarTransaksi daftarTransaksi) {
        this.daftarTransaksi = daftarTransaksi;
    }

    public DaftarBarang getDaftarBarang() {
        return daftarBarang;
    }

    public DaftarTransaksi getDaftarTransaksi() {
        return daftarTransaksi;
    }

    public void tambahBarang(Barang b) throws InputTidakValidException, PenyimpananException {
        daftarBarang.tambahBarang(b);
    }

    public void editBarang(String id, String nama, Double tarif, Double biaya)
            throws BarangTidakDitemukanException, InputTidakValidException, PenyimpananException {
        daftarBarang.editBarang(id, nama, tarif, biaya);
    }

    public void hapusBarang(String id)
            throws BarangTidakDitemukanException, BarangTidakTersediaException, PenyimpananException {
        Barang b = daftarBarang.cariById(id);
        if (b == null) {
            throw new BarangTidakDitemukanException("[GAGAL] Barang dengan ID " + id + " tidak ditemukan.");
        }
        if (!b.isTersedia() || daftarTransaksi.adaBarangDiajukan(id)) {
            throw new BarangTidakTersediaException(
                    "[GAGAL] Barang " + b.getNama() + " tidak dapat dihapus karena sedang disewa atau diajukan dalam transaksi.");
        }
        daftarBarang.hapusBarang(id);
    }

    public ArrayList<Transaksi> lihatTransaksiMenunggu() {
        return daftarTransaksi.filterStatus(StatusTransaksi.MENUNGGU);
    }

    // Admin.terimaTransaksi() (cek ulang ketersediaan barang)
    public void terimaTransaksi(Transaksi t)
            throws BarangTidakTersediaException, TransaksiTidakValidException, PenyimpananException {
        if (t == null) {
            throw new TransaksiTidakValidException("[GAGAL] Objek transaksi kosong.");
        }

        // Periksa tiap barang masih ada dan tersedia
        for (ItemSewa item : t.getItems()) {
            Barang b = daftarBarang.cariById(item.getIdBarang());
            if (b == null || !b.isTersedia()) {
                throw new BarangTidakTersediaException(
                        "[GAGAL] Barang " + item.getNamaBarang()
                                + " sudah tidak tersedia. Transaksi tidak dapat diterima; silakan tolak transaksi.");
            }
        }

        // Kunci ketersediaan barang
        for (ItemSewa item : t.getItems()) {
            Barang b = daftarBarang.cariById(item.getIdBarang());
            if (b != null) {
                b.setTersedia(false);
            }
        }

        t.mulaiSewa(LocalDate.now());
        daftarBarang.simpan();
        daftarTransaksi.simpan();

        new Invoice(t).cetakInvoice();
    }

    public void tolakTransaksi(Transaksi t) throws TransaksiTidakValidException, PenyimpananException {
        if (t == null) {
            throw new TransaksiTidakValidException("[GAGAL] Objek transaksi kosong.");
        }
        t.ubahStatus(StatusTransaksi.DITOLAK);
        daftarTransaksi.simpan();
    }

    // Admin.konfirmasiKembali(): ubah status barang jadi tersedia & cetak invoice akhir
    public void konfirmasiKembali(Transaksi t, LocalDate tglKembali)
            throws TransaksiTidakValidException, PenyimpananException {
        if (t == null) {
            throw new TransaksiTidakValidException("[GAGAL] Objek transaksi kosong.");
        }

        t.selesaikan(tglKembali);

        // Kembalikan barang menjadi tersedia
        for (ItemSewa item : t.getItems()) {
            Barang b = daftarBarang.cariById(item.getIdBarang());
            if (b != null) {
                b.setTersedia(true);
            }
        }

        daftarBarang.simpan();
        daftarTransaksi.simpan();

        new Invoice(t).cetakInvoice();
    }

    public ArrayList<Transaksi> lihatSemuaTransaksi() {
        return daftarTransaksi.getDaftar();
    }
}

/**
 * Kelas representasi akun Penyewa pada sistem rental alat musik.
 *
 * @author Kelompok 4
 */
class Penyewa extends Akun {

    private final KeranjangSewa keranjang;
    private final ArrayList<Transaksi> riwayat;
    private DaftarBarang daftarBarang;
    private DaftarTransaksi daftarTransaksi;

    public Penyewa(String id, String username, String password, String nama,
                   DaftarBarang daftarBarang, DaftarTransaksi daftarTransaksi) {
        super(id, username, password, nama);
        this.keranjang = new KeranjangSewa();
        this.riwayat = new ArrayList<>();
        this.daftarBarang = daftarBarang;
        this.daftarTransaksi = daftarTransaksi;
    }

    @Override
    public String getPeran() {
        return "Penyewa";
    }

    public void setDaftarBarang(DaftarBarang daftarBarang) {
        this.daftarBarang = daftarBarang;
    }

    public void setDaftarTransaksi(DaftarTransaksi daftarTransaksi) {
        this.daftarTransaksi = daftarTransaksi;
    }

    public KeranjangSewa getKeranjang() {
        return keranjang;
    }

    public DaftarBarang getDaftarBarang() {
        return daftarBarang;
    }

    public DaftarTransaksi getDaftarTransaksi() {
        return daftarTransaksi;
    }

    /** Menampilkan daftar seluruh barang dalam katalog ke konsol (format tabel FR-03). */
    public void lihatBarang() {
        ArrayList<Barang> daftar = daftarBarang.getSemuaBarang();
        if (daftar.isEmpty()) {
            System.out.println("[INFO] Belum ada barang.");
            return;
        }

        System.out.printf("%-8s  %-20s  %-8s  %10s  %14s  %-9s%n",
                "ID", "Nama", "Kategori", "Tarif/Hari", "Biaya Tambahan", "Status");
        System.out.println("--------  --------------------  --------  ----------  --------------  ---------");
        for (Barang b : daftar) {
            String status = b.isTersedia() ? "Tersedia" : "Disewa";
            System.out.printf("%-8s  %-20s  %-8s  %10s  %14s  %-9s%n",
                b.getId(),
                b.getNama(),
                b.getKategori().getNamaTampil(),
                FormatHelper.rupiah(b.getTarifHarian()),
                FormatHelper.rupiah(b.getBiayaTambahan()),
                status);
        }
    }

    public void tambahKeKeranjang(String idBarang)
            throws BarangTidakDitemukanException, BarangTidakTersediaException, InputTidakValidException {
        if (idBarang == null || idBarang.trim().isEmpty()) {
            throw new BarangTidakDitemukanException("[GAGAL] ID barang tidak valid.");
        }
        Barang b = daftarBarang.cariById(idBarang);
        if (b == null) {
            throw new BarangTidakDitemukanException("Barang dengan ID " + idBarang.trim().toUpperCase() + " tidak ditemukan.");
        }
        if (!b.isTersedia()) {
            throw new BarangTidakTersediaException(
                    "[GAGAL] Barang " + b.getNama() + " sedang disewa dan tidak dapat dimasukkan ke keranjang.");
        }
        keranjang.tambahBarang(b);
        System.out.printf("[OK] %s ditambahkan ke keranjang. Isi keranjang: %d barang.%n",
                b.getNama(), keranjang.getJumlah());
    }

    public void hapusDariKeranjang(String idBarang) throws BarangTidakDitemukanException {
        keranjang.hapusBarang(idBarang);
    }

    public void lihatKeranjang() {
        if (keranjang.isKosong()) {
            System.out.println("[INFO] Keranjang kosong.");
            return;
        }

        System.out.printf("%-8s  %-20s  %-8s  %10s  %14s  %14s%n",
                "ID", "Nama", "Kategori", "Tarif/Hari", "Biaya Tambahan", "Est. 1 Hari");
        System.out.println("--------  --------------------  --------  ----------  --------------  --------------");
        for (Barang b : keranjang.getBarang()) {
            System.out.printf("%-8s  %-20s  %-8s  %10s  %14s  %14s%n",
                    b.getId(),
                    b.getNama(),
                    b.getKategori().getNamaTampil(),
                    FormatHelper.rupiah(b.getTarifHarian()),
                    FormatHelper.rupiah(b.getBiayaTambahan()),
                    FormatHelper.rupiah(b.hitungBiaya(1)));
        }
    }

    // Penyewa.buatTransaksi() (pemanggilan polimorfik hitungBiaya)
    public Transaksi buatTransaksi(int lamaSewa, Pembayaran p)
            throws InputTidakValidException, BarangTidakDitemukanException,
            BarangTidakTersediaException, PembayaranGagalException, PenyimpananException {
        if (keranjang.isKosong()) {
            throw new InputTidakValidException("[GAGAL] Keranjang kosong. Tambahkan barang terlebih dahulu.");
        }

        // Validasi ketersediaan terkini seluruh barang di keranjang
        for (Barang b : keranjang.getBarang()) {
            Barang terkini = daftarBarang.cariById(b.getId());
            if (terkini == null) {
                throw new BarangTidakDitemukanException(
                        "Barang dengan ID " + b.getId() + " tidak ditemukan.");
            }
            if (!terkini.isTersedia()) {
                throw new BarangTidakTersediaException(
                        "[GAGAL] Barang " + terkini.getNama() + " sudah tidak tersedia.");
            }
        }

        // Buat snapshot item sewa (hitungBiaya dipanggil polimorfik di ItemSewa)
        ArrayList<ItemSewa> snapshotItems = new ArrayList<>();
        double subtotal = 0.0;
        for (Barang b : keranjang.getBarang()) {
            ItemSewa item = new ItemSewa(b, lamaSewa);
            snapshotItems.add(item);
            subtotal += item.getBiayaTotal();
        }

        double totalTagihan = subtotal + p.getBiayaAdmin();

        String idCalon = daftarTransaksi.lihatNomorBerikutnya();
        if (p instanceof QRIS qris) {
            qris.setKodeTransaksi(idCalon);
        }

        // Proses pembayaran
        p.prosesPembayaran(totalTagihan);

        // Jika berhasil, buat dan commit transaksi
        Transaksi baru = new Transaksi(idCalon, this, snapshotItems, lamaSewa, LocalDate.now(), p);
        daftarTransaksi.tambah(baru);
        keranjang.kosongkan();

        return baru;
    }

    public void batalkanTransaksi(Transaksi t) throws TransaksiTidakValidException, PenyimpananException {
        if (t == null) {
            throw new TransaksiTidakValidException("[GAGAL] Transaksi tidak ditemukan.");
        }
        if (t.getStatus() != StatusTransaksi.MENUNGGU) {
            throw new TransaksiTidakValidException(
                    "[GAGAL] Transaksi " + t.getId() + " tidak dapat dibatalkan karena tidak berstatus MENUNGGU.");
        }
        t.ubahStatus(StatusTransaksi.DIBATALKAN);
        daftarTransaksi.simpan();
    }

    public void lihatRiwayat() {
        ArrayList<Transaksi> list = getRiwayat();
        if (list.isEmpty()) {
            System.out.println("[INFO] Belum ada riwayat sewa.");
            return;
        }

        System.out.printf("%-10s  %-12s  %-12s  %-12s  %12s  %-10s%n",
                "ID", "Tgl Sewa", "Jatuh Tempo", "Tgl Kembali", "Total", "Status");
        System.out.println("----------  ------------  ------------  ------------  ------------  ----------");
        for (Transaksi t : list) {
            System.out.printf("%-10s  %-12s  %-12s  %-12s  %12s  %-10s%n",
                    t.getId(),
                    FormatHelper.tanggal(t.getTanggalSewa()),
                    FormatHelper.tanggal(t.getTanggalJatuhTempo()),
                    FormatHelper.tanggal(t.getTanggalKembali()),
                    FormatHelper.rupiah(t.hitungTotalAkhir()),
                    t.getStatus().name());
        }
    }

    public ArrayList<Transaksi> getRiwayat() {
        this.riwayat.clear();
        if (daftarTransaksi != null) {
            ArrayList<Transaksi> miliknya = daftarTransaksi.filterPenyewa(this);
            for (Transaksi t : miliknya) {
                if (t.getStatus() == StatusTransaksi.DISEWA || t.getStatus() == StatusTransaksi.SELESAI) {
                    this.riwayat.add(t);
                }
            }
        }
        return new ArrayList<>(this.riwayat);
    }
}
