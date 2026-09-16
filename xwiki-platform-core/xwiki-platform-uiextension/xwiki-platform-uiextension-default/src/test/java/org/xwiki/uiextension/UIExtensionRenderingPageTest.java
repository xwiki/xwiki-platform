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
package org.xwiki.uiextension;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfigurationComponentList;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.uiextension.script.UIExtensionScriptServiceComponentList;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that the UI extensions rendered from a page or a template are executed with the requested output syntax,
 * even when the page itself is rendered in a different syntax (e.g. when it is loaded with
 * {@code ?outputSyntax=plain}, like the PDF export sheet does).
 *
 * @version $Id$
 */
@XWikiSyntax21ComponentList
@HTML50ComponentList
@RenderingScriptServiceComponentList
@DefaultRenderingConfigurationComponentList
@UIExtensionScriptServiceComponentList
class UIExtensionRenderingPageTest extends PageTest
{
    private static final DocumentReference TEST_PAGE = new DocumentReference("xwiki", "Test", "WebHome");

    private static final String EXTENSION_POINT_ID = "test.extensionPoint";

    private static final String CONTENT = "<em>uix</em>";

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @BeforeEach
    void beforeEach() throws Exception
    {
        // Register a UI extension that behaves like a template based one: the raw block it produces is tagged with
        // the target syntax found in the rendering context. See InternalTemplateManager.
        RenderingContext renderingContext = this.componentManager.getInstance(RenderingContext.class);
        registerUIExtension(() -> new RawBlock(CONTENT, renderingContext.getTargetSyntax()));

        // Render the test page in plain/1.0, to simulate a page loaded with ?outputSyntax=plain. The HTML produced by
        // the UI extension is thus asserted as literal text in the output.
        setOutputSyntax(Syntax.PLAIN_1_0);
    }

    @Test
    void renderExtensions() throws Exception
    {
        assertThat(render("$services.uix.renderExtensions('" + EXTENSION_POINT_ID + "', 'html/5.0')"),
            containsString(CONTENT));
    }

    @Test
    void renderExtensionsWithFilters() throws Exception
    {
        assertThat(
            render("$services.uix.renderExtensions('" + EXTENSION_POINT_ID + "', 'html/5.0', {'sortById': ''})"),
            containsString(CONTENT));
    }

    @Test
    void render() throws Exception
    {
        assertThat(render("""
            #foreach ($uix in $services.uix.getExtensions('%s'))
            $services.uix.render($uix, 'html/5.0')
            #end""".formatted(EXTENSION_POINT_ID)), containsString(CONTENT));
    }

    @Test
    void renderWithTheRenderingScriptServiceDropsTheContent() throws Exception
    {
        // This is how the UI extensions used to be rendered: the UI extension is executed with the target syntax
        // found in the rendering context (plain/1.0 here) and only then rendered in html/5.0, so the raw block it
        // produced is tagged plain/1.0 and is dropped by the html/5.0 renderer.
        assertThat(render("""
            #foreach ($uix in $services.uix.getExtensions('%s'))
            $services.rendering.render($uix.execute(), 'html/5.0')
            #end""".formatted(EXTENSION_POINT_ID)), not(containsString(CONTENT)));
    }

    @Test
    void renderOutputsTheReferenceWhenItFails() throws Exception
    {
        // 'html/5' is resolved to a Syntax (the syntax registry falls back to parsing the identifier) but there's no
        // renderer for it, so the rendering fails and returns null. Velocity then outputs the reference as is, which
        // makes the problem visible in the page instead of silently losing the UI extension.
        assertThat(render("$services.uix.render($services.uix.getExtensions('" + EXTENSION_POINT_ID
            + "')[0], 'html/5')"), containsString("$services.uix.render("));

        assertEquals(getNoRendererWarning("html/5"), this.logCapture.getMessage(0));
    }

    @Test
    void renderDoesNotKeepThePreviousValueWhenItFails() throws Exception
    {
        // Some call sites store the result in a variable and then check whether it's blank. Make sure a failed
        // rendering doesn't leave the previous value in place, which would repeat the output of the previous
        // iteration when rendering the UI extensions of an extension point in a loop.
        assertEquals("[]", render("""
            #set ($content = 'previous')
            #set ($content = $services.uix.render($services.uix.getExtensions('%s')[0], 'html/5'))
            [$!content]""".formatted(EXTENSION_POINT_ID)).replaceAll("\\s", ""));

        assertEquals(getNoRendererWarning("html/5"), this.logCapture.getMessage(0));
    }

    private static String getNoRendererWarning(String outputSyntaxId)
    {
        return String.format("Failed to render the UI extensions in syntax [%1$s]. Root cause is "
            + "[ComponentLookupException: Can't find descriptor for the component with type "
            + "[interface org.xwiki.rendering.renderer.BlockRenderer] and hint [%1$s]]", outputSyntaxId);
    }

    private void registerUIExtension(UIExtensionContent content) throws Exception
    {
        DefaultComponentDescriptor<UIExtension> descriptor = new DefaultComponentDescriptor<>();
        descriptor.setRoleType(UIExtension.class);
        descriptor.setRoleHint("uix");
        this.componentManager.registerComponent(descriptor, new UIExtension()
        {
            @Override
            public String getId()
            {
                return "uix";
            }

            @Override
            public String getExtensionPointId()
            {
                return EXTENSION_POINT_ID;
            }

            @Override
            public Map<String, String> getParameters()
            {
                return Map.of();
            }

            @Override
            public Block execute()
            {
                return content.get();
            }
        });

        // The UI extensions are looked up from the context component manager.
        this.componentManager.registerComponent(ComponentManager.class, "context", this.componentManager);
    }

    private String render(String velocity) throws Exception
    {
        XWikiDocument document = new XWikiDocument(TEST_PAGE);
        document.setSyntax(Syntax.XWIKI_2_1);
        document.setContent("{{velocity}}\n" + velocity + "\n{{/velocity}}");
        this.xwiki.saveDocument(document, this.context);
        this.context.setDoc(document);

        return document.getRenderedContent(this.context);
    }

    @FunctionalInterface
    private interface UIExtensionContent
    {
        Block get();
    }
}
