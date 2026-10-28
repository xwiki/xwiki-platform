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
package org.xwiki.repository.internal.resources;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Provider;

import org.junit.jupiter.api.Test;
import org.xwiki.extension.internal.ExtensionFactory;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionRepository;
import org.xwiki.extension.repository.xwiki.model.jaxb.Property;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AbstractExtensionRESTResource}.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList(ExtensionFactory.class)
class AbstractExtensionRESTResourceTest
{
    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @InjectMockComponents
    private ExtensionVersionRESTResource resource;

    @BeforeComponent
    void beforeComponent()
    {
        when(this.xcontextProvider.get()).thenReturn(mock(XWikiContext.class));
    }

    @Test
    void toExtensionRepositories()
    {
        List<ExtensionRepository> repositories = this.resource.toExtensionRepositories(
            List.of("id1:xwiki:https://host1", "id2:maven;maven.release=false;maven.snapshot=true:https://host2"));

        assertEquals(2, repositories.size());

        ExtensionRepository repository1 = repositories.get(0);
        assertEquals("id1", repository1.getId());
        assertEquals("xwiki", repository1.getType());
        assertEquals("https://host1", repository1.getUri());
        assertTrue(repository1.getProperties().isEmpty());

        ExtensionRepository repository2 = repositories.get(1);
        assertEquals("id2", repository2.getId());
        assertEquals("maven", repository2.getType());
        assertEquals("https://host2", repository2.getUri());
        assertEquals(Map.of("maven.release", "false", "maven.snapshot", "true"), repository2.getProperties().stream()
            .collect(Collectors.toMap(Property::getKey, Property::getStringValue)));
    }

    @Test
    void toExtensionRepositoriesNull()
    {
        assertNull(this.resource.toExtensionRepositories(null));
    }
}
