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
package org.xwiki.resource.test.ui;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies how the requests for Entity Resource Actions are routed: to their Handler when one is registered (for the
 * whole farm through the Resource Reference Handler Servlet, for a wiki through {@code XWikiAction}), and to the
 * legacy {@code XWikiAction}s otherwise.
 * <p>
 * The requests are sent without the browser so that the exact status and content of each response can be checked,
 * both as guest and as an authenticated user.
 * <p>
 * This class is not annotated with {@code @UITest} on purpose: it is run as a {@link org.junit.jupiter.api.Nested}
 * class of {@link AllIT}, which starts XWiki once and shares it across all the test classes.
 *
 * @version $Id$
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EntityResourceActionIT
{
    private static final String PAGE_PATH = "/Space/Page";

    // XWikiAction forwards the URLs of missing terminal pages to the home page of the space with the same name before
    // calling the Handlers it cannot know about before the XWiki Context exists, so target an existing page.
    private static final String EXISTING_PAGE_PATH = "/XWiki/XWikiPreferences";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    @Order(1)
    void farmHandler(TestUtils setup) throws Exception
    {
        // The Handler is registered for the whole farm: the RoutingFilter sends the request to the Resource Reference
        // Handler Servlet, which initializes the XWiki Context and sets the action of the URL in it.
        HttpResponse<String> response = get(setup, "bin/testfarmaction" + PAGE_PATH, false);
        assertEquals(200, response.statusCode(), response.body());
        assertHandledBy(response, "TestFarmEntityResourceActionHandler", "testfarmaction", "null", "xwiki:Space.Page");

        // The request is authenticated before calling the Handler.
        response = get(setup, "bin/testfarmaction" + PAGE_PATH, true);
        assertEquals(200, response.statusCode(), response.body());
        assertHandledBy(response, "TestFarmEntityResourceActionHandler", "testfarmaction", "xwiki:XWiki.superadmin",
            "xwiki:Space.Page");
    }

    @Test
    @Order(2)
    void wikiHandler(TestUtils setup) throws Exception
    {
        // Without a Handler, the action is unknown and the URL is the one of a missing page.
        assertEquals(404, get(setup, "bin/testwikiaction" + PAGE_PATH, false).statusCode());

        // Register a Handler for the main wiki only, like an Extension installed on that wiki would be. Registering a
        // component requires programming right, which is only granted when the viewer of the script has it too.
        setup.loginAsSuperAdmin();
        assertEquals("registered", setup.executeWikiPlain(
            "{{velocity}}$services.testentityresource.registerWikiHandler()registered{{/velocity}}", Syntax.XWIKI_2_1)
            .trim());

        // The RoutingFilter cannot see the Handlers registered for a wiki, so XWikiAction calls it.
        HttpResponse<String> response = get(setup, "bin/testwikiaction" + EXISTING_PAGE_PATH, false);
        assertEquals(200, response.statusCode(), response.body());
        assertHandledBy(response, "TestWikiEntityResourceActionHandler", "testwikiaction", "null",
            "xwiki:XWiki.XWikiPreferences");

        response = get(setup, "bin/testwikiaction" + EXISTING_PAGE_PATH, true);
        assertEquals(200, response.statusCode(), response.body());
        assertHandledBy(response, "TestWikiEntityResourceActionHandler", "testwikiaction", "xwiki:XWiki.superadmin",
            "xwiki:XWiki.XWikiPreferences");
    }

    @Test
    @Order(3)
    void legacyActionsAndServlets(TestUtils setup) throws Exception
    {
        // Actions without any Handler are still executed by XWikiAction.
        HttpResponse<String> response = get(setup, "bin/get/XWiki/XWikiPreferences", false);
        assertEquals(200, response.statusCode(), response.body());
        assertFalse(response.body().contains("handler="), response.body());

        // The Servlets reached through short URLs (which are resolved as Entity Resources by the standard URL scheme)
        // are still called.
        response = get(setup, "rest/wikis/xwiki", true);
        assertEquals(200, response.statusCode(), response.body());
    }

    private HttpResponse<String> get(TestUtils setup, String path, boolean authenticated) throws Exception
    {
        HttpRequest.Builder request =
            HttpRequest.newBuilder(URI.create(setup.getCurrentExecutor().getHttpClientBaseURL() + path)).GET();
        if (authenticated) {
            String credentials = TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName() + ':'
                + TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword();
            request.header("Authorization",
                "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
        }

        return this.httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private void assertHandledBy(HttpResponse<String> response, String handler, String action, String user,
        String document)
    {
        String body = response.body();
        assertTrue(body.contains("handler=" + handler), body);
        assertTrue(body.contains("action=" + action), body);
        assertTrue(body.contains("user=" + user), body);
        assertTrue(body.contains("reference=Document " + document), body);
        assertTrue(body.contains("doc=" + document), body);
    }
}
