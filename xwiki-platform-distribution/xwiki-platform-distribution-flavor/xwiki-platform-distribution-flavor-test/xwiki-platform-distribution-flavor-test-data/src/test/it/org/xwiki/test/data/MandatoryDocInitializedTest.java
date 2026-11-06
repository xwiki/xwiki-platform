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

package org.xwiki.test.data;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xwiki.test.integration.XWikiExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Check that all mandatory document initializers are properly declared in the demo package build and that the pages
 * are not created at first start only. See also XWIKI-22368.
 * <p>
 * This can't be a Docker-based functional test: the Docker test framework starts XWiki with an empty database, while
 * this test needs the database that the demo package build pre-populates.
 *
 * @version $Id$
 * @since 16.7.0
 */
class MandatoryDocInitializedTest
{
    private static final XWikiExecutor EXECUTOR = new XWikiExecutor(0);

    private static final String ADMIN_AUTHORIZATION =
        "Basic " + Base64.getEncoder().encodeToString("Admin:admin".getBytes(StandardCharsets.UTF_8));

    private static final String PAGE_PATH = "Test/MandatoryDocInitialized";

    private static final String GROOVY_CODE =
    """
        {{groovy}}
        import com.xpn.xwiki.doc.MandatoryDocumentInitializer;
        import java.text.SimpleDateFormat;
        import java.lang.management.ManagementFactory;

        // define the pages that should be created at startup
        def startupPages = ["XWiki.XWikiServerXwiki"];
        
        // Pages that are completely ignored from the test
        // SheetClass is initialized in a legacy package: we got it tested from the CI but it's expected that it's
        // initialized late.
        def ignoredPages = ["XWiki.SheetClass"];

        def dateFormat = new SimpleDateFormat("dd/MM/YYYY HH:mm:ss");
        def rb = ManagementFactory.getRuntimeMXBean();
        def startedSince = new Date(rb.getStartTime());
        
        def startupPageRef = [];
        for (startupPage in startupPages) {
          startupPageRef.add(services.model.resolveDocument(startupPage));
        }
        
        def ignoredPagesRef = [];
        for (ignoredPage in ignoredPages) {
          ignoredPagesRef.add(services.model.resolveDocument(ignoredPage));
        }

        def componentManager = services.component.getContextComponentManager();
        def documentInitializersList = componentManager.getInstanceList(MandatoryDocumentInitializer.class);
        
        def foundErrorsCreatedAfter = [];
        def foundErrorsCreatedBefore = [];
        for (initializer in documentInitializersList) {
          def ref = initializer.getDocumentReference();
          // the provided ref might be a local document ref, so we serialize and resolve again to obtain same 
          // canonical ref
          if (!ignoredPagesRef.contains(services.model.resolveDocument(services.model.serialize(ref)))) {
            def classDoc = xwiki.getDocument(ref);
            def creationDate = classDoc.getCreationDate();
            def shouldBeBefore = (!startupPageRef.contains(ref));
            if (shouldBeBefore && creationDate.after(startedSince)) {
              foundErrorsCreatedAfter.add(ref.toString() + " ("+dateFormat.format(creationDate)+")");
            } else if (!shouldBeBefore && creationDate.before(startedSince)) {
              foundErrorsCreatedBefore.add(ref.toString() + " ("+dateFormat.format(creationDate)+")");
            }
          }
        }

        if (foundErrorsCreatedAfter.size() > 0) {
          println "Instance started at " + dateFormat.format(startedSince);
          println foundErrorsCreatedAfter.size() + " page found created after:\\n";
          for (error in foundErrorsCreatedAfter) {
            println "     * " + error;
          }
        }
        if (foundErrorsCreatedBefore.size() > 0) {
          println "Instance started at " + dateFormat.format(startedSince);
          println foundErrorsCreatedBefore.size() + " page found created before while they should be created at startup:\\n";
          for (error in foundErrorsCreatedBefore) {
            println "     * " + error;
          }
        }
        {{/groovy}}
    """;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @BeforeAll
    static void startXWiki() throws Exception
    {
        EXECUTOR.start();
    }

    @AfterAll
    static void stopXWiki() throws Exception
    {
        EXECUTOR.stop();
    }

    @Test
    void docsAreInitialized() throws Exception
    {
        String baseURL = EXECUTOR.getHttpClientBaseURL();

        // Save the script in a page, as Admin so that it gets the Programming Right needed by the Groovy macro.
        String pageXML = "<page xmlns=\"http://www.xwiki.org\"><syntax>xwiki/2.1</syntax><content><![CDATA["
            + GROOVY_CODE + "]]></content></page>";
        HttpResponse<String> saveResponse = this.httpClient.send(HttpRequest.newBuilder()
            .uri(URI.create(baseURL + "rest/wikis/xwiki/spaces/" + PAGE_PATH.replace("/", "/pages/")))
            .header("Authorization", ADMIN_AUTHORIZATION)
            .header("Content-Type", "application/xml")
            .PUT(HttpRequest.BodyPublishers.ofString(pageXML))
            .build(), HttpResponse.BodyHandlers.ofString());
        // 201 when the page is created, 202 when it already exists (when XWiki is reused between runs).
        assertTrue(saveResponse.statusCode() == 201 || saveResponse.statusCode() == 202, saveResponse::body);

        // Execute the script and get its plain text output.
        HttpResponse<String> executeResponse = this.httpClient.send(HttpRequest.newBuilder()
            .uri(URI.create(baseURL + "bin/get/" + PAGE_PATH + "?outputSyntax=plain&basicauth=1"))
            .header("Authorization", ADMIN_AUTHORIZATION)
            .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, executeResponse.statusCode(), executeResponse.body());

        // Using equals on purpose to see the output in case of failure.
        assertEquals("", executeResponse.body().trim());
    }
}
