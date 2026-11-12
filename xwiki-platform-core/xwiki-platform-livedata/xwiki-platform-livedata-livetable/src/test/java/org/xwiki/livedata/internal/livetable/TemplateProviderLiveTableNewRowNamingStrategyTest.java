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
import org.xwiki.livedata.LiveDataException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.SpaceReferenceResolver;
import org.xwiki.model.validation.EntityNameValidationConfiguration;
import org.xwiki.model.validation.EntityNameValidationManager;
import org.xwiki.pagetemplate.TemplateProvider;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TemplateProviderLiveTableNewRowNamingStrategy}.
 *
 * @version $Id$
 */
@ComponentTest
class TemplateProviderLiveTableNewRowNamingStrategyTest
{
    private static final String TITLE_PROPERTY = "doc.title";

    private static final String TITLE = "My movie";

    private static final String LOCATION = "Movies";

    private static final String HOME = "Home";

    private static final String HOST_DOCUMENT = "Apps.Home";

    private static final SpaceReference PROVIDER_LOCATION = new SpaceReference("wiki", List.of("Apps", "Data"));

    private static final SpaceReference PARAMETER_LOCATION = new SpaceReference("wiki", LOCATION);

    private static final SpaceReference HOST_LOCATION = new SpaceReference("wiki", "Apps");

    private static final Map<String, Object> PARAMETERS = Map.of("newRowNamingStrategy", "templateProvider",
        "newRowLocation", LOCATION, "hostDocument", HOST_DOCUMENT);

    @InjectMockComponents
    private TemplateProviderLiveTableNewRowNamingStrategy strategy;

    @MockComponent
    private NewRowTemplateProviderResolver templateProviderResolver;

    @MockComponent
    @Named("current")
    private SpaceReferenceResolver<String> currentSpaceReferenceResolver;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @MockComponent
    private ModelBridge modelBridge;

    @MockComponent
    private EntityNameValidationManager entityNameValidationManager;

    @MockComponent
    private EntityNameValidationConfiguration entityNameValidationConfiguration;

    @MockComponent
    private EntityReferenceProvider entityReferenceProvider;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private TemplateProvider templateProvider;

    @BeforeEach
    void beforeEach() throws Exception
    {
        when(this.currentSpaceReferenceResolver.resolve(LOCATION)).thenReturn(PARAMETER_LOCATION);
        // The wiki can be configured with a default page name other than WebHome.
        when(this.entityReferenceProvider.getDefaultReference(EntityType.DOCUMENT))
            .thenReturn(new EntityReference(HOME, EntityType.DOCUMENT));
        when(this.currentDocumentReferenceResolver.resolve(HOST_DOCUMENT))
            .thenReturn(new DocumentReference("Home", HOST_LOCATION));

        this.templateProvider = new TemplateProvider(new DocumentReference("wiki", "Apps", "MovieTemplateProvider"));
        this.templateProvider.setCreationRestrictions(List.of(PROVIDER_LOCATION));
        when(this.templateProviderResolver.resolve(PARAMETERS)).thenReturn(Optional.of(this.templateProvider));
    }

    @Test
    void generateWhereTheProviderSays() throws Exception
    {
        this.templateProvider.setTerminal(true);

        assertEquals(new DocumentReference(TITLE, PROVIDER_LOCATION),
            this.strategy.generate(PARAMETERS, Map.of(TITLE_PROPERTY, TITLE)));
    }

    @Test
    void generateNonTerminalPage() throws Exception
    {
        assertEquals(new DocumentReference(HOME, new SpaceReference(TITLE, PROVIDER_LOCATION)),
            this.strategy.generate(PARAMETERS, Map.of(TITLE_PROPERTY, TITLE)));
    }

    @Test
    void generateSuffixesTheNameOfAnExistingNonTerminalPage() throws Exception
    {
        when(this.modelBridge.exists(new DocumentReference(HOME, new SpaceReference(TITLE, PROVIDER_LOCATION))))
            .thenReturn(true);

        assertEquals(new DocumentReference(HOME, new SpaceReference(TITLE + "-1", PROVIDER_LOCATION)),
            this.strategy.generate(PARAMETERS, Map.of(TITLE_PROPERTY, TITLE)));
    }

