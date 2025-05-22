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
package org.xwiki.edit.internal;

import jakarta.inject.Named;
import jakarta.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.edit.EditorConfiguration;
import org.xwiki.rendering.syntax.SyntaxContent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultEditConfiguration}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultEditConfigurationTest
{
    @InjectMockComponents
    private DefaultEditConfiguration defaultEditConfig;

    @MockComponent
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @MockComponent
    @Named("request")
    private ConfigurationSource requestConfig;

    @MockComponent
    @Named("editorBindings/all")
    private ConfigurationSource allEditorBindingsSource;

    @MockComponent
    @Named("xwikiproperties")
    private ConfigurationSource xwikiPropertiesSource;

    @BeforeEach
    void configure(MockitoComponentManager mockComponentManager)
    {
        when(this.componentManagerProvider.get()).thenReturn(mockComponentManager);
    }

    @Test
    void getDefaultEditor(MockitoComponentManager mockComponentManager) throws Exception
    {
        assertNull(this.defaultEditConfig.getDefaultEditor(SyntaxContent.class));
        assertNull(this.defaultEditConfig.getDefaultEditor(SyntaxContent.class, "wysiwyg"));

        DefaultParameterizedType syntaxContentConfigType =
            new DefaultParameterizedType(null, EditorConfiguration.class, SyntaxContent.class);
        EditorConfiguration<SyntaxContent> syntaxContentConfig =
            mockComponentManager.registerMockComponent(syntaxContentConfigType);

        assertNull(this.defaultEditConfig.getDefaultEditor(SyntaxContent.class));
        assertNull(this.defaultEditConfig.getDefaultEditor(SyntaxContent.class, "wysiwyg"));

        when(syntaxContentConfig.getDefaultEditor(null)).thenReturn("");
        when(syntaxContentConfig.getDefaultEditor("wysiwyg")).thenReturn("");

        assertNull(this.defaultEditConfig.getDefaultEditor(SyntaxContent.class));
        assertNull(this.defaultEditConfig.getDefaultEditor(SyntaxContent.class, "wysiwyg"));

        when(syntaxContentConfig.getDefaultEditor(null)).thenReturn("alice");
        when(syntaxContentConfig.getDefaultEditor("wysiwyg")).thenReturn("bob");

        assertEquals("alice", this.defaultEditConfig.getDefaultEditor(SyntaxContent.class));
        assertEquals("bob", this.defaultEditConfig.getDefaultEditor(SyntaxContent.class, "wysiwyg"));

        when(this.xwikiPropertiesSource.getProperty("edit.defaultEditor.org.xwiki.rendering.syntax.SyntaxContent",
            String.class)).thenReturn("carol");
        when(this.xwikiPropertiesSource
            .getProperty("edit.defaultEditor.org.xwiki.rendering.syntax.SyntaxContent#wysiwyg", String.class))
                .thenReturn("denis");

        assertEquals("carol", this.defaultEditConfig.getDefaultEditor(SyntaxContent.class));
        assertEquals("denis", this.defaultEditConfig.getDefaultEditor(SyntaxContent.class, "wysiwyg"));

        when(this.allEditorBindingsSource.getProperty("org.xwiki.rendering.syntax.SyntaxContent", String.class))
            .thenReturn("eve");
        when(this.allEditorBindingsSource.getProperty("org.xwiki.rendering.syntax.SyntaxContent#wysiwyg", String.class))
            .thenReturn("frank");

        assertEquals("eve", this.defaultEditConfig.getDefaultEditor(SyntaxContent.class));
        assertEquals("frank", this.defaultEditConfig.getDefaultEditor(SyntaxContent.class, "wysiwyg"));

        when(this.requestConfig.getProperty("SyntaxContent.editor", String.class)).thenReturn("grace");
        when(this.requestConfig.getProperty("SyntaxContent.wysiwyg.editor", String.class)).thenReturn("hank");

        assertEquals("grace", this.defaultEditConfig.getDefaultEditor(SyntaxContent.class));
        assertEquals("hank", this.defaultEditConfig.getDefaultEditor(SyntaxContent.class, "wysiwyg"));
    }
}
