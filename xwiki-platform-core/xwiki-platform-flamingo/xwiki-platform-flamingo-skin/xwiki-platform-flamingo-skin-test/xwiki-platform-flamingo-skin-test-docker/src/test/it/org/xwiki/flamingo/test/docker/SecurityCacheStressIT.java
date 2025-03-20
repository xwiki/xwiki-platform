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
package org.xwiki.flamingo.test.docker;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.text.StringUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Stress test for the security cache. This test takes about 15 minutes on a relatively powerful system of 2021 and
 * doesn't really test anything, it rather gives some performance numbers. Therefore, it isn't executed by default.
 *
 * @version $Id$
 */
@UITest
class SecurityCacheStressIT
{
    private static final String STREE_TEST_SCRIPT = """
        {{velocity wiki="false"}}
        $services.securityCachePerformanceTest.perform()
        {{/velocity}}
        """;

    @Test
    void stressTest(TestUtils testUtils) throws Exception
    {
        testUtils.loginAsSuperAdmin();

        // Create 20 users.
        List<String> users = IntStream.range(0, 20)
            .mapToObj(i -> "User" + i)
            .toList();
        for (String user : users) {
            testUtils.createUser(user, user, null);
        }

        String stressTestName = "securityCacheStressTest";
        testUtils.rest().savePage(new DocumentReference("xwiki", List.of("Test", "Execute"), stressTestName),
            STREE_TEST_SCRIPT, "Security Cache Stress Test");
        String baseURL = StringUtils.removeEnd(testUtils.rest().getBaseURL(), "rest");
        String viewURL = baseURL + "bin/get/Test/Execute/" + stressTestName + "?outputSyntax=plain";

        // Use Java 11 HTTP client to execute the script directly without browser involvement as with the browser we
        // get timeouts.
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMinutes(2))
            .build();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(viewURL))
            .timeout(Duration.ofMinutes(40))
            .GET()
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(StringUtils.isBlank(response.body()), response.body());
    }
}
