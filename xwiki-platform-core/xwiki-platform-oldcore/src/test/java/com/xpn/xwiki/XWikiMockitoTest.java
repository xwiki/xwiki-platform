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

import java.net.MalformedURLException;
import java.net.URL;
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
import org.xwiki.skin.Resource;
import org.xwiki.skin.Skin;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.ReadOnlyXWikiContextProvider;
import com.xpn.xwiki.internal.debug.DebugConfiguration;
import com.xpn.xwiki.internal.event.UserUpdatingDocumentEvent;
import com.xpn.xwiki.internal.render.groovy.ParseGroovyFromString;
import com.xpn.xwiki.internal.skin.InternalSkinManager;
import com.xpn.xwiki.internal.store.StoreConfiguration;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.XWikiRecycleBinStoreInterface;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;
import com.xpn.xwiki.test.mockito.OldcoreMatchers;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiURLFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
@ComponentList({ DefaultBatchOperationExecutor.class, DefaultExecution.class, ReadOnlyXWikiContextProvider.class })
@ReferenceComponentList
public class XWikiMockitoTest
{
    @MockComponent
    private DocumentRevisionProvider documentRevisionProvider;

    @MockComponent
    @Named("xwikicfg")
    private ConfigurationSource xwikiCfgConfigurationSource;

    @MockComponent
    private WikiDescriptorManager wikis;

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

        Utils.setComponentManager(this.componentManager);
        xwiki = new XWiki();
        this.context.setWiki(this.xwiki);

        this.store = mock(XWikiStoreInterface.class);
        xwiki.setStore(store);

        XWikiVersioningStoreInterface versioningStore = mock(XWikiVersioningStoreInterface.class);
        xwiki.setVersioningStore(versioningStore);

