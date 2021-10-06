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

import org.junit.Before;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.observation.EventListener;
import org.xwiki.security.authorization.testwikibuilding.LegacyTestWiki;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.jmock.AbstractComponentTestCase;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.Utils;

/**
 * Abstract class that should be inherited when writing tests case based on XML based mocked wikis
 * 
 * @version $Id$
 * @since 4.2
 */
@AllComponents
public abstract class AbstractWikiTestCase extends AbstractComponentTestCase
{
    /**
     * An execution context.
     */
    private ExecutionContext executionContext;

    @Before
    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        this.executionContext = new ExecutionContext();
        Execution execution = getComponentManager().getInstance(Execution.class);
        execution.setContext(this.executionContext);
        Utils.setComponentManager(getComponentManager());
    }

    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        // Get rid of annoying listeners we don't need
        getComponentManager().unregisterComponent(EventListener.class, "XObjectEventGeneratorListener");
        getComponentManager().unregisterComponent(EventListener.class, "AttachmentEventGeneratorListener");
        getComponentManager().unregisterComponent(EventListener.class, "XClassPropertyEventGeneratorListener");
        getComponentManager().unregisterComponent(EventListener.class, "refactoring.automaticRedirectCreator");
        getComponentManager().unregisterComponent(EventListener.class, "refactoring.backLinksUpdater");
        getComponentManager().unregisterComponent(EventListener.class, "refactoring.relativeLinksUpdater");
        getComponentManager().unregisterComponent(EventListener.class, "refactoring.legacyParentFieldUpdater");
        getComponentManager().unregisterComponent(EventListener.class, "XClassMigratorListener");
    }

    protected void setContext(XWikiContext ctx)
    {
        this.executionContext.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, ctx);
    }

    /**
     * @param filename The filename of the wiki description file.
     * @return A mocked wiki instance that can be used by both the old, and the caching right service implementation.
     */
    protected LegacyTestWiki newTestWiki(String filename, boolean legacymock) throws Exception
    {
        LegacyTestWiki testWiki = new LegacyTestWiki(getMockery(), getComponentManager(), filename, legacymock);
        setContext(testWiki.getXWikiContext());
        return testWiki;
    }
}
