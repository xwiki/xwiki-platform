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

import org.jmock.Mock;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.environment.Environment;
import org.xwiki.environment.internal.ServletEnvironment;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.CoreConfiguration;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.util.XWikiStubContextProvider;
import com.xpn.xwiki.web.Utils;

/**
 * Extension of {@link AbstractXWikiComponentTestCase} that sets up a bridge between the new Execution
 * Context and the old XWikiContext. This allows code that uses XWikiContext to be tested using this Test Case class.
 * 
 * @version $Id$
 * @since 1.6M1
 *
 * @deprecated use JUnit 4.x and {@link com.xpn.xwiki.test.AbstractBridgedComponentTestCase}
 */
@Deprecated
public abstract class AbstractBridgedXWikiComponentTestCase extends AbstractXWikiComponentTestCase
{
    private XWikiContext context;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        // Statically store the component manager in {@link Utils} to be able to access it without
        // the context.
        Utils.setComponentManager(getComponentManager());
        
        this.context = new XWikiContext();

        this.context.setDatabase("xwiki");
        this.context.setMainXWiki("xwiki");

        // We need to initialize the Component Manager so that the components can be looked up
        getContext().put(ComponentManager.class.getName(), getComponentManager());

        // Bridge with old XWiki Context, required for old code.
        Execution execution = getComponentManager().getInstance(Execution.class);
        execution.getContext().setProperty("xwikicontext", this.context);
        XWikiStubContextProvider stubContextProvider =
            getComponentManager().getInstance(XWikiStubContextProvider.class);
        stubContextProvider.initialize(this.context);

        // Since the oldcore module draws the Servlet Environment in its dependencies we need to ensure it's set up
        // correctly with a Servlet Context.
        ServletEnvironment environment = (ServletEnvironment) getComponentManager().getInstance(Environment.class);
        Mock mockServletContext = mock(ServletContext.class); 
        environment.setServletContext((ServletContext) mockServletContext.proxy());
        mockServletContext.stubs().method("getResourceAsStream").with(eq("/WEB-INF/cache/infinispan/config.xml"))
            .will(returnValue(null));
        mockServletContext.stubs().method("getAttribute").with(eq("javax.servlet.context.tempdir"))
            .will(returnValue(new File(System.getProperty("java.io.tmpdir"))));

        Mock mockCoreConfiguration = registerMockComponent(CoreConfiguration.class);
        mockCoreConfiguration.stubs().method("getDefaultDocumentSyntax").will(returnValue(Syntax.XWIKI_1_0));
    }

    @Override
    protected void tearDown() throws Exception
    {
        Utils.setComponentManager(null);
        super.tearDown();
    }

    public XWikiContext getContext()
    {
        return this.context;
    }
}
