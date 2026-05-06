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
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.observation.EventListener;
import org.xwiki.security.authorization.testwikibuilding.LegacyTestWiki;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.Utils;

/**
 * Abstract class that should be inherited when writing tests case based on XML based mocked wikis
 * 
 * @version $Id$
 * @since 4.2
 */
@ComponentTest
@AllComponents
public abstract class AbstractWikiTest
{
    private MockitoComponentManager componentManager;

    private ExecutionContext executionContext;

    @BeforeEach
    void setUp(MockitoComponentManager componentManager) throws Exception
    {
        this.componentManager = componentManager;

        this.executionContext = new ExecutionContext();
        Execution execution = this.componentManager.getInstance(Execution.class);
        execution.setContext(this.executionContext);
        Utils.setComponentManager(this.componentManager);

        // Get rid of annoying listeners we don't need
        this.componentManager.unregisterComponent(EventListener.class, "XObjectEventGeneratorListener");
        this.componentManager.unregisterComponent(EventListener.class, "AttachmentEventGeneratorListener");
        this.componentManager.unregisterComponent(EventListener.class, "XClassPropertyEventGeneratorListener");
        this.componentManager.unregisterComponent(EventListener.class, "refactoring.automaticRedirectCreator");
        this.componentManager.unregisterComponent(EventListener.class, "refactoring.backLinksUpdater");
        this.componentManager.unregisterComponent(EventListener.class, "refactoring.relativeLinksUpdater");
        this.componentManager.unregisterComponent(EventListener.class, "refactoring.legacyParentFieldUpdater");
        this.componentManager.unregisterComponent(EventListener.class, "XClassMigratorListener");
    }

    protected MockitoComponentManager getComponentManager()
    {
        return this.componentManager;
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
        LegacyTestWiki testWiki = new LegacyTestWiki(getComponentManager(), filename, legacymock);
        setContext(testWiki.getXWikiContext());
        return testWiki;
    }
}
