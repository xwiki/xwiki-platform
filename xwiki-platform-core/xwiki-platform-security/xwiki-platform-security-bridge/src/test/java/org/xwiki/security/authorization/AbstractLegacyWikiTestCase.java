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

import org.xwiki.test.AbstractMockingComponentTestCase;

import com.xpn.xwiki.user.impl.xwiki.XWikiRightServiceImpl;
import org.xwiki.security.authorization.internal.XWikiCachingRightService;
import org.xwiki.security.authorization.testwikibuilding.LegacyTestWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.web.Utils;
import org.xwiki.test.annotation.MockingRequirement;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.Execution;


import org.junit.Before;
import junit.framework.Assert;

/**
 * Abstract class that should be inherited when writing tests for comparing the result of different right service
 * implementations.
 * 
 * @since 4.2 
 * @version $Id$ 
 */
public abstract class AbstractLegacyWikiTestCase extends AbstractMockingComponentTestCase {

    /** Instance of the original implementation for reference. */
    private XWikiRightServiceImpl legacyImpl;

    /** The instance that is beeing tested. */
    private XWikiCachingRightService cachingImpl;

    /** An execution context. */
    private ExecutionContext executionContext;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        executionContext = new ExecutionContext();
        Execution execution = getComponentManager().getInstance(Execution.class);
        execution.setContext(executionContext);
        Utils.setComponentManager(getComponentManager());
        legacyImpl = new XWikiRightServiceImpl();
        cachingImpl = new XWikiCachingRightService();
    }

    protected void setContext(XWikiContext ctx) {
        executionContext.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, ctx);
    }

    /**
     * @param filename The filename of the wiki description file.
     * @return A mocked wiki instance that can be used by both the old, and the caching right service implementation.
     */
    protected LegacyTestWiki newTestWiki(String filename) throws Exception
    {
        return new LegacyTestWiki(getMockery(), getComponentManager(), filename);
    }

    /**
     * @param message A message explaining the assertion.
     * @param accessLevel The access level to check.
     * @param userName The name of the user.
     * @param documentName The document name.
     * @param ctx The context.
     */
    protected void assertAccessLevelTrue(String message,
                                         String accessLevel,
                                         String userName,
                                         String documentName,
                                         XWikiContext ctx)
        throws Exception
    {
        setContext(ctx);

        Assert.assertTrue("Old implementation: " + message,
                          legacyImpl.hasAccessLevel(accessLevel, userName, documentName, ctx));

        Assert.assertTrue("Caching implementation: " + message,
                          cachingImpl.hasAccessLevel(accessLevel, userName, documentName, ctx));
    }

    /**
     * @param message A message explaining the assertion.
     * @param accessLevel The access level to check.
     * @param userName The name of the user.
     * @param documentName The document name.
     * @param ctx The context.
     */
    protected void assertAccessLevelFalse(String message,
                                          String accessLevel,
                                          String userName,
                                          String documentName,
                                          XWikiContext ctx)
        throws Exception
    {
        setContext(ctx);

        Assert.assertFalse("Old implementation: " + message,
                           legacyImpl.hasAccessLevel(accessLevel, userName, documentName, ctx));

        Assert.assertFalse("Caching implementation: " + message,
                           cachingImpl.hasAccessLevel(accessLevel, userName, documentName, ctx));
    }

    /**
     * @param message A message explaining the assertion.
     * @param accessLevel The access level to check.
     * @param userName The name of the user.
     * @param documentName The document name.
     * @param ctx The context.
     */
    protected void assertAccessLevelTrueExpectedDifference(String message,
                                         String accessLevel,
                                         String userName,
                                         String documentName,
                                         XWikiContext ctx)
        throws Exception
    {
        setContext(ctx);

        Assert.assertFalse("Old implementation is is expected to differ: " + message,
                          legacyImpl.hasAccessLevel(accessLevel, userName, documentName, ctx));

        Assert.assertTrue("Caching implementation: " + message,
                          cachingImpl.hasAccessLevel(accessLevel, userName, documentName, ctx));
    }

    /**
     * @param message A message explaining the assertion.
     * @param accessLevel The access level to check.
     * @param userName The name of the user.
     * @param documentName The document name.
     * @param ctx The context.
     */
    protected void assertAccessLevelFalseExpectedDifference(String message,
                                          String accessLevel,
                                          String userName,
                                          String documentName,
                                          XWikiContext ctx)
        throws Exception
    {
        setContext(ctx);

        Assert.assertTrue("Old implementation is expected to differ: " + message,
                           legacyImpl.hasAccessLevel(accessLevel, userName, documentName, ctx));

        Assert.assertFalse("Caching implementation: " + message,
                           cachingImpl.hasAccessLevel(accessLevel, userName, documentName, ctx));
    }

    /** @return An instance of the old implementation. */
    protected XWikiRightService getLegacyImpl() {
        return legacyImpl;
    }

    /** @return An instance of the caching implementation. */
    protected XWikiRightService getCachingImpl() {
        return cachingImpl;
    }

}
