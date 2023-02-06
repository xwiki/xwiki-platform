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
package org.xwiki.url.internal.standard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.resource.CreateResourceTypeException;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.ResourceTypeResolver;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.url.ExtendedURL;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;

/**
 * Unit tests for {@link StandardURLStringEntityReferenceResolver}.
 * 
 * @version $Id$
 */
@ComponentTest
class StandardURLStringEntityReferenceResolverTest
{
    @InjectMockComponents
    @Named("resource/standardURL")
    private StandardURLStringEntityReferenceResolver standardReferenceResolver;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    @Named("standard")
    private ResourceReferenceResolver<ExtendedURL> resourceResolver;

    @MockComponent
    @Named("standard")
    private ResourceTypeResolver<ExtendedURL> typeResolver;

    @MockComponent
    private EntityReferenceResolver<String> defaultStringResolver;

    @Mock
    private XWikiContext xcontext;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @BeforeEach
    public void configure(MockitoComponentManager componentManager) throws Exception
    {
        XWikiRequest request = mock(XWikiRequest.class);
        when(request.getContextPath()).thenReturn("/xwiki");

        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getURL()).thenReturn(new URL("http://localhost:8080/xwiki/bin/view/Test/Page"));
        when(this.xcontext.getRequest()).thenReturn(request);
    }

    @Test
    void resolveAttachmentURL() throws Exception
    {
        String urlStringRepresentation = "http://localhost:8080/xwiki/bin/download/Path/To/Page/attachment.png";
        ExtendedURL extendedURL = new ExtendedURL(new URL(urlStringRepresentation), "/xwiki");

        ResourceType type = new ResourceType("attach");
        when(this.typeResolver.resolve(extendedURL, Collections.emptyMap())).thenReturn(type);

        DocumentReference parentReference = new DocumentReference("wiki", Arrays.asList("Path", "To"), "Page");
        AttachmentReference attachReference = new AttachmentReference("attachment.png", parentReference);
        EntityResourceReference reference =
            new EntityResourceReference(attachReference, new EntityResourceAction("download"));
        when(this.resourceResolver.resolve(extendedURL, type, Collections.emptyMap())).thenReturn(reference);

        assertEquals(attachReference,
            standardReferenceResolver.resolve(urlStringRepresentation, EntityType.ATTACHMENT));
    }

    @Test
    void resolveInvalidAttachmentURL() throws Exception
    {
        String urlStringRepresentation = "http://localhost:8080/xwiki/bin/download/Path/To/Page/attachment.png";

        DocumentReference parentReference = new DocumentReference("wiki", "Default", "Page");
        AttachmentReference attachReference =
            new AttachmentReference("http://localhost:8080/test/attachment.png", parentReference);
        when(defaultStringResolver.resolve(urlStringRepresentation, EntityType.ATTACHMENT)).thenReturn(attachReference);
        when(this.typeResolver.resolve(any(ExtendedURL.class), any(Map.class)))
            .thenThrow(new CreateResourceTypeException("error"));

        assertEquals(attachReference,
            standardReferenceResolver.resolve(urlStringRepresentation, EntityType.ATTACHMENT));
        assertEquals(1, logCapture.size());
        assertEquals(String.format(
            "Failed to extract an EntityReference from [%s]. Root cause is [CreateResourceTypeException: error].",
            urlStringRepresentation), logCapture.getMessage(0));
    }

    @Test
    void resolveDocumentURL() throws Exception
    {
        String urlStringRepresentation = "http://localhost:8080/xwiki/bin/view/Path/To/Page";
        ExtendedURL extendedURL = new ExtendedURL(new URL(urlStringRepresentation), "/xwiki");

        ResourceType type = new ResourceType("doc");
        when(this.typeResolver.resolve(extendedURL, Collections.emptyMap())).thenReturn(type);

        DocumentReference docReference = new DocumentReference("wiki", Arrays.asList("Path", "To"), "Page");
        EntityResourceReference reference = new EntityResourceReference(docReference, EntityResourceAction.VIEW);
        when(this.resourceResolver.resolve(extendedURL, type, Collections.emptyMap())).thenReturn(reference);

        assertEquals(docReference, standardReferenceResolver.resolve(urlStringRepresentation, EntityType.DOCUMENT));
    }

    @Test
    void resolveInvalidDocumentURL() throws Exception
    {
        String urlStringRepresentation = "http://localhost:8080/xwiki/bin/view/Path/To/Page";

        DocumentReference docReference = new DocumentReference("wiki", "Default", "Page");
        when(defaultStringResolver.resolve(urlStringRepresentation, EntityType.DOCUMENT)).thenReturn(docReference);
        when(this.typeResolver.resolve(any(ExtendedURL.class), any(Map.class)))
            .thenThrow(new CreateResourceTypeException("error"));

        assertEquals(docReference, standardReferenceResolver.resolve(urlStringRepresentation, EntityType.DOCUMENT));
        assertEquals(1, logCapture.size());
        assertEquals(String.format(
            "Failed to extract an EntityReference from [%s]. Root cause is [CreateResourceTypeException: error].",
            urlStringRepresentation), logCapture.getMessage(0));
    }

}
