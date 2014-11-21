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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.event.DocumentRolledBackEvent;
import org.xwiki.bridge.event.DocumentRollingBackEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.EntityReferenceValueProvider;
import org.xwiki.model.reference.ObjectReferenceResolver;
import org.xwiki.observation.ObservationManager;
import org.xwiki.rendering.syntax.SyntaxFactory;
import org.xwiki.resource.ResourceReferenceManager;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.XWikiCfgConfigurationSource;
import com.xpn.xwiki.internal.skin.SkinManager;
import com.xpn.xwiki.internal.template.TemplateManager;
import com.xpn.xwiki.store.AttachmentRecycleBinStore;
import com.xpn.xwiki.store.XWikiRecycleBinStoreInterface;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiMessageTool;

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

    private ConfigurationSource xwikiCfgConfigurationSource;

    @Before
    public void setUp() throws Exception
    {
        this.mocker.registerMockComponent(EntityReferenceSerializer.TYPE_STRING);
        this.mocker.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "local");
        this.mocker.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "compact");
        this.mocker.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "compactwiki");
        this.mocker.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "uid");
        this.mocker.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "local/uid");
        this.mocker.registerMockComponent(EntityReferenceResolver.TYPE_STRING, "relative");
        this.mocker.registerMockComponent(EntityReferenceResolver.TYPE_STRING, "currentmixed");
        this.mocker.registerMockComponent(EntityReferenceResolver.TYPE_STRING, "xclass");
        this.mocker.registerMockComponent(DocumentReferenceResolver.TYPE_REFERENCE, "current");
        this.mocker.registerMockComponent(DocumentReferenceResolver.TYPE_REFERENCE, "explicit");
        this.mocker.registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "current");
        this.mocker.registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "explicit");
        this.mocker.registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "currentmixed");
        this.mocker.registerMockComponent(ObjectReferenceResolver.TYPE_REFERENCE, "current");
        this.mocker.registerMockComponent(EntityReferenceValueProvider.class);
        this.mocker.registerMockComponent(SyntaxFactory.class);
        this.mocker.registerMockComponent(ResourceReferenceManager.class);
        this.mocker.registerMockComponent(Environment.class);
        this.mocker.registerMockComponent(ObservationManager.class);
        this.mocker.registerMockComponent(ConfigurationSource.class, XWikiCfgConfigurationSource.ROLEHINT);

        Utils.setComponentManager(mocker);
        xwiki = new XWiki();
        when(context.getWiki()).thenReturn(xwiki);

        XWikiStoreInterface store = mock(XWikiStoreInterface.class);
        xwiki.setStore(store);

        XWikiVersioningStoreInterface versioningStore = mock(XWikiVersioningStoreInterface.class);
        xwiki.setVersioningStore(versioningStore);

        this.xwikiCfgConfigurationSource = this.mocker.registerMockComponent(ConfigurationSource.class, "xwikicfg");
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

        DocumentReference sourceReference = new DocumentReference("foo", "Space", "Source");
        XWikiDocument source = mock(XWikiDocument.class);
        when(source.copyDocument(targetReference, context)).thenReturn(target);

        when(xwiki.getStore().loadXWikiDoc(any(XWikiDocument.class), same(context))).thenReturn(source, target);

        Assert.assertTrue(xwiki.copyDocument(sourceReference, targetReference, context));

        // The target document needs to be new in order for the attachment version to be preserved on save.
        verify(target).setNew(true);

        verify(xwiki.getStore()).saveXWikiDoc(target, context);
    }

    /**
     * Verify that {@link XWiki#rollback(XWikiDocument, String, XWikiContext)} fires the right events.
     */
    @Test
    public void rollbackFiresEvents() throws Exception
    {
        ObservationManager observationManager = mocker.getInstance(ObservationManager.class);

        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);

        XWikiDocument originalDocument = mock(XWikiDocument.class);
        // Mark the document as existing so that the roll-back method will fire an update event.
        when(originalDocument.isNew()).thenReturn(false);

        XWikiDocument result = mock(XWikiDocument.class);
        when(result.clone()).thenReturn(result);
        when(result.getDocumentReference()).thenReturn(documentReference);
        when(result.getOriginalDocument()).thenReturn(originalDocument);

        String revision = "3.5";
        when(xwiki.getVersioningStore().loadXWikiDoc(document, revision, context)).thenReturn(result);

        when(context.getMessageTool()).thenReturn(mock(XWikiMessageTool.class));

        xwiki.rollback(document, revision, context);

        verify(observationManager).notify(new DocumentRollingBackEvent(documentReference, revision), result, context);
        verify(observationManager).notify(new DocumentUpdatingEvent(documentReference), result, context);
        verify(observationManager).notify(new DocumentUpdatedEvent(documentReference), result, context);
        verify(observationManager).notify(new DocumentRolledBackEvent(documentReference, revision), result, context);
    }

    /**
     * @see "XWIKI-9399: Attachment version is incremented when a document is rolled back even if the attachment did not
     *      change"
     */
    @Test
    public void rollbackDoesNotSaveUnchangedAttachment() throws Exception
    {
        String version = "1.1";
        String fileName = "logo.png";
        Date date = new Date();
        XWikiAttachment currentAttachment = mock(XWikiAttachment.class);
        when(currentAttachment.getAttachmentRevision(version, context)).thenReturn(currentAttachment);
        when(currentAttachment.getDate()).thenReturn(new Timestamp(date.getTime()));
        when(currentAttachment.getVersion()).thenReturn(version);
        when(currentAttachment.getFilename()).thenReturn(fileName);

        XWikiAttachment oldAttachment = mock(XWikiAttachment.class);
        when(oldAttachment.getFilename()).thenReturn(fileName);
        when(oldAttachment.getVersion()).thenReturn(version);
        when(oldAttachment.getDate()).thenReturn(date);

        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.getAttachmentList()).thenReturn(Arrays.asList(currentAttachment));
        when(document.getAttachment(fileName)).thenReturn(currentAttachment);

        XWikiDocument result = mock(XWikiDocument.class);
        when(result.clone()).thenReturn(result);
        when(result.getDocumentReference()).thenReturn(documentReference);
        when(result.getAttachmentList()).thenReturn(Arrays.asList(oldAttachment));
        when(result.getAttachment(fileName)).thenReturn(oldAttachment);

        String revision = "3.5";
        when(xwiki.getVersioningStore().loadXWikiDoc(document, revision, context)).thenReturn(result);

        AttachmentRecycleBinStore attachmentRecycleBinStore = mock(AttachmentRecycleBinStore.class);
        xwiki.setAttachmentRecycleBinStore(attachmentRecycleBinStore);

        DocumentReference reference = document.getDocumentReference();
        when(context.getMessageTool()).thenReturn(mock(XWikiMessageTool.class));
        when(xwiki.getStore().loadXWikiDoc(any(XWikiDocument.class), same(context))).thenReturn(
            new XWikiDocument(reference));

        xwiki.rollback(document, revision, context);

        verify(attachmentRecycleBinStore, never()).saveToRecycleBin(same(currentAttachment), any(String.class),
            any(Date.class), same(context), eq(true));
        verify(oldAttachment, never()).setMetaDataDirty(true);
    }

    @Test
    public void deleteAllDocumentsAndWithoutSendingToTrash() throws Exception
    {
        XWiki xwiki = new XWiki();

        XWikiDocument document = mock(XWikiDocument.class);
        DocumentReference reference = new DocumentReference("wiki", "space", "page");
        when(document.getDocumentReference()).thenReturn(reference);

        // Make sure we have a trash for the test.
        XWikiRecycleBinStoreInterface recycleBinStoreInterface = mock(XWikiRecycleBinStoreInterface.class);
        xwiki.setRecycleBinStore(recycleBinStoreInterface);
        when(xwikiCfgConfigurationSource.getProperty("xwiki.recyclebin", "1")).thenReturn("1");

        // Configure the mocked Store to later verify if it's called
        XWikiStoreInterface storeInterface = mock(XWikiStoreInterface.class);
        xwiki.setStore(storeInterface);
        XWikiContext xwikiContext = mock(XWikiContext.class);

        xwiki.deleteAllDocuments(document, false, xwikiContext);

        // Verify that saveToRecycleBin is never called since otherwise it would mean the doc has been saved in the
        // trash
        verify(recycleBinStoreInterface, never()).saveToRecycleBin(any(XWikiDocument.class), any(String.class),
            any(Date.class), any(XWikiContext.class), any(Boolean.class));

        // Verify that deleteXWikiDoc() is called
        verify(storeInterface).deleteXWikiDoc(document, xwikiContext);
    }
}
