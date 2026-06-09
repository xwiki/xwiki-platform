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
package org.xwiki.security.authorization;

import org.junit.jupiter.api.BeforeEach;
import org.xwiki.security.authorization.internal.XWikiCachingRightService;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.impl.xwiki.XWikiRightServiceImpl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Abstract class that should be inherited when writing tests for comparing the result of different right service
 * implementations.
 * 
 * @version $Id$
 * @since 4.2
 */
public abstract class AbstractLegacyWikiTest extends AbstractWikiTest
{
    /**
     * Instance of the original implementation for reference.
     */
    private XWikiRightServiceImpl legacyImpl;

    /**
     * The instance that is beeing tested.
     */
    private XWikiCachingRightService cachingImpl;

    @BeforeEach
    void setUpLegacyImpl()
    {
        this.legacyImpl = new XWikiRightServiceImpl();
        this.cachingImpl = new XWikiCachingRightService();
    }

    /**
     * @param message A message explaining the assertion.
     * @param accessLevel The access level to check.
     * @param userName The name of the user.
     * @param documentName The document name.
     * @param ctx The context.
     */
    protected void assertAccessLevelTrue(String message, String accessLevel, String userName, String documentName,
        XWikiContext ctx) throws Exception
    {
        setContext(ctx);

        assertTrue(this.legacyImpl.hasAccessLevel(accessLevel, userName, documentName, ctx),
            "Old implementation: " + message);

        assertTrue(this.cachingImpl.hasAccessLevel(accessLevel, userName, documentName, ctx),
            "Caching implementation: " + message);
    }

    /**
     * @param message A message explaining the assertion.
     * @param accessLevel The access level to check.
     * @param userName The name of the user.
     * @param documentName The document name.
     * @param ctx The context.
     */
    protected void assertAccessLevelFalse(String message, String accessLevel, String userName, String documentName,
        XWikiContext ctx) throws Exception
    {
        setContext(ctx);

        assertFalse(this.legacyImpl.hasAccessLevel(accessLevel, userName, documentName, ctx),
            "Old implementation: " + message);

        assertFalse(this.cachingImpl.hasAccessLevel(accessLevel, userName, documentName, ctx),
            "Caching implementation: " + message);
    }

    /**
     * @param message A message explaining the assertion.
     * @param ctx The context.
     */
    protected void assertAdminRightsTrue(String message, XWikiContext ctx)
    {
        setContext(ctx);

        assertTrue(this.legacyImpl.hasAdminRights(ctx), "Old implementation: " + message);

        assertTrue(this.cachingImpl.hasAdminRights(ctx), "Caching implementation: " + message);
    }

    /**
     * @param message A message explaining the assertion.
     * @param ctx The context.
     */
    protected void assertAdminRightsFalse(String message, XWikiContext ctx)
    {
        setContext(ctx);

        assertFalse(this.legacyImpl.hasAdminRights(ctx), "Old implementation: " + message);

        assertFalse(this.cachingImpl.hasAdminRights(ctx), "Caching implementation: " + message);
    }

    /**
     * @param message A message explaining the assertion.
     * @param ctx The context.
     */
    protected void assertAdminRightsFalseExpectedDifference(String message, XWikiContext ctx)
    {
        setContext(ctx);

        assertTrue(this.legacyImpl.hasAdminRights(ctx), "Old implementation: " + message);

        assertFalse(this.cachingImpl.hasAdminRights(ctx), "Caching implementation: " + message);
    }

    /**
     * @param message A message explaining the assertion.
     * @param ctx The context.
     */
    protected void assertWikiAdminRightsTrue(String message, XWikiContext ctx)
    {
        setContext(ctx);

        assertTrue(this.legacyImpl.hasWikiAdminRights(ctx), "Old implementation: " + message);

        assertTrue(this.cachingImpl.hasWikiAdminRights(ctx), "Caching implementation: " + message);
    }

    /**
     * @param message A message explaining the assertion.
     * @param ctx The context.
     */
    protected void assertWikiAdminRightsFalse(String message, XWikiContext ctx)
    {
        setContext(ctx);

        assertFalse(this.legacyImpl.hasWikiAdminRights(ctx), "Old implementation: " + message);

        assertFalse(this.cachingImpl.hasWikiAdminRights(ctx), "Caching implementation: " + message);
    }

    /**
     * @param message A message explaining the assertion.
     * @param ctx The context.
     */
    protected void assertWikiAdminRightsFalseExpectedDifference(String message, XWikiContext ctx)
    {
        setContext(ctx);

        assertTrue(this.legacyImpl.hasWikiAdminRights(ctx), "Old implementation: " + message);

        assertFalse(this.cachingImpl.hasWikiAdminRights(ctx), "Caching implementation: " + message);
    }

    /**
     * @param message A message explaining the assertion.
     * @param accessLevel The access level to check.
     * @param userName The name of the user.
     * @param documentName The document name.
     * @param ctx The context.
     */
    protected void assertAccessLevelTrueExpectedDifference(String message, String accessLevel, String userName,
        String documentName, XWikiContext ctx) throws Exception
    {
        setContext(ctx);

        assertFalse(this.legacyImpl.hasAccessLevel(accessLevel, userName, documentName, ctx),
            "Old implementation is is expected to differ: " + message);

        assertTrue(this.cachingImpl.hasAccessLevel(accessLevel, userName, documentName, ctx),
            "Caching implementation: " + message);
    }

    /**
     * @param message A message explaining the assertion.
     * @param accessLevel The access level to check.
     * @param userName The name of the user.
     * @param documentName The document name.
     * @param ctx The context.
     */
    protected void assertAccessLevelFalseExpectedDifference(String message, String accessLevel, String userName,
        String documentName, XWikiContext ctx) throws Exception
    {
        setContext(ctx);

        assertTrue(this.legacyImpl.hasAccessLevel(accessLevel, userName, documentName, ctx),
            "Old implementation is expected to differ: " + message);

        assertFalse(this.cachingImpl.hasAccessLevel(accessLevel, userName, documentName, ctx),
            "Caching implementation: " + message);
    }

    /**
     * @return An instance of the old implementation.
     */
    protected XWikiRightService getLegacyImpl()
    {
        return this.legacyImpl;
    }

    /**
     * @return An instance of the caching implementation.
     */
    protected XWikiRightService getCachingImpl()
    {
        return this.cachingImpl;
    }
}
