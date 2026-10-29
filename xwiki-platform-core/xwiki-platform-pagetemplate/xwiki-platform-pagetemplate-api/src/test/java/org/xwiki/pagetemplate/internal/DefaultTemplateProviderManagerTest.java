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
package org.xwiki.pagetemplate.internal;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Named;
import jakarta.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.SpaceReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.pagetemplate.TemplateProvider;
import org.xwiki.pagetemplate.TemplateProviderException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultTemplateProviderManager}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultTemplateProviderManagerTest
{
    private static final DocumentReference PROVIDER_REFERENCE =
        new DocumentReference("wiki", List.of("Movies", "Code"), "MoviesTemplateProvider");

    private static final DocumentReference TEMPLATE_REFERENCE =
        new DocumentReference("wiki", List.of("Movies", "Code"), "MoviesTemplate");

    private static final SpaceReference MOVIES_SPACE = new SpaceReference("Movies", new WikiReference("wiki"));

    private static final SpaceReference SANDBOX_SPACE = new SpaceReference("Sandbox", new WikiReference("wiki"));

    @InjectMockComponents
    private DefaultTemplateProviderManager manager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    @Named("current")
    private SpaceReferenceResolver<String> spaceReferenceResolver;

    @Mock
    private XWikiContext xcontext;

    @Mock
    private XWiki xwiki;

    @Mock
    private XWikiDocument document;

    @Mock
    private BaseObject object;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.contextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);
        when(this.xwiki.getDocument(PROVIDER_REFERENCE, this.xcontext)).thenReturn(this.document);
        when(this.object.getStringValue(anyString())).thenReturn("");
        when(this.object.getIntValue(anyString(), anyInt()))
            .thenAnswer(invocation -> invocation.getArgument(1));
        when(this.object.getListValue(anyString())).thenReturn(List.of());
        when(this.documentReferenceResolver.resolve("Movies.Code.MoviesTemplate", PROVIDER_REFERENCE))
            .thenReturn(TEMPLATE_REFERENCE);
        when(this.spaceReferenceResolver.resolve("Movies", PROVIDER_REFERENCE)).thenReturn(MOVIES_SPACE);
        when(this.spaceReferenceResolver.resolve("Sandbox", PROVIDER_REFERENCE)).thenReturn(SANDBOX_SPACE);
    }

    @Test
    void getTemplateProviderWithoutObject() throws Exception
    {
        assertEquals(Optional.empty(), this.manager.getTemplateProvider(PROVIDER_REFERENCE));
    }

    @Test
    void getTemplateProvider() throws Exception
    {
        when(this.document.getXObject(TemplateProviderClassDocumentInitializer.CLASS_REFERENCE))
            .thenReturn(this.object);
        when(this.object.getStringValue("name")).thenReturn("Movie");
        when(this.object.getStringValue("description")).thenReturn("A movie");
        when(this.object.getStringValue("icon")).thenReturn("film");
        when(this.object.getStringValue("action")).thenReturn("saveandedit");
        when(this.object.getStringValue("template")).thenReturn("Movies.Code.MoviesTemplate");
        when(this.object.getIntValue("terminal", -1)).thenReturn(0);
        when(this.object.getListValue("visibilityRestrictions")).thenReturn(List.of("Sandbox"));
        when(this.object.getListValue("creationRestrictions")).thenReturn(List.of("Movies", ""));
        when(this.object.getIntValue("creationRestrictionsAreSuggestions", 0)).thenReturn(1);

        TemplateProvider templateProvider = this.manager.getTemplateProvider(PROVIDER_REFERENCE).orElseThrow();

        assertEquals(PROVIDER_REFERENCE, templateProvider.getReference());
        assertEquals("Movie", templateProvider.getName());
        assertEquals("A movie", templateProvider.getDescription());
        assertEquals("film", templateProvider.getIcon());
        assertEquals("saveandedit", templateProvider.getAction());
        assertEquals(Optional.of(TEMPLATE_REFERENCE), templateProvider.getTemplateReference());
        assertFalse(templateProvider.isTerminal());
        assertEquals(List.of(SANDBOX_SPACE), templateProvider.getVisibilityRestrictions());
        assertEquals(List.of(MOVIES_SPACE), templateProvider.getCreationRestrictions());
        assertTrue(templateProvider.areCreationRestrictionsSuggestions());
    }

    @Test
    void getTemplateProviderWithDefaults() throws Exception
    {
        when(this.document.getXObject(TemplateProviderClassDocumentInitializer.CLASS_REFERENCE))
            .thenReturn(this.object);

        TemplateProvider templateProvider = this.manager.getTemplateProvider(PROVIDER_REFERENCE).orElseThrow();

        assertNull(templateProvider.getName());
        assertNull(templateProvider.getAction());
        assertEquals(Optional.empty(), templateProvider.getTemplateReference());
        assertTrue(templateProvider.isTerminal());
        assertEquals(List.of(), templateProvider.getVisibilityRestrictions());
        assertEquals(List.of(), templateProvider.getCreationRestrictions());
        assertFalse(templateProvider.areCreationRestrictionsSuggestions());
    }

    @Test
    void getTemplateProviderWithLegacyProperties() throws Exception
    {
        when(this.document.getXObject(TemplateProviderClassDocumentInitializer.CLASS_REFERENCE))
            .thenReturn(this.object);
        when(this.object.getStringValue("type")).thenReturn("space");
        when(this.object.getListValue("spaces")).thenReturn(List.of("Movies"));

        TemplateProvider templateProvider = this.manager.getTemplateProvider(PROVIDER_REFERENCE).orElseThrow();

        assertFalse(templateProvider.isTerminal());
        assertEquals(List.of(MOVIES_SPACE), templateProvider.getVisibilityRestrictions());
        assertEquals(List.of(MOVIES_SPACE), templateProvider.getCreationRestrictions());
    }

    @Test
    void getTemplateProviderWhenLoadingFails() throws Exception
    {
        XWikiException cause = new XWikiException();
        when(this.xwiki.getDocument(PROVIDER_REFERENCE, this.xcontext)).thenThrow(cause);

        TemplateProviderException exception =
            assertThrows(TemplateProviderException.class, () -> this.manager.getTemplateProvider(PROVIDER_REFERENCE));

        assertEquals("Failed to load template provider [wiki:Movies.Code.MoviesTemplateProvider].",
            exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
