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
package org.xwiki.rest.internal.resources.spaces;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.query.Query;
import org.xwiki.rest.internal.resources.AbstractAttachmentsResourceTest;
import org.xwiki.rest.model.jaxb.Attachment;
import org.xwiki.rest.model.jaxb.Attachments;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SpaceAttachmentsResourceImpl}.
 * 
 * @version $Id$
 */
@OldcoreTest
class SpaceAttachmentsResourceImplTest extends AbstractAttachmentsResourceTest
{
    @InjectMockComponents
    private SpaceAttachmentsResourceImpl spaceAttachmentsResource;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @BeforeEach
    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        setUriInfo(this.spaceAttachmentsResource);
    }

    @Test
    void getAttachments() throws Exception
    {
        Query query = mock(Query.class);
        when(this.queryManager.createQuery("select doc.space, doc.name, doc.version, attachment"
            + " from XWikiDocument as doc, XWikiAttachment as attachment where attachment.docId = doc.id and "
            + "(doc.space = :localSpaceReference or doc.space like :localSpaceReferencePrefix) and "
            + "upper(doc.fullName) like :page", Query.HQL)).thenReturn(query);
        mockPreifxQueryParam(query, "localSpaceReferencePrefix", "Path.To.");
        mockContainsQueryParam(query, "page", "XYZ");
        when(query.setOffset(10)).thenReturn(query);
        when(query.setLimit(5)).thenReturn(query);

        XWikiAttachment xwikiAttachment = mock(XWikiAttachment.class);
        AttachmentReference xwikiAttachmentReference = mock(AttachmentReference.class, "image");
        when(xwikiAttachment.getReference()).thenReturn(xwikiAttachmentReference);
        when(this.authorization.hasAccess(Right.VIEW, xwikiAttachmentReference)).thenReturn(true);

        XWikiAttachment forbiddenAttachment = mock(XWikiAttachment.class);
        AttachmentReference forbiddenAttachmentReference = mock(AttachmentReference.class, "forbidden");
        when(forbiddenAttachment.getReference()).thenReturn(forbiddenAttachmentReference);
        when(this.authorization.hasAccess(Right.VIEW, forbiddenAttachmentReference)).thenReturn(false);

        List<Object> results = Arrays.asList(new Object[] {"Path.To", "Page", "1.3", xwikiAttachment},
            new Object[] {"Path.To", "ForbiddenPage", "1.3", forbiddenAttachment});
        when(query.execute()).thenReturn(results);

        SpaceReference spaceReference = new SpaceReference("test", "Path", "To");
        when(this.defaultSpaceReferenceResover.resolve(eq("Path.To"), any())).thenReturn(spaceReference);
        when(this.localEntityReferenceSerializer.serialize(spaceReference)).thenReturn("Path.To");

        Attachment attachment = mock(Attachment.class);
        when(this.modelFactory.toRestAttachment(eq(this.uriInfo.getBaseUri()), any(), eq(false), eq(false)))
            .thenReturn(attachment);

        Attachments attachments =
            this.spaceAttachmentsResource.getAttachments("test", "Path/spaces/To", "", "xyz", "", "", 10, 5, false);

        verify(query).bindValue("localSpaceReference", "Path.To");
        verify(this.authorization).hasAccess(Right.VIEW, xwikiAttachmentReference);
        verify(this.authorization).hasAccess(Right.VIEW, forbiddenAttachmentReference);

        assertEquals(Collections.singletonList(attachment), attachments.getAttachments());
    }
}
