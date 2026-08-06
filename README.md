# Workflow Engine

Stajım boyunca yazdığım küçük bir süreç motoru. Her onay süreci için ayrı kod
yazmak yerine, süreci JSON olarak tanımlayıp aynı motorla çalıştırıyorum. Örnek
olarak izin talebi süreci üzerinden çalışıyor.

## Süreç nasıl işliyor

1. Çalışan izin talebi girer
2. Talep yöneticiye düşer
3. Yönetici onaylar
4. Gün sayısı 5'ten fazlaysa talep İK'ya düşer, değilse direkt bir sonraki adıma geçer
5. İK onaylarsa bildirim gönderilir
6. Süreç tamamlanır

## Motor içeride nasıl çalışıyor

1. Süreç adımları ve geçişler veritabanında JSON olarak tutuluyor
2. Motor çalıştığında bu JSON'u okuyor
3. Şu anki adıma bakıyor
4. Adım bir onay adımıysa durup bekliyor
5. Adım otomatik bir adımsa hemen bir sonrakine geçiyor
6. Adım bir koşul adımıysa değişkene bakıp yön seçiyor

Yeni bir süreç eklemek için kod yazmaya gerek yok, sadece yeni bir JSON tanımı
yüklemek yeterli.

## Kullanılanlar

Java, Spring Boot, Spring Data JPA, Hibernate, PostgreSQL, Jackson, springdoc-openapi

## Projeyi çalıştırmak için

1. PostgreSQL'de bir veritabanı aç
2. application.properties dosyasına bağlantı bilgilerini yaz
3. mvn spring-boot:run komutunu çalıştır
4. localhost 8080 swagger-ui adresine giderek API'yi gör
5. index.html live serverde çalıştır
6. kolay arayüzü ile kullanabilirsiniz

## API neler yapıyor

1. Yeni süreç başlatmak için POST /instances/start/processCode
2. Süreç durumunu görmek için GET /instances/id
3. Süreci iptal etmek, askıya almak veya devam ettirmek için POST /instances/id/cancel, suspend, resume
4. Bana düşen bekleyen görevleri görmek için GET /tasks
5. Görevi onaylamak için POST /tasks/id/complete
6. Yeni süreç tanımı yüklemek için POST /definitions
7. Tüm denetim kayıtlarını görmek için GET /audit

## Hata yönetimi nasıl çalışıyor

1. Başta her hata 500 dönüyordu, sonra düzelttim
2. Gönderilen JSON bozuksa 400 dönüyor
3. Aradığın kayıt yoksa 404 dönüyor
4. Zaten tamamlanmış bir şeyi tekrar işlemeye çalışırsan 409 dönüyor
5. Gerçekten beklenmedik bir şey olursa 500 dönüyor
6. Hepsi GlobalExceptionHandler üzerinden tek yerden yönetiliyor

## Arayüz nasıl çalışıyor

1. index.html tek dosyalık basit bir panel
2. React'i CDN'den çekiyor, kurulum gerekmiyor
3. Açtığında bir login ekranı çıkıyor
4. Yönetici girerse kendine düşen onayları görüyor
5. İK girerse kendine düşen onayları görüyor
6. Admin girerse tüm denetim kayıtlarını görüyor
7. Kullanıcı girerse yeni izin talebi oluşturabiliyor

## Ek Bilgiler

1. Backend localhost 8080'de çalışıyor olmalı
2. Test için Swagger UI kullandım

Örnek DefinitionJson:
{
  "processName": "İzin Talebi Süreci",
  "processCode": "izin-talebi",
  "version": 1,
  "isActive": true,
  "startNodeId": "start_node",
  "definitionJson": "{\"key\": \"izin-talebi\", \"name\": \"İzin Talebi Süreci\", \"startNode\": \"start_node\", \"nodes\": [{\"id\": \"start_node\", \"type\": \"START\"}, {\"id\": \"manager_approval\", \"type\": \"USER_TASK\", \"name\": \"Yönetici Onayı\", \"assignee\": \"yonetici\"}, {\"id\": \"check_days\", \"type\": \"EXCLUSIVE_GATEWAY\"}, {\"id\": \"hr_approval\", \"type\": \"USER_TASK\", \"name\": \"İK Onayı\", \"assignee\": \"ik\"}, {\"id\": \"notification\", \"type\": \"SERVICE_TASK\"}, {\"id\": \"end_node\", \"type\": \"END\"}], \"transitions\": [{\"from\": \"start_node\", \"to\": \"manager_approval\"}, {\"from\": \"manager_approval\", \"to\": \"check_days\"}, {\"from\": \"check_days\", \"condition\": {\"variable\": \"gunSayisi\", \"operator\": \"gt\", \"value\": 5}, \"to\": \"hr_approval\"}, {\"from\": \"check_days\", \"default\": true, \"to\": \"notification\"}, {\"from\": \"hr_approval\", \"to\": \"notification\"}, {\"from\": \"notification\", \"to\": \"end_node\"}]}",
  "active": true
}

