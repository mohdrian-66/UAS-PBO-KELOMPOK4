package com.rentalmusik;

/**
 * Kelas abstrak dasar yang merepresentasikan sebuah alat musik dalam sistem rental.
 * Menerapkan prinsip Abstraction dan Encapsulation.
 *
 * @author Kelompok 4
 */
public abstract class Barang {

    private String id;
    private String nama;
    private double tarifHarian;
    private boolean tersedia;

    protected Barang() {
    }

    public Barang(String id, String nama, double tarifHarian, boolean tersedia)
            throws InputTidakValidException {
        this.id = id;
        setNama(nama);
        setTarifHarian(tarifHarian);
        this.tersedia = tersedia;
    }

    /** Menghitung biaya dasar sewa barang sebelum biaya tambahan. */
    protected double hitungBiayaDasar(int hari) {
        return this.tarifHarian * hari;
    }

    /** Menghitung total biaya sewa untuk durasi hari tertentu secara polimorfik. */
    public abstract double hitungBiaya(int hari);

    public abstract KategoriBarang getKategori();

    public abstract double getBiayaTambahan();

    public abstract void setBiayaTambahan(double biaya) throws InputTidakValidException;

    public abstract String getNamaBiayaTambahan();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNama() {
        return nama;
    }

    public void setNama(String nama) throws InputTidakValidException {
        if (nama == null) {
            throw new InputTidakValidException("Nama barang tidak valid atau sudah ada.");
        }
        String trimmed = nama.trim();
        if (trimmed.isEmpty() || trimmed.length() > Konstanta.PANJANG_NAMA_MAKS) {
            throw new InputTidakValidException("Nama barang tidak valid atau sudah ada.");
        }
        if (trimmed.contains("|") || trimmed.contains(";") || trimmed.contains("~")) {
            throw new InputTidakValidException("Nama barang tidak valid atau sudah ada.");
        }
        this.nama = trimmed;
    }

    public double getTarifHarian() {
        return tarifHarian;
    }

    public void setTarifHarian(double tarifHarian) throws InputTidakValidException {
        if (tarifHarian < Konstanta.TARIF_MIN || tarifHarian > Konstanta.TARIF_MAKS) {
            throw new InputTidakValidException("Tarif harian harus antara Rp1.000 dan Rp10.000.000.");
        }
        this.tarifHarian = tarifHarian;
    }

    public boolean isTersedia() {
        return tersedia;
    }

    public boolean getTersedia() {
        return tersedia;
    }

    public void setTersedia(boolean tersedia) {
        this.tersedia = tersedia;
    }
}

/**
 * Enumerasi kategori alat musik yang tersedia di sistem rental.
 *
 * @author Kelompok 4
 */
enum KategoriBarang {

    PETIK("Petik", "PTK", "Biaya Perawatan Senar", Konstanta.BIAYA_PERAWATAN_SENAR_DEFAULT),
    PUKUL("Pukul", "PKL", "Biaya Angkut/Setup", Konstanta.BIAYA_ANGKUT_SETUP_DEFAULT),
    TIUP("Tiup", "TPU", "Biaya Sanitasi", Konstanta.BIAYA_SANITASI_DEFAULT);

    private final String namaTampil;
    private final String prefixId;
    private final String namaBiayaTambahan;
    private final double biayaTambahanDefault;

    KategoriBarang(String namaTampil, String prefixId, String namaBiayaTambahan, double biayaTambahanDefault) {
        this.namaTampil = namaTampil;
        this.prefixId = prefixId;
        this.namaBiayaTambahan = namaBiayaTambahan;
        this.biayaTambahanDefault = biayaTambahanDefault;
    }

    public String getNamaTampil() {
        return namaTampil;
    }

    public String getPrefixId() {
        return prefixId;
    }

    public String getNamaBiayaTambahan() {
        return namaBiayaTambahan;
    }

    public double getBiayaTambahanDefault() {
        return biayaTambahanDefault;
    }
}

/**
 * Kelas representasi alat musik kategori Petik.
 *
 * @author Kelompok 4
 */
class AlatPetik extends Barang {

    private double biayaPerawatanSenar;

    public AlatPetik(String id, String nama, double tarifHarian, boolean tersedia)
            throws InputTidakValidException {
        this(id, nama, tarifHarian, Konstanta.BIAYA_PERAWATAN_SENAR_DEFAULT, tersedia);
    }

