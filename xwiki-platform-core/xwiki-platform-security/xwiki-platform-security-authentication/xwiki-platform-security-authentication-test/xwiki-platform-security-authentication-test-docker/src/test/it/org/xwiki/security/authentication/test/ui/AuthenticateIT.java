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
package org.xwiki.security.authentication.test.ui;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.utils.Base64;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.ProtocolException;
import org.junit.jupiter.api.Test;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Miscellaneous authentication related tests.
 *
 * @version $Id$
 */
@UITest
class AuthenticateIT
{
    private final BasicCookieStore cookieStore = new BasicCookieStore();

    private String getValue(Header header)
    {
        if (header != null) {
            return header.getValue();
        }

        return null;
    }

    private String[] extractSessionIdAndUserIdFromResponse(ClassicHttpResponse response) throws ProtocolException
    {
        Optional<Cookie> cookie =
            this.cookieStore.getCookies().stream().filter(c -> "JSESSIONID".equals(c.getName())).findFirst();

        String sessionId = cookie.isPresent() ? cookie.get().getValue() : null;
        String userId = getValue(response.getHeader("xwiki-user"));

        return new String[] {sessionId, userId};
    }

    @Test
    void sessionId(TestUtils testUtils) throws Exception
    {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpClientContext context = HttpClientContext.create();
            context.setCookieStore(this.cookieStore);
            HttpGet httpGet = new HttpGet(testUtils.rest().getBaseURL() + "/xwiki/rest/");

            // First, query as guest user (to start a session)
            String sessionId = client.execute(httpGet, context, this::extractSessionIdAndUserIdFromResponse)[0];

            // Try again as guest (to make sure the session id does not change)
            assertEquals(sessionId, client.execute(httpGet, context, this::extractSessionIdAndUserIdFromResponse)[0]);

            // Try to authenticate (to make sure the session id changes)
            String auth =
                TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName() + ":" + TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword();
            byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.ISO_8859_1));
            String authHeader = "Basic " + new String(encodedAuth);
            httpGet.setHeader(HttpHeaders.AUTHORIZATION, authHeader);

            String[] response = client.execute(httpGet, context, this::extractSessionIdAndUserIdFromResponse);

            assertEquals("xwiki:XWiki.superadmin", response[1]);
            assertNotEquals(sessionId, response[0]);
        }
    }
}
