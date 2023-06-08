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
package org.xwiki.annotation.io.internal;

import java.util.Date;
import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.xwiki.annotation.Annotation;
import org.xwiki.annotation.AnnotationConfiguration;
import org.xwiki.annotation.reference.TypedStringEntityReferenceResolver;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link DefaultIOService}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultIOServiceTest
{
    private static final DocumentReference ANNOTATION_CLASS_REFERENCE =
        new DocumentReference("xwiki", "XWiki", "AnnotationClass");

    @InjectMockComponents
    private DefaultIOService ioService;

    @MockComponent
    private Execution execution;

    /**
     * Entity reference handler to resolve the reference target.
     */
    @MockComponent
    private TypedStringEntityReferenceResolver referenceResolver;

    /**
     * Default entity reference serializer to create document full names.
     */
    @MockComponent
    private EntityReferenceSerializer<String> serializer;

    /**
     * Local entity reference serializer, to create references which are robust to import / export.
     */
    @MockComponent
    @Named("local")
    private EntityReferenceSerializer<String> localSerializer;

    /**
     * The Annotation Application's configuration.
     */
    @MockComponent
    private AnnotationConfiguration configuration;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki xwiki;

    @BeforeEach
    void setUp()
    {
        when(this.execution.getContext()).thenReturn(this.executionContext);
        when(this.executionContext.getProperty("xwikicontext")).thenReturn(this.context);
        when(this.context.getWiki()).thenReturn(this.xwiki);
        when(this.configuration.getAnnotationClassReference())
            .thenReturn(ANNOTATION_CLASS_REFERENCE);
    }

    @Test
    void addAnnotation() throws Exception
    {
        String target = "XWiki.Test";
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Test");
        String serializedReference = "xwiki:XWiki.Test";

        XWikiDocument document = mock(XWikiDocument.class);
        BaseObject baseObject = mock(BaseObject.class);

        when(this.referenceResolver.resolve(target, EntityType.DOCUMENT)).thenReturn(documentReference);
        when(this.serializer.serialize(documentReference)).thenReturn(serializedReference);
        when(this.xwiki.getDocument(serializedReference, this.context)).thenReturn(document);
        when(document.newXObject(
            ANNOTATION_CLASS_REFERENCE.removeParent(ANNOTATION_CLASS_REFERENCE.extractReference(EntityType.WIKI)),
            this.context))
            .thenReturn(baseObject);

        when(baseObject.getDocumentReference()).thenReturn(documentReference);

        Annotation annotation = new Annotation("selection", "", "");
        this.ioService.addAnnotation(target, annotation);

        verify(baseObject).set(Annotation.STATE_FIELD, "SAFE", this.context);
        verify(baseObject).set(eq(Annotation.DATE_FIELD), any(Date.class), eq(this.context));
        verify(baseObject).set(Annotation.AUTHOR_FIELD, annotation.getAuthor(), this.context);
        verify(baseObject, never()).set(eq(Annotation.TARGET_FIELD), any(), any());
        verify(document).setAuthor(any());
        verify(this.xwiki).saveDocument(document, "Added annotation on \"selection\"", this.context);
    }

    @Test
    void getAnnotations() throws Exception
    {
        String target = "Space.Page";
        DocumentReference documentReference = new DocumentReference("xwiki", "Space", "Page");

        XWikiDocument document = mock(XWikiDocument.class);

        when(this.referenceResolver.resolve(target, EntityType.DOCUMENT)).thenReturn(documentReference);
        when(this.localSerializer.serialize(documentReference)).thenReturn(target);
        when(this.serializer.serialize(documentReference)).thenReturn("xwiki:Space.Page");
        when(this.xwiki.getDocument("xwiki:Space.Page", this.context)).thenReturn(document);
        BaseObject baseObject0 = mock(BaseObject.class);
        when(baseObject0.getStringValue(Annotation.TARGET_FIELD)).thenReturn("");
        BaseObject baseObject1 = mock(BaseObject.class);
        when(baseObject1.getStringValue(Annotation.TARGET_FIELD)).thenReturn("Space.Page");
        BaseObject baseObject2 = mock(BaseObject.class);
        when(baseObject2.getStringValue(Annotation.TARGET_FIELD)).thenReturn("OtherSpace.Page");
        when(document.getXObjects(ANNOTATION_CLASS_REFERENCE)).thenReturn(List.of(
            baseObject0,
            baseObject1,
            baseObject2
        ));

        when(baseObject0.getStringValue(Annotation.STATE_FIELD)).thenReturn("SAFE");
        when(baseObject1.getStringValue(Annotation.STATE_FIELD)).thenReturn("SAFE");
        when(baseObject0.getPropertyNames()).thenReturn(new String[] {});
        when(baseObject1.getPropertyNames()).thenReturn(new String[] {});

        assertEquals(2, this.ioService.getAnnotations(target).size());
    }

    @Test
    void getAnnotationNotMatchingTarget() throws Exception
    {
        String target = "Space.Page";
        DocumentReference documentReference = new DocumentReference("xwiki", "Space", "Page");

        XWikiDocument document = mock(XWikiDocument.class);

        when(this.referenceResolver.resolve(target, EntityType.DOCUMENT)).thenReturn(documentReference);
        when(this.localSerializer.serialize(documentReference)).thenReturn(target);
        when(this.serializer.serialize(documentReference)).thenReturn("xwiki:Space.Page");
        when(this.xwiki.getDocument("xwiki:Space.Page", this.context)).thenReturn(document);
        BaseObject baseObject = mock(BaseObject.class);
        when(document.getXObject(ANNOTATION_CLASS_REFERENCE, 1)).thenReturn(baseObject);
        when(baseObject.getStringValue(Annotation.TARGET_FIELD)).thenReturn("OtherSpace");

        assertNull(this.ioService.getAnnotation(target, "1"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "Space.Page" })
    void getAnnotationNotMatchingTarget(String xObjectTarget) throws Exception
    {
        String target = "Space.Page";
        DocumentReference documentReference = new DocumentReference("xwiki", "Space", "Page");

        XWikiDocument document = mock(XWikiDocument.class);

        when(this.referenceResolver.resolve(target, EntityType.DOCUMENT)).thenReturn(documentReference);
        when(this.localSerializer.serialize(documentReference)).thenReturn(target);
        when(this.serializer.serialize(documentReference)).thenReturn("xwiki:Space.Page");
        when(this.xwiki.getDocument("xwiki:Space.Page", this.context)).thenReturn(document);
        BaseObject baseObject = mock(BaseObject.class);
        when(document.getXObject(ANNOTATION_CLASS_REFERENCE, 1)).thenReturn(baseObject);
        when(baseObject.getStringValue(Annotation.TARGET_FIELD)).thenReturn(xObjectTarget);
        when(baseObject.getStringValue(Annotation.STATE_FIELD)).thenReturn("SAFE");
        when(baseObject.getPropertyNames()).thenReturn(new String[] { "target" });
        BaseProperty baseProperty = mock(BaseProperty.class);
        when(baseObject.get("target")).thenReturn(baseProperty);
        when(baseProperty.getValue()).thenReturn(xObjectTarget);

        Annotation annotation = this.ioService.getAnnotation(target, "1");
        assertEquals("Space.Page", annotation.get("target"));
    }
}
