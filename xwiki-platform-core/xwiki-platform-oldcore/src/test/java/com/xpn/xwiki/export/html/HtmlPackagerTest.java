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
package com.xpn.xwiki.export.html;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.inject.Provider;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.resource.internal.entity.EntityResourceActionLister;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.url.filesystem.FilesystemExportContext;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link HtmlPackager}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
class HtmlPackagerTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @MockComponent
    private EntityResourceActionLister entityResourceActionLister;

    @MockComponent
    private ExecutionContextManager executionContextManager;

    private FilesystemExportContext exportContext = new FilesystemExportContext();

    private ByteArrayOutputStream zipContent = new ByteArrayOutputStream();

    @BeforeEach
    void beforeEach(@TempDir File webappDir) throws Exception
    {
        XWikiContext xcontext = this.oldcore.getXWikiContext();
        xcontext.setURL(new URL("http://localhost:8080/xwiki/bin/export/Main/WebHome?format=html"));

        XWikiRequest request = mock(XWikiRequest.class);
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getContextPath()).thenReturn("/xwiki");
        when(request.getServletPath()).thenReturn("/bin/");
        xcontext.setRequest(request);

        XWikiResponse response = mock(XWikiResponse.class);
        when(response.getOutputStream()).thenReturn(new ServletOutputStream()
        {
            @Override
            public void write(int b)
            {
                zipContent.write(b);
            }

            @Override
            public boolean isReady()
            {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener)
            {
                // Not needed.
            }
        });
        xcontext.setResponse(response);

        this.oldcore.getMocker().registerComponent(
            new DefaultParameterizedType(null, Provider.class, FilesystemExportContext.class),
            (Provider<FilesystemExportContext>) () -> this.exportContext);

        // Simulate a web application containing a skin and a sensitive configuration file.
        FileUtils.write(new File(webappDir, "skins/flamingo/style.css"), "css", StandardCharsets.UTF_8);
        FileUtils.write(new File(webappDir, "WEB-INF/xwiki.cfg"), "secret", StandardCharsets.UTF_8);
        when(this.oldcore.getSpyXWiki().getEngineContext().getRealPath(anyString()))
            .then(invocation -> new File(webappDir, invocation.<String>getArgument(0)).getPath());
    }

    @Test
    void exportOnlyIncludesSkinsFromTheSkinsFolder() throws Exception
    {
        this.exportContext.addNeededSkin("flamingo");
        // Skin names trying to package a folder outside of the skins folder.
        this.exportContext.addNeededSkin("..");
        this.exportContext.addNeededSkin("flamingo/../..");
        this.exportContext.addNeededSkin("");

        new HtmlPackager().export(this.oldcore.getXWikiContext());

        assertEquals(Set.of("skins/flamingo/style.css", "index.html"), getZipEntries());
    }

    private Set<String> getZipEntries() throws Exception
    {
        Set<String> entries = new HashSet<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(this.zipContent.toByteArray()))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                entries.add(entry.getName());
            }
        }

        return entries;
    }
}
