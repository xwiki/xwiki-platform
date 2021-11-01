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
package org.xwiki.livedata.internal.livetable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.livedata.LiveData;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataEntryDescriptor;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataMeta;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.livedata.LiveDataQuery.Source;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import ch.qos.logback.classic.Level;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LiveTableLiveDataEntryStore}.
 *
 * @version $Id$
 * @since 12.10
 */
@ComponentTest
class LiveTableLiveDataEntryStoreTest
{
    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private LiveTableLiveDataEntryStore entryStore;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private TemplateManager templateManager;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    @MockComponent
    private LiveTableRequestHandler liveTableRequestHandler;

    @MockComponent
    private ModelBridge modelBridge;

    @MockComponent
    @Named("liveTable")
    private Provider<LiveDataConfiguration> liveDataConfigurationProvider;

    @Mock
    private XWikiContext xcontext;

    @Mock
    private XWiki xwiki;

    @Mock
    private LiveDataConfiguration liveDataConfiguration;

    @Mock
    private LiveDataMeta liveDataMeta;

    @Mock
    private LiveDataEntryDescriptor entryDescriptor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    @BeforeEach
    void before()
    {
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);

        when(this.liveTableRequestHandler.getLiveTableResults(any(), any()))
            .thenAnswer(invocation -> ((Supplier<String>) invocation.getArgument(1)).get());
        when(this.liveDataConfigurationProvider.get()).thenReturn(this.liveDataConfiguration);
        when(this.liveDataConfiguration.getMeta()).thenReturn(this.liveDataMeta);
        when(this.liveDataMeta.getEntryDescriptor()).thenReturn(this.entryDescriptor);
        when(this.entryDescriptor.getIdProperty()).thenReturn("doc.fullName");
    }

    @Test
    void getFromTemplate() throws Exception
    {
        this.entryStore.getParameters().put("template", "getdocuments");

        Map<String, Object> row = new HashMap<>();
        row.put("doc_title", "Some title");
        row.put("status", "pending");
        row.put("hidden", true);
        row.put("doc_author", "mflorea");
        row.put("doc_url", "http://www.xwiki.org");

        Map<String, Object> liveTableResults = new HashMap<>();
        liveTableResults.put("totalrows", 23);
        liveTableResults.put("rows", Collections.singletonList(row));

        String liveTableResultsJSON = this.objectMapper.writeValueAsString(liveTableResults);
        when(this.templateManager.render("getdocuments")).thenReturn(liveTableResultsJSON);

        Map<String, Object> entry = new HashMap<>();
        entry.put("doc.title", "Some title");
        entry.put("status", "pending");
        entry.put("hidden", true);
        entry.put("doc.author", "mflorea");
        entry.put("doc.url", "http://www.xwiki.org");

        LiveData expectedLiveData = new LiveData();
        expectedLiveData.setCount(23);
        expectedLiveData.getEntries().add(entry);

        assertEquals(expectedLiveData, this.entryStore.get(new LiveDataQuery()));
    }

    @Test
    void getFromResultPageWhenAccessDenied() throws Exception
    {
        this.entryStore.getParameters().put("resultPage", "Some.Page");

        DocumentReference documentReference = new DocumentReference("foo", "Some", "Page");
        when(this.currentDocumentReferenceResolver.resolve("Some.Page")).thenReturn(documentReference);

        DocumentReference userReference = new DocumentReference("xwiki", "Users", "Alice");
        doThrow(new AccessDeniedException(Right.VIEW, userReference, documentReference)).when(this.authorization)
            .checkAccess(Right.VIEW, documentReference);

        try {
            this.entryStore.get(new LiveDataQuery());
            fail();
        } catch (LiveDataException e) {
            assertTrue(e.getCause() instanceof AccessDeniedException, "Expecting AccessDeniedException");
        }
    }

    @Test
    void getFromResultPage() throws Exception
    {
        this.entryStore.getParameters().put("resultPage", "test");

        // Verify that the query source parameters take precedence.
        LiveDataQuery query = new LiveDataQuery();
        query.setSource(new Source());
        query.getSource().setParameter("resultPage", "Panels.LiveTableResults");

        DocumentReference documentReference = new DocumentReference("foo", "Panels", "LiveTableResults");
        when(this.currentDocumentReferenceResolver.resolve("Panels.LiveTableResults")).thenReturn(documentReference);

        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(documentReference, this.xcontext)).thenReturn(document);
        when(document.getRenderedContent(Syntax.PLAIN_1_0, this.xcontext)).thenReturn("{\"totalrows\":3,\"rows\":[]}");

        LiveData expectedLiveData = new LiveData();
        expectedLiveData.setCount(3);

        assertEquals(expectedLiveData, this.entryStore.get(query));
    }

    @Test
    void getFromDefaultResultPage() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("foo", "XWiki", "LiveTableResults");
        when(this.currentDocumentReferenceResolver.resolve("XWiki.LiveTableResults")).thenReturn(documentReference);

        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(documentReference, this.xcontext)).thenReturn(document);
        when(document.getRenderedContent(Syntax.PLAIN_1_0, this.xcontext)).thenReturn("{\"totalrows\":7,\"rows\":[]}");

        LiveData expectedLiveData = new LiveData();
        expectedLiveData.setCount(7);

        assertEquals(expectedLiveData, this.entryStore.get(new LiveDataQuery()));
    }

    @Test
    void getFromDefaultResultPageWithInvalidJSON() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("foo", "XWiki", "LiveTableResults");
        when(this.currentDocumentReferenceResolver.resolve("XWiki.LiveTableResults")).thenReturn(documentReference);

        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(documentReference, this.xcontext)).thenReturn(document);
        when(document.getRenderedContent(Syntax.PLAIN_1_0, this.xcontext)).thenReturn("no JSON");

        try {
            this.entryStore.get(new LiveDataQuery());
            fail();
        } catch (LiveDataException e) {
            assertEquals("Failed to execute the live data query.", e.getMessage());
        }
    }

    @Test
    void updateUndefinedClassName() throws Exception
    {
        String entryId = "testEntry";
        String property = "propName";
        Object value = "theValue";
        LiveDataException liveDataException =
            assertThrows(LiveDataException.class, () -> this.entryStore.update(entryId, property, value));
        assertEquals("Can't update object properties if the object type (class name) is undefined.",
            liveDataException.getMessage());
    }

    @Test
    void updateXClassField() throws Exception
    {
        String entryId = "testEntry";
        String property = "propName";
        Object value = "theValue";
        String docName = "MyApp.MyClass";
        DocumentReference documentEntityReference = new DocumentReference("xwiki", "MyApp", "testEntry");
        DocumentReference documentClassReference = new DocumentReference("xwiki", "MyApp", "MyClass");

        this.entryStore.getParameters().put("className", docName);
        when(this.currentDocumentReferenceResolver.resolve(entryId)).thenReturn(documentEntityReference);
        when(this.currentDocumentReferenceResolver.resolve(docName)).thenReturn(documentClassReference);

        this.entryStore.update(entryId, property, value);
        verify(this.modelBridge).update(property, value, documentEntityReference, documentClassReference);
    }

    @Test
    void saveXClassUndefined()
    {
        Map<String, Object> entry = new HashMap<>();
        entry.put("doc.hidden", "true");
        entry.put("myProperty", asList("1", "0"));
        LiveDataException liveDataException =
            assertThrows(LiveDataException.class, () -> this.entryStore.save(entry));
        assertEquals("Can't update object properties if the object type (class name) is undefined.",
            liveDataException.getMessage());
    }

    @Test
    void saveEntryIdUndefined()
    {
        Map<String, Object> entry = new HashMap<>();
        entry.put("doc.hidden", "true");
        entry.put("myProperty", asList("1", "0"));
        this.entryStore.getParameters().put("className", "MyTest.MyClass");
        LiveDataException liveDataException =
            assertThrows(LiveDataException.class, () -> this.entryStore.save(entry));
        assertEquals("Entry id [doc.fullName] missing. Can't load the document to update.",
            liveDataException.getMessage());
    }

    @Test
    void save() throws Exception
    {
        Map<String, Object> entry = new HashMap<>();
        entry.put("doc.hidden", "true");
        entry.put("myProperty", asList("1", "0"));
        entry.put("doc.fullName", "MyTest.MyObject");
        DocumentReference objectDocumentReference = new DocumentReference("xwiki", "MyTest", "MyObject");
        DocumentReference classDocumentReference = new DocumentReference("xwiki", "MyTest", "MyClass");

        when(this.currentDocumentReferenceResolver.resolve("MyTest.MyObject")).thenReturn(objectDocumentReference);
        when(this.currentDocumentReferenceResolver.resolve("MyTest.MyClass")).thenReturn(classDocumentReference);

        this.entryStore.getParameters().put("className", "MyTest.MyClass");
        Optional<Object> save = this.entryStore.save(entry);

        assertEquals(Optional.of("MyTest.MyObject"), save);
        verify(this.modelBridge).updateAll(entry, objectDocumentReference, classDocumentReference);
    }

    @Test
    void xwikivarsRenderingThrowsAnException() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("foo", "XWiki", "LiveTableResults");
        when(this.currentDocumentReferenceResolver.resolve("XWiki.LiveTableResults")).thenReturn(documentReference);

        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(documentReference, this.xcontext)).thenReturn(document);
        when(document.getRenderedContent(Syntax.PLAIN_1_0, this.xcontext)).thenReturn("{}");

        when(this.templateManager.render("xwikivars.vm")).thenThrow(new NullPointerException());

        LiveDataQuery liveDataQuery = new LiveDataQuery();
        this.entryStore.get(liveDataQuery);

        assertEquals(1, this.logCapture.size());
        assertEquals(
            "Failed to evaluate [xwikivars.vm] when getting the Livetable results from page [XWiki.LiveTableResults]. "
                + "Cause: [NullPointerException: ].", this.logCapture.getMessage(0));
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
    }
}
