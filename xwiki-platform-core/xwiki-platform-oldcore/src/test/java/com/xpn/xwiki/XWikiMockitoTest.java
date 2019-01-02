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

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentDeletingEvent;
import org.xwiki.bridge.event.DocumentRolledBackEvent;
import org.xwiki.bridge.event.DocumentRollingBackEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.internal.DefaultExecution;
import org.xwiki.environment.Environment;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.internal.reference.EntityReferenceFactory;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.model.reference.PageObjectReference;
import org.xwiki.model.reference.PageReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.refactoring.internal.batch.DefaultBatchOperationExecutor;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.resource.ResourceReferenceManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.store.StoreConfiguration;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.AttachmentRecycleBinStore;
import com.xpn.xwiki.store.XWikiRecycleBinStoreInterface;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiURLFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link XWiki}.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList({ DefaultBatchOperationExecutor.class, DefaultExecution.class })
@ReferenceComponentList
public class XWikiMockitoTest
{
    @MockComponent
    private EntityReferenceFactory entityReferenceFactory;

    @MockComponent
    private DocumentRevisionProvider documentRevisionProvider;

    @MockComponent
    @Named("xwikicfg")
    private ConfigurationSource xwikiCfgConfigurationSource;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    protected Map<DocumentReference, XWikiDocument> documents = new ConcurrentHashMap<>();

    /**
     * The object being tested.
     */
    private XWiki xwiki;

    /**
     * A mock {@link XWikiContext};
     */
    private XWikiContext context = new XWikiContext();

    private XWikiStoreInterface store;

    @BeforeEach
    public void beforeEach() throws Exception
    {
        this.componentManager.registerMockComponent(ResourceReferenceManager.class);
        this.componentManager.registerMockComponent(Environment.class);
        this.componentManager.registerMockComponent(ObservationManager.class);
        this.componentManager.registerMockComponent(StoreConfiguration.class);
        this.componentManager.registerMockComponent(WikiDescriptorManager.class);

        when(this.entityReferenceFactory.getReference(any())).thenAnswer((invocation) -> invocation.getArgument(0));

        Utils.setComponentManager(this.componentManager);
        xwiki = new XWiki();
        this.context.setWiki(this.xwiki);

        this.store = mock(XWikiStoreInterface.class);
        xwiki.setStore(store);

        XWikiVersioningStoreInterface versioningStore = mock(XWikiVersioningStoreInterface.class);
        xwiki.setVersioningStore(versioningStore);

        Execution execution = this.componentManager.getInstance(Execution.class);
        ExecutionContext executionContext = new ExecutionContext();
        execution.setContext(executionContext);

        when(this.store.loadXWikiDoc(any(XWikiDocument.class), any(XWikiContext.class)))
            .thenAnswer(new Answer<XWikiDocument>()
            {
                @Override
                public XWikiDocument answer(InvocationOnMock invocation) throws Throwable
                {
                    XWikiDocument target = invocation.getArgument(0);
                    DocumentReference reference = target.getDocumentReference();

                    XWikiDocument document = documents.get(reference);

                    if (document == null) {
                        document = new XWikiDocument(reference, target.getLocale());
                        document.setSyntax(Syntax.PLAIN_1_0);
                        document.setOriginalDocument(document.clone());
                    }

                    return document;
                }
            });
    }

