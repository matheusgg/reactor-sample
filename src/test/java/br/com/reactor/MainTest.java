package br.com.reactor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@RunWith(JUnit4.class)
public class MainTest {

    private Main subject = new Main();

    @Test
    public void appendOp () throws Exception {
        final Flux<String> flux = Flux.just("Item 1", "Item 2");

        StepVerifier.create(this.subject.appendOp(flux))
                .expectNext("Item 1")
                .expectNext("Item 2")
                .expectErrorMessage("boom")
                .verify();
    }
}