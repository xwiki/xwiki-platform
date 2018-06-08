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
package org.xwiki.test.page;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.environment.Environment;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.management.JMXBeanRegistration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.query.Query;
import org.xwiki.rendering.internal.transformation.MutableRenderingContext;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.resource.internal.entity.EntityResourceActionLister;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.cache.rendering.RenderingCache;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.MockitoOldcoreRule;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.web.XWikiServletRequestStub;
import com.xpn.xwiki.web.XWikiServletResponseStub;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests that wishes to unit test wiki page should extend this class and call {@link #renderPage(DocumentReference)}
 * to load and render a page located in the classpath.
 *
 * @version $Id$
 * @since 7.3M1
 */
@OldcoreTest
@PageComponentList
public class PageTest
{
    @InjectMockitoOldcore
    protected MockitoOldcore oldcore;

    /**
     * The stubbed request used to simulate a real Servlet Request.
     */
    protected XWikiServletRequestStub request;

    /**
     * The stubbed response used to simulate a real Servlet Response.
     */
    protected XWikiServletResponseStub response;

    /**
     * The mocked XWiki instance, provided for ease of use (can also be retrieved through {@link #oldcore}).
     */
    protected XWiki xwiki;

    /**
     * The configured XWiki Context, provided for ease of use (can also be retrieved through {@link #oldcore}).
     */
    protected XWikiContext context;

    /**
     * The Component Manager to use for getting Component instances or registering Mock Components in the test,
     * provided for ease of use (can also be retrieved through {@link #oldcore}).
     */
    @InjectComponentManager
    protected MockitoComponentManager componentManager;

    /**
     * Set up components before Components declared in {@link org.xwiki.test.annotation.ComponentList} are handled.
     *
     * @param componentManager the component manager to use to register mock components
     * @throws Exception in case of errors
     */
    @BeforeComponent
    public void setUpComponentsForPageTest(MockitoComponentManager componentManager) throws Exception
    {
        componentManager.registerMockComponent(JMXBeanRegistration.class);
        componentManager.registerMockComponent(Environment.class);
        componentManager.registerMockComponent(JobProgressManager.class);
        componentManager.registerMockComponent(RenderingCache.class);
        componentManager.registerMockComponent(EntityResourceActionLister.class);

        CacheManager cacheManager = componentManager.registerMockComponent(CacheManager.class);
        when(cacheManager.createNewCache(any(CacheConfiguration.class))).thenReturn(mock(Cache.class));
    }

    /**
     * Set up of Components after the Components declared in {@link org.xwiki.test.annotation.ComponentList} have been
     * handled but before {@link MockitoOldcoreRule#before(Class)} has been called (i.e. before it has created Mocks
     * and configured Components).
     *
     * @param componentManager the component manager
     * @throws Exception in case of errors
     */
    @AfterComponent
    public void configureComponentsBeforeOldcoreRuleForPageTest(MockitoComponentManager componentManager)
        throws Exception
    {
        // Configure the Execution Context
        ExecutionContext ec = new ExecutionContext();
        componentManager.<Execution>getInstance(Execution.class).setContext(ec);
    }

    /**
     * @param documentReference the reference of the Document to load from the ClassLoader
     * @return the loaded document
     * @throws Exception in case of errors
     */
    protected XWikiDocument loadPage(DocumentReference documentReference) throws Exception
    {
        List<String> path = new ArrayList<>();
        for (SpaceReference spaceReference : documentReference.getSpaceReferences()) {
            path.add(spaceReference.getName());
        }
        path.add(documentReference.getName() + ".xml");
        XWikiDocument document = new XWikiDocument(documentReference);
        document.fromXML(getClass().getClassLoader().getResourceAsStream(StringUtils.join(path, '/')));
        this.xwiki.saveDocument(document, "registering document", true, this.context);
        return document;
    }

    /**
     * @param reference the reference of the Document to load and render (and thus load from the Classloader)
     * @return the result of rendering the Document corresponding to the passed reference
     * @throws Exception in case of errors
     */
    protected String renderPage(DocumentReference reference) throws Exception
    {
        XWikiDocument doc = loadPage(reference);

        // Set up the current doc in the context so that $doc is bound in scripts
        context.setDoc(doc);

        return doc.getRenderedContent(this.context);
    }

    /**
     * Sets the Syntax with which the Document to test will be rendered into. If not called, the Document will be
     * rendered as XHTML.
     *
     * @param syntax the Syntax to render the Document into
     * @throws Exception in case of errors
     */
    protected void setOutputSyntax(Syntax syntax) throws Exception
    {
        MutableRenderingContext renderingContext = componentManager.getInstance(RenderingContext.class);
        renderingContext.push(renderingContext.getTransformation(), renderingContext.getXDOM(),
            renderingContext.getDefaultSyntax(), "test", renderingContext.isRestricted(), syntax);
    }

    /**
     * Configures the various Components and their mocks with default values for page tests.
     *
     * @throws Exception in case of errors
     */
    @BeforeEach
    public void setUpForPageTest() throws Exception
    {
        // Configure mocks from OldcoreRule
        context = oldcore.getXWikiContext();
        xwiki = oldcore.getSpyXWiki();

        // We need this one because some component in its init creates a query...
        when(oldcore.getQueryManager().createQuery(any(String.class), any(String.class))).thenReturn(mock(Query.class));

        // Set up a fake Request
        // Configure request so that $!request.outputSyntax" == 'plain
        // Need to be executed before ecm.initialize() so that XWikiScriptContextInitializer will initialize the
        // script context properly
        request = new XWikiServletRequestStub();
        request.setScheme("http");
        context.setRequest(request);

        response = new XWikiServletResponseStub();
        context.setResponse(response);

        ExecutionContextManager ecm = componentManager.getInstance(ExecutionContextManager.class);
        ecm.initialize(oldcore.getExecutionContext());

        // Let the user have view access to all pages
        when(oldcore.getMockRightService().hasAccessLevel(eq("view"), eq("XWiki.XWikiGuest"), any(),
            eq(context))).thenReturn(true);

        // Set up URL Factory
        URLFactorySetup.setUp(xwiki, context);

        // Set up Localization
        LocalizationSetup.setUp(componentManager);

        // Set up Skin Extensions
        SkinExtensionSetup.setUp(xwiki, context);
    }

    /**
     * Clean up after the test.
     *
     * @throws Exception in case of errors
     */
    @AfterEach
    public void tearDown() throws Exception
    {
        MutableRenderingContext renderingContext = componentManager.getInstance(RenderingContext.class);
        renderingContext.pop();
    }

    /**
     * Adds a tool to the Velocity context.
     *
     * @param name the name of the tool
     * @param tool the tool to register; can be a mock
     * @throws Exception in case of errors
     * @since 7.4M1
     */
    protected void registerVelocityTool(String name, Object tool) throws Exception
    {
        VelocityManager velocityManager = this.oldcore.getMocker().getInstance(VelocityManager.class);
        velocityManager.getVelocityContext().put(name, tool);
    }
}
