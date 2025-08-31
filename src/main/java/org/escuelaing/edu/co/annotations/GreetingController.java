package org.escuelaing.edu.co.annotations;

import java.util.concurrent.atomic.AtomicLong;

@RestController
public class GreetingController {
    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();

    @GetMapping("/greeting")
    public String greeting(@RequestParam(value = "name", defaultValue = "World", required = false) String name) {
        long id = counter.incrementAndGet();
        return String.format(template, name) + " (id=" + id + ")";
    }
}
