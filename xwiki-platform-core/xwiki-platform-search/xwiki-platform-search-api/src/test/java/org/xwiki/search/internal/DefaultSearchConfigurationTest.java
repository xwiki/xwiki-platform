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
package org.xwiki.search.internal;

import java.util.List;

import jakarta.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultSearchConfiguration}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultSearchConfigurationTest
{
    @InjectMockComponents
    private DefaultSearchConfiguration configuration;

    @MockComponent
    @Named("xwikiproperties")
    private ConfigurationSource xwikiProperties;

    @MockComponent
    @Named("search")
    private ConfigurationSource configDocument;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> stringDocumentReferenceResolver;

    @MockComponent
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @BeforeEach
    void beforeEach()
    {
        when(this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT))
            .thenReturn(new EntityReference("WebHome", EntityType.DOCUMENT));
    }

    @Test
    void getEngine()
    {
        when(this.xwikiProperties.getProperty("search.engine", String.class, "database")).thenReturn("lucene");
        assertEquals("lucene", this.configuration.getEngine());

        when(this.configDocument.containsKey("engine")).thenReturn(true);
        when(this.configDocument.getProperty("engine", String.class, "database")).thenReturn("solr");
        assertEquals("solr", this.configuration.getEngine());
    }

    @Test
    void getExclusions()
    {
        when(this.xwikiProperties.getProperty("search.exclusions", List.class, List.of()))
            .thenReturn(List.of("test:Space.WebHome"));
        when(this.stringDocumentReferenceResolver.resolve("test:Space.WebHome"))
            .thenReturn(new DocumentReference("test", "Space", "WebHome"));

        assertEquals(List.of(new SpaceReference("test", "Space")), this.configuration.getExclusions());

        when(this.configDocument.containsKey("exclusions")).thenReturn(true);
        when(this.configDocument.getProperty("exclusions", List.class, List.of()))
            .thenReturn(List.of("Terminal.Page", "Nested.Page.WebHome"));
        when(this.stringDocumentReferenceResolver.resolve("Terminal.Page"))
            .thenReturn(new DocumentReference("xwiki", "Terminal", "Page"));
        when(this.stringDocumentReferenceResolver.resolve("Nested.Page.WebHome"))
            .thenReturn(new DocumentReference("xwiki", List.of("Nested", "Page"), "WebHome"));

        assertEquals(
            List.of(new DocumentReference("xwiki", "Terminal", "Page"), new SpaceReference("xwiki", "Nested", "Page")),
            this.configuration.getExclusions());
    }
}
