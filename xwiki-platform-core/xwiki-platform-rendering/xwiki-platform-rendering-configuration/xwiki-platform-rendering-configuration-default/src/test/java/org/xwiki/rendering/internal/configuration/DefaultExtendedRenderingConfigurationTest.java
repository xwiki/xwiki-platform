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
package org.xwiki.rendering.internal.configuration;

import java.util.Arrays;
import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxType;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.CoreConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.rendering.internal.configuration.DefaultExtendedRenderingConfiguration}.
 * 
 * @version $Id$
 * @since 8.2M1
 */
@ComponentTest
public class DefaultExtendedRenderingConfigurationTest
{
    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @InjectMockComponents
    private DefaultExtendedRenderingConfiguration defaultExtendedRenderingConfiguration;

    @BeforeComponent
    public void setup() throws Exception
    {
        Provider<ComponentManager> contextComponentManagerProvider = this.componentManager.registerMockComponent(
            new DefaultParameterizedType(null, Provider.class, ComponentManager.class), "context");
        when(contextComponentManagerProvider.get()).thenReturn(this.componentManager);
    }

    @Test
    public void getImageWidthLimit() throws Exception
    {
        ConfigurationSource source = componentManager.getInstance(ConfigurationSource.class);
        when(source.getProperty("rendering.imageWidthLimit", -1)).thenReturn(100);

        assertEquals(100, this.defaultExtendedRenderingConfiguration.getImageWidthLimit());
    }

    @Test
    public void getImageHeightLimit() throws Exception
    {
        ConfigurationSource source = componentManager.getInstance(ConfigurationSource.class);
        when(source.getProperty("rendering.imageHeightLimit", -1)).thenReturn(150);

        assertEquals(150, this.defaultExtendedRenderingConfiguration.getImageHeightLimit());
    }

    @Test
    public void isImageDimensionsIncludedInImageURL() throws Exception
    {
        ConfigurationSource source = componentManager.getInstance(ConfigurationSource.class);
        when(source.getProperty("rendering.imageDimensionsIncludedInImageURL", true)).thenReturn(false);
        assertFalse(this.defaultExtendedRenderingConfiguration.isImageDimensionsIncludedInImageURL());

        when(source.getProperty("rendering.imageDimensionsIncludedInImageURL", true)).thenReturn(true);
        assertTrue(this.defaultExtendedRenderingConfiguration.isImageDimensionsIncludedInImageURL());
    }

    @Test
    public void getConfiguredAndDisabledSyntaxesWhenNoConfigXObjectAndExistingXWikiCfgProperty() throws Exception
    {
        ConfigurationSource renderingSource = componentManager.getInstance(ConfigurationSource.class, "rendering");
        when(renderingSource.getProperty("disabledSyntaxes")).thenReturn(null);

        ConfigurationSource xwikiCfgSource = componentManager.getInstance(ConfigurationSource.class, "xwikicfg");
        when(xwikiCfgSource.getProperty("xwiki.rendering.syntaxes", List.class))
            .thenReturn(Arrays.asList("xwiki/2.0", "xwiki/2.1"));

        // Register some Syntaxes for the test
        Parser xwikiSyntax20Parser = componentManager.registerMockComponent(Parser.class, Syntax.XWIKI_2_0.toIdString());
        when(xwikiSyntax20Parser.getSyntax()).thenReturn(Syntax.XWIKI_2_0);
        Parser xwikiSyntax21Parser = componentManager.registerMockComponent(Parser.class, Syntax.XWIKI_2_1.toIdString());
        when(xwikiSyntax21Parser.getSyntax()).thenReturn(Syntax.XWIKI_2_1);
        Syntax syntax1 = new Syntax(new SyntaxType("syntax1", "Syntax 1"), "1.0");

        Parser syntax1Parser = componentManager.registerMockComponent(Parser.class, syntax1.toIdString());
        when(syntax1Parser.getSyntax()).thenReturn(syntax1);
        Syntax syntax2 = new Syntax(new SyntaxType("syntax2", "Syntax 2"), "1.0");
        Parser syntax2Parser = componentManager.registerMockComponent(Parser.class, syntax2.toIdString());
        when(syntax2Parser.getSyntax()).thenReturn(syntax2);

        List<Syntax> disabledSyntaxes = defaultExtendedRenderingConfiguration.getDisabledSyntaxes();
        assertEquals(2, disabledSyntaxes.size());
        assertTrue(disabledSyntaxes.contains(syntax1));
        assertTrue(disabledSyntaxes.contains(syntax2));

        List<Syntax> configuredSyntaxes = defaultExtendedRenderingConfiguration.getConfiguredSyntaxes();
        assertEquals(2, configuredSyntaxes.size());
        assertTrue(configuredSyntaxes.contains(Syntax.XWIKI_2_0));
        assertTrue(configuredSyntaxes.contains(Syntax.XWIKI_2_1));
    }

