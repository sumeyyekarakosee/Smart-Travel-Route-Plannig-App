# Akıllı Gezi Rota Planlama Uygulaması

Java + JavaFX ile geliştirilmiş, İstanbul üzerindeki gezi noktaları için rota oluşturan bir uygulamadır.

## Özellikler

- Başlangıç noktası seçimi
- İlçeler / semtler ve tarihi yerleri ayrı listeleme
- Çoklu lokasyon seçimi
- Dijkstra tabanlı rota hesaplama
- Rota tercihi seçimi:
  - En Kısa Mesafe
  - En Düşük Maliyet
  - En Kısa Süre
  - En Az Aktarma
  - Dengeli Rota
- Günlük gezi süresi ve bütçe kontrolü
- Alternatif rota tablosu
- Günlük gezi planı
- What-If simülasyonu:
  - Lokasyon çıkarma
  - Lokasyon ekleme
  - Undo
  - Eski / yeni rota karşılaştırması
- Leaflet/OpenStreetMap tabanlı gerçek harita görünümü
- Açık, renkli ve temiz tasarım

## Proje Yapısı

- `src/Main.java`
  - Uygulamanın giriş noktasıdır.
- `src/ui/TravelRouteApp.java`
  - JavaFX uygulamasını başlatır.
- `src/ui/controller`
  - Ana ekran ve What-If ekranı controller sınıfları.
- `src/ui/view`
  - Harita çizim mantığı.
- `resources/fxml`
  - FXML arayüz dosyaları.
- `resources/styles`
  - CSS stil dosyası.
- `resources/locations.txt`
  - Lokasyon verileri.
- `resources/routes.txt`
  - Ulaşım bağlantıları.

## Veri Dosyaları

### `resources/locations.txt`

Format:

```text
id;name;visitTime;entryFee;rating;x;y
```

Örnek:

```text
1;Kadıköy;90;0;4.7;620;360
21;Ayasofya;120;25;4.9;465;310
```

İlk 20 kayıt:

- İlçeler / semtler

21 ve sonrası:

- Tarihi / turistik yerler

### `resources/routes.txt`

Format:

```text
fromId;toId;distance;time;cost;transport;transfer
```

Örnek:

```text
1;2;5.0;20;20;Metro;0
```

## Gereksinimler

- Java 23
- JavaFX SDK
> Not: Bu projede JavaFX, JDK ile birlikte gelmiyor. Çalıştırmak için JavaFX SDK yolunu ayrıca vermen gerekir.
> Harita görünümü OpenStreetMap raster tile'larını kullanır ve internet bağlantısı gerekir.

## IntelliJ IDEA ile Çalıştırma

1. Projeyi IntelliJ IDEA ile aç.
2. Project SDK olarak Java 23 seç.
3. JavaFX SDK kurulu değilse indir ve klasör yolunu not et.
4. `File > Project Structure > Libraries` kısmından JavaFX `lib` klasörünü ekle.
5. `Run/Debug Configurations` bölümünde ana sınıfı `Main` olarak seç.
6. VM options içine JavaFX modüllerini ekle:

```text
--module-path "JAVAFX_SDK_LIB_PATH"
--add-modules javafx.controls,javafx.fxml
```

Örnek:

```text
--module-path "C:\javafx-sdk-23\lib"
--add-modules javafx.controls,javafx.fxml
```

7. `Main` sınıfını çalıştır.

## Komut Satırı ile Çalıştırma

Proje kök klasöründe şu adımları uygula:

1. Derleme klasörü oluştur:

```powershell
New-Item -ItemType Directory -Path out -Force
```

2. Kaynakları derle:

```powershell
javac --module-path "C:\javafx-sdk-23\lib" --add-modules javafx.controls,javafx.fxml -d out (Get-ChildItem -Recurse .\src\*.java).FullName
```

3. Uygulamayı çalıştır:

```powershell
java --module-path "C:\javafx-sdk-23\lib" --add-modules javafx.controls,javafx.fxml -cp out Main
```

> JavaFX yolu sende farklıysa `C:\javafx-sdk-23\lib` kısmını kendi klasörünle değiştir.

## Kullanım Akışı

1. Uygulamayı aç.
2. Sol panelden bir başlangıç noktası seç.
3. İlçeler ve tarihi yerler sekmelerinden gezilecek yerleri işaretle.
4. Rota türünü seç.
5. Günlük gezi süresi ve bütçe gir.
6. `Rota Oluştur` butonuna bas.
7. Sağ panelde rota özeti, adımlar, günlük plan ve alternatif rotaları incele.
8. İstersen `Rota Kaydet` ile sonucu dosyaya yaz.
9. `What-If Başlat` ile lokasyon çıkarma / ekleme simülasyonu yap.

## Notlar

- Harita Leaflet + OpenStreetMap ile gerçek coğrafi katman olarak çalışır.
- Rota hesaplamalarında temel algoritma Dijkstra’dır.
- What-If ekleme işlemi için insertion heuristic kullanılır.
- Rota kayıtları `resources/saved_routes.txt` içine eklenir.

## Sorun Giderme

### Uygulama açılmıyor

- JavaFX SDK yolunu kontrol et.
- VM options içinde `javafx.controls` ve `javafx.fxml` modüllerinin eklendiğinden emin ol.

### Veri yüklenmiyor

- `resources/locations.txt` ve `resources/routes.txt` dosyalarının yerinde olduğundan emin ol.
- Dosya formatında `;` ayracı kullanıldığını kontrol et.

### Bütçe alanı hata veriyor

- Sayısal değer gir.
- Örnek: `2500`

## Geliştirme Notu

Ana giriş noktası artık `Main` sınıfıdır; bu sınıf JavaFX uygulamasını başlatan `ui.TravelRouteApp` sınıfına yönlendirir.
