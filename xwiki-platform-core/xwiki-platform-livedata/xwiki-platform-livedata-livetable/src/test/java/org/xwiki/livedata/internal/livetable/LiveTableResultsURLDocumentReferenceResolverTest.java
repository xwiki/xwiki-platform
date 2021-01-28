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

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.ResourceTypeResolver;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.url.ExtendedURL;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LiveTableResultsURLDocumentReferenceResolver}.
 * 
 * @version $Id$
 */
@ComponentTest
class LiveTableResultsURLDocumentReferenceResolverTest
{
    @InjectMockComponents
    private LiveTableResultsURLDocumentReferenceResolver urlDocumentReferenceResolver;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private ResourceTypeResolver<ExtendedURL> typeResolver;

    @MockComponent
    private ResourceReferenceResolver<ExtendedURL> resourceResolver;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<EntityReference> currentEntityDocumentReferenceResolver;

    @MockComponent
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    @Mock
    private XWikiContext xcontext;

    @BeforeEach
    void configure() throws Exception
    {
        XWikiRequest request = mock(XWikiRequest.class);
        when(request.getContextPath()).thenReturn("/xwiki");

        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getURL()).thenReturn(new URL("http://localhost:8080/xwiki/bin/view/Test/Page"));
        when(this.xcontext.getRequest()).thenReturn(request);
    }

    @Test
    void resolveDocumentURL() throws Exception
    {
        ExtendedURL extendedURL =
            new ExtendedURL(new URL("http://localhost:8080/xwiki/bin/view/Path/To/Page?xpage=test"), "/xwiki");

        ResourceType type = new ResourceType("doc");
        when(this.typeResolver.resolve(extendedURL, Collections.emptyMap())).thenReturn(type);

        LocalDocumentReference entityReference = new LocalDocumentReference(Arrays.asList("Path", "To"), "Page");
        EntityResourceReference reference = new EntityResourceReference(entityReference, EntityResourceAction.VIEW);
        when(this.resourceResolver.resolve(extendedURL, type, Collections.emptyMap())).thenReturn(reference);

        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("Path", "To"), "Page");
        when(this.currentEntityDocumentReferenceResolver.resolve(entityReference)).thenReturn(documentReference);

        when(this.defaultEntityReferenceSerializer.serialize(documentReference)).thenReturn("xwiki:Path.To.Page");

        assertEquals("xwiki:Path.To.Page",
            this.urlDocumentReferenceResolver.resolve("/xwiki/bin/view/Path/To/Page?xpage=test"));
    }

    @Test
    void resolveNonDocumentURL() throws Exception
    {
        assertNull(this.urlDocumentReferenceResolver.resolve("/xwiki/rest/wikis"));
    }
}
