import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Utility class for sending multipart/form-data requests
 */
class FileClient {
    private final HttpClient client;

    public FileClient() {
        this.client = HttpClient.newHttpClient();
    }

    /**
     * Sends POST multipart/form-data request with the file to the server's address
     *
     * @param address  server's address
     * @param fileData container with file name, file contents and mime type
     * @return HttpResponse from the server
     * @throws IOException          when a general IO error occurs
     * @throws InterruptedException when the thread is interrupted
     */
    public HttpResponse<byte[]> post(String address, FileData fileData) throws IOException, InterruptedException {
        String boundary = UUID.randomUUID().toString();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(address))
                .headers("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(ofMultipartData(fileData, boundary))
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    /**
     * Sends GET request to fetch a file by the specified URI
     *
     * @param address the file URI
     * @return HttpResponse
     * @throws IOException          when a general IO error occurs
     * @throws InterruptedException when the thread is interrupted
     */
    public HttpResponse<byte[]> get(String address) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(address))
                .GET()
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    /**
     * Utility method to create a BodyPublisher for multipart/form-data media type
     *
     * @param fileData container with file name, file contents and mime type
     * @param boundary request parts separator
     * @return BodyPublisher
     */
    private HttpRequest.BodyPublisher ofMultipartData(FileData fileData, String boundary) {
        List<byte[]> byteArrays = new ArrayList<>();

        byteArrays.add("--%s\r\nContent-Disposition: form-data; name="
                .formatted(boundary).getBytes(StandardCharsets.UTF_8));
        byteArrays.add(("\"file\"; filename=\"%s\"\r\nContent-Type: %s\r\n\r\n")
                .formatted(fileData.getOriginalName(), fileData.getMimeType()).getBytes(StandardCharsets.UTF_8));
        byteArrays.add(fileData.getContents());
        byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));
        byteArrays.add("--%s--".formatted(boundary).getBytes(StandardCharsets.UTF_8));

        return HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
    }
}
