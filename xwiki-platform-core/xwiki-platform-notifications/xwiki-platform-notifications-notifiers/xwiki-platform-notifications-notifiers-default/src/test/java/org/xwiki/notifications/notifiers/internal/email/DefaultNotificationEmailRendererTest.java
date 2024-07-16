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

package org.xwiki.notifications.notifiers.internal.email;

import java.util.Map;

import javax.inject.Named;
import javax.inject.Provider;
import javax.script.ScriptContext;

import org.junit.jupiter.api.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.transformation.MutableRenderingContext;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateContent;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.ExternalServletURLFactory;
import com.xpn.xwiki.web.XWikiServletRequestStub;
import com.xpn.xwiki.web.XWikiURLFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultEmailTemplateRenderer}.
 * @version $Id$
 * @since 16.1.0RC1
 */
@ComponentTest
class DefaultNotificationEmailRendererTest
{
    @InjectMockComponents
    private DefaultEmailTemplateRenderer emailTemplateRenderer;

    @MockComponent
    @Named("xhtml/1.0")
    private BlockRenderer htmlBlockRenderer;

    @MockComponent
    @Named("plain/1.0")
    private BlockRenderer plainTextBlockRenderer;

    @MockComponent
    private TemplateManager templateManager;

    @MockComponent
    private ScriptContextManager scriptContextManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private Execution execution;

    @MockComponent
    private ExecutionContextManager executionManager;

    private MutableRenderingContext renderingContext;

    @BeforeComponent
    void beforeComponent(MockitoComponentManager componentManager) throws Exception
    {
        this.renderingContext = mock(MutableRenderingContext.class);
        componentManager.registerComponent(RenderingContext.class, renderingContext);
    }

    @Test
    void renderHtml()
    {
        Block block = mock(Block.class);
        doAnswer(invocation -> {
            WikiPrinter printer = invocation.getArgument(1);
            printer.print("42");
            return null;
        }).when(this.htmlBlockRenderer).render(eq(block), any());
        assertEquals("42", this.emailTemplateRenderer.renderHTML(block));
    }

    @Test
    void renderPlainText()
    {
        Block block = mock(Block.class);
        doAnswer(invocation -> {
            WikiPrinter printer = invocation.getArgument(1);
            printer.print("444");
            return null;
        }).when(this.plainTextBlockRenderer).render(eq(block), any());
        assertEquals("444", this.emailTemplateRenderer.renderPlainText(block));
    }

    @Test
    void executeTemplate() throws Exception
    {
        CompositeEvent compositeEvent = mock(CompositeEvent.class);
        String userId = "fooUser";
        Template template = mock(Template.class);
        Syntax syntax = Syntax.XWIKI_2_1;
        Map<String, Object> customBindings = Map.of(
            "myKey", "aValue",
            "anotherKey", 8282
        );

        ExecutionContext currentContext = mock(ExecutionContext.class, "current");
        when(this.execution.getContext()).thenReturn(currentContext);
        ExecutionContext cloneContext = mock(ExecutionContext.class, "clone");
        when(this.executionManager.clone(currentContext)).thenReturn(cloneContext);

        XWikiContext context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(context);

        DocumentReference currentUser = mock(DocumentReference.class, "currentUser");
        when(context.getUserReference()).thenReturn(currentUser);

        XWikiURLFactory urlFactory = mock(XWikiURLFactory.class);
        when(context.getURLFactory()).thenReturn(urlFactory);

        XWiki wiki = mock(XWiki.class);
        when(context.getWiki()).thenReturn(wiki);

        XWikiServletRequestStub xWikiServletRequestStub = mock(XWikiServletRequestStub.class);
        when(context.getRequest()).thenReturn(xWikiServletRequestStub);

        ScriptContext scriptContext = mock(ScriptContext.class);
        when(this.scriptContextManager.getScriptContext()).thenReturn(scriptContext);

        TemplateContent templateContent = mock(TemplateContent.class);
        when(template.getContent()).thenReturn(templateContent);

        DocumentReference templateAuthor = mock(DocumentReference.class, "templateAuthor");
        when(templateContent.getAuthorReference()).thenReturn(templateAuthor);

        XDOM xdom = mock(XDOM.class);
        when(this.templateManager.execute(template)).thenReturn(xdom);

        assertEquals(xdom,
            this.emailTemplateRenderer.executeTemplate(compositeEvent, userId, template, syntax, customBindings));

        verify(this.execution).pushContext(cloneContext);
        verify(this.execution).popContext();
        verify(context).setUserReference(templateAuthor);
        verify(context).setUserReference(currentUser);
        verify(scriptContext).setAttribute("event", compositeEvent, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute("emailUser", userId, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute("myKey", "aValue", ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute("anotherKey", 8282, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).removeAttribute("event", ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).removeAttribute("emailUser", ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).removeAttribute("myKey", ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).removeAttribute("anotherKey", ScriptContext.ENGINE_SCOPE);
        verify(context).setURLFactory(any(ExternalServletURLFactory.class));
        verify(context).setURLFactory(urlFactory);
        verify(renderingContext).push(null, null, syntax, null, false, syntax);
        verify(renderingContext).pop();
    }
}