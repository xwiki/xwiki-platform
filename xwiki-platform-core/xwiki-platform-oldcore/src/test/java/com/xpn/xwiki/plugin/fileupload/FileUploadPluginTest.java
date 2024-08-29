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
package com.xpn.xwiki.plugin.fileupload;

import java.util.List;

import javax.servlet.http.Part;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.attachment.AttachmentAccessWrapper;
import org.xwiki.attachment.validation.AttachmentValidationException;
import org.xwiki.attachment.validation.AttachmentValidator;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiRequest;

import static ch.qos.logback.classic.Level.DEBUG;
import static com.xpn.xwiki.plugin.fileupload.FileUploadPlugin.FILE_LIST_KEY;
import static com.xpn.xwiki.web.UploadAction.FILE_FIELD_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link FileUploadPlugin}.
 *
 * @version $Id$
 * @since 14.10
 */
@ComponentTest
class FileUploadPluginTest
{
    private FileUploadPlugin fileUploadPlugin;

    @MockComponent
    private XWikiContext context;

    @MockComponent
    private AttachmentValidator attachmentValidator;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.DEBUG);

    @Mock
    private XWikiRequest request;

    @Mock
    private Part part0;

    @BeforeEach
    void setUp() throws Exception
    {
        this.fileUploadPlugin = new FileUploadPlugin("fileUploadPlugin", "", this.context);
        this.componentManager.registerComponent(ComponentManager.class, "context", this.componentManager);
        Utils.setComponentManager(this.componentManager);
        when(this.context.getRequest()).thenReturn(this.request);
        when(this.request.getParts()).thenReturn(List.of(this.part0));
        when(this.part0.getName()).thenReturn(FILE_FIELD_NAME + "_aaa");
    }

    @Test
    void loadFileList() throws Exception
    {
        this.fileUploadPlugin.loadFileList(100, 10, "/tmp", this.context);
        verify(this.attachmentValidator).validateAttachment(any(AttachmentAccessWrapper.class));
        verify(this.context).put(eq(FILE_LIST_KEY), any());
        assertEquals("Loading uploaded files", this.logCapture.getMessage(0));
        assertEquals(DEBUG, this.logCapture.getLogEvent(0).getLevel());
    }

    @Test
    void loadFileListValidationError() throws Exception
    {
        doThrow(AttachmentValidationException.class).when(this.attachmentValidator)
            .validateAttachment(any(AttachmentAccessWrapper.class));
        assertThrows(AttachmentValidationException.class, () -> this.fileUploadPlugin.loadFileList(100, 10, "/tmp",
            this.context));
        verify(this.attachmentValidator).validateAttachment(any(AttachmentAccessWrapper.class));
        verify(this.context, never()).put(eq(FILE_LIST_KEY), any());
        assertEquals("Loading uploaded files", this.logCapture.getMessage(0));
        assertEquals(DEBUG, this.logCapture.getLogEvent(0).getLevel());
    }
}