    @Test
    public void getConfiguredAndDisabledSyntaxesWhenNoConfigXObjectAndNoXWikiCfgProperty() throws Exception
    {
        ConfigurationSource renderingSource = componentManager.getInstance(ConfigurationSource.class, "rendering");
        when(renderingSource.getProperty("disabledSyntaxes")).thenReturn(null);

        ConfigurationSource xwikiCfgSource = componentManager.getInstance(ConfigurationSource.class, "xwikicfg");
        when(xwikiCfgSource.getProperty("xwiki.rendering.syntaxes", List.class)).thenReturn(null);

        CoreConfiguration coreConfiguration = componentManager.getInstance(CoreConfiguration.class);
        Syntax defaultSyntax = new Syntax(new SyntaxType("xwiki", "XWiki"), "2.1");
        when(coreConfiguration.getDefaultDocumentSyntax()).thenReturn(defaultSyntax);

        // Register some Syntaxes for the test

        Parser defaultSyntaxParser = componentManager.registerMockComponent(Parser.class, "xwiki/2.1");
        when(defaultSyntaxParser.getSyntax()).thenReturn(defaultSyntax);

        Parser syntax1Parser = componentManager.registerMockComponent(Parser.class, "syntax1/1.0");
        Syntax syntax1 = new Syntax(new SyntaxType("syntax1", "Syntax 1"), "1.0");
        when(syntax1Parser.getSyntax()).thenReturn(syntax1);
        Parser syntax2Parser = componentManager.registerMockComponent(Parser.class, "syntax2/1.0");
        Syntax syntax2 = new Syntax(new SyntaxType("syntax2", "Syntax 2"), "1.0");
        when(syntax2Parser.getSyntax()).thenReturn(syntax2);

        List<Syntax> disabledSyntaxes = defaultExtendedRenderingConfiguration.getDisabledSyntaxes();
        assertEquals(2, disabledSyntaxes.size());
        assertTrue(disabledSyntaxes.contains(syntax1));
        assertTrue(disabledSyntaxes.contains(syntax2));

        List<Syntax> configuredSyntaxes = defaultExtendedRenderingConfiguration.getConfiguredSyntaxes();
        assertEquals(1, configuredSyntaxes.size());
        assertTrue(configuredSyntaxes.contains(defaultSyntax));
    }

    @Test
    public void getConfiguredAndDisabledSyntaxesWhenConfigXObjectExists() throws Exception
    {
        ConfigurationSource renderingSource = componentManager.getInstance(ConfigurationSource.class, "rendering");
        when(renderingSource.getProperty("disabledSyntaxes")).thenReturn(Arrays.asList("syntax1/1.0", "syntax2/1.0"));

        // Register some Syntaxes for the test

        Parser syntax1Parser = componentManager.registerMockComponent(Parser.class, "syntax1/1.0");
        Syntax syntax1 = new Syntax(new SyntaxType("syntax1", "Syntax 1"), "1.0");
        when(syntax1Parser.getSyntax()).thenReturn(syntax1);
        Parser syntax2Parser = componentManager.registerMockComponent(Parser.class, "syntax2/1.0");
        Syntax syntax2 = new Syntax(new SyntaxType("syntax2", "Syntax 2"), "1.0");
        when(syntax2Parser.getSyntax()).thenReturn(syntax2);

        Parser xwikiSyntax20Parser = componentManager.registerMockComponent(Parser.class, "xwiki/2.0");
        Syntax xwikiSyntax20 = new Syntax(new SyntaxType("xwiki", "XWiki"), "2.0");
        when(xwikiSyntax20Parser.getSyntax()).thenReturn(xwikiSyntax20);

        List<Syntax> disabledSyntaxes = defaultExtendedRenderingConfiguration.getDisabledSyntaxes();
        assertEquals(2, disabledSyntaxes.size());
        assertTrue(disabledSyntaxes.contains(syntax1));
        assertTrue(disabledSyntaxes.contains(syntax2));

        List<Syntax> configuredSyntaxes = defaultExtendedRenderingConfiguration.getConfiguredSyntaxes();
        assertEquals(1, configuredSyntaxes.size());
        assertTrue(configuredSyntaxes.contains(xwikiSyntax20));
    }
}
