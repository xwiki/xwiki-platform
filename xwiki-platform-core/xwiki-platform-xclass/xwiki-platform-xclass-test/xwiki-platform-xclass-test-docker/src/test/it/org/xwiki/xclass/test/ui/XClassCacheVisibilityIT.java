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
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.text.StringUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that an XClass document stays visible to the store while a document using it is being loaded.
 * <p>
 * Loading a document resolves the XClass of each of its objects inside the transaction opened for that load. On a
 * database whose transaction isolation is {@code REPEATABLE READ} - the InnoDB default, so MySQL and MariaDB - that
 * nested read is answered from the snapshot taken when the load started, so an XClass committed after that point is
 * invisible. {@code XWikiCacheStore} then caches that verdict with no expiry, which leaves the class definition
 * unusable: property display returns an empty string, edit forms and administration sections lose their fields, and a
 * XAR export of the class carries the outdated definition.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
@UITest
class XClassCacheVisibilityIT
{
    private static final String TEST_SCRIPT = """
        {{velocity wiki="false"}}
        $services.xclassVisibilityTest.perform()
        {{/velocity}}
        """;

    private static final String ISOLATION_SCRIPT = """
        {{velocity wiki="false"}}
        $services.xclassVisibilityTest.transactionIsolation
        {{/velocity}}
        """;

    /**
     * Covers both an XClass that is created and one that gains a property while a document using it is being loaded.
     * The ordering of the load, the snapshot it reads from and the save is decided by latches rather than by timing,
     * so this either reproduces the problem or doesn't, the same way on every machine.
     */
    @Test
    void xclassStaysVisibleWhileBeingSaved(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();

        String result = setup.executeWiki(TEST_SCRIPT, Syntax.XWIKI_2_1, Map.of("outputSyntax", "plain"));

        assertTrue(StringUtils.isBlank(result), result);
    }

    /**
     * Guards the configuration side of the fix. XWiki sets {@code hibernate.connection.isolation} to READ COMMITTED
     * when the Hibernate configuration doesn't, because it would otherwise inherit the driver default, which is
     * REPEATABLE READ on MySQL and MariaDB. This fails loudly if that setting is ever dropped again, instead of the
     * problem resurfacing as unrelated tests failing at random.
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
                + "that load is invisible, and the document cache keeps that verdict indefinitely.");
    }
}
