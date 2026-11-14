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
package org.xwiki.test.webstandards;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.xwiki.http.internal.XWikiHTTPClient;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceSerializer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.test.integration.junit.LogCapture;
import org.xwiki.test.integration.junit.LogCaptureConfiguration;
import org.xwiki.test.integration.junit.LogCaptureValidator;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.validator.HTML5Validator;
import org.xwiki.validator.RSSValidator;
import org.xwiki.validator.ValidationError;
import org.xwiki.validator.Validator;
import org.xwiki.validator.XWikiValidator;
import org.xwiki.xar.XarEntry;
import org.xwiki.xar.XarPackage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Verifies that the pages of the flavor, and the administration and RSS URLs listed in the pom, are valid against
 * HTML5, XWiki's own rules and RSS, and that XWiki doesn't log any error while rendering them.
 *
 * @version $Id$
 */
class WebStandardsTest
{
    private static final XWikiExecutor EXECUTOR = new XWikiExecutor(0);

    private static final DefaultStringEntityReferenceSerializer SERIALIZER =
        new DefaultStringEntityReferenceSerializer();

    private static final WikiReference WIKI_REFERENCE = new WikiReference("xwiki");

    private static XWikiHTTPClient adminClient;

    private static XWikiHTTPClient guestClient;

    private record Target(String url, DocumentReference documentReference, XWikiHTTPClient client)
    {
        String getName()
        {
            return String.format("[%s] %s", this.documentReference != null
                ? SERIALIZER.serialize(this.documentReference) : this.url,
                this.client == adminClient ? "as Admin" : "as guest");
        }
    }

    @BeforeAll
    static void startXWiki() throws Exception
    {
        // Don't set any extension repository since we don't need it and we don't want to require internet connection
        // for the tests (it would also make the validated content depend on what the remote repositories return).
        EXECUTOR.loadXWikiPropertiesConfiguration().setProperty("extension.repositories", "");
        EXECUTOR.saveXWikiProperties();
        EXECUTOR.start();

        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
        // The code that prevents circular redirects ignores the query string when comparing the redirect location
        // with the current location. The browser doesn't behave like this and we have pages that redirect to
        // themselves with different query string parameters.
        requestConfigBuilder.setCircularRedirectsAllowed(true);
        // Limit the number of redirects because we don't detect circular redirect any more.
        requestConfigBuilder.setMaxRedirects(3);
        adminClient = new XWikiHTTPClient(
            XWikiHTTPClient.builder().setDefaultRequestConfig(requestConfigBuilder.build()));
        adminClient.setDefaultCredentials(TestUtils.ADMIN_CREDENTIALS);
        guestClient = new XWikiHTTPClient();
    }

    @AfterAll
    static void stopXWiki() throws Exception
    {
        // Always stop XWiki, even when the clients couldn't be created, so that it doesn't keep using the port.
        try {
            if (adminClient != null) {
                adminClient.close();
            }
            if (guestClient != null) {
                guestClient.close();
            }
        } finally {
            EXECUTOR.stop();
        }
    }

    @TestFactory
    Stream<DynamicTest> html5() throws Exception
    {
        return validate(new HTML5Validator(), getTargets());
    }

    @TestFactory
    Stream<DynamicTest> xwiki() throws Exception
    {
        return validate(new XWikiValidator(), getTargets());
    }

    @TestFactory
    Stream<DynamicTest> rss()
    {
        List<Target> targets = new ArrayList<>();
        targets.addAll(getURLTargets("rssUrlsToTestAsAdmin", adminClient));
        targets.addAll(getURLTargets("rssUrlsToTestAsGuest", guestClient));
        return validate(new RSSValidator(), targets);
    }

    private Stream<DynamicTest> validate(Validator validator, List<Target> targets)
    {
        return targets.stream().map(target -> DynamicTest.dynamicTest(target.getName(), () -> {
            // Capture the XWiki logs (XWiki is started by this JVM which prints them) to detect server-side errors.
            // The capture also covers the validation since the logs of the XWiki process are received asynchronously.
            LogCapture logCapture = new LogCapture();
            logCapture.startCapture();
            Throwable failure = null;
            try {
                byte[] responseBody = getResponseBody(target);
                validator.setDocument(new ByteArrayInputStream(responseBody));
                assertValid(validator.validate(), target, responseBody);
            } catch (Throwable e) {
                failure = e;
                throw e;
            } finally {
                validateLogs(logCapture.stopCapture(), failure);
            }
        }));
    }

