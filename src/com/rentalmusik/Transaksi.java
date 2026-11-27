package com.rentalmusik;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

/**
 * Entitas yang merepresentasikan transaksi peminjaman alat musik.
 *
 * @author Kelompok 4
 */
public class Transaksi {

    private String id;
    private Penyewa penyewa;
    private ArrayList<ItemSewa> items;
    private int lamaSewa;
    private LocalDate tanggalDibuat;
    private LocalDate tanggalSewa;
    private LocalDate tanggalJatuhTempo;
    private LocalDate tanggalKembali;
    private StatusTransaksi status;
    private Pembayaran pembayaran;

    public Transaksi(String id, Penyewa penyewa, ArrayList<ItemSewa> items, int lamaSewa,
                     LocalDate tanggalDibuat, Pembayaran pembayaran) {
        this.id = id;
        this.penyewa = penyewa;
        this.items = items != null ? items : new ArrayList<>();
        this.lamaSewa = lamaSewa;
        this.tanggalDibuat = tanggalDibuat;
        this.pembayaran = pembayaran;
        this.status = StatusTransaksi.MENUNGGU;
        this.tanggalSewa = null;
        this.tanggalJatuhTempo = null;
        this.tanggalKembali = null;
    }

    public Transaksi(String id, Penyewa penyewa, ArrayList<ItemSewa> items, int lamaSewa,
                     LocalDate tanggalDibuat, LocalDate tanggalSewa, LocalDate tanggalJatuhTempo,
                     LocalDate tanggalKembali, StatusTransaksi status, Pembayaran pembayaran) {
        this.id = id;
        this.penyewa = penyewa;
        this.items = items != null ? items : new ArrayList<>();
        this.lamaSewa = lamaSewa;
        this.tanggalDibuat = tanggalDibuat;
        this.tanggalSewa = tanggalSewa;
        this.tanggalJatuhTempo = tanggalJatuhTempo;
        this.tanggalKembali = tanggalKembali;
        this.status = status;
        this.pembayaran = pembayaran;
    }

    public double hitungSubtotal() {
        double subtotal = 0.0;
        for (ItemSewa item : items) {
            subtotal += item.getBiayaTotal();
        }
        return subtotal;
    }

    public double hitungBiayaAdmin() {
        if (pembayaran != null) {
            return pembayaran.getBiayaAdmin();
        }
        return 0.0;
    }

    public double hitungTotal() {
        return hitungSubtotal() + hitungBiayaAdmin();
    }

    // hitungHariTerlambat() dan hitungDenda() di Transaksi
    public int hitungHariTerlambat() {
        if (tanggalKembali == null || tanggalJatuhTempo == null) {
            return 0;
        }
        long selisih = ChronoUnit.DAYS.between(tanggalJatuhTempo, tanggalKembali);
        return (int) Math.max(0, selisih);
    }

    // hitungDenda() di Transaksi: selisih hari * jumlah barang * tarif denda
    public double hitungDenda() {
        int hariTerlambat = hitungHariTerlambat();
        return (double) hariTerlambat * items.size() * Konstanta.DENDA_PER_BARANG_PER_HARI;
    }

    public double hitungTotalAkhir() {
        return hitungTotal() + hitungDenda();
    }

    // validasi transisi status di Transaksi.ubahStatus()
    public void ubahStatus(StatusTransaksi baru) throws TransaksiTidakValidException {
        if (this.status == baru) {
            return;
        }

        boolean valid = switch (this.status) {
            case MENUNGGU -> baru == StatusTransaksi.DISEWA
                    || baru == StatusTransaksi.DITOLAK
                    || baru == StatusTransaksi.DIBATALKAN;
            case DISEWA -> baru == StatusTransaksi.SELESAI;
            case SELESAI, DITOLAK, DIBATALKAN -> false;
        };

        if (!valid) {
            throw new TransaksiTidakValidException(
                    "Transisi status dari " + this.status.getNamaTampil()
                            + " ke " + baru.getNamaTampil() + " tidak valid.");
        }

        this.status = baru;
    }

    public void mulaiSewa(LocalDate tanggal) throws TransaksiTidakValidException {
        ubahStatus(StatusTransaksi.DISEWA);
        this.tanggalSewa = tanggal;
        this.tanggalJatuhTempo = tanggal.plusDays(this.lamaSewa);
    }

