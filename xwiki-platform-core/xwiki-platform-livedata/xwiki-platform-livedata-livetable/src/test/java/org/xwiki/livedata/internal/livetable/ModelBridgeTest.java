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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wysiwyg.converter.HTMLConverter;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;

import ch.qos.logback.classic.Level;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Test of {@link ModelBridge}.
 *
 * @version $Id$
 * @since 13.4RC1
 */
@ComponentTest
class ModelBridgeTest
{
    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private ModelBridge modelBridge;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    @Named("local")
    private EntityReferenceSerializer<String> localSerializer;

    @MockComponent
    private HTMLConverter htmlConverter;

    @Mock
    private XWikiContext xcontext;

    @Mock
    private XWiki xwiki;

    @Mock
    private XWikiDocument document;

    @Mock
    private BaseObject baseObject;

    @Mock
    private PropertyInterface propertyInterface;

    @Mock
    private BaseClass baseClass;

    @BeforeEach
    void setUp()
    {
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);
    }

    @Test
    void updateWhenEditIsDisallowed() throws Exception
    {
        String property = "propertyName";
        Object value = "value";
        DocumentReference documentReference = new DocumentReference("xwiki", "MyApp", "mydoc");
        DocumentReference classReference = new DocumentReference("xwiki", "MyApp", "MyClass");

        doThrow(AccessDeniedException.class).when(this.authorization).checkAccess(Right.EDIT, documentReference);

        assertThrows(AccessDeniedException.class,
            () -> this.modelBridge.update(property, value, documentReference, classReference));
        verifyNoInteractions(this.xcontextProvider);
    }

    @Test
    void updateNewDocument() throws Exception
    {
        String property = "propertyName";
        Object value = "value";
        DocumentReference documentReference = new DocumentReference("xwiki", "MyApp", "mydoc");
        DocumentReference classReference = new DocumentReference("xwiki", "MyApp", "MyClass");

        when(this.xwiki.getDocument(documentReference, this.xcontext)).thenReturn(this.document);
        when(this.document.isNew()).thenReturn(true);

        LiveDataException liveDataException = assertThrows(LiveDataException.class,
            () -> this.modelBridge.update(property, value, documentReference, classReference));
        assertEquals("We do not support updating new documents.", liveDataException.getMessage());
    }

    @Test
    void updateXClassStringField() throws Exception
    {
        String property = "propertyName";
        Object value = "value";
        DocumentReference documentReference = new DocumentReference("xwiki", "MyApp", "mydoc");
        DocumentReference classReference = new DocumentReference("xwiki", "MyApp", "MyClass");

        when(this.xwiki.getDocument(documentReference, this.xcontext)).thenReturn(this.document);
        when(this.document.getXObject(classReference, 0)).thenReturn(this.baseObject);
        // The list of properties includes the property to update. 
        when(this.baseObject.getPropertyNames()).thenReturn(new String[] { "propertyA", "propertyName", "propertyB" });
        when(this.baseObject.get(property)).thenReturn(this.propertyInterface);
        when(this.baseObject.getXClass(this.xcontext)).thenReturn(this.baseClass);

        when(this.propertyInterface.toFormString()).thenReturn("updatedValue");
        when(this.document.validate(this.xcontext)).thenReturn(true);
        when(this.document.isContentDirty()).thenReturn(true);

        Optional<Object> update = this.modelBridge.update(property, value, documentReference, classReference);
        
        assertEquals("updatedValue", update.get());
        
        verify(this.baseClass).fromMap(Collections.singletonMap("propertyName", "value"), this.baseObject);
        verify(this.xwiki).saveDocument(this.document, "LiveData update.", true, this.xcontext);
    }

    @Test
    void updateXClassListField() throws Exception
    {
        String property = "propertyName";
        Object value = Arrays.asList("a", "b");
        DocumentReference documentReference = new DocumentReference("xwiki", "MyApp", "mydoc");
        DocumentReference classReference = new DocumentReference("xwiki", "MyApp", "MyClass");

        when(this.xwiki.getDocument(documentReference, this.xcontext)).thenReturn(this.document);
        when(this.document.getXObject(classReference, 0)).thenReturn(this.baseObject);
        // The list of properties includes the property to update. 
        when(this.baseObject.getPropertyNames()).thenReturn(new String[] { "propertyA", "propertyName", "propertyB" });
        when(this.baseObject.get(property)).thenReturn(this.propertyInterface);
        when(this.baseObject.getXClass(this.xcontext)).thenReturn(this.baseClass);

        when(this.propertyInterface.toFormString()).thenReturn("updatedValue");
        when(this.document.validate(this.xcontext)).thenReturn(true);
        when(this.document.isContentDirty()).thenReturn(true);

        Optional<Object> update = this.modelBridge.update(property, value, documentReference, classReference);
        
        assertEquals("updatedValue", update.get());

        ArgumentCaptor<Map> mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(this.baseClass).fromMap(mapCaptor.capture(), eq(this.baseObject));
        
        Map properties = mapCaptor.getValue();
        assertTrue(properties.containsKey(property));
        assertArrayEquals(new String[] { "a", "b" }, (String[]) properties.get(property));

        verify(this.xwiki).saveDocument(this.document, "LiveData update.", true, this.xcontext);
    }

    @Test
    void updateDocumentHiddenFieldButNotModified() throws Exception
    {
        String property = "doc.hidden";
        Object value = "true";
        DocumentReference documentReference = new DocumentReference("xwiki", "MyApp", "mydoc");

        when(this.xwiki.getDocument(documentReference, this.xcontext)).thenReturn(this.document);

        when(this.document.isContentDirty()).thenReturn(false);
        when(this.document.isHidden()).thenReturn(true);

        Optional<Object> update = this.modelBridge.update(property, value, documentReference, null);
        assertTrue((Boolean) update.get());

        verify(this.document).setHidden(true);
        verify(this.xwiki, never()).saveDocument(this.document, "LiveData update.", true, this.xcontext);
    }

    @Test
    void updateDocumentTitleFieldButNotValid() throws Exception
    {
        String property = "doc.title";
        Object value = "newTitle";
        DocumentReference documentReference = new DocumentReference("xwiki", "MyApp", "mydoc");

        when(this.xwiki.getDocument(documentReference, this.xcontext)).thenReturn(this.document);

        when(this.document.isContentDirty()).thenReturn(true);
        when(this.document.getTitle()).thenReturn("oldTitle");
        when(this.document.validate(this.xcontext)).thenReturn(false);

        LiveDataException liveDataException = assertThrows(LiveDataException.class,
            () -> this.modelBridge.update(property, value, documentReference, null));
        assertEquals("Document not validated.", liveDataException.getMessage());

        verify(this.document).setTitle("newTitle");
        verify(this.xwiki, never()).saveDocument(this.document, "LiveData update.", true, this.xcontext);
    }

    @Test
    void updateDocumentContentField() throws Exception
    {
        String property = "doc.content";
        Object value = "newContent";
        DocumentReference documentReference = new DocumentReference("xwiki", "MyApp", "mydoc");

        when(this.xwiki.getDocument(documentReference, this.xcontext)).thenReturn(this.document);

        when(this.document.isContentDirty()).thenReturn(true);
        when(this.document.getContent()).thenReturn("oldContent");
        when(this.document.validate(this.xcontext)).thenReturn(true);

        Optional<Object> update = this.modelBridge.update(property, value, documentReference, null);
        assertEquals("oldContent", update.get());
        verify(this.document).setContent("newContent");
        verify(this.xwiki).saveDocument(this.document, "LiveData update.", true, this.xcontext);
    }

    @Test
    void updateXObjectNotFound() throws Exception
    {
        String property = "property1";
        Object value = "newContent";
        DocumentReference documentReference = new DocumentReference("xwiki", "MyApp", "mydoc");
        DocumentReference classReference = new DocumentReference("xwiki", "MyApp", "MyClass");

        when(this.xwiki.getDocument(documentReference, this.xcontext)).thenReturn(this.document);

        LiveDataException liveDataException = assertThrows(LiveDataException.class,
            () -> this.modelBridge.update(property, value, documentReference, classReference));

        assertEquals("XObject [xwiki:MyApp.MyClass] not found at index [0] in [document]",
            liveDataException.getMessage());
        verify(this.xwiki, never()).saveDocument(this.document, "LiveData update.", true, xcontext);
    }

    @Test
    void updateAll() throws Exception
    {
        Map<String, Object> entry = new HashMap<>();
        entry.put("doc.hidden", "false");
        entry.put("field", 55);
        DocumentReference documentReference = new DocumentReference("xwiki", "MyTest", "MyDoc");
        DocumentReference classReference = new DocumentReference("xwiki", "MyTest", "MyClass");

        when(this.xwiki.getDocument(documentReference, this.xcontext)).thenReturn(this.document);
        when(this.document.getXObject(classReference, 0)).thenReturn(this.baseObject);
        when(this.baseObject.getXClass(this.xcontext)).thenReturn(this.baseClass);
        when(this.baseObject.getPropertyNames()).thenReturn(new String[] { "field" });
        when(this.baseObject.get("field")).thenReturn(this.propertyInterface);

        this.modelBridge.updateAll(entry, documentReference, classReference);
        verify(this.baseClass).fromMap(Collections.singletonMap("field", 55), this.baseObject);
        verify(this.document).setHidden(false);

        verify(this.authorization).checkAccess(Right.EDIT, documentReference);
    }

    @Test
    void updateAllNewDocument() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("xwiki", "MyTest", "MyDoc");
        DocumentReference classReference = new DocumentReference("xwiki", "MyTest", "MyClass");

        when(this.xwiki.getDocument(documentReference, this.xcontext)).thenReturn(this.document);
        when(this.document.isNew()).thenReturn(true);

        LiveDataException liveDataException = assertThrows(LiveDataException.class,
            () -> this.modelBridge.updateAll(new HashMap<>(), documentReference, classReference));

        assertEquals("We do not support updating new documents.", liveDataException.getMessage());

        verify(this.xwiki, never()).saveDocument(this.document, "LiveData update.", true, xcontext);
    }

    @Test
    void updatePropertyNotFound() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("xwiki", "MyTest", "MyDoc");
        DocumentReference classReference = new DocumentReference("xwiki", "MyTest", "MyClass");
        when(this.xwiki.getDocument(documentReference, this.xcontext)).thenReturn(this.document);
        this.modelBridge.update("doc.p1", "v1", documentReference, classReference);
        assertEquals(1, this.logCapture.size());
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
        assertEquals("Unknown property [p1]. Document [document] will not be updated with value [v1].",
            this.logCapture.getMessage(0));
    }

    @Test
    void updatePropertyWithHtmlConversion() throws Exception
    {
        Map<String, Object> entry = new HashMap<>();
        entry.put("doc.hidden", "false");
        entry.put("field_known", "to convert");
        entry.put("field_known_syntax", "xwiki/2.0");
        entry.put("RequiresHTMLConversion", "MyTest.MyClass_0_field_known,MyTest.MyClass_0_field_unknown");
        DocumentReference documentReference = new DocumentReference("xwiki", "MyTest", "MyDoc");
        DocumentReference classReference = new DocumentReference("xwiki", "MyTest", "MyClass");

        when(this.xwiki.getDocument(documentReference, this.xcontext)).thenReturn(this.document);
        when(this.document.getXObject(classReference, 0)).thenReturn(this.baseObject);
        when(this.baseObject.getXClass(this.xcontext)).thenReturn(this.baseClass);
        when(this.baseObject.getPropertyNames()).thenReturn(new String[] { "field_known" });
        when(this.baseObject.get("field_known")).thenReturn(this.propertyInterface);
        when(this.localSerializer.serialize(classReference)).thenReturn("MyTest.MyClass");
        when(this.htmlConverter.fromHTML("to convert", "xwiki/2.0")).thenReturn("converted value");

        this.modelBridge.updateAll(entry, documentReference, classReference);
        verify(this.baseClass).fromMap(Collections.singletonMap("field_known", "converted value"), this.baseObject);
        verify(this.document).setHidden(false);

        verify(this.authorization).checkAccess(Right.EDIT, documentReference);
    }
}
