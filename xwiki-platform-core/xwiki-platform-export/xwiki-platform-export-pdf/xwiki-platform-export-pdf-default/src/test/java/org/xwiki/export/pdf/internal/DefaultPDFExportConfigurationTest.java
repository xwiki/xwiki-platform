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
package org.xwiki.export.pdf.internal;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultPDFExportConfiguration}.
 * 
 * @version $Id$
 */
@ComponentTest
class DefaultPDFExportConfigurationTest
{
    @InjectMockComponents
    private DefaultPDFExportConfiguration config;

    @MockComponent
    @Named("xwikiproperties")
    private ConfigurationSource xwikiProperties;

    @MockComponent
    @Named("export/pdf")
    private ConfigurationSource configDocument;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Test
    void getDockerNetwork()
    {
        when(this.xwikiProperties.getProperty("export.pdf.dockerNetwork", "bridge")).thenReturn("test");
        assertEquals("test", this.config.getDockerNetwork());

        when(this.configDocument.containsKey("dockerNetwork")).thenReturn(true);
        when(this.configDocument.getProperty("dockerNetwork", "bridge")).thenReturn("dev");
        assertEquals("dev", this.config.getDockerNetwork());
    }

    @Test
    void getChromeRemoteDebuggingPort()
    {
        when(this.xwikiProperties.getProperty("export.pdf.chromeRemoteDebuggingPort", 9222)).thenReturn(9876);
        assertEquals(9876, this.config.getChromeRemoteDebuggingPort());

        when(this.configDocument.containsKey("chromeRemoteDebuggingPort")).thenReturn(true);
        when(this.configDocument.getProperty("chromeRemoteDebuggingPort", 9222)).thenReturn(6789);
        assertEquals(6789, this.config.getChromeRemoteDebuggingPort());
    }

    @Test
    void isServerSide()
    {
        when(this.xwikiProperties.getProperty("export.pdf.serverSide", false)).thenReturn(true);
        assertEquals(true, this.config.isServerSide());

        when(this.configDocument.containsKey("serverSide")).thenReturn(true);
        when(this.configDocument.getProperty("serverSide", false)).thenReturn(false);
        assertEquals(false, this.config.isServerSide());
    }

    @Test
    void getTemplates()
    {
        when(this.configDocument.getProperty("templates", List.class,
            Collections.singletonList("XWiki.PDFExport.Template")))
                .thenReturn(Arrays.asList("firstTemplate", "secondTemplate"));

        DocumentReference firstTemplateRef = new DocumentReference("test", "First", "Template");
        when(this.documentReferenceResolver.resolve("firstTemplate")).thenReturn(firstTemplateRef);

        DocumentReference secondTemplateRef = new DocumentReference("test", "Second", "Template");
        when(this.documentReferenceResolver.resolve("secondTemplate")).thenReturn(secondTemplateRef);

        assertEquals(Arrays.asList(firstTemplateRef, secondTemplateRef), this.config.getTemplates());
    }

    void getXWikiURI() throws Exception
    {
        // The old way to configure the XWiki URI was through the "xwikiHost" property. We keep supporting it for
        // backward compatibility with old XWiki instances that have this configuration set.
        when(this.xwikiProperties.getProperty("export.pdf.xwikiHost", "host.xwiki.internal")).thenReturn("xwiki-old");
        assertEquals(new URI("//xwiki-old"), this.config.getXWikiURI());

        when(this.xwikiProperties.getProperty("export.pdf.xwikiURI", "xwiki-old")).thenReturn("//xwiki-new:9293");
        assertEquals(new URI("//xwiki-new:9293"), this.config.getXWikiURI());

        when(this.configDocument.containsKey("xwikiURI")).thenReturn(true);
        when(this.configDocument.getProperty("xwikiURI", "xwiki-old")).thenReturn("https://xwiki-new:9293");
        assertEquals(new URI("https://xwiki-new:9293"), this.config.getXWikiURI());
    }

    @Test
    void isXWikiURISpecified()
    {
        assertFalse(this.config.isXWikiURISpecified());

        when(this.configDocument.containsKey("xwikiURI")).thenReturn(true);
        assertTrue(this.config.isXWikiURISpecified());

        when(this.configDocument.containsKey("xwikiURI")).thenReturn(false);
        when(this.xwikiProperties.containsKey("export.pdf.xwikiURI")).thenReturn(true);
        assertTrue(this.config.isXWikiURISpecified());

        when(this.xwikiProperties.containsKey("export.pdf.xwikiURI")).thenReturn(false);
        when(this.configDocument.containsKey("xwikiHost")).thenReturn(true);
        assertTrue(this.config.isXWikiURISpecified());

        when(this.configDocument.containsKey("xwikiHost")).thenReturn(false);
        when(this.xwikiProperties.containsKey("export.pdf.xwikiHost")).thenReturn(true);
        assertTrue(this.config.isXWikiURISpecified());

        when(this.xwikiProperties.containsKey("export.pdf.xwikiHost")).thenReturn(false);
        assertFalse(this.config.isXWikiURISpecified());
    }
}
