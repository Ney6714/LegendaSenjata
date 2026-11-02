LegendWeapons 1.0 - NarSMP
==========================
Senjata: lightning_sword, warden_axe, dracula_blade, sparta_blade
Command (OP / legend.admin):
  /legend list
  /legend give <senjata> [pemain]

CARA BUILD
1) Pasang JDK 21 + Maven, buka terminal di folder ini, jalankan:
      mvn clean package
2) Ambil target/LegendWeapons-1.0.jar (BUKAN yang berawalan original-)
3) Cek jar (buka dengan 7-Zip): harus ada plugin.yml dan folder com/narsmp/legend/*.class
4) Matikan server, taruh jar di folder plugins/, start server (jangan /reload)

TANPA KOMPUTER / TANPA INSTALL:
Upload folder ini ke repo GitHub -> tab Actions -> "Build jar" -> Run workflow
-> download artifact "LegendWeapons-jar" (isinya jar siap pakai).
