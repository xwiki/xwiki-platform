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
package org.xwiki.livedata.internal.livetable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.internal.web.PageTemplateRequiredRightsChecker;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.pagetemplate.TemplateProvider;
import org.xwiki.pagetemplate.TemplateProviderException;
import org.xwiki.pagetemplate.TemplateProviderManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link NewRowTemplateProviderResolver}.
 *
 * @version $Id$
 */
@ComponentTest
class NewRowTemplateProviderResolverTest
{
    private static final String PROVIDER_PARAMETER = "newRowTemplateProvider";

    private static final String PROVIDER = "Apps.Code.CustomProvider";

    private static final String CLASS_NAME = "Apps.Code.MovieClass";

    private static final DocumentReference PROVIDER_REFERENCE =
        new DocumentReference("wiki", List.of("Apps", "Code"), "CustomProvider");

    private static final DocumentReference CLASS_REFERENCE =
        new DocumentReference("wiki", List.of("Apps", "Code"), "MovieClass");

    private static final DocumentReference CONVENTION_PROVIDER_REFERENCE =
        new DocumentReference("wiki", List.of("Apps", "Code"), "MovieTemplateProvider");

    private static final DocumentReference TEMPLATE_REFERENCE =
        new DocumentReference("wiki", List.of("Apps", "Code"), "MovieTemplate");

    @InjectMockComponents
    private NewRowTemplateProviderResolver resolver;

    @MockComponent
    private TemplateProviderManager templateProviderManager;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @MockComponent
    private PageTemplateRequiredRightsChecker requiredRightsChecker;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private TemplateProvider templateProvider;

    @BeforeEach
    void beforeEach() throws Exception
    {
        when(this.currentDocumentReferenceResolver.resolve(PROVIDER)).thenReturn(PROVIDER_REFERENCE);
        when(this.currentDocumentReferenceResolver.resolve(CLASS_NAME)).thenReturn(CLASS_REFERENCE);
        when(this.authorization.hasAccess(Right.VIEW, PROVIDER_REFERENCE)).thenReturn(true);
        when(this.authorization.hasAccess(Right.VIEW, CONVENTION_PROVIDER_REFERENCE)).thenReturn(true);
        when(this.authorization.hasAccess(Right.VIEW, TEMPLATE_REFERENCE)).thenReturn(true);

        this.templateProvider = new TemplateProvider(PROVIDER_REFERENCE);
        this.templateProvider.setTemplateReference(TEMPLATE_REFERENCE);
        when(this.templateProviderManager.getTemplateProvider(PROVIDER_REFERENCE))
            .thenReturn(Optional.of(this.templateProvider));
        when(this.templateProviderManager.getTemplateProvider(CONVENTION_PROVIDER_REFERENCE))
            .thenReturn(Optional.of(this.templateProvider));
    }

    @Test
    void resolveExplicitProvider() throws Exception
    {
        assertSame(this.templateProvider,
            this.resolver.resolve(Map.of(PROVIDER_PARAMETER, PROVIDER, "className", CLASS_NAME)).orElseThrow());
    }

    @Test
    void resolveProviderNamedAfterTheClass() throws Exception
    {
        assertSame(this.templateProvider, this.resolver.resolve(Map.of("className", CLASS_NAME)).orElseThrow());
        verify(this.templateProviderManager).getTemplateProvider(CONVENTION_PROVIDER_REFERENCE);
    }

    @Test
    void resolveWhateverTheNamingStrategy() throws Exception
    {
        // The template of the new entries doesn't depend on how their pages are named.
        assertSame(this.templateProvider, this.resolver.resolve(
            Map.of("newRowNamingStrategy", "uuid", "className", CLASS_NAME)).orElseThrow());
    }

    @Test
    void resolveWhenDisabled() throws Exception
    {
        when(this.currentDocumentReferenceResolver.resolve("XWiki.NoTemplateProvider"))
            .thenReturn(new DocumentReference("wiki", "XWiki", "NoTemplateProvider"));

        assertEquals(Optional.empty(), this.resolver.resolve(
            Map.of(PROVIDER_PARAMETER, "XWiki.NoTemplateProvider", "className", CLASS_NAME)));
        verify(this.templateProviderManager, never()).getTemplateProvider(any());
    }