    private void validateLogs(String logs, Throwable failure)
    {
        try {
            new LogCaptureValidator().validate(logs, new LogCaptureConfiguration(), false);
        } catch (AssertionError e) {
            // Don't hide the failure of the validation itself (e.g. an unexpected HTTP status code).
            if (failure == null) {
                throw e;
            }
            failure.addSuppressed(e);
        }
    }

    private byte[] getResponseBody(Target target) throws IOException
    {
        return target.client().executeGet(target.url(), (response, context) -> {
            URI uri;
            try {
                uri = context.getRequest().getUri();
            } catch (URISyntaxException e) {
                throw new HttpException("Failed to access latest request URI", e);
            }
            String uriMessage = target.url().equals(uri.toString()) ? String.format("[%s]", uri)
                : String.format("[%s] (redirected to [%s])", target.url(), uri);

            // HTTP code 423 is sent when trying to perform an action on a protected page and thus it's valid.
            int statusCode = response.getCode();
            assertTrue(statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_LOCKED,
                String.format("Method %s failed with status [%s]", uriMessage, response.getReasonPhrase()));

            return EntityUtils.toByteArray(response.getEntity());
        });
    }

    private void assertValid(List<ValidationError> errors, Target target, byte[] responseBody) throws IOException
    {
        StringBuilder message = new StringBuilder("Validation errors in " + target.url());
        Set<Integer> errorLines = new HashSet<>();
        boolean hasError = false;
        for (ValidationError error : errors) {
            if (error.getType() != ValidationError.Type.WARNING) {
                message.append('\n').append(error);
                if (error.getLine() >= 0) {
                    message.append(" at line [").append(error.getLine()).append("] column [")
                        .append(error.getColumn()).append(']');
                    for (int line = error.getLine() - 2; line <= error.getLine() + 2; line++) {
                        errorLines.add(line);
                    }
                }
                hasError = true;
            }
        }

        if (hasError) {
            message.append("\n\nExtracts of the invalid content:");
            String[] lines = new String(responseBody, StandardCharsets.UTF_8).split("\n", -1);
            int lastErrorLine = -1;
            for (int index = 1; index <= lines.length; index++) {
                if (errorLines.contains(index)) {
                    if (lastErrorLine != index - 1) {
                        message.append('\n');
                    }
                    message.append("\n  ").append(index).append('\t').append(lines[index - 1]);
                    lastErrorLine = index;
                }
            }
            fail(message.toString());
        }
    }

    /**
     * @return the pages of the flavor (filtered with the {@code documentsToTest} regex, if set) and the
     *     administration URLs, all as Admin, plus the URLs to test as guest
     */
    private List<Target> getTargets() throws Exception
    {
        List<Target> targets = new ArrayList<>();
        for (DocumentReference documentReference : readXarContents()) {
            targets.add(new Target(getURL(documentReference), documentReference, adminClient));
        }
        targets.addAll(getURLTargets("urlsToTestAsAdmin", adminClient));
        targets.addAll(getURLTargets("urlsToTestAsGuest", guestClient));
        return targets;
    }

    private List<Target> getURLTargets(String property, XWikiHTTPClient client)
    {
        List<Target> targets = new ArrayList<>();
        for (String url : StringUtils.split(System.getProperty(property, ""))) {
            // The URL is either absolute or a path relative to the XWiki server.
            targets.add(new Target(URI.create(EXECUTOR.getHttpClientBaseURL()).resolve(url).toString(), null, client));
        }
        return targets;
    }

    private String getURL(DocumentReference documentReference)
    {
        StringBuilder url = new StringBuilder(EXECUTOR.getHttpClientBaseURL()).append("bin/view/");
        for (SpaceReference spaceReference : documentReference.getSpaceReferences()) {
            url.append(URLEncoder.encode(spaceReference.getName(), StandardCharsets.UTF_8)).append('/');
        }
        url.append(URLEncoder.encode(documentReference.getName(), StandardCharsets.UTF_8));
        return url.toString();
    }

    private List<DocumentReference> readXarContents() throws Exception
    {
        // The pages of the flavor, exported by the packager plugin.
        String path = System.getProperty("pathToDocuments");
        String patternFilter = System.getProperty("documentsToTest");
        Pattern pattern = StringUtils.isEmpty(patternFilter) ? null : Pattern.compile(patternFilter);

        List<DocumentReference> result = new ArrayList<>();
        for (XarEntry entry : new XarPackage(new File(path)).getEntries()) {
            if (pattern == null || pattern.matcher(SERIALIZER.serialize(entry)).matches()) {
                result.add(new DocumentReference(entry, WIKI_REFERENCE));
            }
        }
        // Don't let the tests pass without validating any page if the export of the pages went wrong.
        assertFalse(result.isEmpty(), String.format("No page found in [%s]", path));
        return result;
    }
}
