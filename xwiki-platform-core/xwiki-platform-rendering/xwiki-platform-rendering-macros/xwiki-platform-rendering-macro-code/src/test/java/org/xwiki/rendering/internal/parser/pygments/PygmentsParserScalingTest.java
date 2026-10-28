/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.rendering.internal.parser.pygments;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.xwiki.rendering.parser.HighlightParser;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

/**
 * Measures how the Pygments highlighting scales when several threads highlight code in parallel, as it happens when
 * several requests render pages with code macros at the same time.
 * <p>
 * This is a measurement, not a regular test: it takes about a minute and its result depends on the machine. It only
 * runs when the {@code xwiki.test.codeMacroScaling} system property is {@code true}, and writes its report to
 * {@code target/code-macro-scaling.txt}. Run it without the JaCoCo agent ({@code -Dxwiki.jacoco.skip=true}): its
 * instrumentation makes every Jython object finalizable, which dominates the measurement and exhausts the memory.
 *
 * @version $Id$
 */
@ComponentTest
@AllComponents(excludes = DefaultPygmentsParserConfiguration.class)
@EnabledIfSystemProperty(named = "xwiki.test.codeMacroScaling", matches = "true")
class PygmentsParserScalingTest
{
    private static final int MAX_THREADS = 8;

    private static final int WARMUP_OPERATIONS = 300;

    private static final int OPERATIONS_PER_THREAD = Integer.getInteger("xwiki.test.codeMacroScaling.operations", 150);

    private static final String LANGUAGE = System.getProperty("xwiki.test.codeMacroScaling.language", "java");

    private static final int CONTROL_ROUNDS = 20;

    private static final List<Pattern> CONTROL_RULES = Stream.of("/\\*\\*?(?:.|\\n)*?\\*/", "\"(?:\\\\.|[^\"\\\\])*\"",
        "(?:package|import|public|private|final|class|new|int|for|if|else|return|this)\\b", "[A-Z]\\w*", "[a-z_]\\w*",
        "\\d+", "\\s+", "[{}()\\[\\];.,<>=+*%!-]+").map(Pattern::compile).toList();

    private static final String SNIPPET = """
        package org.example.demo%1$d;

        import java.util.ArrayList;
        import java.util.List;

        /**
         * Sample class number %1$d.
         */
        public class Sample%1$d
        {
            private final List<String> values%1$d = new ArrayList<>();

            public int compute(int input)
            {
                int result = input * %1$d + %2$d;
                for (int i = 0; i < %2$d; i++) {
                    if (i %% 2 == 0) {
                        result += i;
                    } else {
                        this.values%1$d.add("value " + i + " of %1$d");
                    }
                }
                return result;
            }
        }
        """;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    private PygmentsParserConfiguration configuration;

    @BeforeEach
    void beforeEach()
    {
        when(this.configuration.getStyle()).thenReturn("default");
    }

    @Test
    void parallelScaling() throws Exception
    {
        HighlightParser parser = this.componentManager.getInstance(HighlightParser.class, "default");

        StringBuilder report = new StringBuilder();
        report.append(String.format("%d operations per thread, %d available processors%n",
            OPERATIONS_PER_THREAD, Runtime.getRuntime().availableProcessors()));

        report.append(String.format("%nPygments highlighting (language: %s)%n", LANGUAGE));
        measureScaling(variant -> highlight(parser, variant), report);

        // The same harness with a CPU-bound tokenizer that shares no state between threads, to tell the scaling limits
        // of the machine (fewer physical cores than threads, lower all-core clock, background load) apart from those
        // of the highlighting.
        report.append(String.format("%nControl: java.util.regex tokenizer without shared state%n"));
        measureScaling(PygmentsParserScalingTest::tokenize, report);

        Path reportFile = Path.of("target", "code-macro-scaling.txt");
        Files.writeString(reportFile, report, StandardCharsets.UTF_8);
    }

    private void measureScaling(Operation operation, StringBuilder report) throws Exception
    {
        for (int i = 0; i < WARMUP_OPERATIONS; i++) {
            operation.run(i);
        }

        report.append(String.format("%-8s %12s %14s %10s %12s%n", "threads", "time (ms)", "operations/s", "speedup",
            "efficiency"));

        double singleThreadThroughput = 0;
        for (int threads = 1; threads <= MAX_THREADS; threads++) {
            double throughput = measureThroughput(operation, threads);
            if (threads == 1) {
                singleThreadThroughput = throughput;
            }
            double speedup = throughput / singleThreadThroughput;
            long totalMillis = Math.round(threads * OPERATIONS_PER_THREAD / throughput * 1000);
            report.append(String.format("%-8d %12d %14.1f %10.2f %11.0f%%%n", threads, totalMillis, throughput,
                speedup, speedup / threads * 100));
        }
    }

    /**
     * Each thread runs the same number of operations, so that the ideal throughput grows linearly with the number of
     * threads.
     *
     * @return the number of operations per second over all threads
     */
    private double measureThroughput(Operation operation, int threads) throws Exception
    {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            CyclicBarrier barrier = new CyclicBarrier(threads + 1);
            List<Future<?>> futures = new ArrayList<>();
            for (int thread = 0; thread < threads; thread++) {
                int offset = (thread + 1) * 100_000;
                futures.add(executor.submit(() -> {
                    barrier.await();
                    for (int i = 0; i < OPERATIONS_PER_THREAD; i++) {
                        operation.run(offset + i);
                    }
                    return null;
                }));
            }

            barrier.await();
            long start = System.nanoTime();
            for (Future<?> future : futures) {
                future.get();
            }
            long elapsed = System.nanoTime() - start;

            return threads * OPERATIONS_PER_THREAD / (elapsed / 1e9);
        } finally {
            executor.shutdownNow();
        }
    }

    private static String snippet(int variant)
    {
        return String.format(SNIPPET, variant, variant % 17 + 3);
    }

    private static void highlight(HighlightParser parser, int variant) throws Exception
    {
        assertFalse(parser.highlight(LANGUAGE, new StringReader(snippet(variant))).isEmpty());
    }

    /**
     * Tokenizes the snippet like a regular expression lexer does: at each position, try the rules in order.
     */
    private static void tokenize(int variant)
    {
        String code = snippet(variant);
        List<String> tokens = new ArrayList<>();
        for (int round = 0; round < CONTROL_ROUNDS; round++) {
            Matcher matcher = CONTROL_RULES.get(0).matcher(code);
            int position = 0;
            while (position < code.length()) {
                int end = position + 1;
                for (Pattern rule : CONTROL_RULES) {
                    matcher.usePattern(rule).region(position, code.length());
                    if (matcher.lookingAt()) {
                        end = matcher.end();
                        break;
                    }
                }
                tokens.add(code.substring(position, end));
                position = end;
            }
        }
        assertFalse(tokens.isEmpty());
    }

    @FunctionalInterface
    private interface Operation
    {
        void run(int variant) throws Exception;
    }
}
