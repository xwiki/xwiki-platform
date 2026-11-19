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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.function.FailableConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.user.MemberCandidacy;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultWikiUserManager}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
class DefaultWikiUserManagerTest
{
    private static final String WIKI_ID = "subwiki";

    private static final DocumentReference GROUP_REFERENCE =
        new DocumentReference(WIKI_ID, XWiki.SYSTEM_SPACE, "XWikiAllGroup");

    private static final DocumentReference GROUP_CLASS_REFERENCE =
        new DocumentReference(WIKI_ID, XWiki.SYSTEM_SPACE, "XWikiGroups");

    private static final String ALICE = "xwiki:XWiki.Alice";

    private static final String BOB = "xwiki:XWiki.Bob";

    private static final String CAROL = "xwiki:XWiki.Carol";

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @InjectMockComponents
    private DefaultWikiUserManager wikiUserManager;

    @MockComponent
    private WikiUserConfigurationHelper wikiUserConfigurationHelper;

    private XWikiContext xcontext;

    private XWiki xwiki;

    @BeforeEach
    void setUp() throws Exception
    {
        this.xcontext = this.oldcore.getXWikiContext();
        this.xwiki = this.oldcore.getSpyXWiki();

        WikiDescriptor descriptor = new WikiDescriptor(WIKI_ID, WIKI_ID);
        descriptor.setOwnerId("xwiki:XWiki.Owner");
        when(this.oldcore.getWikiDescriptorManager().getById(WIKI_ID)).thenReturn(descriptor);

        XWikiDocument groupClassDocument = this.xwiki.getDocument(GROUP_CLASS_REFERENCE, this.xcontext).clone();
        groupClassDocument.getXClass().addTextField("member", "Member", 30);
        this.xwiki.saveDocument(groupClassDocument, this.xcontext);

        this.oldcore.checkDocumentRevision(true);
    }

    private void saveGroup(String... members) throws Exception
    {
        XWikiDocument groupDocument = this.xwiki.getDocument(GROUP_REFERENCE, this.xcontext).clone();
        for (String member : members) {
            addMemberObject(groupDocument, member);
        }
        this.xwiki.saveDocument(groupDocument, this.xcontext);
    }

    private List<String> getStoredGroupMembers() throws XWikiException
    {
        return this.xwiki.getDocument(GROUP_REFERENCE, this.xcontext)
            .getXObjects(GROUP_CLASS_REFERENCE).stream()
            .filter(Objects::nonNull).map(object -> object.getStringValue("member")).toList();
    }

    /**
     * Runs the given action right after the member group document is loaded for the given time, e.g. to simulate a
     * request saving the group after the tested code loaded it and before it saves it.
     */
    private void afterGroupLoad(int loadNumber, FailableConsumer<XWikiDocument, XWikiException> action)
        throws XWikiException
    {
        AtomicInteger loads = new AtomicInteger();
        doAnswer(invocation -> {
            Object document = invocation.callRealMethod();
            if (loads.incrementAndGet() == loadNumber) {
                XWikiDocument concurrentDocument = this.xwiki.getDocument(GROUP_REFERENCE, this.xcontext).clone();
                action.accept(concurrentDocument);
                this.xwiki.saveDocument(concurrentDocument, this.xcontext);
            }
            return document;
        }).when(this.xwiki).getDocument(eq(GROUP_REFERENCE), any(XWikiContext.class));
    }

    private void addMemberObject(XWikiDocument groupDocument, String member) throws XWikiException
    {
        BaseObject object = groupDocument.newXObject(GROUP_CLASS_REFERENCE, this.xcontext);
        object.set("member", member, this.xcontext);
    }

    @Test
    void addMember() throws Exception
    {
        this.wikiUserManager.addMember(ALICE, WIKI_ID);

        // An empty member is added to an empty group (XWIKI-6275).
        assertEquals(List.of("", ALICE), getStoredGroupMembers());
        XWikiDocument groupDocument = this.xwiki.getDocument(GROUP_REFERENCE, this.xcontext);
        assertTrue(groupDocument.isHidden());
        assertEquals("Add [xwiki:XWiki.Alice] to the group.", groupDocument.getComment());
    }

    @Test
    void addMemberAfterConcurrentSave() throws Exception
    {
        saveGroup("", ALICE);
        // Another user joins the wiki while the member is being added: the first load is the one of getMembers.
        afterGroupLoad(2, document -> addMemberObject(document, BOB));

        this.wikiUserManager.addMember(CAROL, WIKI_ID);

        // Both members are kept.
        assertEquals(List.of("", ALICE, BOB, CAROL), getStoredGroupMembers());
    }