    public AlatPetik(String id, String nama, double tarifHarian, double biayaPerawatanSenar, boolean tersedia)
            throws InputTidakValidException {
        super(id, nama, tarifHarian, tersedia);
        setBiayaTambahan(biayaPerawatanSenar);
    }

    // hitungBiaya() di AlatPetik: tarif dasar + biaya perawatan senar
    @Override
    public double hitungBiaya(int hari) {
        return hitungBiayaDasar(hari) + this.biayaPerawatanSenar;
    }

    @Override
    public KategoriBarang getKategori() {
        return KategoriBarang.PETIK;
    }

    @Override
    public double getBiayaTambahan() {
        return this.biayaPerawatanSenar;
    }

    @Override
    public void setBiayaTambahan(double biaya) throws InputTidakValidException {
        if (biaya < 0 || biaya > Konstanta.BIAYA_TAMBAHAN_MAKS) {
            throw new InputTidakValidException("Biaya tambahan harus antara Rp0 dan Rp10.000.000.");
        }
        this.biayaPerawatanSenar = biaya;
    }

    @Override
    public String getNamaBiayaTambahan() {
        return "Biaya Perawatan Senar";
    }
}

/**
 * Kelas representasi alat musik kategori Pukul / Perkusi.
 *
 * @author Kelompok 4
 */
class AlatPukul extends Barang {

    private double biayaAngkutSetup;

    public AlatPukul(String id, String nama, double tarifHarian, boolean tersedia)
            throws InputTidakValidException {
        this(id, nama, tarifHarian, Konstanta.BIAYA_ANGKUT_SETUP_DEFAULT, tersedia);
    }

    public AlatPukul(String id, String nama, double tarifHarian, double biayaAngkutSetup, boolean tersedia)
            throws InputTidakValidException {
        super(id, nama, tarifHarian, tersedia);
        setBiayaTambahan(biayaAngkutSetup);
    }

    // hitungBiaya() di AlatPukul: tarif dasar + biaya angkut/setup
    @Override
    public double hitungBiaya(int hari) {
        return hitungBiayaDasar(hari) + this.biayaAngkutSetup;
    }

    @Override
    public KategoriBarang getKategori() {
        return KategoriBarang.PUKUL;
    }

    @Override
    public double getBiayaTambahan() {
        return this.biayaAngkutSetup;
    }

    @Override
    public void setBiayaTambahan(double biaya) throws InputTidakValidException {
        if (biaya < 0 || biaya > Konstanta.BIAYA_TAMBAHAN_MAKS) {
            throw new InputTidakValidException("Biaya tambahan harus antara Rp0 dan Rp10.000.000.");
        }
        this.biayaAngkutSetup = biaya;
    }

    @Override
    public String getNamaBiayaTambahan() {
        return "Biaya Angkut/Setup";
    }
}

/**
 * Kelas representasi alat musik kategori Tiup.
 *
 * @author Kelompok 4
 */
class AlatTiup extends Barang {

    private double biayaSanitasi;

    public AlatTiup(String id, String nama, double tarifHarian, boolean tersedia)
            throws InputTidakValidException {
        this(id, nama, tarifHarian, Konstanta.BIAYA_SANITASI_DEFAULT, tersedia);
    }

    public AlatTiup(String id, String nama, double tarifHarian, double biayaSanitasi, boolean tersedia)
            throws InputTidakValidException {
        super(id, nama, tarifHarian, tersedia);
        setBiayaTambahan(biayaSanitasi);
    }

    // hitungBiaya() di AlatTiup: tarif dasar + biaya sanitasi
    @Override
    public double hitungBiaya(int hari) {
        return hitungBiayaDasar(hari) + this.biayaSanitasi;
    }

    @Override
    public KategoriBarang getKategori() {
        return KategoriBarang.TIUP;
    }

    @Override
    public double getBiayaTambahan() {
        return this.biayaSanitasi;
    }

    @Override
    public void setBiayaTambahan(double biaya) throws InputTidakValidException {
        if (biaya < 0 || biaya > Konstanta.BIAYA_TAMBAHAN_MAKS) {
            throw new InputTidakValidException("Biaya tambahan harus antara Rp0 dan Rp10.000.000.");
        }
        this.biayaSanitasi = biaya;
    }

    @Override
    public String getNamaBiayaTambahan() {
        return "Biaya Sanitasi";
    }
}