    @Test
    void resolveWithoutClassName() throws Exception
    {
        assertEquals(Optional.empty(), this.resolver.resolve(Map.of()));
        verify(this.templateProviderManager, never()).getTemplateProvider(any());
    }

    @Test
    void resolveWithoutViewRight() throws Exception
    {
        when(this.authorization.hasAccess(Right.VIEW, PROVIDER_REFERENCE)).thenReturn(false);

        assertEquals(Optional.empty(), this.resolver.resolve(Map.of(PROVIDER_PARAMETER, PROVIDER)));
        verify(this.templateProviderManager, never()).getTemplateProvider(any());
    }

    @Test
    void resolveExplicitPageThatIsNotAProvider() throws Exception
    {
        when(this.templateProviderManager.getTemplateProvider(PROVIDER_REFERENCE)).thenReturn(Optional.empty());

        assertEquals(Optional.empty(), this.resolver.resolve(Map.of(PROVIDER_PARAMETER, PROVIDER)));
        assertEquals("New entries are created without template because [wiki:Apps.Code.CustomProvider] is not a "
            + "template provider.", this.logCapture.getMessage(0));
    }

    @Test
    void resolveMissingProviderNamedAfterTheClass() throws Exception
    {
        when(this.templateProviderManager.getTemplateProvider(CONVENTION_PROVIDER_REFERENCE))
            .thenReturn(Optional.empty());

        // An application without template provider is common, so it is not worth a warning.
        assertEquals(Optional.empty(), this.resolver.resolve(Map.of("className", CLASS_NAME)));
    }

    @Test
    void resolveWhenLoadingFails() throws Exception
    {
        when(this.templateProviderManager.getTemplateProvider(PROVIDER_REFERENCE))
            .thenThrow(new TemplateProviderException("Error", null));

        LiveDataException exception = assertThrows(LiveDataException.class,
            () -> this.resolver.resolve(Map.of(PROVIDER_PARAMETER, PROVIDER)));
        assertEquals("Failed to load the template provider [wiki:Apps.Code.CustomProvider].",
            exception.getMessage());
    }

    @Test
    void getTemplate() throws Exception
    {
        assertEquals(Optional.of(TEMPLATE_REFERENCE), this.resolver.getTemplate(Map.of(PROVIDER_PARAMETER, PROVIDER)));
    }

    @Test
    void getTemplateWithoutViewRightOnTheTemplate() throws Exception
    {
        when(this.authorization.hasAccess(Right.VIEW, TEMPLATE_REFERENCE)).thenReturn(false);

        assertEquals(Optional.empty(), this.resolver.getTemplate(Map.of(PROVIDER_PARAMETER, PROVIDER)));
    }

    @Test
    void hasRequiredRights() throws Exception
    {
        SpaceReference target = new SpaceReference("wiki", "Movies");
        when(this.requiredRightsChecker.hasRequiredRights(TEMPLATE_REFERENCE, target)).thenReturn(true);

        assertTrue(this.resolver.hasRequiredRights(
            Map.of(PROVIDER_PARAMETER, PROVIDER), target));
    }

    @Test
    void hasRequiredRightsWhenMissing() throws Exception
    {
        SpaceReference target = new SpaceReference("wiki", "Movies");

        assertFalse(this.resolver.hasRequiredRights(Map.of(PROVIDER_PARAMETER, PROVIDER), target));
    }

    @Test
    void hasRequiredRightsWithoutTemplate() throws Exception
    {
        assertTrue(this.resolver.hasRequiredRights(Map.of(), new SpaceReference("wiki", "Movies")));
    }

    @Test
    void getTemplateWhenTheProviderHasNone() throws Exception
    {
        when(this.templateProviderManager.getTemplateProvider(PROVIDER_REFERENCE))
            .thenReturn(Optional.of(new TemplateProvider(PROVIDER_REFERENCE)));

        assertEquals(Optional.empty(), this.resolver.getTemplate(Map.of(PROVIDER_PARAMETER, PROVIDER)));
    }
}
