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

import java.util.Arrays;

import javax.inject.Named;
import javax.servlet.ServletOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.xwiki.filter.input.InputFilterStream;
import org.xwiki.filter.input.InputFilterStreamFactory;
import org.xwiki.filter.instance.input.DocumentInstanceInputProperties;
import org.xwiki.filter.output.BeanOutputFilterStream;
import org.xwiki.filter.output.BeanOutputFilterStreamFactory;
import org.xwiki.filter.output.OutputFilterStreamFactory;
import org.xwiki.filter.type.FilterStreamType;
import org.xwiki.filter.xar.output.XAROutputProperties;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.internal.export.DocumentSelectionResolver;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link com.xpn.xwiki.web.ExportAction}.
 *
 * @version $Id$
 * @since 6.3M2
 */
@OldcoreTest
public class ExportActionTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    private DocumentSelectionResolver documentSelectionResolver;

    @MockComponent
    @Named("xwiki+instance")
    InputFilterStreamFactory inputFilterStreamFactory;

    @Mock
    BeanOutputFilterStreamFactory<XAROutputProperties> xarFilterStreamFactory;

    private ExportAction action = new ExportAction();

    @Mock
    private XWikiRequest request;

    @Mock
    private XWikiResponse response;

    @SuppressWarnings("unchecked")
    @BeforeEach
    public void configure() throws Exception
    {
        this.oldcore.getXWikiContext().setRequest(this.request);

        ServletOutputStream outputStream = mock(ServletOutputStream.class);
        when(this.response.getOutputStream()).thenReturn(outputStream);
        this.oldcore.getXWikiContext().setResponse(this.response);

        // Register some mock filters so that the export does nothing.
        when(this.inputFilterStreamFactory.createInputFilterStream(anyMap())).thenReturn(mock(InputFilterStream.class));
        this.oldcore.getMocker().registerComponent(OutputFilterStreamFactory.class,
            FilterStreamType.XWIKI_XAR_CURRENT.serialize(), this.xarFilterStreamFactory);
        when(this.xarFilterStreamFactory.createOutputFilterStream(any(XAROutputProperties.class)))
            .thenReturn(mock(BeanOutputFilterStream.class));
    }

    @Test
    public void exportXARForbidden() throws Exception
    {
        assertEquals("exception", this.action.render(this.oldcore.getXWikiContext()));
        assertEquals("needadminrights", this.oldcore.getXWikiContext().get("message"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void exportPartialXAR() throws Exception
    {
        // XAR export requires administration right.
        XWikiContext context = this.oldcore.getXWikiContext();
        when(this.oldcore.getMockRightService().hasWikiAdminRights(context)).thenReturn(true);

        when(this.request.get("format")).thenReturn("xar");
        when(this.request.get("name")).thenReturn("my export");
        when(this.request.get("description")).thenReturn("my description");
        when(this.request.get("version")).thenReturn("my version");
        when(this.request.get("author")).thenReturn("my author");
        when(this.request.get("licence")).thenReturn("my license");
        when(this.request.get("backup")).thenReturn("false");
        when(this.request.get("history")).thenReturn("true");

        when(this.documentSelectionResolver.isSelectionSpecified()).thenReturn(true);

        DocumentReference page1Ref = new DocumentReference("xwiki", "Space", "Page1");
        DocumentReference page2Ref = new DocumentReference("xwiki", "Space", "Page2");
        when(this.documentSelectionResolver.getSelectedDocuments()).thenReturn(Arrays.asList(page1Ref, page2Ref));

        assertNull(this.action.render(context));

        // Verify that the parameters passed to the input stream factory are defining the correct pages.
        ArgumentCaptor<DocumentInstanceInputProperties> inputProperties =
            ArgumentCaptor.forClass(DocumentInstanceInputProperties.class);
        verify(this.inputFilterStreamFactory).createInputFilterStream(inputProperties.capture());
        assertFalse(inputProperties.getValue().isVerbose());
        assertTrue(inputProperties.getValue().isWithJRCSRevisions());
        assertFalse(inputProperties.getValue().isWithRevisions());
        assertTrue(inputProperties.getValue().getEntities().matches(page1Ref));
        assertTrue(inputProperties.getValue().getEntities().matches(page2Ref));

        ArgumentCaptor<XAROutputProperties> xarProperties = ArgumentCaptor.forClass(XAROutputProperties.class);
        verify(this.xarFilterStreamFactory).createOutputFilterStream(xarProperties.capture());
        assertEquals("my export", xarProperties.getValue().getPackageName());
        assertEquals("my description", xarProperties.getValue().getPackageDescription());
        assertEquals("my version", xarProperties.getValue().getPackageVersion());
        assertEquals("my author", xarProperties.getValue().getPackageAuthor());
        assertEquals("my license", xarProperties.getValue().getPackageLicense());
        assertFalse(xarProperties.getValue().isPackageBackupPack());
        assertTrue(xarProperties.getValue().isPreserveVersion());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void exportFullXAR() throws Exception
    {
        XWikiContext context = this.oldcore.getXWikiContext();
        WikiReference wikiReference = new WikiReference("mywiki");
        context.setWikiReference(wikiReference);

        // XAR export requires administration right.
        when(this.oldcore.getMockRightService().hasWikiAdminRights(context)).thenReturn(true);
        when(this.request.get("format")).thenReturn("xar");

        assertNull(this.action.render(context));

        // Verify that the parameters passed to the input stream factory are defining the correct pages.
        ArgumentCaptor<DocumentInstanceInputProperties> inputProperties =
            ArgumentCaptor.forClass(DocumentInstanceInputProperties.class);
        verify(this.inputFilterStreamFactory).createInputFilterStream(inputProperties.capture());
        assertTrue(inputProperties.getValue().getEntities().matches(wikiReference));
        assertFalse(inputProperties.getValue().isWithJRCSRevisions());

        ArgumentCaptor<XAROutputProperties> xarProperties = ArgumentCaptor.forClass(XAROutputProperties.class);
        verify(this.xarFilterStreamFactory).createOutputFilterStream(xarProperties.capture());
        assertEquals("backup", xarProperties.getValue().getPackageName());
    }
}
