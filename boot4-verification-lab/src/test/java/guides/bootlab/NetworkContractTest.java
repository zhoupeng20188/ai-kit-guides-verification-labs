package guides.bootlab;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NetworkContractTest {
    @Autowired Environment environment;

    @Test void realHttpBasicRequestsPreserveTheSecurityContract() throws Exception {
        int port = environment.getRequiredProperty("local.server.port", Integer.class);
        URI uri = URI.create("http://127.0.0.1:" + port + "/notes");
        var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        String authorization = "Basic " + Base64.getEncoder().encodeToString(
                "lab-user:lab-only".getBytes(StandardCharsets.UTF_8));
        var anonymous = client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        var authenticated = client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5))
                .header("Authorization", authorization).GET().build(), HttpResponse.BodyHandlers.ofString());
        var post = client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5))
                .header("Authorization", authorization).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"text\":\"demo\"}")).build(),
                HttpResponse.BodyHandlers.ofString());
        var directError = client.send(HttpRequest.newBuilder(
                URI.create("http://127.0.0.1:" + port + "/error")).timeout(Duration.ofSeconds(5))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        System.out.printf("OBSERVATION real_http: anonymous_get=%d basic_get=%d basic_post_without_csrf=%d%n",
                anonymous.statusCode(), authenticated.statusCode(), post.statusCode());
        assertEquals(401, anonymous.statusCode());
        assertEquals(200, authenticated.statusCode());
        assertEquals(403, post.statusCode());
        assertEquals(401, directError.statusCode(), "A direct REQUEST to /error remains protected");
    }
}