    /**
     * Verify that attachment versions are not incremented when a document is copied.
     * 
     * @see <a href="https://jira.xwiki.org/browse/XWIKI-8157">XWIKI-8157: The "Copy Page" action adds an extra version
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

        assertTrue(xwiki.copyDocument(sourceReference, targetReference, context));

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
        ObservationManager observationManager = this.componentManager.getInstance(ObservationManager.class);

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
        when(this.documentRevisionProvider.getRevision(document, revision)).thenReturn(result);

        this.componentManager.registerMockComponent(ContextualLocalizationManager.class);

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
        when(this.documentRevisionProvider.getRevision(document, revision)).thenReturn(result);

        AttachmentRecycleBinStore attachmentRecycleBinStore = mock(AttachmentRecycleBinStore.class);
        xwiki.setAttachmentRecycleBinStore(attachmentRecycleBinStore);

        XWikiDocument emptyDocument = new XWikiDocument(document.getDocumentReference());
        this.componentManager.registerMockComponent(ContextualLocalizationManager.class);
        when(xwiki.getStore().loadXWikiDoc(any(XWikiDocument.class), same(context))).thenReturn(emptyDocument);

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

    @Test
    public void deleteDocument() throws Exception
    {
        final DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);

        final XWikiDocument originalDocument = mock(XWikiDocument.class);
        when(document.getOriginalDocument()).thenReturn(originalDocument);

        this.xwiki.deleteDocument(document, this.context);

        ObservationManager observation = this.componentManager.getInstance(ObservationManager.class);

        ArgumentMatcher<XWikiDocument> matcher = new ArgumentMatcher<XWikiDocument>()
        {
            @Override
            public boolean matches(XWikiDocument argument)
            {
                return argument.getDocumentReference().equals(documentReference)
                    && argument.getOriginalDocument() == originalDocument;
            }
        };

        // Make sure the right events have been sent
        verify(observation).notify(eq(new DocumentDeletingEvent(documentReference)), argThat(matcher),
            same(this.context));
        verify(observation).notify(eq(new DocumentDeletedEvent(documentReference)), argThat(matcher),
            same(this.context));

        verifyNoMoreInteractions(observation);
    }

    @Test
    public void getPlainUserName() throws XWikiException
    {
        XWikiDocument document = mock(XWikiDocument.class);
        DocumentReference userReference = new DocumentReference("wiki", "XWiki", "user");
        when(document.getDocumentReference()).thenReturn(userReference);
        when(this.store.loadXWikiDoc(any(XWikiDocument.class), any(XWikiContext.class))).thenReturn(document);
        BaseObject userObject = mock(BaseObject.class);
        when(document.getObject("XWiki.XWikiUsers")).thenReturn(userObject);

        when(userObject.getStringValue("first_name")).thenReturn("first<name");
        when(userObject.getStringValue("last_name")).thenReturn("last'name");
        assertEquals("first<name last'name", xwiki.getPlainUserName(userReference, context));

        when(userObject.getStringValue("first_name")).thenReturn("first<name");
        when(userObject.getStringValue("last_name")).thenReturn("");
        assertEquals("first<name", xwiki.getPlainUserName(userReference, context));

        when(userObject.getStringValue("first_name")).thenReturn("");
        when(userObject.getStringValue("last_name")).thenReturn("last'name");
        assertEquals("last'name", xwiki.getPlainUserName(userReference, context));
    }

    @Test
    public void getURLWithDotsAndBackslashInSpaceName() throws Exception
    {
        XWikiURLFactory urlFactory = mock(XWikiURLFactory.class);
        context.setURLFactory(urlFactory);

        DocumentReference reference = new DocumentReference("wiki", Arrays.asList("space.withdot.and\\and:"), "page");

        this.xwiki.getURL(reference, "view", null, null, context);

        verify(urlFactory).createURL("space\\.withdot\\.and\\\\and\\:", "page", "view", null, null, "wiki", context);
    }

    @Test
    public void getURLWithLocale() throws Exception
    {
        XWikiURLFactory urlFactory = mock(XWikiURLFactory.class);
        context.setURLFactory(urlFactory);

        DocumentReference reference = new DocumentReference("wiki", "Space", "Page", Locale.FRENCH);

        this.xwiki.getURL(reference, "view", null, null, context);
        verify(urlFactory).createURL("Space", "Page", "view", "language=fr", null, "wiki", context);

        this.xwiki.getURL(reference, "view", "language=ro", null, context);
        verify(urlFactory).createURL("Space", "Page", "view", "language=ro&language=fr", null, "wiki", context);
    }

    @Test
    public void getEntityURLWithDefaultAction() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("tennis", Arrays.asList("Path", "To"), "Success");
        AttachmentReference attachmentReference = new AttachmentReference("image.png", documentReference);

        XWikiURLFactory urlFactory = mock(XWikiURLFactory.class);
        context.setURLFactory(urlFactory);

        this.xwiki.getURL(documentReference, this.context);
        verify(urlFactory).createURL("Path.To", "Success", "view", null, null, "tennis", this.context);

        this.xwiki.getURL(attachmentReference, this.context);
        verify(urlFactory).createAttachmentURL("image.png", "Path.To", "Success", "download", null, "tennis",
            this.context);
    }

    @Test
    public void getSpacePreference() throws Exception
    {
        this.componentManager.registerMockComponent(ConfigurationSource.class, "wiki");
        ConfigurationSource spaceConfiguration =
            this.componentManager.registerMockComponent(ConfigurationSource.class, "space");

        when(this.xwikiCfgConfigurationSource.getProperty(any(), anyString())).then(new Answer<String>()
        {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable
            {
                return invocation.getArgument(1);
            }
        });

        WikiReference wikiReference = new WikiReference("wiki");
        SpaceReference space1Reference = new SpaceReference("space1", wikiReference);
        SpaceReference space2Reference = new SpaceReference("space2", space1Reference);

        // Without preferences and current doc

        assertEquals("", this.xwiki.getSpacePreference("pref", this.context));
        assertEquals("defaultvalue", this.xwiki.getSpacePreference("pref", "defaultvalue", this.context));
        assertEquals("", this.xwiki.getSpacePreference("pref", space2Reference, this.context));
        assertEquals("defaultvalue",
            this.xwiki.getSpacePreference("pref", space2Reference, "defaultvalue", this.context));

        // Without preferences but with current doc

        this.context.setDoc(new XWikiDocument(new DocumentReference("document", space2Reference)));

        assertEquals("", this.xwiki.getSpacePreference("pref", this.context));
        assertEquals("defaultvalue", this.xwiki.getSpacePreference("pref", "defaultvalue", this.context));
        assertEquals("", this.xwiki.getSpacePreference("pref", space2Reference, this.context));
        assertEquals("defaultvalue",
            this.xwiki.getSpacePreference("pref", space2Reference, "defaultvalue", this.context));

        // With preferences

        final Map<String, Map<String, String>> spacesPreferences = new HashMap<>();
        Map<String, String> space1Preferences = new HashMap<>();
        space1Preferences.put("pref", "prefvalue1");
        space1Preferences.put("pref1", "pref1value1");
        Map<String, String> space2Preferences = new HashMap<>();
        space2Preferences.put("pref", "prefvalue2");
        space2Preferences.put("pref2", "pref2value2");
        spacesPreferences.put(space1Reference.getName(), space1Preferences);
        spacesPreferences.put(space2Reference.getName(), space2Preferences);

        when(spaceConfiguration.getProperty(any(), same(String.class))).then(new Answer<String>()
        {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable
            {
                if (context.getDoc() != null) {
                    Map<String, String> spacePreferences =
                        spacesPreferences.get(context.getDoc().getDocumentReference().getParent().getName());
                    if (spacePreferences != null) {
                        return spacePreferences.get(invocation.getArgument(0));
                    }
                }

                return null;
            }
        });

        this.context.setDoc(new XWikiDocument(new DocumentReference("document", space1Reference)));
        assertEquals("prefvalue1", this.xwiki.getSpacePreference("pref", this.context));
        assertEquals("prefvalue1", this.xwiki.getSpacePreference("pref", "defaultvalue", this.context));
        assertEquals("pref1value1", this.xwiki.getSpacePreference("pref1", this.context));
        assertEquals("", this.xwiki.getSpacePreference("pref2", this.context));

        this.context.setDoc(new XWikiDocument(new DocumentReference("document", space2Reference)));
        assertEquals("prefvalue2", this.xwiki.getSpacePreference("pref", this.context));
        assertEquals("prefvalue2", this.xwiki.getSpacePreference("pref", "defaultvalue", this.context));
        assertEquals("pref1value1", this.xwiki.getSpacePreference("pref1", this.context));
        assertEquals("pref2value2", this.xwiki.getSpacePreference("pref2", this.context));

        assertEquals("", this.xwiki.getSpacePreference("nopref", space1Reference, this.context));
        assertEquals("defaultvalue",
            this.xwiki.getSpacePreference("nopref", space1Reference, "defaultvalue", this.context));
        assertEquals("prefvalue1", this.xwiki.getSpacePreference("pref", space1Reference, this.context));
        assertEquals("prefvalue1",
            this.xwiki.getSpacePreference("pref", space1Reference, "defaultvalue", this.context));
        assertEquals("pref1value1", this.xwiki.getSpacePreference("pref1", space1Reference, this.context));
        assertEquals("", this.xwiki.getSpacePreference("pref2", space1Reference, this.context));

        assertEquals("", this.xwiki.getSpacePreference("nopref", space2Reference, this.context));
        assertEquals("defaultvalue",
            this.xwiki.getSpacePreference("nopref", space2Reference, "defaultvalue", this.context));
        assertEquals("prefvalue2", this.xwiki.getSpacePreference("pref", space2Reference, this.context));
        assertEquals("prefvalue2",
            this.xwiki.getSpacePreference("pref", space2Reference, "defaultvalue", this.context));
        assertEquals("pref1value1", this.xwiki.getSpacePreference("pref1", space2Reference, this.context));
        assertEquals("pref2value2", this.xwiki.getSpacePreference("pref2", space2Reference, this.context));
    }

    @Test
    public void getDocumentWithEntityReference() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Main", "WebHome");

        assertEquals(documentReference,
            this.xwiki.getDocument(new WikiReference("wiki"), this.context).getDocumentReference());

        assertEquals(documentReference, this.xwiki
            .getDocument(new ObjectReference("object", documentReference), this.context).getDocumentReference());
    }

    @Test
    public void getDocumentWithPageReference() throws Exception
    {
        PageReference pageReference = new PageReference("wiki", "Main", "Space");
        PageObjectReference pageObjectReference = new PageObjectReference("object", pageReference);
        DocumentReference webhomeDocumentReference =
            new DocumentReference("wiki", Arrays.asList("Main", "Space"), "WebHome");
        DocumentReference finalDocumentReference = new DocumentReference("wiki", "Main", "Space");

        assertEquals(webhomeDocumentReference,
            this.xwiki.getDocument(pageReference, this.context).getDocumentReference());
        assertEquals(webhomeDocumentReference,
            this.xwiki.getDocument(pageObjectReference, this.context).getDocumentReference());

        XWikiDocument finalDocument = new XWikiDocument(finalDocumentReference);
        finalDocument.setNew(false);
        this.documents.put(finalDocument.getDocumentReference(), finalDocument);

        assertEquals(finalDocumentReference,
            this.xwiki.getDocument(pageReference, this.context).getDocumentReference());
        assertEquals(finalDocumentReference,
            this.xwiki.getDocument(pageObjectReference, this.context).getDocumentReference());

        XWikiDocument webhomeDocument = new XWikiDocument(webhomeDocumentReference);
        webhomeDocument.setNew(false);
        this.documents.put(webhomeDocument.getDocumentReference(), webhomeDocument);

        assertEquals(webhomeDocumentReference,
            this.xwiki.getDocument(pageReference, this.context).getDocumentReference());
        assertEquals(webhomeDocumentReference,
            this.xwiki.getDocument(pageObjectReference, this.context).getDocumentReference());
    }
}
