package ru.brill;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.netty.handler.codec.http.HttpHeaderNames;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.concurrent.TimedSemaphore;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private static final String HEADER_VALUE = "application/json";
    private final String url = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final TimedSemaphore semaphore;
    private final HttpClient client;
    private final ObjectMapper mapper;

    public CrptApi(Integer threadsNumber, TimeUnit timeUnit, Long duration) {
        this.semaphore = new TimedSemaphore(duration, timeUnit, threadsNumber);

        this.mapper = new ObjectMapper().findAndRegisterModules();
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        this.client = HttpClient.create()
                .baseUrl(url)
                .headers(h -> h.set(HttpHeaderNames.CONTENT_TYPE, HEADER_VALUE)
                        .set(HttpHeaderNames.ACCEPT, HEADER_VALUE));
    }

    public void send(Document document, String signature) {
        acquire();
        System.out.println("Поток " + Thread.currentThread().getName() + " занял слот семафора , время " +
                LocalDateTime.now() + ". С командой отправить Документ " + document.getDocId()); // При возможности логирования log.info()
        String requestBody;
        try {
            requestBody = mapper.writeValueAsString(document);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        client.headers(h -> h.set("Signature", signature))
                .post()
                .uri("/post")
                .send(ByteBufFlux.fromString(Mono.just(requestBody)))
                .responseContent()
                .aggregate()
                .asString()
                .doOnError(error -> {
                    throw new RuntimeException(error);
                })
                .block();
        System.out.println("Отправка " + document.getDocId() + " завершена, время " + LocalDateTime.now()); // При возможности логирования log.info());
        System.out.println("В семафоре заблокировано потоков " + semaphore.getAcquireCount());
        System.out.println("В семафоре осталось свободных потоков " + semaphore.getAvailablePermits());
        System.out.println("В настройках семафора указан период блокировки потоков " + semaphore.getPeriod() + " в единицах " + semaphore.getUnit());
        System.out.println("В настройках семафора указан лимит потоков " + semaphore.getLimit());
    }

    private void acquire() {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void shutDown() {
        semaphore.shutdown();
    }
}

@Data
@AllArgsConstructor
class Document {
    private Description description;
    @JsonProperty("doc_id")
    private String docId;
    @JsonProperty("doc_status")
    private String docStatus;
    @JsonProperty("doc_type")
    private String docType;
    private boolean importRequest;
    @JsonProperty("owner_inn")
    private String ownerInn;
    @JsonProperty("participant_inn")
    private String participantInn;
    @JsonProperty("producer_inn")
    private String producerInn;
    @JsonProperty("production_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate productionDate; // 2020-01-23
    @JsonProperty("production_type")
    private String productionType;
    private List<Product> products;
    @JsonProperty("reg_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate regDate;
    @JsonProperty("reg_number")
    private String regNumber;
}

@Data
@AllArgsConstructor
class Description {
    private String participantInn;
}

@Data
@AllArgsConstructor
class Product {
    @JsonProperty("certificate_document")
    private String certificateDocument;
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("certificate_document_date")
    private LocalDate certificateDocumentDate;
    @JsonProperty("certificate_document_number")
    private String certificateDocumentNumber;
    @JsonProperty("owner_inn")
    private String ownerInn;
    @JsonProperty("producer_inn")
    private String producerInn;
    @JsonProperty("production_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate production_date;
    @JsonProperty("tnved_code")
    private String tnvedCode;
    @JsonProperty("uit_code")
    private String uitCode;
    @JsonProperty("new")
    private String newDoc;
}