        Execution execution = this.componentManager.getInstance(Execution.class);
        ExecutionContext executionContext = new ExecutionContext();
        executionContext.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, this.context);
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
        DocumentReference targetReferenceWithLocale = new DocumentReference("bar", "Space", "Target", Locale.ROOT);
        XWikiDocument target = mock(XWikiDocument.class);
        when(target.isNew()).thenReturn(true);
        when(target.getDocumentReference()).thenReturn(targetReference);
        when(target.getDocumentReferenceWithLocale()).thenReturn(targetReferenceWithLocale);
        when(target.getLocalReferenceMaxLength()).thenReturn(255);
        when(target.getOriginalDocument()).thenReturn(new XWikiDocument(targetReference));

        DocumentReference sourceReference = new DocumentReference("foo", "Space", "Source");
        XWikiDocument source = mock(XWikiDocument.class);
        when(source.copyDocument(targetReference, false, context)).thenReturn(target);

        when(xwiki.getStore().loadXWikiDoc(OldcoreMatchers.isDocument(sourceReference), same(context)))
            .thenReturn(source);
        when(xwiki.getStore().loadXWikiDoc(OldcoreMatchers.isDocument(targetReferenceWithLocale), same(context)))
            .thenReturn(target);

        assertTrue(xwiki.copyDocument(sourceReference, targetReference, context));

        verify(xwiki.getStore()).saveXWikiDoc(target, context);
    }

    /**
     * Verify that {@link XWiki#rollback(XWikiDocument, String, boolean, boolean, XWikiContext)} fires the right events.
     */
    @Test
    public void rollbackFiresEvents() throws Exception
    {
        ObservationManager observationManager = this.componentManager.getInstance(ObservationManager.class);

        XWikiDocument originalDocument = mock(XWikiDocument.class);
        // Mark the document as existing so that the roll-back method will fire an update event.
        when(originalDocument.isNew()).thenReturn(false);

        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.clone()).thenReturn(document);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.getOriginalDocument()).thenReturn(originalDocument);

        XWikiDocument result = mock(XWikiDocument.class);
        when(result.getDocumentReference()).thenReturn(documentReference);

        DocumentReference userReference = new DocumentReference("xwiki", "XWiki", "ContextUser");
        this.context.setUserReference(userReference);

        String revision = "3.5";
        when(this.documentRevisionProvider.getRevision(document, revision)).thenReturn(result);

        this.componentManager.registerMockComponent(ContextualLocalizationManager.class);

        xwiki.rollback(document, revision, true, true, context);

        verify(observationManager).notify(new DocumentRollingBackEvent(documentReference, revision), document, context);
        verify(observationManager).notify(new DocumentUpdatingEvent(documentReference), document, context);
        verify(observationManager).notify(new DocumentUpdatedEvent(documentReference), document, context);
        verify(observationManager).notify(new DocumentRolledBackEvent(documentReference, revision), document, context);
        verify(observationManager).notify(new UserUpdatingDocumentEvent(userReference, documentReference),
            document, context);
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

    @Test
    public void getExistsWithPageReference() throws Exception
    {
        PageReference pageReference = new PageReference("wiki", "Main", "Space");
        DocumentReference webhomeDocumentReference =
            new DocumentReference("wiki", Arrays.asList("Main", "Space"), "WebHome");
        DocumentReference finalDocumentReference = new DocumentReference("wiki", "Main", "Space");

        assertFalse(this.xwiki.exists(pageReference, this.context));

        when(this.store.exists(OldcoreMatchers.isDocument(finalDocumentReference), same(context))).thenReturn(true);

        assertTrue(this.xwiki.exists(pageReference, this.context));

        when(this.store.exists(OldcoreMatchers.isDocument(finalDocumentReference), same(context))).thenReturn(false);

        assertFalse(this.xwiki.exists(pageReference, this.context));

        when(this.store.exists(OldcoreMatchers.isDocument(webhomeDocumentReference), same(context))).thenReturn(true);

        assertTrue(this.xwiki.exists(pageReference, this.context));
    }

    @Test
    public void parseGroovyFromPage() throws Exception
    {
        ParseGroovyFromString parser = this.componentManager.registerMockComponent(ParseGroovyFromString.class);

        this.context.setWikiId("wiki");
        XWikiDocument document =
            new XWikiDocument(new DocumentReference(this.context.getWikiId(), "Space", "Document"));
        document.setContent("source");

        this.documents.put(document.getDocumentReference(), document);

        String result = "result";

        when(parser.parseGroovyFromString(document.getContent(), this.context)).then(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                assertSame(document, ((XWikiContext) invocation.getArgument(1)).get(XWikiDocument.CKEY_SDOC));

                return result;
            }
        });

        assertEquals(result, this.xwiki.parseGroovyFromPage("Space.Document", this.context));
        assertEquals(result, this.xwiki.parseGroovyFromPage("Space.Document", "page", this.context));
    }

    @Test
    public void getServerURLWhenPathBased() throws MalformedURLException, WikiManagerException
    {
        this.context.setMainXWiki("mainwiki");
        this.context.setWikiId("mainwiki");

        when(this.xwikiCfgConfigurationSource.getProperty("xwiki.virtual.usepath", "1")).thenReturn("1");

        assertNull(this.xwiki.getServerURL("subwiki", this.context));

        WikiDescriptor subwikiDescriptor = new WikiDescriptor("subwiki", "subwiki");
        when(this.wikis.getById(subwikiDescriptor.getId())).thenReturn(subwikiDescriptor);
        WikiDescriptor mainwikiDescriptor = new WikiDescriptor(this.context.getMainXWiki(), "mainwiki.com");
        when(this.wikis.getById(mainwikiDescriptor.getId())).thenReturn(mainwikiDescriptor);
        when(this.wikis.getMainWikiDescriptor()).thenReturn(mainwikiDescriptor);

        assertEquals(new URL("http://mainwiki.com"), this.xwiki.getServerURL("subwiki", this.context));

        mainwikiDescriptor.setSecure(null);
        mainwikiDescriptor.setPort(8080);

        assertEquals(new URL("http://mainwiki.com:8080"), this.xwiki.getServerURL("subwiki", this.context));
    }

    @Test
    void getSkinFileWithMinification() throws Exception
    {
        DebugConfiguration debugConfig = this.componentManager.registerMockComponent(DebugConfiguration.class);

        InternalSkinManager skinManager = this.componentManager.registerMockComponent(InternalSkinManager.class);
        Skin currentSkin = mock(Skin.class, "current");
        when(skinManager.getCurrentSkin(true)).thenReturn(currentSkin);

        Resource jsSource = mock(Resource.class, "jsSource");
        when(jsSource.getURL(false)).thenReturn("path/to/test.js");

        Resource jsMinified = mock(Resource.class, "jsMinified");
        when(jsMinified.getURL(false)).thenReturn("path/to/test.min.js");

        Resource cssSource = mock(Resource.class, "cssSource");
        when(cssSource.getURL(false)).thenReturn("path/to/test.css");

        Resource cssMinified = mock(Resource.class, "cssMinified");
        when(cssMinified.getURL(false)).thenReturn("path/to/test.min.css");

        XWikiURLFactory urlFactory = mock(XWikiURLFactory.class);
        this.context.setURLFactory(urlFactory);

        // minify = false

        when(currentSkin.getResource("test.min.js")).thenReturn(jsMinified);
        // test.js is missing so it should fall-back on test.min.js
        assertEquals("path/to/test.min.js", this.xwiki.getSkinFile("test.js", this.context));

        when(currentSkin.getResource("test.js")).thenReturn(jsSource);
        // test.js is available so it should be returned.
        assertEquals("path/to/test.js", this.xwiki.getSkinFile("test.js", this.context));

        // Expect the version indicated by the debug configuration.
        assertEquals("path/to/test.js", this.xwiki.getSkinFile("test.min.js", this.context));

        // minify = true
        when(debugConfig.isMinify()).thenReturn(true);

        when(currentSkin.getResource("test.css")).thenReturn(cssSource);
        // test.min.css is missing so it should fall-back on test.css
        assertEquals("path/to/test.css", this.xwiki.getSkinFile("test.css", this.context));

        when(currentSkin.getResource("test.min.css")).thenReturn(cssMinified);
        // test.min.css is available so it should be returned.
        assertEquals("path/to/test.min.css", this.xwiki.getSkinFile("test.css", this.context));

        // Expect the version indicated by the debug configuration.
        assertEquals("path/to/test.min.css", this.xwiki.getSkinFile("test.min.css", this.context));

        when(currentSkin.getResource("test.min.css")).thenReturn(null);
        assertEquals("path/to/test.css", this.xwiki.getSkinFile("test.min.css", this.context));

        // Verify that the debug configuration is used only for JavaScript and CSS.
        Resource pngMinified = mock(Resource.class, "pngMinified");
        when(pngMinified.getURL(false)).thenReturn("path/to/test.min.png");
        when(currentSkin.getResource("test.min.png")).thenReturn(pngMinified);
        assertNull(this.xwiki.getSkinFile("test.png", this.context));
    }
}
