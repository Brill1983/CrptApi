package ru.brill;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws InterruptedException {

        CrptApi sender = new CrptApi(2, TimeUnit.SECONDS, 5L);

        Product product = new Product("string", LocalDate.now(), "string",
                "string", "string", LocalDate.now(), "string", "string", "string");
        Document document = new Document(new Description("string"), "string", "string",
                "LP_INTRODUCE_GOODS", true, "string", "string", "string",
                LocalDate.now(), "string", List.of(product), LocalDate.now(), "string");

        Thread thread1 = new Thread(() -> {
            sender.send(document, "Подпись 1");
        });

        Thread thread2 = new Thread(() -> {
            sender.send(document, "Подпись 2");
        });

        Thread thread3 = new Thread(() -> {
            sender.send(document, "Подпись 3");
        });

        thread1.start();
        thread2.start();
        thread3.start();
        Thread.sleep(100L);
        thread1.join();
        thread2.join();
        thread3.join();
        sender.shutDown();
    }
}