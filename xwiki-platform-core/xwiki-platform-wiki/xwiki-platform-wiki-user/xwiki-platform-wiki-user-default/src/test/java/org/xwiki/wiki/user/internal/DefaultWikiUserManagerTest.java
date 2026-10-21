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
package org.xwiki.wiki.user.internal;

import java.util.Date;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.user.MemberCandidacy;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultWikiUserManager}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultWikiUserManagerTest
{
    private static final String WIKI_ID = "subwiki";

    @InjectMockComponents
    private DefaultWikiUserManager wikiUserManager;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private WikiUserConfigurationHelper wikiUserConfigurationHelper;

    @MockComponent
    private EntityReferenceSerializer<String> documentReferenceSerializer;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @Mock
    private XWikiContext xcontext;

    @Mock
    private XWiki xwiki;

    @Mock
    private XWikiDocument groupDocument;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);
        DocumentReference groupReference = new DocumentReference(WIKI_ID, XWiki.SYSTEM_SPACE, "XWikiAllGroup");
        when(this.xwiki.getDocument(groupReference, this.xcontext)).thenReturn(this.groupDocument);
    }

    @Test
    void getCandidacy() throws Exception
    {
        Date creationDate = new Date(1000L);
        Date closureDate = new Date(2000L);

        BaseObject object = mock(BaseObject.class);
        when(object.getNumber()).thenReturn(42);
        when(object.getStringValue(WikiCandidateMemberClassInitializer.FIELD_USER)).thenReturn("mainWiki:XWiki.U1");
        when(object.getLargeStringValue(WikiCandidateMemberClassInitializer.FIELD_USER_COMMENT))
            .thenReturn("please join");
        when(object.getStringValue(WikiCandidateMemberClassInitializer.FIELD_ADMIN)).thenReturn("subwiki:XWiki.Admin");
        when(object.getLargeStringValue(WikiCandidateMemberClassInitializer.FIELD_ADMIN_COMMENT))
            .thenReturn("welcome");
        when(object.getLargeStringValue(WikiCandidateMemberClassInitializer.FIELD_ADMIN_PRIVATE_COMMENT))
            .thenReturn("private note");
        when(object.getStringValue(WikiCandidateMemberClassInitializer.FIELD_STATUS)).thenReturn("pending");
        when(object.getStringValue(WikiCandidateMemberClassInitializer.FIELD_TYPE)).thenReturn("invitation");
        when(object.getDateValue(WikiCandidateMemberClassInitializer.FIELD_DATE_OF_CREATION)).thenReturn(creationDate);
        when(object.getDateValue(WikiCandidateMemberClassInitializer.FIELD_DATE_OF_CLOSURE)).thenReturn(closureDate);

        when(this.groupDocument.getXObject(WikiCandidateMemberClassInitializer.REFERENCE, 42)).thenReturn(object);

        MemberCandidacy candidacy = this.wikiUserManager.getCandidacy(WIKI_ID, 42);

        assertEquals(42, candidacy.getId());
        assertEquals(WIKI_ID, candidacy.getWikiId());
        assertEquals("mainWiki:XWiki.U1", candidacy.getUserId());
        assertEquals("please join", candidacy.getUserComment());
        assertEquals("subwiki:XWiki.Admin", candidacy.getAdminId());
        assertEquals("welcome", candidacy.getAdminComment());
        assertEquals("private note", candidacy.getAdminPrivateComment());
        assertEquals(MemberCandidacy.Status.PENDING, candidacy.getStatus());
        assertEquals(MemberCandidacy.CandidateType.INVITATION, candidacy.getType());
        assertSame(creationDate, candidacy.getDateOfCreation());
        // Verify that the closure date is read from the closure field and not from the creation field (XWIKI-17062).
        assertSame(closureDate, candidacy.getDateOfClosure());
    }

    /**
     * When the candidacy has been removed in the meantime (e.g. a canceled invitation) but a stale candidacy id is
     * still requested (e.g. on a form resubmission after a page refresh), {@code getCandidacy} must return
     * {@code null} instead of throwing a NullPointerException. See XWIKI-17062.
     */
    @Test
    void getCandidacyWhenItDoesNotExist() throws Exception
    {
        when(this.groupDocument.getXObject(WikiCandidateMemberClassInitializer.REFERENCE, 42)).thenReturn(null);

        assertNull(this.wikiUserManager.getCandidacy(WIKI_ID, 42));
    }
}
