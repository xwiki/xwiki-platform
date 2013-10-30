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
package com.xpn.xwiki.test;

import java.io.File;

import javax.servlet.ServletContext;

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.mockito.Mockito;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.environment.Environment;
import org.xwiki.environment.internal.ServletEnvironment;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import com.xpn.xwiki.CoreConfiguration;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.util.XWikiStubContextProvider;
import com.xpn.xwiki.web.Utils;

/**
 * Test rule to initialize and manipulate various oldcore APIs.
 * 
 * @version $Id$
 * @since 5.2M1
 */
public class MockitoOldcoreRule implements MethodRule
{
    private final MethodRule parent;

    private final MockitoComponentManagerRule componentManager;

    private XWikiContext context;

    private XWiki mockXWiki;

    private XWikiStoreInterface mockStore;

    private XWikiRightService mockRightService;

    public MockitoOldcoreRule()
    {
        this(new MockitoComponentManagerRule());
    }

    public MockitoOldcoreRule(MockitoComponentManagerRule componentManager)
    {
        this(componentManager, componentManager);
    }

    public MockitoOldcoreRule(MockitoComponentManagerRule componentManager, MethodRule parent)
    {
        this.componentManager = componentManager;
        this.parent = parent;
    }

    public MockitoComponentManagerRule getMocker()
    {
        return this.componentManager;
    }

    public Statement apply(final Statement base, final FrameworkMethod method, final Object target)
    {
        final Statement statement = new Statement()
        {
            @Override
            public void evaluate() throws Throwable
            {
                before();
                try {
                    base.evaluate();
                } finally {
                    after();
                }
            }
        };

        return this.parent != null ? this.parent.apply(statement, method, target) : statement;
    }

    protected void before() throws Exception
    {
        // Statically store the component manager in {@link Utils} to be able to access it without
        // the context.
        Utils.setComponentManager(this.componentManager);

        this.context = new XWikiContext();

        this.context.setDatabase("xwiki");
        this.context.setMainXWiki("xwiki");

        this.mockXWiki = Mockito.mock(XWiki.class);
        getXWikiContext().setWiki(this.mockXWiki);
        getXWikiContext().setDatabase("xwiki");

        this.mockStore = Mockito.mock(XWikiStoreInterface.class);
        this.mockRightService = Mockito.mock(XWikiRightService.class);

        Mockito.when(mockXWiki.getStore()).thenReturn(mockStore);
        Mockito.when(mockXWiki.getRightService()).thenReturn(mockRightService);

        // We need to initialize the Component Manager so that the components can be looked up
        getXWikiContext().put(ComponentManager.class.getName(), this.componentManager);

        // Since the oldcore module draws the Servlet Environment in its dependencies we need to ensure it's set up
        // correctly with a Servlet Context.
        ServletEnvironment environment = (ServletEnvironment) this.componentManager.getInstance(Environment.class);

        ServletContext servletContextMock = Mockito.mock(ServletContext.class);
        environment.setServletContext(servletContextMock);
        Mockito.when(servletContextMock.getAttribute("javax.servlet.context.tempdir")).thenReturn(
            new File(System.getProperty("java.io.tmpdir")));

        // Initialize the Execution Context
        ExecutionContextManager ecm = this.componentManager.getInstance(ExecutionContextManager.class);
        ExecutionContext ec = new ExecutionContext();
        ecm.initialize(ec);

        // Bridge with old XWiki Context, required for old code.
        Execution execution = this.componentManager.getInstance(Execution.class);
        execution.getContext().setProperty("xwikicontext", this.context);
        XWikiStubContextProvider stubContextProvider =
            this.componentManager.getInstance(XWikiStubContextProvider.class);
        stubContextProvider.initialize(this.context);

        CoreConfiguration coreConfigurationMock = this.componentManager.registerMockComponent(CoreConfiguration.class);
        Mockito.when(coreConfigurationMock.getDefaultDocumentSyntax()).thenReturn(Syntax.XWIKI_1_0);
    }

    protected void after() throws Exception
    {
        Utils.setComponentManager(null);

        Execution execution = this.componentManager.getInstance(Execution.class);
        execution.removeContext();
    }

    public XWikiContext getXWikiContext()
    {
        return this.context;
    }

    public XWiki getMockXWiki()
    {
        return this.mockXWiki;
    }

    public XWikiRightService getMockRightService()
    {
        return this.mockRightService;
    }

    public XWikiStoreInterface getMockStore()
    {
        return this.mockStore;
    }
}
