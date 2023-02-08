import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    private final TimeUnit timeUnit;
    private int requestLimit;

    private static final String PRODUCT_GROUP = "MOCK";
    private static final String REQUEST_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create?pg=";
    private static final String MOCK_TOKEN = "token";

    private final Semaphore semaphore;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.semaphore = new Semaphore(requestLimit);
    }

    private String sendHttpRequest(Document document, String signature) {

        String response = "";

        try {
            semaphore.tryAcquire(1, timeUnit);

            final CloseableHttpClient httpClient = HttpClients.createDefault();
            final HttpPost httpPost = new HttpPost(REQUEST_URL + PRODUCT_GROUP);
            final List<NameValuePair> params = new ArrayList<>();


            params.add(new BasicNameValuePair("document_format", DocumentFormat.MANUAL.toString()));
            params.add(new BasicNameValuePair("product_document", mapToBase64(convertToJsonObject(document))));
            params.add(new BasicNameValuePair("product_group", PRODUCT_GROUP));
            params.add(new BasicNameValuePair("signature", mapToBase64(signature)));
            params.add(new BasicNameValuePair("type", document.getDocType()));

            httpPost.setEntity(new UrlEncodedFormEntity(params));
            httpPost.setHeader("'content-type ", "application/json'");
            httpPost.addHeader("Authorization", "Bearer " + MOCK_TOKEN);

            CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
            response = String.valueOf(httpResponse.getStatusLine().getStatusCode());

            httpClient.close();
        } catch (IOException | InterruptedException e) {
            response = e.toString();
        } finally {
            semaphore.release();
        }

        return response;

    }

    private synchronized String convertToJsonObject(Document document) {
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z").create();
        return new Gson().toJson(document);
    }

    private synchronized String mapToBase64(String s) {
        Base64 base64 = new Base64();
        byte[] encodedString = base64.encode(s.getBytes());

        return new String(encodedString);
    }

    @JsonAutoDetect
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    private static class Product {

        @SerializedName("certificate_document")
        private String certificateDocument;

        @SerializedName("certificate_document_name")
        private LocalDate certificateDocumentName;

        @SerializedName("certificate_document_number")
        private String certificateDocumentNumber;

        @SerializedName("owner_inn")
        private String ownerInn;

        @SerializedName("production_date")
        private LocalDate productionDate;

        @SerializedName("tnved_code")
        private String tnvedCode;

        @SerializedName("uit_code")
        private String uitCode;

        @SerializedName("uitu_code")
        private String uituCode;
    }

    @JsonAutoDetect
    @AllArgsConstructor
    @Getter
    @Setter
    private static class Description {

        @SerializedName("participant_inn")
        private String participantInn;
    }

    @JsonAutoDetect
    @AllArgsConstructor
    @Getter
    @Setter
    private static class Document {

        @Expose
        private Description description;

        @SerializedName("doc_id")
        private String docId;

        @SerializedName("doc_status")
        private String docStatus;

        @SerializedName("doc_type")
        private String docType;

        @Expose
        private String importRequest;

        @SerializedName("owner_inn")
        private String ownerInn;

        @SerializedName("participant_inn")
        private String participantInn;

        @SerializedName("producer_inn")
        private String producerInn;

        @SerializedName("production_date")
        private LocalDate productionDate;

        @SerializedName("production_type")
        private String productionType;

        private List<Product> products;

        @SerializedName("reg_date")
        private String registrationDate;

        @SerializedName("reg_number")
        private String registrationNumber;
    }

    enum DocumentFormat {
        MANUAL,
        XML,
        CSV
    }
}