    public void selesaikan(LocalDate tanggal) throws TransaksiTidakValidException {
        if (this.tanggalSewa != null && tanggal.isBefore(this.tanggalSewa)) {
            throw new TransaksiTidakValidException(
                    "Tanggal kembali tidak boleh sebelum tanggal sewa ("
                            + FormatHelper.tanggal(this.tanggalSewa) + ").");
        }
        ubahStatus(StatusTransaksi.SELESAI);
        this.tanggalKembali = tanggal;
    }

    public String getId() {
        return id;
    }

    public Penyewa getPenyewa() {
        return penyewa;
    }

    public void setPenyewa(Penyewa penyewa) {
        this.penyewa = penyewa;
    }

    public ArrayList<ItemSewa> getItems() {
        return items;
    }

    public int getLamaSewa() {
        return lamaSewa;
    }

    public LocalDate getTanggalDibuat() {
        return tanggalDibuat;
    }

    public LocalDate getTanggalSewa() {
        return tanggalSewa;
    }

    public LocalDate getTanggalJatuhTempo() {
        return tanggalJatuhTempo;
    }

    public LocalDate getTanggalKembali() {
        return tanggalKembali;
    }

    public StatusTransaksi getStatus() {
        return status;
    }

    public Pembayaran getPembayaran() {
        return pembayaran;
    }

    public void setPembayaran(Pembayaran pembayaran) {
        this.pembayaran = pembayaran;
    }
}

/**
 * Enumerasi status siklus hidup transaksi penyewaan alat musik.
 *
 * @author Kelompok 4
 */
enum StatusTransaksi {

    MENUNGGU("Menunggu"),
    DISEWA("Disewa"),
    SELESAI("Selesai"),
    DITOLAK("Ditolak"),
    DIBATALKAN("Dibatalkan");

    private final String namaTampil;

    StatusTransaksi(String namaTampil) {
        this.namaTampil = namaTampil;
    }

    public String getNamaTampil() {
        return namaTampil;
    }
}

/**
 * Snapshot data barang sewa pada saat transaksi dibuat (immutable).
 *
 * @author Kelompok 4
 */
final class ItemSewa {

    private final String idBarang;
    private final String namaBarang;
    private final KategoriBarang kategori;
    private final double tarifHarian;
    private final double biayaTambahan;
    private final double biayaTotal;

    public ItemSewa(Barang barang, int lamaSewa) {
        this.idBarang = barang.getId();
        this.namaBarang = barang.getNama();
        this.kategori = barang.getKategori();
        this.tarifHarian = barang.getTarifHarian();
        this.biayaTambahan = barang.getBiayaTambahan();
        this.biayaTotal = barang.hitungBiaya(lamaSewa);
    }

    public ItemSewa(String idBarang, String namaBarang, KategoriBarang kategori,
                    double tarifHarian, double biayaTambahan, double biayaTotal) {
        this.idBarang = idBarang;
        this.namaBarang = namaBarang;
        this.kategori = kategori;
        this.tarifHarian = tarifHarian;
        this.biayaTambahan = biayaTambahan;
        this.biayaTotal = biayaTotal;
    }

    public String getIdBarang() {
        return idBarang;
    }

    public String getNamaBarang() {
        return namaBarang;
    }

    public KategoriBarang getKategori() {
        return kategori;
    }

    public double getTarifHarian() {
        return tarifHarian;
    }

    public double getBiayaTambahan() {
        return biayaTambahan;
    }

    public double getBiayaTotal() {
        return biayaTotal;
    }
}

/**
 * Kelas pencetak dokumen Invoice penyewaan alat musik dengan format lebar 60 karakter.
 *
 * @author Kelompok 4
 */
class Invoice {

    private final Transaksi transaksi;
    private final double total;
    private final double denda;

    public Invoice(Transaksi transaksi) {
        this.transaksi = transaksi;
        this.total = transaksi.hitungTotal();
        this.denda = transaksi.hitungDenda();
    }

