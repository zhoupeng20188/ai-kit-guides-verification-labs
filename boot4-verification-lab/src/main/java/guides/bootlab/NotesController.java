package guides.bootlab;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class NotesController {
    private final AtomicInteger accepted = new AtomicInteger();

    @GetMapping("/notes")
    Map<String, Integer> count() {
        return Map.of("accepted", accepted.get());
    }

    @PostMapping("/notes")
    Map<String, Integer> create(@RequestBody Map<String, String> note) {
        return Map.of("accepted", accepted.incrementAndGet());
    }
}
