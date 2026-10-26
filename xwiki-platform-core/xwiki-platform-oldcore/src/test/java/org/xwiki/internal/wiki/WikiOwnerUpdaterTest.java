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
package org.xwiki.internal.wiki;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

/**
 * Unit tests for {@link WikiOwnerUpdater}.
 *
 * @version $Id$
 */
@OldcoreTest
@ComponentList(WikiOwnerUpdater.class)
@ReferenceComponentList
class WikiOwnerUpdaterTest
{
    private static final LocalDocumentReference WIKI_DESCRIPTOR_CLASS =
        new LocalDocumentReference("XWiki", "XWikiServerClass");

    private static final DocumentReference OWNER = new DocumentReference("xwiki", "XWiki", "Owner");

    private static final DocumentReference ADMIN = new DocumentReference("xwiki", "XWiki", "Admin");

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private WikiOwnerUpdater updater;

    private XWikiContext context;

    @BeforeEach
    void beforeEach() throws Exception
    {
        this.updater = this.oldcore.getMocker().getInstance(WikiOwnerUpdater.class);
        this.context = this.oldcore.getXWikiContext();
        this.context.setUserReference(ADMIN);
    }

    private DocumentReference getWikiDescriptorReference(String wikiId)
    {
        return new DocumentReference("xwiki", "XWiki",
            "XWikiServer" + Character.toUpperCase(wikiId.charAt(0)) + wikiId.substring(1));
    }

    private void createWikiDescriptor(String wikiId, String owner) throws Exception
    {
        this.oldcore.addWiki(wikiId);

        XWikiDocument descriptor = new XWikiDocument(getWikiDescriptorReference(wikiId));
        BaseObject descriptorObject = new BaseObject();
        descriptorObject.setXClassReference(WIKI_DESCRIPTOR_CLASS);
        descriptorObject.setStringValue("owner", owner);
        descriptor.addXObject(descriptorObject);
        this.oldcore.getSpyXWiki().saveDocument(descriptor, this.context);
    }

    private XWikiDocument getWikiDescriptor(String wikiId) throws Exception
    {
        return this.oldcore.getSpyXWiki().getDocument(getWikiDescriptorReference(wikiId), this.context);
    }

    private String getWikiDescriptorOwner(String wikiId) throws Exception
    {
        return getWikiDescriptor(wikiId).getXObject(WIKI_DESCRIPTOR_CLASS).getStringValue("owner");
    }

    private DocumentReference getOriginalMetadataAuthor(String wikiId) throws Exception
    {
        UserReferenceSerializer<DocumentReference> serializer =
            this.oldcore.getMocker().getInstance(UserReferenceSerializer.TYPE_DOCUMENT_REFERENCE, "document");
        return serializer.serialize(getWikiDescriptor(wikiId).getAuthors().getOriginalMetadataAuthor());
    }

    @Test
    void replaceOwnerWithoutNewOwner() throws Exception
    {
        createWikiDescriptor("xwiki", "XWiki.superadmin");
        createWikiDescriptor("subwiki", "xwiki:XWiki.Owner");
        createWikiDescriptor("unprefixed", "XWiki.Owner");
        createWikiDescriptor("other", "xwiki:XWiki.OtherUser");
        createWikiDescriptor("local", "local:XWiki.Owner");

        this.updater.replaceOwner(OWNER, null);

        assertEquals("XWiki.superadmin", getWikiDescriptorOwner("xwiki"));
        assertEquals("", getWikiDescriptorOwner("subwiki"));
        assertEquals(ADMIN, getOriginalMetadataAuthor("subwiki"));
        assertEquals("", getWikiDescriptorOwner("unprefixed"));
        assertEquals("xwiki:XWiki.OtherUser", getWikiDescriptorOwner("other"));
        // A user with the same name in another wiki is another user.
        assertEquals("local:XWiki.Owner", getWikiDescriptorOwner("local"));
    }

    @Test
    void replaceOwnerWithNewOwner() throws Exception
    {
        createWikiDescriptor("subwiki", "XWiki.Owner");
        createWikiDescriptor("other", "xwiki:XWiki.OtherUser");

        this.updater.replaceOwner(OWNER, new DocumentReference("xwiki", "XWiki", "NewOwner"));

        assertEquals("xwiki:XWiki.NewOwner", getWikiDescriptorOwner("subwiki"));
        assertEquals(ADMIN, getOriginalMetadataAuthor("subwiki"));
        assertEquals("xwiki:XWiki.OtherUser", getWikiDescriptorOwner("other"));
    }

    @Test
    void replaceOwnerWhenAWikiFails() throws Exception
    {
        createWikiDescriptor("broken", "XWiki.Owner");
        createWikiDescriptor("subwiki", "XWiki.Owner");
        doThrow(new XWikiException()).when(this.oldcore.getSpyXWiki())
            .getDocument(eq(getWikiDescriptorReference("broken")), any(XWikiContext.class));

        this.updater.replaceOwner(OWNER, null);

        assertEquals("", getWikiDescriptorOwner("subwiki"));
        assertEquals("Failed to update the owner [xwiki:XWiki.Owner] of the wiki [broken].",
            this.logCapture.getMessage(0));
    }
}
