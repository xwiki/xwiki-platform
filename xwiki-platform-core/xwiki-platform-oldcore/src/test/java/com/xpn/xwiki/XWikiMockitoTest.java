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
package com.xpn.xwiki;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.environment.Environment;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.EntityReferenceValueProvider;
import org.xwiki.model.reference.ObjectReferenceResolver;
import org.xwiki.observation.ObservationManager;
import org.xwiki.rendering.syntax.SyntaxFactory;
import org.xwiki.test.mockito.MockitoComponentManagerRule;
import org.xwiki.url.standard.XWikiURLBuilder;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentContent;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.template.PrivilegedTemplateRenderer;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;
import com.xpn.xwiki.web.Utils;

/**
 * Unit tests for {@link XWiki}.
 * 
 * @version $Id$
 */
public class XWikiMockitoTest
{
    /**
     * A component manager that allows us to register mock components.
     */
    @Rule
    public MockitoComponentManagerRule mocker = new MockitoComponentManagerRule();

    /**
     * The object being tested.
     */
    private XWiki xwiki;

    /**
     * A mock {@link XWikiContext};
     */
    private XWikiContext context = mock(XWikiContext.class);

    @Before
    public void setUp() throws Exception
    {
        mocker.registerMockComponent(EntityReferenceSerializer.TYPE_STRING);
        mocker.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "local");
        mocker.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "compact");
        mocker.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "compactwiki");
        mocker.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "uid");
        mocker.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "local/uid");
        mocker.registerMockComponent(EntityReferenceResolver.TYPE_STRING, "relative");
        mocker.registerMockComponent(EntityReferenceResolver.TYPE_STRING, "currentmixed");
        mocker.registerMockComponent(EntityReferenceResolver.TYPE_STRING, "xclass");
        mocker.registerMockComponent(DocumentReferenceResolver.TYPE_REFERENCE, "current");
        mocker.registerMockComponent(DocumentReferenceResolver.TYPE_REFERENCE, "explicit");
        mocker.registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "current");
        mocker.registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "explicit");
        mocker.registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "currentmixed");
        mocker.registerMockComponent(ObjectReferenceResolver.TYPE_REFERENCE, "current");
        mocker.registerMockComponent(EntityReferenceValueProvider.class);
        mocker.registerMockComponent(SyntaxFactory.class);
        mocker.registerMockComponent(PrivilegedTemplateRenderer.class);
        mocker.registerMockComponent(XWikiURLBuilder.class, "entity");
        mocker.registerMockComponent(Environment.class);
        mocker.registerMockComponent(ObservationManager.class);

        Utils.setComponentManager(mocker);
        xwiki = new XWiki();

        XWikiStoreInterface store = mock(XWikiStoreInterface.class);
        xwiki.setStore(store);

        XWikiVersioningStoreInterface versioningStore = mock(XWikiVersioningStoreInterface.class);
        xwiki.setVersioningStore(versioningStore);
    }

    /**
     * Verify that attachment versions are not incremented when a document is copied.
     * 
     * @see <a href="http://jira.xwiki.org/browse/XWIKI-8157">XWIKI-8157: The "Copy Page" action adds an extra version
     *      to the attached file</a>
     */
    @Test
    public void copyDocumentPreservesAttachmentsVersion() throws Exception
    {
        DocumentReference targetReference = new DocumentReference("bar", "Space", "Target");
        XWikiDocument target = mock(XWikiDocument.class);
        when(target.isNew()).thenReturn(true);
        when(target.getDocumentReference()).thenReturn(targetReference);

        // After the source is copied into the target document, the target document has an attachment.
        XWikiAttachment attachment = mock(XWikiAttachment.class);
        XWikiAttachmentContent attachmentContent = mock(XWikiAttachmentContent.class);
        when(target.getAttachmentList()).thenReturn(Collections.singletonList(attachment));
        when(attachment.getAttachment_content()).thenReturn(attachmentContent);

        DocumentReference sourceReference = new DocumentReference("foo", "Space", "Source");
        XWikiDocument source = mock(XWikiDocument.class);
        when(source.copyDocument(targetReference, context)).thenReturn(target);

        when(xwiki.getStore().loadXWikiDoc(any(XWikiDocument.class), same(context))).thenReturn(source, target);

        Assert.assertTrue(xwiki.copyDocument(sourceReference, targetReference, context));

        // Dirty flags must be reset in order to prevent the save method from incrementing the attachment version.
        verify(attachment).setMetaDataDirty(false);
        verify(attachmentContent).setContentDirty(false);

        // Attachments must be saved separately to avoid incrementing their version.
        verify(target).saveAllAttachments(false, true, context);
    }
}
