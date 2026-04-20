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
package com.xpn.xwiki.store;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.suigeneris.jrcs.rcs.Archive;
import com.xpn.xwiki.internal.store.hibernate.HibernateStore;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentArchive;
import com.xpn.xwiki.doc.XWikiAttachmentContent;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link XWikiHibernateAttachmentStore}.
 *
 * @version $Id$
 */
@OldcoreTest
class XWikiHibernateAttachmentStoreTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @InjectMockComponents
    private XWikiHibernateAttachmentStore store;

    private Session mockSession;

    @BeforeEach
    void setup() throws Exception
    {
        HibernateStore mockHibernateStore = this.oldcore.getMocker().getInstance(HibernateStore.class);
        this.mockSession = mock(Session.class);
        when(mockHibernateStore.getCurrentSession()).thenReturn(this.mockSession);
        when(mockHibernateStore.getSessionFactory()).thenReturn(mock(SessionFactory.class));

        Query<Long> mockQuery = mock();
        when(this.mockSession.createQuery(anyString(), eq(Long.class))).thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.uniqueResult()).thenReturn(null);
    }

    private XWikiAttachment createAttachment(XWikiAttachmentArchive archive) throws Exception
    {
        XWikiAttachment attachment = mock(XWikiAttachment.class);
        XWikiAttachmentContent content = mock(XWikiAttachmentContent.class);
        when(attachment.getAttachment_content()).thenReturn(content);
        when(attachment.getAttachment_archive()).thenReturn(archive);
        when(attachment.getReference()).thenReturn(null);
        when(attachment.toStringXML(anyBoolean(), anyBoolean(), any())).thenReturn("<attachment/>");
        when(attachment.getFilename()).thenReturn("test.txt");
        when(attachment.getVersion()).thenReturn("1.1");
        archive.setAttachment(attachment);
        return attachment;
    }

    @Test
    void saveAttachmentContentSeedsEmptyArchive() throws Exception
    {
        XWikiAttachmentArchive archive = new XWikiAttachmentArchive();
        XWikiAttachment attachment = createAttachment(archive);

        this.store.saveAttachmentContent(attachment, false, this.oldcore.getXWikiContext(), false);

        assertFalse(archive.getArchiveAsString().isEmpty());
    }

    @Test
    void saveAttachmentContentDoesNotSeedWhenArchiveAlreadyHasHistory() throws Exception
    {
        XWikiAttachmentArchive archive = new XWikiAttachmentArchive();
        archive.setRCSArchive(new Archive(new Object[] {"content"}, "test.txt", "1.2"));
        String originalArchiveContent = archive.getArchiveAsString();

        XWikiAttachment attachment = createAttachment(archive);

        this.store.saveAttachmentContent(attachment, false, this.oldcore.getXWikiContext(), false);

        assertEquals(originalArchiveContent, archive.getArchiveAsString());
    }
}
