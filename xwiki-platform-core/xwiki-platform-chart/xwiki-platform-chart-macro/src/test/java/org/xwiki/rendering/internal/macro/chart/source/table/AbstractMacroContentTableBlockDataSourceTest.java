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
package org.xwiki.rendering.internal.macro.chart.source.table;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;

import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.internal.DefaultModelConfiguration;
import org.xwiki.model.internal.reference.DefaultEntityReferenceProvider;
import org.xwiki.model.internal.reference.RelativeStringEntityReferenceResolver;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.listener.ListenerRegistry;
import org.xwiki.rendering.internal.parser.reference.DefaultUntypedLinkReferenceParser;
import org.xwiki.rendering.internal.parser.reference.type.AttachmentResourceReferenceTypeParser;
import org.xwiki.rendering.internal.parser.reference.type.DocumentResourceReferenceTypeParser;
import org.xwiki.rendering.internal.parser.reference.type.SpaceResourceReferenceTypeParser;
import org.xwiki.rendering.internal.parser.reference.type.URLResourceReferenceTypeParser;
import org.xwiki.rendering.internal.parser.wikimodel.WikiModelParserListenerBuilder;
import org.xwiki.rendering.internal.parser.xwiki20.XWiki20ImageReferenceParser;
import org.xwiki.rendering.internal.parser.xwiki20.XWiki20LinkReferenceParser;
import org.xwiki.rendering.internal.parser.xwiki20.XWiki20Parser;
import org.xwiki.rendering.internal.renderer.DefaultLinkLabelGenerator;
import org.xwiki.rendering.internal.renderer.plain.PlainTextBlockRenderer;
import org.xwiki.rendering.internal.renderer.plain.PlainTextRenderer;
import org.xwiki.rendering.internal.renderer.plain.PlainTextRendererFactory;
import org.xwiki.rendering.internal.syntax.DefaultSyntaxRegistry;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.mockito.Mockito.when;

/**
 * Helper to write unit tests for Table-based data sources.
 *
 * @version $Id$
 * @since 4.2M1
 */
// @formatter:off
@ComponentList({
    PlainTextBlockRenderer.class,
    PlainTextRendererFactory.class,
    XWiki20Parser.class,
    XWiki20LinkReferenceParser.class,
    DefaultUntypedLinkReferenceParser.class,
    URLResourceReferenceTypeParser.class,
    XWiki20ImageReferenceParser.class,
    PlainTextRenderer.class,
    DefaultLinkLabelGenerator.class,
    DefaultEntityReferenceProvider.class,
    DefaultModelConfiguration.class,
    DocumentResourceReferenceTypeParser.class,
    SpaceResourceReferenceTypeParser.class,
    AttachmentResourceReferenceTypeParser.class,
    RelativeStringEntityReferenceResolver.class,
    ListenerRegistry.class,
    WikiModelParserListenerBuilder.class,
    DefaultSyntaxRegistry.class
})
//@formatter:on
abstract class AbstractMacroContentTableBlockDataSourceTest
{
    @InjectMockComponents
    @Named("inline")
    protected MacroContentTableBlockDataSource source;

    @InjectComponentManager
    private ComponentManager componentManager;

    protected Map<String, String> map(String... keyValues)
    {
        Map<String, String> map = new HashMap<>();

        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }

        return map;
    }

    protected void setUpContentExpectation(String macroContent) throws Exception
    {
        DefaultComponentDescriptor<ComponentManager> componentDescriptor = new DefaultComponentDescriptor<>();
        componentDescriptor.setRoleType(ComponentManager.class);
        componentDescriptor.setRoleHint("context");
        this.componentManager.registerComponent(componentDescriptor, this.componentManager);

        // In order to make it easy to write unit tests, we allow tests to pass a string written in XWiki/2.0 syntax
        // which we then parser to generate an XDOM that we use in the expectation.
        XDOM expectedXDOM = this.componentManager.<Parser>getInstance(Parser.class,
            Syntax.XWIKI_2_0.toIdString()).parse(new StringReader(macroContent));

        MacroContentParser parser = this.componentManager.getInstance(MacroContentParser.class);
        when(parser.parse(macroContent, null, true, false)).thenReturn(expectedXDOM);
    }
}
