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
package com.xpn.xwiki.web;

import java.net.URL;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.resource.internal.entity.EntityResourceActionLister;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.url.filesystem.FilesystemExportContext;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.internal.model.LegacySpaceResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ExportURLFactory}.
 *
 * @version $Id$
 */
@ComponentTest
class ExportURLFactoryMockitoTest
{
    @InjectComponentManager
    private MockitoComponentManager componentManager;

    private ExportURLFactory factory;

    private XWikiContext context;

    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    private EntityReferenceSerializer<String> fsPathEntityReferenceSerializer;

    private LegacySpaceResolver legacySpaceResolver;

    private EntityReferenceResolver<String> relativeEntityReferenceResolver;

    @BeforeEach
    void setUp() throws Exception
    {
        this.context = new XWikiContext();
        this.context.setURL(new URL("http://localhost:8080/xwiki/bin/export/Main/WebHome?format=html"));
        XWikiRequest request = mock(XWikiRequest.class);
        when(request.getHeader("x-forwarded-host")).thenReturn(null);
        this.context.setRequest(request);
        XWikiResponse response = mock(XWikiResponse.class);
        when(response.encodeURL(anyString())).thenAnswer((Answer) invocationOnMock -> {
            Object[] args = invocationOnMock.getArguments();
            return args[0];
        });
        this.context.setResponse(response);

        XWiki xwiki = mock(XWiki.class);
        when(xwiki.getWebAppPath(any(XWikiContext.class))).thenReturn("/xwiki");
        when(xwiki.Param("xwiki.url.protocol", "http")).thenReturn("http");
        when(xwiki.getServletPath("xwiki", this.context)).thenReturn("/bin/");
        when(xwiki.getServerURL("xwiki", this.context)).thenReturn(new URL("http://localhost:8080"));
        this.context.setWiki(xwiki);

        // Setup component mocks required by XWikiServletURLFactory that ExportURLFactory inherits from
        this.relativeEntityReferenceResolver =
            this.componentManager.registerMockComponent(EntityReferenceResolver.TYPE_STRING, "relative");
        this.componentManager.registerMockComponent(EntityResourceActionLister.class);
        Utils.setComponentManager(this.componentManager);

        // Setup component mocks required by ExportURLFactory

        this.currentDocumentReferenceResolver =
            this.componentManager.registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "current");
        this.defaultEntityReferenceSerializer =
            this.componentManager.registerMockComponent(EntityReferenceSerializer.TYPE_STRING);
        this.fsPathEntityReferenceSerializer =
            this.componentManager.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "fspath");
        this.legacySpaceResolver = this.componentManager.registerMockComponent(LegacySpaceResolver.class);

        this.factory = new ExportURLFactory();
    }

    @Test
    void createURLWhenNotInExportedPages() throws Exception
    {
        DocumentReference pageReference = new DocumentReference("xwiki", "Main", "WebHome");
        when(this.currentDocumentReferenceResolver.resolve("Main.WebHome")).thenReturn(pageReference);
        when(this.defaultEntityReferenceSerializer.serialize(pageReference)).thenReturn("xwiki:Main.WebHome");

        when(this.legacySpaceResolver.resolve("Main")).thenReturn(Arrays.asList("Main"));
        when(this.fsPathEntityReferenceSerializer.serialize(pageReference)).thenReturn("Main/WebHome");
        DocumentReference pageReference2 = new DocumentReference("xwiki", "Main", "SomeOtherPage");
        when(this.fsPathEntityReferenceSerializer.serialize(pageReference2)).thenReturn("Main/SomeOtherPage");

        when(this.relativeEntityReferenceResolver.resolve("Main", EntityType.SPACE)).thenReturn(
            new EntityReference("Main", EntityType.SPACE));

        this.factory.init(Arrays.asList(pageReference), null, new FilesystemExportContext(), this.context);
        assertEquals(new URL("http://localhost:8080/xwiki/bin/Main/SomeOtherPage"),
            this.factory.createURL("Main", "SomeOtherPage", "view", null, null, "xwiki", this.context));
    }

    @Test
    void createURLWhenInExportedPages() throws Exception
    {
        DocumentReference pageReference = new DocumentReference("xwiki", "Main", "WebHome");
        when(this.currentDocumentReferenceResolver.resolve("Main.WebHome")).thenReturn(pageReference);
        when(this.defaultEntityReferenceSerializer.serialize(pageReference)).thenReturn("xwiki:Main.WebHome");

        when(this.legacySpaceResolver.resolve("Main")).thenReturn(Arrays.asList("Main"));
        when(this.fsPathEntityReferenceSerializer.serialize(pageReference)).thenReturn("Main/WebHome");

        when(this.relativeEntityReferenceResolver.resolve("Main", EntityType.SPACE)).thenReturn(
                new EntityReference("Main", EntityType.SPACE));

        FilesystemExportContext exportContext = new FilesystemExportContext();
        // Simulate locating the doc in pages/Main/WebHome (ie 3 levels deep)
        exportContext.setDocParentLevels(3);

        this.factory.init(Arrays.asList(pageReference), null, exportContext, this.context);
        assertEquals(new URL("file://../../../pages/Main/WebHome.html"),
            this.factory.createURL("Main", "WebHome", "view", null, null, "xwiki", this.context));
    }
}
