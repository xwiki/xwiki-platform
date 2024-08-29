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
package org.xwiki.search.solr.internal.metadata;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import javax.inject.Provider;

import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.mail.GeneralMailConfiguration;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.search.solr.internal.SolrSearchCoreUtils;
import org.xwiki.search.solr.internal.api.FieldUtils;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.EmailClass;
import com.xpn.xwiki.objects.classes.PasswordClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import com.xpn.xwiki.web.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link ObjectPropertySolrMetadataExtractor}.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList({ SolrSearchCoreUtils.class, SolrLinkSerializer.class })
@ReferenceComponentList
class ObjectPropertySolrMetadataExtractorTest
{
    @InjectMockComponents
    private ObjectPropertySolrMetadataExtractor metadataExtractor;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private Execution execution;

    @MockComponent
    private GeneralMailConfiguration mailConfiguration;

    private final XWikiContext xcontext = mock(XWikiContext.class);

    /**
     * The document from which we extract the meta data.
     */
    private final XWikiDocument document = mock(XWikiDocument.class);

    private final XWikiDocument translatedDocument = mock(XWikiDocument.class, Locale.FRENCH.toString());

    private final DocumentReference documentReference =
        new DocumentReference("wiki", Arrays.asList("Path", "To", "Page"), "WebHome");

    private final DocumentReference frenchDocumentReference =
        new DocumentReference(this.documentReference, Locale.FRENCH);

    private final DocumentAuthors documentAuthors = mock(DocumentAuthors.class);

    @BeforeEach
    void setUp(MockitoComponentManager componentManager) throws Exception
    {
        // XWikiContext Provider
        when(this.contextProvider.get()).thenReturn(this.xcontext);

        // XWikiContext trough Execution
        ExecutionContext executionContext = new ExecutionContext();
        executionContext.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, this.xcontext);
        when(this.execution.getContext()).thenReturn(executionContext);

        // XWiki
        XWiki wiki = mock(XWiki.class);
        when(this.xcontext.getWiki()).thenReturn(wiki);

        // XWikiDocument
        when(wiki.getDocument(this.documentReference, this.xcontext)).thenReturn(this.document);
        when(wiki.getDocument(this.frenchDocumentReference, this.xcontext)).thenReturn(this.translatedDocument);
        when(this.document.getDocumentReference()).thenReturn(this.documentReference);
        when(this.document.isHidden()).thenReturn(false);
        when(this.document.getLocale()).thenReturn(Locale.ROOT);
        when(this.document.getRealLocale()).thenReturn(Locale.US);
        when(this.document.getAuthors()).thenReturn(this.documentAuthors);

        when(this.document.getTranslatedDocument(Locale.FRENCH, this.xcontext)).thenReturn(this.translatedDocument);
        when(this.translatedDocument.getRealLocale()).thenReturn(Locale.FRENCH);
        when(this.translatedDocument.getLocale()).thenReturn(Locale.FRENCH);
        when(this.translatedDocument.getDocumentReference()).thenReturn(this.frenchDocumentReference);
        when(this.translatedDocument.getAuthors()).thenReturn(this.documentAuthors);

        // Make sure that Utils.getComponent works (used in BaseObjectReference).
        Utils.setComponentManager(componentManager);
    }

    @ParameterizedTest
    @MethodSource("getDocumentWithPropertyParameters")
    void getDocumentWithProperty(PropertyClass propertyClass, Object value, boolean obfuscate, boolean visible)
        throws Exception
    {
        when(this.mailConfiguration.shouldObfuscate()).thenReturn(obfuscate);
        // Setup object
        BaseObject object = mock(BaseObject.class);

        // Setup property
        BaseProperty<ObjectPropertyReference> property = mock(BaseProperty.class);
        String propertyName = "property";
        when(property.getName()).thenReturn(propertyName);
        when(property.getValue()).thenReturn(value);
        when(property.getObject()).thenReturn(object);

        // Mock the class reference
        DocumentReference classReference = new DocumentReference("wiki", Arrays.asList("Path", "To"), "Class");

        BaseClass xclass = mock(BaseClass.class);
        when(object.getXClass(this.xcontext)).thenReturn(xclass);
        when(object.getFieldList()).thenReturn(List.of(property));
        when(property.getPropertyClass(this.xcontext)).thenReturn(propertyClass);
        when(object.getXClassReference()).thenReturn(classReference);
        when(object.getRelativeXClassReference())
            .thenReturn(classReference.removeParent(classReference.getWikiReference()));
        when(xclass.get(propertyName)).thenReturn(propertyClass);

        BaseObjectReference objectReference = new BaseObjectReference(classReference, 0, this.documentReference);
        ObjectPropertyReference propertyReference = new ObjectPropertyReference(propertyName, objectReference);
        when(property.getReference()).thenReturn(propertyReference);
        when(this.document.getXObjectProperty(propertyReference)).thenReturn(property);

        // Construct a property reference based on the French document for getting the document.
        BaseObjectReference frenchObjectReference =
            new BaseObjectReference(classReference, 0, this.frenchDocumentReference);
        ObjectPropertyReference frenchPropertyReference =
            new ObjectPropertyReference(propertyName, frenchObjectReference);
        when(this.document.getXObjectProperty(frenchPropertyReference)).thenReturn(property);

        SolrInputDocument solrDocument = this.metadataExtractor.getSolrDocument(frenchPropertyReference);

        // Verify the fields
        String serializedClassReference = "Path.To.Class";
        assertEquals(List.of(serializedClassReference), solrDocument.getFieldValues(FieldUtils.CLASS));
        assertEquals(0, solrDocument.getFieldValue(FieldUtils.NUMBER));
        assertEquals(propertyName, solrDocument.getFieldValue(FieldUtils.PROPERTY_NAME));
        String fieldName = FieldUtils.getFieldName(FieldUtils.PROPERTY_VALUE, null);
        String localizedFieldName = FieldUtils.getFieldName(FieldUtils.PROPERTY_VALUE, Locale.FRENCH);
        if (visible) {
            List<?> values;
            if (value instanceof List) {
                values = (List<?>) value;
            } else {
                values = Collections.singletonList(value);
            }
            assertEquals(values, solrDocument.getFieldValues(fieldName));
            assertEquals(values, solrDocument.getFieldValues(localizedFieldName));
        } else {
            assertNull(solrDocument.getFieldValue(fieldName));
            assertNull(solrDocument.getFieldValue(localizedFieldName));
        }
    }

    static Stream<Arguments> getDocumentWithPropertyParameters()
    {
        return Stream.of(
            arguments(mock(StringClass.class), "value", false, true),
            arguments(mock(EmailClass.class), "email@example.com", false, true),
            arguments(mock(EmailClass.class), "hidden@example.com", true, false),
            arguments(mock(PasswordClass.class), "passw0rd", false, false),
            arguments(mock(StaticListClass.class), List.of("red", "green"), false, true)
        );
    }

}
