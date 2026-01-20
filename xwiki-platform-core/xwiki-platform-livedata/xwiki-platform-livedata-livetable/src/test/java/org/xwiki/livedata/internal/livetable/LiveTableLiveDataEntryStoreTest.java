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
import java.util.Set;

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
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.fasterxml.jackson.databind.ObjectMapper;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LiveTableLiveDataEntryStore}.
 *
 * @version $Id$
 * @since 12.10
 */
@SuppressWarnings({ "checkstyle:MultipleStringLiterals", "checkstyle:ClassFanOutComplexity" })
@ComponentTest
class LiveTableLiveDataEntryStoreTest
{
    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private LiveTableLiveDataEntryStore entryStore;

    @MockComponent
    private LiveTableLiveDataResultsRenderer resultsRenderer;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    @MockComponent
    private ModelBridge modelBridge;

    @MockComponent
    @Named(LiveTableLiveDataEntryStore.ROLE_HINT)
    private Provider<LiveDataConfiguration> liveDataConfigurationProvider;

    @Mock
    private LiveDataConfiguration liveDataConfiguration;

    @Mock
    private LiveDataMeta liveDataMeta;

    @Mock
    private LiveDataEntryDescriptor entryDescriptor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void before()
    {
        when(this.liveDataConfigurationProvider.get()).thenReturn(this.liveDataConfiguration);
        when(this.liveDataConfiguration.getMeta()).thenReturn(this.liveDataMeta);
        when(this.liveDataMeta.getEntryDescriptor()).thenReturn(this.entryDescriptor);
        when(this.entryDescriptor.getIdProperty()).thenReturn("doc.fullName");
    }

    @Test
    void getWithObject()
    {
        assertThrows(UnsupportedOperationException.class, () -> this.entryStore.get("testEntry"));
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
        when(this.resultsRenderer.getLiveTableResultsFromTemplate(eq("getdocuments"), any()))
            .thenReturn(liveTableResultsJSON);

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
    void getFromResultPage() throws Exception
    {
        this.entryStore.getParameters().put("resultPage", "test");

        // Verify that the query source parameters take precedence.
        LiveDataQuery query = new LiveDataQuery();
        query.setSource(new Source());
        query.getSource().setParameter("resultPage", "Panels.LiveTableResults");

        DocumentReference documentReference = new DocumentReference("foo", "Panels", "LiveTableResults");
        when(this.currentDocumentReferenceResolver.resolve("Panels.LiveTableResults")).thenReturn(documentReference);

        when(this.resultsRenderer.getLiveTableResultsFromPage(eq("Panels.LiveTableResults"), any()))
            .thenReturn("{\"totalrows\":3,\"rows\":[]}");

        LiveData expectedLiveData = new LiveData();
        expectedLiveData.setCount(3);

        assertEquals(expectedLiveData, this.entryStore.get(query));
    }

    @Test
    void getFromDefaultResultPage() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("foo", "XWiki", "LiveTableResults");
        when(this.currentDocumentReferenceResolver.resolve("XWiki.LiveTableResults")).thenReturn(documentReference);

        when(this.resultsRenderer.getLiveTableResultsFromPage(eq("XWiki.LiveTableResults"), any()))
            .thenReturn("{\"totalrows\":7,\"rows\":[]}");

        LiveData expectedLiveData = new LiveData();
        expectedLiveData.setCount(7);

        assertEquals(expectedLiveData, this.entryStore.get(new LiveDataQuery()));
    }

    @Test
    void getFromDefaultResultPageWithInvalidJSON() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("foo", "XWiki", "LiveTableResults");
        when(this.currentDocumentReferenceResolver.resolve("XWiki.LiveTableResults")).thenReturn(documentReference);

        when(this.resultsRenderer.getLiveTableResultsFromPage("XWiki.LiveTableResults", null)).thenReturn("no JSON");

