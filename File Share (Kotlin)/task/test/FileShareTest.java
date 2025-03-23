import org.hyperskill.hstest.dynamic.DynamicTest;
import org.hyperskill.hstest.dynamic.input.DynamicTesting;
import org.hyperskill.hstest.exception.outcomes.WrongAnswer;
import org.hyperskill.hstest.stage.SpringTest;
import org.hyperskill.hstest.testcase.CheckResult;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.UUID;

import static org.hyperskill.hstest.testing.expect.Expectation.expect;
import static org.hyperskill.hstest.testing.expect.json.JsonChecker.isObject;

public class FileShareTest extends SpringTest {
    private final String uploadUrl = "http://localhost:" + this.port + "/api/v1/upload";
    private final String downloadUrl = "http://localhost:" + this.port + "/api/v1/download";
    private final String infoUrl = "/api/v1/info";
    private final Path storagePath = Path.of("../uploads");

    CheckResult emptyStorageAndCheckInfo() {
        clearStorage();

        var response = get(infoUrl).send();

        checkStatusCode(
                response.getRequest().getMethod(),
                response.getRequest().getEndpoint(),
                response.getStatusCode(),
                200
        );

        checkJson(response, 0, 0);

        return CheckResult.correct();
    }

    CheckResult testInfo(int count, int size) {
        var response = get(infoUrl).send();

        checkStatusCode(
                response.getRequest().getMethod(),
                response.getRequest().getEndpoint(),
                response.getStatusCode(),
                200
        );

        checkJson(response, count, size);

        return CheckResult.correct();
    }

    CheckResult testNotFound() {
        try {
            FileClient client = new FileClient();
            var location = downloadUrl + "/" + UUID.randomUUID();
            HttpResponse<byte[]> response = client.get(location);
            checkStatusCode(
                    response.request().method(),
                    response.request().uri().toString(),
                    response.statusCode(),
                    404
            );
            return CheckResult.correct();
        } catch (IOException | InterruptedException e) {
            return CheckResult.wrong("Error occurred during the test execution: " + e.getMessage());
        }
    }

    CheckResult testPostAndGetFile(String filepath, String filename) {
        try {
            FileClient client = new FileClient();

            FileData fileData = FileData.of(filepath).setOriginalName(filename);

            HttpResponse<byte[]> postResponse = client.post(uploadUrl, fileData);

            checkStatusCode(
                    postResponse.request().method(),
                    postResponse.request().uri().toString(),
                    postResponse.statusCode(),
                    201
            );

            String location = postResponse.headers()
                    .firstValue("Location")
                    .orElseThrow(() -> new WrongAnswer("Response should contain the 'Location' header."));

            if (location.isBlank()) {
                return CheckResult.wrong("The value of the 'Location' header should not be blank");
            }

            var sanitizedLink = location
                    .replaceAll(".*/", "")
                    .replaceAll("\\W", "");

            if (!sanitizedLink.matches(fileData.getOriginalName().replaceAll("\\W", ""))) {
                return CheckResult.wrong("""
                        The 'Location' value does not look like containing a link to the uploaded file:
                        Location: %s
                        Uploaded file name: %s
                        \r
                        """.formatted(location, fileData.getOriginalName()));
            }

            HttpResponse<byte[]> getResponse = client.get(location);

            checkStatusCode(
                    getResponse.request().method(),
                    getResponse.request().uri().toString(),
                    getResponse.statusCode(),
                    200
            );

            if (!Arrays.equals(fileData.getContents(), getResponse.body())) {
                return CheckResult.wrong("""
                        GET %s
                        returned a request body that does not match the expected file content
                        """.formatted(location));
            }

            return CheckResult.correct();
        } catch (IOException | InterruptedException e) {
            return CheckResult.wrong("Error occurred during the test execution: " + e.getMessage());
        }
    }

    @DynamicTest
    DynamicTesting[] dt = {
            this::emptyStorageAndCheckInfo,
            () -> testPostAndGetFile("./test/files/file 1.jpg", "file 1.jpg"),
            () -> testPostAndGetFile("./test/files/file2.jpg", "file 1.jpg"),
            () -> testPostAndGetFile("./test/files/file3.txt", "file3.txt"),
            this::testNotFound,
            () -> testInfo(2, 32992),
            this::reloadServer,
            () -> testInfo(2, 32992),
            this::emptyStorageAndCheckInfo,
    };

    private void checkStatusCode(String method, String endpoint, int actual, int expected) {
        if (actual != expected) {
            throw new WrongAnswer("""
                    %s %s should respond with status code %d, responded with %d
                    \r
                    """.formatted(method, endpoint, expected, actual));
        }
    }

    private void checkJson(org.hyperskill.hstest.mocks.web.response.HttpResponse response,
                           int expectedCount,
                           int expectedSize) {
        try {
            response.getJson();
        } catch (Exception e) {
            throw new WrongAnswer("GET %s should return a valid JSON".formatted(infoUrl));
        }

        expect(response.getContent()).asJson().check(isObject()
                .value("total_files", expectedCount)
                .value("total_bytes", expectedSize)
        );
    }

    private void clearStorage() {
        try (var stream = Files.walk(storagePath)) {
            stream.sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(storagePath))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new WrongAnswer("Error clearing the file storage folder: " + e.getMessage());
                        }
                    });
        } catch (Exception ex) {
            throw new WrongAnswer("Error clearing the file storage folder: " + ex.getMessage());
        }
    }

    private CheckResult reloadServer() {
        try {
            reloadSpring();
        } catch (Exception ex) {
            return CheckResult.wrong("Error reloading the application: " + ex.getMessage());
        }
        return CheckResult.correct();
    }
}
