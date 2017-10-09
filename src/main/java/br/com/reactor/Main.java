package br.com.reactor;

import static java.time.Duration.ofSeconds;
import static java.util.Collections.unmodifiableList;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;

/**
 * https://projectreactor.io/docs/core/release/reference/docs/index.html
 */
@Slf4j
public class Main {

    public static void main (final String... args) throws InterruptedException {
        Hooks.onOperatorDebug();
        //        createFluxMono();
        //        subscribe();
        //        generate();
        //        create();
        //        errors();
    }

    public <T> Flux<T> appendOp (final Flux<T> source) {
        return source.concatWith(Mono.error(new IllegalStateException("boom")));
    }

    public static void errors () throws InterruptedException {
        Flux.interval(ofSeconds(3))
                .map(e -> {
                    if (e < 5) {
                        return "Execution " + e;
                    }
                    throw new IllegalStateException("Some Error!");
                })
                .onErrorReturn("You broke!")
                .subscribe(LOGGER::info);
        SECONDS.sleep(60);
    }

    /**
     * Com o metodo generate e possivel executar emissoes assincronas
     */
    public static void create () {
        final Flux<Object> flux = Flux.create(sink -> {
            final CustomEventListener<String> listener = new CustomEventListener<String>() {

                @Override
                public void onChunk (final List<String> chunk) {
                    chunk.forEach(sink::next);
                }

                @Override
                public void onComplete () {
                    sink.complete();
                }
            };
            new CustomConsoleEventProcessor(listener).start();
        });
        flux.subscribe(item -> LOGGER.info(item.toString()));
    }

    /**
     * Com o metodo generate e possivel executar emissoes sincronas
     */
    public static void generate () {
        final Flux<Object> generate = Flux.generate(AtomicLong::new, (state, sink) -> {
            final long i = state.getAndIncrement();
            sink.next("5 x " + i + " = " + 5 * i);
            if (i == 10) {
                sink.complete();
            }
            return state;
        }).log();
        generate.subscribe(item -> LOGGER.info(item.toString()));
    }

    /**
     * Nenhuma operacao e executada ate houver uma inscricao no Flux ou Mono
     */
    public static void subscribe () {
        final Flux<Integer> range1 = Flux.range(0, 10);
        range1.subscribe();

        final Flux<Integer> range2 = Flux.range(0, 10);
        range2.subscribe(n -> LOGGER.info(n.toString()));

        final Flux<Integer> range3 = Flux.range(0, 10)
                .map(n -> {
                    if (n == 5) {
                        throw new IllegalArgumentException();
                    }
                    return n;
                });
        range3.subscribe(n -> LOGGER.info(n.toString()),
                error -> LOGGER.error("Error =============> " + error.getMessage()));

        final Flux<Integer> range4 = Flux.range(0, 10);
        range4.subscribe(n -> LOGGER.info(n.toString()),
                error -> LOGGER.error("Error ===========> " + error.getMessage()),
                () -> LOGGER.info("Done!"));

        final Flux<Integer> range5 = Flux.range(0, 10);
        range5.subscribe(n -> LOGGER.info(n.toString()),
                error -> LOGGER.error("Error ===========> " + error.getMessage()),
                () -> LOGGER.info("Done!"), s -> s.request(10));
        range5.subscribe(new CustomSubscriber<>());
    }

    public static void createFluxMono () {
        final Flux<String> flux = Flux.just("Item 1", "Item 2", "Item 3");
        LOGGER.info(flux.toString());

        final Flux<Integer> range = Flux.range(0, 10);
        LOGGER.info(range.toString());

        final Mono<String> mono = Mono.just("Item 1");
        LOGGER.info(mono.toString());

        final Mono<Object> empty = Mono.empty();
        LOGGER.info(empty.toString());
    }

    @Slf4j
    public static class CustomSubscriber<T> extends BaseSubscriber<T> {

        @Override
        protected void hookOnSubscribe (final Subscription subscription) {
            LOGGER.info("================> Subscribed!");
            super.request(1);
        }

        @Override
        protected void hookOnNext (final T value) {
            LOGGER.info("================> {}", value);
            super.request(1);
        }
    }

    public interface CustomEventListener<T> {

        void onChunk (List<T> chunk);

        void onComplete ();
    }

    public static class CustomConsoleEventProcessor extends Thread {

        private static final int BUFFER_SIZE = 3;

        private static final String DELIMITER = "";

        private final CustomEventListener<String> listener;

        private final Scanner scanner;

        private List<String> buffer;

        public CustomConsoleEventProcessor (final CustomEventListener<String> listener) {
            this.listener = listener;
            this.scanner = new Scanner(System.in);
            this.buffer = new ArrayList<>();
        }

        @Override
        public void run () {
            this.startProcessing();
        }

        private void startProcessing () {
            this.scanner.useDelimiter(DELIMITER);
            String line;
            do {
                line = this.scanner.nextLine();
                this.buffer.add(line);
                if (this.buffer.size() + 1 > BUFFER_SIZE) {
                    this.listener.onChunk(unmodifiableList(this.buffer));
                    this.buffer.clear();
                }
            } while (!DELIMITER.equals(line));
            try {
                this.scanner.close();
            } finally {
                this.listener.onComplete();
            }
        }
    }
}
