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
import java.util.Date;

import javax.inject.Provider;
import javax.servlet.ServletContext;

import org.jmock.Mock;
import org.jmock.core.Invocation;
import org.jmock.core.stub.CustomStub;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.context.Execution;
import org.xwiki.environment.Environment;
import org.xwiki.environment.internal.ServletEnvironment;
import org.xwiki.rendering.configuration.ExtendedRenderingConfiguration;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.CoreConfiguration;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.util.XWikiStubContextProvider;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiResponse;

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

    protected File permanentDirectory;

    protected File temporaryDirectory;

    protected Mock mockWikiDescriptorManager;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        // Statically store the component manager in {@link Utils} to be able to access it without
        // the context.
        Utils.setComponentManager(getComponentManager());

        this.context = new XWikiContext();

        this.context.setWikiId("xwiki");
        this.context.setMainXWiki("xwiki");

        // Make sure response.encodeURL() calls don't fail
        Mock xwikiResponse = mock(XWikiResponse.class);
        xwikiResponse.stubs().method("setLocale");
        xwikiResponse.stubs().method("addCookie");
        xwikiResponse.stubs().method("encodeURL").will(
            new CustomStub("Implements XWikiResponse.encodeURL")
            {
                @Override
                public Object invoke(Invocation invocation) throws Throwable
                {
                    return invocation.parameterValues.get(0);
                }
            });
        this.context.setResponse((XWikiResponse) xwikiResponse.proxy());

        // We need to initialize the Component Manager so that the components can be looked up
        getContext().put(ComponentManager.class.getName(), getComponentManager());

        // Bridge with old XWiki Context, required for old code.
        Execution execution = getComponentManager().getInstance(Execution.class);
        this.context.declareInExecutionContext(execution.getContext());
        XWikiStubContextProvider stubContextProvider =
            getComponentManager().getInstance(XWikiStubContextProvider.class);
        stubContextProvider.initialize(this.context);

        // Bridge with XWiki Context Provider, required by newer code.
        Mock mockContextProvider = mock(Provider.class);
        mockContextProvider.stubs().method("get").will(returnValue(this.context));

        DefaultComponentDescriptor<Provider<XWikiContext>> contextProviderDescriptor =
            new DefaultComponentDescriptor<Provider<XWikiContext>>();
        contextProviderDescriptor.setRoleType(new DefaultParameterizedType(null, Provider.class, XWikiContext.class));
        contextProviderDescriptor.setRoleHint("default");
        getComponentManager().registerComponent(contextProviderDescriptor,
            (Provider<XWikiContext>) mockContextProvider.proxy());

        // Since the oldcore module draws the Servlet Environment in its dependencies we need to ensure it's set up
        // correctly with a Servlet Context.
        ServletEnvironment environment = getComponentManager().getInstance(Environment.class);
        Mock mockServletContext = mock(ServletContext.class);
        environment.setServletContext((ServletContext) mockServletContext.proxy());
        mockServletContext.stubs().method("getResourceAsStream").will(returnValue(null));
        mockServletContext.stubs().method("getResource").will(returnValue(null));
        mockServletContext.stubs().method("getAttribute").with(eq("javax.servlet.context.tempdir"))
            .will(returnValue(new File(System.getProperty("java.io.tmpdir"))));

        File testDirectory = new File("target/test-" + new Date().getTime());
        this.temporaryDirectory = new File(testDirectory, "temporary-dir");
        this.permanentDirectory = new File(testDirectory, "permanent-dir");
        environment.setTemporaryDirectory(this.temporaryDirectory);
        environment.setPermanentDirectory(this.permanentDirectory);

        Mock mockCoreConfiguration = registerMockComponent(CoreConfiguration.class);
        mockCoreConfiguration.stubs().method("getDefaultDocumentSyntax").will(returnValue(Syntax.XWIKI_1_0));

        this.mockWikiDescriptorManager = registerMockComponent(WikiDescriptorManager.class);
        this.mockWikiDescriptorManager.stubs().method("getCurrentWikiId")
            .will(new CustomStub("Implements WikiDescriptorManager.getCurrentWikiId")
            {
                @Override
                public String invoke(Invocation invocation) throws Throwable
                {
                    return getContext().getWikiId();
                }
            });
        this.mockWikiDescriptorManager.stubs().method("getMainWikiId")
            .will(new CustomStub("Implements WikiDescriptorManager.getMainWikiId")
            {
                @Override
                public String invoke(Invocation invocation) throws Throwable
                {
                    return getContext().getMainXWiki();
                }
            });
        this.mockWikiDescriptorManager.stubs().method("getById")
            .will(new CustomStub("Implements WikiDescriptorManager.getById")
            {
                @Override
                public String invoke(Invocation invocation) throws Throwable
                {
                    return null;
                }
            });

        // In order not to create a cyclic dependency we have the platform-rendering-xwiki module (which contains
        // XWikiWikiModel requires for oldcore testing) not depend on platform-rendering-configuration-default. As a
        // consequence we need to provide a mock ExtendedRenderingConfiguration component as otherwise injecting
        // WikiModel would fail (since XWikiWikiModel depends on ExtendedRenderingConfiguration).
        registerMockComponent(ExtendedRenderingConfiguration.class);
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