        try {
            this.entryStore.get(new LiveDataQuery());
            fail();
        } catch (LiveDataException e) {
            assertEquals("Failed to execute the live data query.", e.getMessage());
        }
    }

    @Test
    void updateUndefinedClassName()
    {
        LiveDataException liveDataException =
            assertThrows(LiveDataException.class, () -> this.entryStore.update("testEntry", "propName", "theValue"));
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
    void updateWithCustomClassName() throws Exception
    {
        String entryId = "testEntry";
        String property = "propName";
        Object value = "theValue";
        String defaultClass = "MyApp.DefaultClass";
        String customClass = "MyApp.CustomClass";
        DocumentReference documentReference = new DocumentReference("xwiki", "MyApp", "testEntry");
        DocumentReference customClassReference = new DocumentReference("xwiki", "MyApp", "CustomClass");

        this.entryStore.getParameters().put("className", defaultClass);
        this.entryStore.getParameters().put(property + "_class", customClass);
        when(this.currentDocumentReferenceResolver.resolve(entryId)).thenReturn(documentReference);
        when(this.currentDocumentReferenceResolver.resolve(customClass)).thenReturn(customClassReference);

        this.entryStore.update(entryId, property, value);
        verify(this.modelBridge).update(property, value, documentReference, customClassReference);
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

        verify(this.modelBridge).updateAll(entry, objectDocumentReference, classDocumentReference, Map.of());
    }

    @Test
    void saveWithCustomPropertyClass() throws Exception
    {
        Map<String, Object> entry = new HashMap<>();
        entry.put("doc.hidden", "true");
        entry.put("customProperty", "value");
        entry.put("doc.fullName", "MyTest.MyObject");
        DocumentReference objectDocumentReference = new DocumentReference("xwiki", "MyTest", "MyObject");
        DocumentReference customClassReference = new DocumentReference("xwiki", "MyTest", "CustomClass");

        when(this.currentDocumentReferenceResolver.resolve("MyTest.MyObject"))
            .thenReturn(objectDocumentReference);
        when(this.currentDocumentReferenceResolver.resolve("MyTest.CustomClass"))
            .thenReturn(customClassReference);

        this.entryStore.getParameters().put("className", "MyTest.MyClass");
        this.entryStore.getParameters().put("customProperty_class", "MyTest.CustomClass");

        this.entryStore.save(entry);

        verify(this.modelBridge).updateAll(entry, objectDocumentReference, null,
            Map.of("customProperty", customClassReference));
    }

    @Test
    void saveWithPropertiesRequiringHTMLConversion() throws Exception
    {
        String htmlConversionProperties = "MyTest.CustomClass_0_customHTMLProperty";
        Map<String, Object> entry = Map.of(
            "doc.hidden", "true",
            "RequiresHTMLConversion", htmlConversionProperties,
            "customHTMLProperty_syntax", "xwiki/2.1",
            "customHTMLProperty_cache", "cacheValue",
            "customHTMLProperty", "<p>value</p>",
            "doc.fullName", "MyTest.MyObject"
        );

        DocumentReference objectDocumentReference = new DocumentReference("xwiki", "MyTest", "MyObject");
        DocumentReference customClassReference = new DocumentReference("xwiki", "MyTest", "CustomClass");

        when(this.currentDocumentReferenceResolver.resolve("MyTest.MyObject"))
            .thenReturn(objectDocumentReference);
        when(this.currentDocumentReferenceResolver.resolve("MyTest.CustomClass"))
            .thenReturn(customClassReference);

        this.entryStore.getParameters().put("customHTMLProperty_class", "MyTest.CustomClass");

        when(this.modelBridge.getPropertiesRequiringHTMLConversion(htmlConversionProperties, null,
            Map.of("customHTMLProperty", customClassReference), 0))
            .thenReturn(Set.of("customHTMLProperty"));

        this.entryStore.save(entry);

        verify(this.modelBridge).updateAll(eq(entry), eq(objectDocumentReference), any(),
            eq(Map.of("customHTMLProperty", customClassReference)));
    }
}