    @Test
    void addMemberAddedConcurrently() throws Exception
    {
        saveGroup("", ALICE);
        // The same user is added by another request, e.g. a second click on the join button.
        afterGroupLoad(2, document -> addMemberObject(document, BOB));

        this.wikiUserManager.addMember(BOB, WIKI_ID);

        assertEquals(List.of("", ALICE, BOB), getStoredGroupMembers());
    }

    @Test
    void addMembersAfterConcurrentSave() throws Exception
    {
        saveGroup("", ALICE);
        afterGroupLoad(2, document -> addMemberObject(document, BOB));

        this.wikiUserManager.addMembers(List.of(ALICE, BOB, CAROL), WIKI_ID);

        assertEquals(List.of("", ALICE, BOB, CAROL), getStoredGroupMembers());
    }

    @Test
    void removeMemberAfterConcurrentSave() throws Exception
    {
        saveGroup("", ALICE, BOB);
        afterGroupLoad(1, document -> addMemberObject(document, CAROL));

        this.wikiUserManager.removeMember(ALICE, WIKI_ID);

        assertEquals(List.of("", BOB, CAROL), getStoredGroupMembers());
    }

    @Test
    void removeMembers() throws Exception
    {
        saveGroup("", ALICE, BOB, CAROL);

        this.wikiUserManager.removeMembers(List.of(ALICE, CAROL), WIKI_ID);

        assertEquals(List.of("", BOB), getStoredGroupMembers());
    }

    @Test
    void askToJoinAfterConcurrentSave() throws Exception
    {
        saveGroup("", ALICE);
        afterGroupLoad(1, document -> addMemberObject(document, BOB));

        MemberCandidacy candidacy = this.wikiUserManager.askToJoin(CAROL, WIKI_ID, "please");

        assertEquals(List.of("", ALICE, BOB), getStoredGroupMembers());
        MemberCandidacy storedCandidacy = this.wikiUserManager.getCandidacy(WIKI_ID, candidacy.getId());
        assertEquals(CAROL, storedCandidacy.getUserId());
        assertEquals("please", storedCandidacy.getUserComment());
        assertEquals(MemberCandidacy.Status.PENDING, storedCandidacy.getStatus());
        assertEquals(MemberCandidacy.CandidateType.REQUEST, storedCandidacy.getType());
    }

    @Test
    void getCandidacy() throws Exception
    {
        Date creationDate = new Date(1000L);
        Date closureDate = new Date(2000L);

        XWikiDocument groupDocument = this.xwiki.getDocument(GROUP_REFERENCE, this.xcontext).clone();
        BaseObject object = new BaseObject();
        object.setXClassReference(WikiCandidateMemberClassInitializer.REFERENCE);
        object.setStringValue(WikiCandidateMemberClassInitializer.FIELD_USER, "mainWiki:XWiki.U1");
        object.setLargeStringValue(WikiCandidateMemberClassInitializer.FIELD_USER_COMMENT, "please join");
        object.setStringValue(WikiCandidateMemberClassInitializer.FIELD_ADMIN, "subwiki:XWiki.Admin");
        object.setLargeStringValue(WikiCandidateMemberClassInitializer.FIELD_ADMIN_COMMENT, "welcome");
        object.setLargeStringValue(WikiCandidateMemberClassInitializer.FIELD_ADMIN_PRIVATE_COMMENT, "private note");
        object.setStringValue(WikiCandidateMemberClassInitializer.FIELD_STATUS, "pending");
        object.setStringValue(WikiCandidateMemberClassInitializer.FIELD_TYPE, "invitation");
        object.setDateValue(WikiCandidateMemberClassInitializer.FIELD_DATE_OF_CREATION, creationDate);
        object.setDateValue(WikiCandidateMemberClassInitializer.FIELD_DATE_OF_CLOSURE, closureDate);
        groupDocument.setXObject(42, object);
        this.xwiki.saveDocument(groupDocument, this.xcontext);

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
        assertEquals(creationDate, candidacy.getDateOfCreation());
        // Verify that the closure date is read from the closure field and not from the creation field (XWIKI-17062).
        assertEquals(closureDate, candidacy.getDateOfClosure());
    }

    /**
     * When the candidacy has been removed in the meantime (e.g. a canceled invitation) but a stale candidacy id is
     * still requested (e.g. on a form resubmission after a page refresh), {@code getCandidacy} must return
     * {@code null} instead of throwing a NullPointerException. See XWIKI-17062.
     */
    @Test
    void getCandidacyWhenItDoesNotExist() throws Exception
    {
        assertNull(this.wikiUserManager.getCandidacy(WIKI_ID, 42));
    }
}
