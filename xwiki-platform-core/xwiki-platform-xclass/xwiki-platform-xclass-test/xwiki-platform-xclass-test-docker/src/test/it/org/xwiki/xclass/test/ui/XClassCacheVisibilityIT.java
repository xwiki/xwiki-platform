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
package org.xwiki.xclass.test.ui;

import java.sql.Connection;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.text.StringUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that an XClass document stays visible to the store while it is being created concurrently with a document
 * that uses it.
 * <p>
 * Loading a document resolves the XClass of each of its objects inside the transaction opened for that load. On a
 * database whose transaction isolation is {@code REPEATABLE READ} — the InnoDB default, so MySQL and MariaDB — that
 * nested read is answered from the snapshot taken when the load started, so an XClass committed after that point is
 * invisible. {@code XWikiCacheStore} then caches that verdict with no expiry, which leaves the XClass permanently
 * unusable: administration sections render without their fields, and the Solr indexer silently skips the document.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
@UITest
class XClassCacheVisibilityIT
{
    /**
     * One object of each of this many distinct XClasses, so that loading the probe document has to read that many
     * separate XClass documents. Distinct classes rather than many objects of one class deliberately: batching the
     * loading of an object's properties would be a welcome optimisation, and it must not silently shrink the window
     * this test aims at.
     * <p>
     * Sized from measurement rather than taste: 50 classes made the probe load in 37 ms on a developer machine, too
     * short to aim a sweep of 20 rounds at. Each class costs roughly 0.75 ms there, so this gives about 300 ms, and
     * more on a slower machine.
     */
    private static final int FILLER_CLASS_COUNT = 400;

    /**
     * Also the resolution of the delay sweep across the load window.
     */
    private static final int ROUNDS = 20;

    private static final String TEST_SCRIPT = """
        {{velocity wiki="false"}}
        $services.xclassVisibilityTest.perform(%d, %d)
        {{/velocity}}
        """;

    private static final String ISOLATION_SCRIPT = """
        {{velocity wiki="false"}}
        $services.xclassVisibilityTest.transactionIsolation
        {{/velocity}}
        """;

    @Test
    void xclassStaysVisibleWhileBeingRecreated(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();

        Duration previousTimeout = setup.getDriver().manage().timeouts().getPageLoadTimeout();
        setup.getDriver().manage().timeouts().pageLoadTimeout(Duration.ofMinutes(10));
        try {
            String result = setup.executeWiki(TEST_SCRIPT.formatted(FILLER_CLASS_COUNT, ROUNDS), Syntax.XWIKI_2_1,
                Map.of("outputSyntax", "plain"));

            assertTrue(StringUtils.isBlank(result), result);
        } finally {
            setup.getDriver().manage().timeouts().pageLoadTimeout(previousTimeout);
        }
    }

    /**
     * Guards the configuration side of the fix. XWiki does not set {@code hibernate.connection.isolation}, so it
     * inherits whatever the driver defaults to; on MySQL and MariaDB that is {@code REPEATABLE READ}, under which the
     * nested XClass read above can be served from a stale snapshot. This fails loudly if that setting is ever dropped
     * again, instead of the problem resurfacing as unrelated tests failing at random.
     */
    @Test
    void transactionIsolationIsNotRepeatableRead(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();

        String result =
            setup.executeWiki(ISOLATION_SCRIPT, Syntax.XWIKI_2_1, Map.of("outputSyntax", "plain")).trim();

        assertTrue(result.matches("-?\\d+"), "Could not read the transaction isolation level, got [" + result + "]");

        assertEquals(Connection.TRANSACTION_READ_COMMITTED, Integer.parseInt(result),
            "The store is not running at READ COMMITTED (2). A document load resolves its objects' XClasses inside "
                + "the load's own transaction, so at REPEATABLE READ (4) an XClass committed by another thread during "
                + "that load is invisible, and the document cache keeps that verdict indefinitely. XWiki does not set "
                + "hibernate.connection.isolation by default, so it inherits the driver default, which is REPEATABLE "
                + "READ on MySQL and MariaDB.");
    }
}
