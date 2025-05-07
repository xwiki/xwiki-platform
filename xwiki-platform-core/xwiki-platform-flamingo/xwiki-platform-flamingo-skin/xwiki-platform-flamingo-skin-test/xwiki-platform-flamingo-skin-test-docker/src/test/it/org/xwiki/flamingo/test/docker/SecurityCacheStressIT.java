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

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.text.StringUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Stress test for the security cache. Modify rights while simultaneously querying the cache in several background
 * threads.
 *
 * @version $Id$
 */
@UITest
class SecurityCacheStressIT
{
    private static final String STRESS_TEST_SCRIPT = """
        {{velocity wiki="false"}}
        $services.securityCachePerformanceTest.perform(%s)
        {{/velocity}}
        """;

    private static final String QUOTE = "\"";

    @Test
    void stressTest(TestUtils testUtils) throws Exception
    {
        testUtils.loginAsSuperAdmin();

        // Create 20 users.
        List<String> users = IntStream.range(0, 20)
            .mapToObj(i -> "SecurityCacheStressITUser" + i)
            .toList();
        for (String user : users) {
            testUtils.createUser(user, user, null);
        }

        String usersParameter =
            "[" + users.stream().map(u -> QUOTE + u + QUOTE).collect(Collectors.joining(", ")) + "]";

        // Set the page load timeout to 10 minutes, but first get the current one to set it back after the test.
        Duration currentTimeout = testUtils.getDriver().manage().timeouts().getPageLoadTimeout();
        testUtils.getDriver().manage().timeouts().pageLoadTimeout(Duration.ofMinutes(10));
        try {
            String result = testUtils.executeWiki(STRESS_TEST_SCRIPT.formatted(usersParameter), Syntax.XWIKI_2_1,
                Map.of("outputSyntax", "plain"));

            assertTrue(StringUtils.isBlank(result), result);
        } finally {
            testUtils.getDriver().manage().timeouts().pageLoadTimeout(currentTimeout);
        }
    }
}
