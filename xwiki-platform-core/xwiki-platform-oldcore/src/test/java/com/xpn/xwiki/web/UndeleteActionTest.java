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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentContent;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiRecycleBinStoreInterface;

/**
 * Unit tests for {@link UndeleteAction}.
 * 
 * @version $Id$
 */
public class UndeleteActionTest
{
    /**
     * A component manager that allows us to register mock components.
     */
    @Rule
    public MockitoComponentManagerRule mocker = new MockitoComponentManagerRule();

    /**
     * The object being tested.
     */
    private UndeleteAction undeleteAction = new UndeleteAction();

    /**
     * A mock {@link XWikiContext};
     */
    private XWikiContext context = mock(XWikiContext.class);

    @Before
    public void setUp() throws Exception
    {
        mocker.registerMockComponent(CSRFToken.class);
        Utils.setComponentManager(mocker);

        when(context.getRequest()).thenReturn(mock(XWikiRequest.class));

        XWiki xwiki = mock(XWiki.class);
        when(context.getWiki()).thenReturn(xwiki);

        XWikiDocument document = mock(XWikiDocument.class);
        when(context.getDoc()).thenReturn(document);
    }

    @Test
    public void missingCSRFToken() throws Exception
    {
        assertFalse(undeleteAction.action(context));

        CSRFToken csrfToken = mocker.getInstance(CSRFToken.class);
        verify(csrfToken).isTokenValid(null);
    }

    /**
     * @see "XWIKI-9421: Attachment version is incremented when a document is restored from recycle bin"
     */
    @Test
    public void restore() throws Exception
    {
        CSRFToken csrfToken = mocker.getInstance(CSRFToken.class);
        when(csrfToken.isTokenValid(null)).thenReturn(true);

        when(context.getDoc().isNew()).thenReturn(true);
        when(context.getWiki().hasRecycleBin(context)).thenReturn(true);

        when(context.getRequest().getParameter("id")).thenReturn("13");

        XWikiRecycleBinStoreInterface recycleBin = mock(XWikiRecycleBinStoreInterface.class);
        when(context.getWiki().getRecycleBinStore()).thenReturn(recycleBin);

        XWikiDocument restoredDocument = mock(XWikiDocument.class);
        when(recycleBin.restoreFromRecycleBin(context.getDoc(), 13, context, true)).thenReturn(restoredDocument);

        XWikiAttachment attachment = mock(XWikiAttachment.class);
        when(restoredDocument.getAttachmentList()).thenReturn(Arrays.asList(attachment));
        when(attachment.getAttachment_content()).thenReturn(mock(XWikiAttachmentContent.class));

        assertFalse(undeleteAction.action(context));

        verify(attachment).setMetaDataDirty(false);
        verify(attachment.getAttachment_content()).setContentDirty(false);
        verify(context.getWiki()).saveDocument(restoredDocument, "restored from recycle bin", context);
        verify(context.getWiki().getRecycleBinStore()).deleteFromRecycleBin(context.getDoc(), 13, context, true);
        verify(restoredDocument).saveAllAttachments(false, true, context);
    }
}