    /** Mencetak invoice lengkap ke layar sesuai spesifikasi format lebar 60 karakter. */
    public void cetakInvoice() {
        int lebar = 60;
        String garisGanda = FormatHelper.garis('=', lebar);
        String garisTunggal = FormatHelper.garis('-', lebar);

        System.out.println(garisGanda);
        System.out.println(pusatkanTeks("INVOICE RENTAL ALAT MUSIK", lebar));
        System.out.println(garisGanda);

        String idTrx = transaksi.getId();
        String noInvoice = idTrx.startsWith("TRX-") ? idTrx.replace("TRX-", "INV-") : "INV-" + idTrx;

        System.out.printf("%-15s: %s%n", "No. Invoice", noInvoice);
        System.out.printf("%-15s: %s%n", "No. Transaksi", idTrx);
        System.out.printf("%-15s: %s%n", "Penyewa",
                transaksi.getPenyewa() != null ? transaksi.getPenyewa().getNama() : "-");
        System.out.printf("%-15s: %s%n", "Status", transaksi.getStatus().name());
        System.out.printf("%-15s: %s%n", "Tgl Transaksi", FormatHelper.tanggal(transaksi.getTanggalDibuat()));
        System.out.printf("%-15s: %s%n", "Tgl Sewa", FormatHelper.tanggal(transaksi.getTanggalSewa()));
        System.out.printf("%-15s: %s%n", "Jatuh Tempo", FormatHelper.tanggal(transaksi.getTanggalJatuhTempo()));
        System.out.printf("%-15s: %s%n", "Tgl Kembali", FormatHelper.tanggal(transaksi.getTanggalKembali()));
        System.out.printf("%-15s: %d hari%n", "Lama Sewa", transaksi.getLamaSewa());
        System.out.printf("%-15s: %s%n", "Metode Bayar",
                transaksi.getPembayaran() != null ? transaksi.getPembayaran().getNama() : "-");

        System.out.println(garisTunggal);

        ArrayList<ItemSewa> items = transaksi.getItems();
        for (int i = 0; i < items.size(); i++) {
            ItemSewa item = items.get(i);
            int noUrut = i + 1;
            String headerItem = String.format("%d. %s (%s) - %s",
                    noUrut, item.getNamaBarang(), item.getIdBarang(), item.getKategori().getNamaTampil());
            System.out.println(headerItem);

            double biayaHarianTotal = item.getTarifHarian() * transaksi.getLamaSewa();
            String barisHarianKiri = String.format("   %s x %d hari",
                    FormatHelper.rupiah(item.getTarifHarian()), transaksi.getLamaSewa());
            System.out.println(FormatHelper.rataKanan(barisHarianKiri, FormatHelper.rupiah(biayaHarianTotal), lebar));

            String barisTambahanKiri = "   " + item.getKategori().getNamaBiayaTambahan();
            System.out.println(FormatHelper.rataKanan(barisTambahanKiri, FormatHelper.rupiah(item.getBiayaTambahan()), lebar));

            String barisSubtotalItemKiri = "   Subtotal item";
            System.out.println(FormatHelper.rataKanan(barisSubtotalItemKiri, FormatHelper.rupiah(item.getBiayaTotal()), lebar));
        }

        System.out.println(garisTunggal);

        System.out.println(FormatHelper.rataKanan("Subtotal sewa", FormatHelper.rupiah(transaksi.hitungSubtotal()), lebar));
        String labelAdmin = "Biaya admin (" + (transaksi.getPembayaran() != null ? transaksi.getPembayaran().getNama() : "-") + ")";
        System.out.println(FormatHelper.rataKanan(labelAdmin, FormatHelper.rupiah(transaksi.hitungBiayaAdmin()), lebar));
        System.out.println(FormatHelper.rataKanan("TOTAL TAGIHAN SEWA", FormatHelper.rupiah(total), lebar));

        if (transaksi.getStatus() == StatusTransaksi.SELESAI) {
            int hariTerlambat = transaksi.hitungHariTerlambat();
            if (hariTerlambat > 0) {
                String labelDenda = String.format("Denda (%d hari x %d barang x Rp20.000)",
                        hariTerlambat, items.size());
                System.out.println(FormatHelper.rataKanan(labelDenda, FormatHelper.rupiah(denda), lebar));
            } else {
                System.out.println(FormatHelper.rataKanan("Denda (0 hari)", "Rp0", lebar));
            }
            System.out.println(garisTunggal);
            System.out.println(FormatHelper.rataKanan("TOTAL AKHIR", FormatHelper.rupiah(transaksi.hitungTotalAkhir()), lebar));
        } else if (transaksi.getStatus() == StatusTransaksi.DISEWA) {
            System.out.println("Catatan: denda Rp20.000 per barang per hari keterlambatan.");
        }

        System.out.println(garisGanda);
    }

    private String pusatkanTeks(String teks, int lebar) {
        if (teks == null) {
            teks = "";
        }
        int totalSpasi = lebar - teks.length();
        if (totalSpasi <= 0) {
            return teks;
        }
        int spasiKiri = totalSpasi / 2;
        return " ".repeat(spasiKiri) + teks;
    }
}
