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
package org.xwiki.rendering.internal.resolver;

import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.PageReference;
import org.xwiki.model.reference.PageReferenceResolver;
import org.xwiki.rendering.listener.reference.PageResourceReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Validate {@link PageResourceReferenceEntityReferenceResolver}.
 * 
 * @version $Id$
 */
@ComponentTest
class PageResourceReferenceEntityReferenceResolverTest
{
    @MockComponent
    @Named("current")
    protected EntityReferenceResolver<String> currentEntityReferenceResolver;

    @MockComponent
    protected EntityReferenceResolver<EntityReference> defaultEntityReferenceResolver;

    @MockComponent
    private PageReferenceResolver<String> defaultPageReferenceResolver;

    @InjectMockComponents
    private PageResourceReferenceEntityReferenceResolver resolver;

    @Test
    void resolve()
    {
        PageResourceReference page = new PageResourceReference("page1/page2");
        page.addBaseReference("wiki:space.document");

        DocumentReference baseReference = new DocumentReference("wiki", "space", "document");
        PageReference pageReference = new PageReference("wiki", "page1", "page2");
        DocumentReference documentReference = new DocumentReference("wiki", List.of("page1", "page2"), "WebHome");
        when(this.currentEntityReferenceResolver.resolve(page.getBaseReferences().get(0), EntityType.DOCUMENT, null))
            .thenReturn(baseReference);
        when(this.defaultPageReferenceResolver.resolve("page1/page2", baseReference)).thenReturn(pageReference);
        when(this.defaultEntityReferenceResolver.resolve(pageReference, EntityType.DOCUMENT))
            .thenReturn(documentReference);

        assertEquals(documentReference, this.resolver.resolve(page, EntityType.DOCUMENT));
        assertEquals(pageReference, this.resolver.resolve(page, EntityType.PAGE));
    }
}
