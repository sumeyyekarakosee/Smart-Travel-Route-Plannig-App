/*
  FileManager sınıfı, TXT dosyalarındaki ham verileri okuyarak
  uygulamada kullanılan model nesnelerine dönüştürmekten sorumludur.

  Bu sınıf projenin veri erişim / parsing katmanı olarak görev yapar.

  Temel sorumlulukları:
  - Lokasyon verilerini okumak
  - Rota bağlantılarını okumak
  - TXT satırlarını parse etmek
  - Ham verileri Java nesnelerine dönüştürmek
  - Hatalı veri formatlarını kontrol etmek
 */




package data;

import model.Location;
import model.RouteEdge;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

public class FileManager {

    /*
      locations.txt dosyasını okuyarak her geçerli satırı
      bir Location nesnesine dönüştürür.

      Beklenen veri formatı:
      - id;name;category;visitTime;entryFee;rating;x;y

      Hatalı veya eksik satırlar güvenli şekilde atlanır.
     */

    public List<Location> loadLocations(String filePath) {

        // Parse edilen Location nesnelerini tutan dinamik liste
        List<Location> locations = new ArrayList<>();


        // Dosya okuma işlemi tamamlandıktan sonra BufferedReader otomatik kapatılır
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            String line;


            // Dosya sonuna ulaşılana kadar satır satır okuma yapılır
            while ((line = br.readLine()) != null) {


                // Boş satırlar ve yorum satırları (#) işlenmeden geçilir
                if (line.trim().isEmpty() || line.startsWith("#")) {
                    continue;
                }


                // Txt satırındaki veriler ';' karakterine göre ayrıştırılır
                String[] parts = line.split(";");



                // Beklenen alan sayısı kontrol edilerek hatalı veri engellenir
                if (parts.length != 8) {
                    System.out.println("Invalid location data: " + line);
                    continue;
                }


                // String veriler uygun Java veri tiplerine dönüştürülür
                int id = Integer.parseInt(parts[0]);
                String name = parts[1];
                String category = parts[2];

                int visitTime = Integer.parseInt(parts[3]);

                double entryFee = Double.parseDouble(parts[4]);
                double rating = Double.parseDouble(parts[5]);

                double x = Double.parseDouble(parts[6]);
                double y = Double.parseDouble(parts[7]);


                // Parse edilen veriler kullanılarak Location nesnesi oluşturulur
                Location location = new Location(
                        id,
                        name,
                        category,
                        visitTime,
                        entryFee,
                        rating,
                        x,
                        y
                );


                // Oluşturulan nesne sonuç listesine eklenir
                locations.add(location);
            }


            // Dosya erişimi veya okuma sırasında oluşabilecek hatalar yakalanır
        } catch (IOException e) {
            e.printStackTrace();
        }

        return locations;
    }



    /*
      routes.txt dosyasını okuyarak her geçerli satırı
      bir RouteEdge nesnesine dönüştürür.

      Beklenen veri formatı:
      fromId;toId;distance;time;cost;transportType;transferCount

      Hatalı veya eksik satırlar güvenli şekilde atlanır.
     */


    public List<RouteEdge> loadRoutes(String filePath) {


        // Parse edilen RouteEdge nesnelerini tutan dinamik liste
        List<RouteEdge> routes = new ArrayList<>();


        // Dosya okuma işlemi tamamlandıktan sonra BufferedReader otomatik kapatılır
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            String line;

            // Boş satırlar ve yorum satırları (#) işlenmeden geçilir
            while ((line = br.readLine()) != null) {


                // TXT satırındaki veriler ';' karakterine göre ayrıştırılır
                if (line.trim().isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split(";");


                // Beklenen alan sayısı kontrol edilerek hatalı veri engellenir
                if (parts.length != 7) {
                    System.out.println("Invalid route data: " + line);
                    continue;
                }


                // String veriler uygun Java veri tiplerine dönüştürülür
                int fromId = Integer.parseInt(parts[0]);
                int toId = Integer.parseInt(parts[1]);

                double distance = Double.parseDouble(parts[2]);

                int time = Integer.parseInt(parts[3]);

                double cost = Double.parseDouble(parts[4]);

                String transportType = parts[5];

                int transferCount = Integer.parseInt(parts[6]);


                // Parse edilen veriler kullanılarak RouteEdge nesnesi oluşturulur
                RouteEdge route = new RouteEdge(
                        fromId,
                        toId,
                        distance,
                        time,
                        cost,
                        transportType,
                        transferCount
                );

                routes.add(route);

            }


            // Dosya erişimi veya okuma sırasında oluşabilecek hatalar yakalanır
        } catch (IOException e) {
            e.printStackTrace();
        }

        return routes;
    }


}