    @Test
    void generateTerminalPageSuffixesANameTakenByANestedPage() throws Exception
    {
        this.templateProvider.setTerminal(true);
        when(this.modelBridge.exists(new DocumentReference(HOME, new SpaceReference(TITLE, PROVIDER_LOCATION))))
            .thenReturn(true);

        assertEquals(new DocumentReference(TITLE + "-1", PROVIDER_LOCATION),
            this.strategy.generate(PARAMETERS, Map.of(TITLE_PROPERTY, TITLE)));
    }

    @Test
    void generateSuffixesANameTakenByATerminalPage() throws Exception
    {
        when(this.modelBridge.exists(new DocumentReference(TITLE, PROVIDER_LOCATION))).thenReturn(true);

        assertEquals(new DocumentReference(HOME, new SpaceReference(TITLE + "-1", PROVIDER_LOCATION)),
            this.strategy.generate(PARAMETERS, Map.of(TITLE_PROPERTY, TITLE)));
    }

    @Test
    void generateWithoutProvider() throws Exception
    {
        when(this.templateProviderResolver.resolve(PARAMETERS)).thenReturn(Optional.empty());

        assertEquals(new DocumentReference(TITLE, PARAMETER_LOCATION),
            this.strategy.generate(PARAMETERS, Map.of(TITLE_PROPERTY, TITLE)));
    }

    @Test
    void generateWithoutLocation() throws Exception
    {
        Map<String, Object> parameters = Map.of("newRowNamingStrategy", "templateProvider");
        when(this.templateProviderResolver.resolve(parameters)).thenReturn(Optional.empty());

        LiveDataException exception = assertThrows(LiveDataException.class,
            () -> this.strategy.generate(parameters, Map.of(TITLE_PROPERTY, TITLE)));
        assertEquals("Missing location for row creation.", exception.getMessage());
    }

    @Test
    void getNewEntryLocation()
    {
        assertEquals(Optional.of(PROVIDER_LOCATION), this.strategy.getNewEntryLocation(PARAMETERS));
    }

    @Test
    void getNewEntryLocationWhenTheProviderAllowsAnyLocation()
    {
        this.templateProvider.setCreationRestrictions(List.of());

        assertEquals(Optional.of(PARAMETER_LOCATION), this.strategy.getNewEntryLocation(PARAMETERS));
    }

    @Test
    void getNewEntryLocationNextToTheHostDocument() throws Exception
    {
        Map<String, Object> parameters = Map.of("hostDocument", HOST_DOCUMENT);
        when(this.templateProviderResolver.resolve(parameters)).thenReturn(Optional.empty());

        assertEquals(Optional.of(HOST_LOCATION), this.strategy.getNewEntryLocation(parameters));
    }

    @Test
    void getNewEntryLocationWhenTheProviderFailsToLoad() throws Exception
    {
        when(this.templateProviderResolver.resolve(PARAMETERS)).thenThrow(new LiveDataException("Error"));

        assertEquals(Optional.empty(), this.strategy.getNewEntryLocation(PARAMETERS));
        assertEquals("Failed to find where new entries are created: [LiveDataException: Error]",
            this.logCapture.getMessage(0));
    }

    @Test
    void isCreationAllowed()
    {
        when(this.authorization.hasAccess(Right.EDIT, PROVIDER_LOCATION)).thenReturn(true);

        assertTrue(this.strategy.isCreationAllowed(PARAMETERS));
    }

    @Test
    void isCreationAllowedWithoutEditRight()
    {
        assertFalse(this.strategy.isCreationAllowed(PARAMETERS));
    }

    @Test
    void isCreationAllowedWithoutLocation() throws Exception
    {
        Map<String, Object> parameters = Map.of();
        when(this.templateProviderResolver.resolve(parameters)).thenReturn(Optional.empty());

        assertFalse(this.strategy.isCreationAllowed(parameters));
    }

    @Test
    void isCreationAllowedWhenTheProviderFailsToLoad() throws Exception
    {
        when(this.templateProviderResolver.resolve(PARAMETERS)).thenThrow(new LiveDataException("Error"));

        assertFalse(this.strategy.isCreationAllowed(PARAMETERS));
        assertEquals("Adding entries is disabled because the template provider failed to load: "
            + "[LiveDataException: Error]", this.logCapture.getMessage(0));
    }
}
