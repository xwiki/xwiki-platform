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
package org.xwiki.test.page.accessibility;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The Node.js side of the WCAG page test validation: a long-lived Node.js process running the
 * {@code axe-bridge.js} module resource (jsdom + axe-core) through {@code node --input-type=module -e},
 * communicating JSON over
 * stdin/stdout. The process is spawned once per JVM, when a validation first needs it, and is destroyed when
 * the JVM stops; validation calls are serialized over the single process.
 * <p>
 * The locations the process is spawned from are resolved by {@link AxeBridgeEnvironment}. When they cannot be
 * resolved (e.g. from a module depending on {@code xwiki-platform-test-page} through the Maven repository,
 * with no Node.js executable around), the bridge is considered missing and the validation is skipped or fails
 * depending on the {@code xwiki.testPage.wcag.strict} system property; see {@link WCAGValidator}.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
final class AxeBridge
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AxeBridge.class);

    /**
     * The fields of the JSON protocol exchanged with the Node.js side.
     */
    private static final String ID_FIELD = "id";

    private static final String HTML_FIELD = "html";

    private static final String TAGS_FIELD = "tags";

    private static final String TARGET_FIELD = "target";

    private static final String BRIDGE_SCRIPT_RESOURCE = "/org/xwiki/test/page/accessibility/axe-bridge.js";

    private static final String DISABLED_RULES_FIELD = "disabledRules";

    /**
     * The time after which a page validation is considered hung and the Node.js process is killed, in seconds.
     */
    private static final long VALIDATION_TIMEOUT = 60;

    /**
     * Marks the death of the Node.js process in the responses queue.
     */
    private static final String PROCESS_DIED = "<axe-bridge process died>";

    private static AxeBridge instance;

    private final Process process;

    private final BufferedWriter requests;

    private final BlockingQueue<String> responses = new ArrayBlockingQueue<>(8);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private int requestCount;

    static {
        // Make sure the Node.js process never outlives the JVM.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            synchronized (AxeBridge.class) {
                if (instance != null) {
                    instance.close();
                }
            }
        }, "xwiki-page-test-axe-bridge-shutdown"));
    }

    private AxeBridge(AxeBridgeEnvironment environment) throws IOException
    {
        this.process = new ProcessBuilder(environment.node().toString(), "--input-type=module", "-e",
            readBridgeScript(), environment.npmRoot().toString())
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start();
        this.requests = new BufferedWriter(new OutputStreamWriter(this.process.getOutputStream(),
            StandardCharsets.UTF_8));
        Thread readerThread = new Thread(this::readProcessOutput, "xwiki-page-test-axe-bridge");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private static String readBridgeScript() throws IOException
    {
        try (InputStream script = AxeBridge.class.getResourceAsStream(BRIDGE_SCRIPT_RESOURCE)) {
            if (script == null) {
                throw new IOException("The axe-bridge.js module resource is missing from the classpath");
            }
            return new String(script.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void readProcessOutput()
    {
        try (BufferedReader stdout = new BufferedReader(new InputStreamReader(this.process.getInputStream(),
            StandardCharsets.UTF_8))) {
            String line;
            while ((line = stdout.readLine()) != null) {
                if (!this.responses.offer(line)) {
                    LOGGER.warn("Dropping an unexpected response of the axe-core Node.js bridge");
                }
            }
        } catch (IOException e) {
            LOGGER.debug("Failed to read from the axe-core Node.js bridge", e);
        } finally {
            if (!this.responses.offer(PROCESS_DIED)) {
                LOGGER.warn("Failed to mark the death of the axe-core Node.js bridge");
            }
        }
    }

    /**
     * @return the bridge, spawning the Node.js process if it is not already running; the process is kept alive
     *     for the rest of the JVM
     * @throws IOException when the process cannot be spawned
     */
    static synchronized AxeBridge getInstance() throws IOException
    {
        if (instance != null && !instance.process.isAlive()) {
            // The process died at some point: give a fresh one a chance on the next call.
            instance.close();
            instance = null;
        }
        if (instance == null) {
            instance = new AxeBridge(AxeBridgeEnvironment.resolve());
        }
        return instance;
    }

    /**
     * Checks whether the Node.js bridge can run at all.
     *
     * @return {@code null} when the bridge is available, a human readable reason explaining what is missing
     *     otherwise
     */
    static synchronized String getUnavailabilityReason()
    {
        try {
            AxeBridgeEnvironment.resolve();
            return null;
        } catch (IOException e) {
            return e.getMessage();
        }
    }

    /**
     * Validates one page with axe-core, through the Node.js bridge.
     *
     * @param html the rendered page to validate
     * @param tags the axe-core tags of the rules to take into account
     * @param disabledRules the ids of the rules to disable
     * @return the outcome of the validation
     * @throws IOException when the bridge cannot be reached or answers outside the protocol
     */
    synchronized AxePageResult analyze(String html, List<String> tags, List<String> disabledRules)
        throws IOException
    {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put(ID_FIELD, ++this.requestCount);
        request.put(HTML_FIELD, html);
        request.put(TAGS_FIELD, tags);
        request.put(DISABLED_RULES_FIELD, disabledRules);

        this.requests.write(this.objectMapper.writeValueAsString(request));
        this.requests.write('\n');
        this.requests.flush();

        JsonNode response = this.objectMapper.readTree(awaitResponse());
        if (response.path(ID_FIELD).asInt(-1) != this.requestCount) {
            throw new IOException(String.format("Unexpected response from the axe-core Node.js bridge: [%s]",
                response));
        }
        if (!response.path("ok").asBoolean()) {
            throw new IOException(String.format("The axe-core Node.js bridge failed to validate the page: [%s]",
                response.path("error").asText()));
        }
        return new AxePageResult(parseRules(response.get("violations")), parseRules(response.get("incomplete")));
    }

    private String awaitResponse() throws IOException
    {
        String responseLine;
        try {
            responseLine = this.responses.poll(VALIDATION_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for the axe-core Node.js bridge", e);
        }
        if (responseLine == null) {
            this.process.destroy();
            throw new IOException(String.format("The axe-core Node.js bridge did not answer within [%d] seconds",
                VALIDATION_TIMEOUT));
        }
        if (PROCESS_DIED.equals(responseLine)) {
            this.process.destroy();
            throw new IOException("The axe-core Node.js bridge died while validating the page");
        }
        return responseLine;
    }

    private static List<AxeRuleResult> parseRules(JsonNode rules)
    {
        List<AxeRuleResult> results = new ArrayList<>();
        if (rules != null) {
            for (JsonNode rule : rules) {
                List<AxeNodeResult> nodes = new ArrayList<>();
                for (JsonNode node : rule.get("nodes")) {
                    List<String> target = new ArrayList<>();
                    node.get(TARGET_FIELD).forEach(selector -> target.add(selector.asText()));
                    nodes.add(new AxeNodeResult(node.path(HTML_FIELD).asText(), target,
                        node.path("failureSummary").asText()));
                }
                List<String> tags = new ArrayList<>();
                rule.get(TAGS_FIELD).forEach(tag -> tags.add(tag.asText()));
                results.add(new AxeRuleResult(rule.path(ID_FIELD).asText(), rule.path("impact").asText(),
                    rule.path("help").asText(), rule.path("description").asText(),
                    rule.path("helpUrl").asText(), tags, nodes));
            }
        }
        return results;
    }

    private void close()
    {
        this.process.destroy();
        try {
            this.requests.close();
        } catch (IOException e) {
            LOGGER.debug("Failed to close the axe-core Node.js bridge", e);
        }
    }

}
