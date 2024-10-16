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

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.TimeoutException;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * UI test for the xwiki-page-ready JavaScript module.
 *
 * @version $Id$
 */
@UITest
class PageReadyIT
{
    @Test
    @Order(1)
    void testTimeoutReporting(TestUtils testUtils, TestReference testReference)
    {
        // Create a page with an HTML macro that causes the pageReady wait to fail.
        testUtils.loginAsSuperAdmin();
        int originalTimeout = testUtils.getDriver().getTimeout();
        try {
            // Reduce the timeout to make the test faster.
            testUtils.getDriver().setTimeout(5);
            TimeoutException exception =
                assertThrows(TimeoutException.class, () -> testUtils.createPage(testReference, """
                    {{html clean="false"}}
                    <script>
                    require(['xwiki-page-ready'], function(pageReady) {
                      pageReady.delayPageReady(new Promise((resolve, reject) => {
                        // Intentionally don't resolve or reject the promise to make the pageReady wait fail.
                      }), 'testing pageReady');
                    });
                    </script>
                    {{/html}}
                    """));
            // Verify that we got the pending delay in the message. We don't care for this test if there were any
            // other pending delays.
            assertThat(exception.getMessage(), containsString("testing pageReady"));
        } finally {
            testUtils.getDriver().setTimeout(originalTimeout);
        }
    }
}
