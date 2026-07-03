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
package org.xwiki.index.tree.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.properties.converter.Converter;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Unit tests for {@link AttachmentsTreeNode}.
 *
 * @version $Id$
 */
@ComponentTest
class AttachmentsTreeNodeTest
{
    @InjectMockComponents
    private AttachmentsTreeNode attachmentsTreeNode;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    @Named("entityTreeNodeId")
    private Converter<EntityReference> entityTreeNodeIdConverter;

    @MockComponent
    protected EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    @Mock
    private XWikiContext xcontext;

    @Mock
    private XWiki xwiki;

    @Mock
    private XWikiDocument document;

    private DocumentReference documentReference = new DocumentReference("wiki", "space", "page");

    @BeforeEach
    void configure() throws Exception
    {
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);
        when(this.xwiki.getDocument(this.documentReference, this.xcontext)).thenReturn(this.document);

        when(this.entityTreeNodeIdConverter.convert(EntityReference.class, "document:wiki:space.page"))
            .thenReturn(this.documentReference);
        when(this.defaultEntityReferenceSerializer.serialize(this.documentReference)).thenReturn("wiki:space.page");
    }

    @Test
    void getChildren()
    {
        this.attachmentsTreeNode.getProperties().put("showAddAttachment", true);

        assertEquals(0, this.attachmentsTreeNode.getChildCount("attachments:wiki:space.page"));
        assertEquals(List.of(), this.attachmentsTreeNode.getChildren("attachments:wiki:space.page", 0, 10));

        XWikiAttachment alice = mock(XWikiAttachment.class, "alice");
        when(alice.getReference()).thenReturn(new AttachmentReference("alice", this.documentReference));
        XWikiAttachment bob = mock(XWikiAttachment.class, "bob");
        when(bob.getReference()).thenReturn(new AttachmentReference("bob", this.documentReference));
        when(this.document.getAttachmentList()).thenReturn(List.of(alice, bob));

        when(this.entityTreeNodeIdConverter.convert(String.class, alice.getReference()))
            .thenReturn("attachment:wiki:space.page@alice");
        when(this.entityTreeNodeIdConverter.convert(String.class, bob.getReference()))
            .thenReturn("attachment:wiki:space.page@bob");

        assertEquals(2, this.attachmentsTreeNode.getChildCount("attachments:wiki:space.page"));
        assertEquals(List.of("attachment:wiki:space.page@alice", "attachment:wiki:space.page@bob"),
            this.attachmentsTreeNode.getChildren("attachments:wiki:space.page", 0, 10));

        when(this.authorization.hasAccess(Right.EDIT, this.documentReference)).thenReturn(true);
        assertEquals(3, this.attachmentsTreeNode.getChildCount("attachments:wiki:space.page"));
        assertEquals(
            List.of("addAttachment:wiki:space.page", "attachment:wiki:space.page@alice",
                "attachment:wiki:space.page@bob"),
            this.attachmentsTreeNode.getChildren("attachments:wiki:space.page", 0, 10));
    }
}
