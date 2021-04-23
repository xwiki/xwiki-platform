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
import java.util.Collection;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.StringListProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.ComputedFieldClass;
import com.xpn.xwiki.objects.classes.DateClass;
import com.xpn.xwiki.objects.classes.LevelsClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.web.XWikiRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LiveTableLiveDataPropertyStore}.
 * 
 * @version $Id$
 * @since 12.10
 */
@ComponentTest
class LiveTableLiveDataPropertyStoreTest
{
    @InjectMockComponents
    private LiveTableLiveDataPropertyStore propertyStore;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @MockComponent
    @Named("liveTable")
    private Provider<LiveDataConfiguration> defaultConfigProvider;

    @MockComponent
    @Named("local")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    @Mock
    private XWikiContext xcontext;

    @Mock
    private XWiki xwiki;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void before()
    {
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);

        XWikiRequest request = mock(XWikiRequest.class);
        when(this.xcontext.getRequest()).thenReturn(request);
        when(request.getContextPath()).thenReturn("/xwiki");

        this.objectMapper.setSerializationInclusion(Include.NON_DEFAULT);
        this.propertyStore.getParameters().clear();

        LiveDataPropertyDescriptor docTitle = new LiveDataPropertyDescriptor();
        docTitle.setId("doc.title");

        LiveDataConfiguration defaultConfig = new LiveDataConfiguration();
        defaultConfig.initialize();
        defaultConfig.getMeta().getPropertyDescriptors().add(docTitle);

        when(this.defaultConfigProvider.get()).thenReturn(defaultConfig);
    }

    @Test
    void getAll() throws Exception
    {
        this.propertyStore.getParameters().put("className", "Some.Class");

        DocumentReference classReference = new DocumentReference("wiki", "Some", "Class");
        when(this.currentDocumentReferenceResolver.resolve("Some.Class")).thenReturn(classReference);
        when(this.localEntityReferenceSerializer.serialize(classReference)).thenReturn("Some.Class");
        when(this.authorization.hasAccess(Right.VIEW, classReference)).thenReturn(true);

        XWikiDocument classDocument = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(classReference, this.xcontext)).thenReturn(classDocument);

        BaseClass xclass = mock(BaseClass.class);
        when(classDocument.getXClass()).thenReturn(xclass);
        when(xclass.getDocumentReference()).thenReturn(classReference);

        ComputedFieldClass computedField = mock(ComputedFieldClass.class);
        when(computedField.getName()).thenReturn("total");
        when(computedField.getTranslatedPrettyName(this.xcontext)).thenReturn("Total");
        when(computedField.getHint()).thenReturn("The computed total amount.");
        when(computedField.getClassType()).thenReturn("Computed");

        DateClass dateField = mock(DateClass.class);
        when(dateField.getName()).thenReturn("birthdate");
        when(dateField.getTranslatedPrettyName(this.xcontext)).thenReturn("Birthdate");
        when(dateField.getHint()).thenReturn("The date when you were born.");
        when(dateField.getClassType()).thenReturn("Date");
        when(dateField.getDateFormat()).thenReturn("h:mm a");

        StaticListClass listField = mock(StaticListClass.class);
        when(listField.getName()).thenReturn("status");
        when(listField.getTranslatedPrettyName(this.xcontext)).thenReturn("Status");
        when(listField.getHint()).thenReturn("The status.");
        when(listField.getClassType()).thenReturn("List");
        when(listField.newProperty()).thenReturn(new StringListProperty());
        when(listField.isMultiSelect()).thenReturn(true);
        when(listField.getObject()).thenReturn(xclass);

        LevelsClass levelsField = mock(LevelsClass.class);
        when(levelsField.getName()).thenReturn("levels");
        when(levelsField.getClassType()).thenReturn("Levels");
        when(levelsField.getList(this.xcontext)).thenReturn(Arrays.asList("edit", "delete"));

        when(xclass.getEnabledProperties()).thenReturn(Arrays.asList(dateField, computedField, listField, levelsField));

        StringBuilder expectedClassProps = new StringBuilder();
        expectedClassProps.append("{'id':'birthdate','name':'Birthdate','description':'The date when you were born.'"
            + ",'type':'Date','displayer':{'id':'xClassProperty'},'filter':{'id':'date','dateFormat':'h:mm a'}},");
        expectedClassProps.append("{'id':'total','name':'Total','description':'The computed total amount.',"
            + "'type':'Computed','displayer':{'id':'xClassProperty'}},");
        expectedClassProps.append("{'id':'status','name':'Status','description':'The status.',"
            + "'type':'List','sortable':false,'displayer':{'id':'xClassProperty'},"
            + "'filter':{'id':'list','operators':[{'id':'equals'}],"
            + "'searchURL':'/xwiki/rest/wikis/wiki/classes/Some.Class/properties/status/values?fp={encodedQuery}'}},");
        expectedClassProps.append("{'id':'levels','type':'Levels','displayer':{'id':'xClassProperty'},"
            + "'filter':{'id':'list','options':['edit','delete']}}");

        Collection<LiveDataPropertyDescriptor> properties = this.propertyStore.get();

        String expectedJSON =
            "[" + getExpectedDocPropsJSON() + "," + expectedClassProps.toString().replace('\'', '"') + "]";
        assertEquals(expectedJSON, this.objectMapper.writeValueAsString(properties));
    }

    @Test
    void getNoClassName() throws Exception
    {
        Collection<LiveDataPropertyDescriptor> properties = this.propertyStore.get();

        String expectedJSON = "[" + getExpectedDocPropsJSON() + "]";
        assertEquals(expectedJSON, this.objectMapper.writeValueAsString(properties));

        verify(this.xcontextProvider, never()).get();
    }

    @Test
    void getWithAccessDenied() throws Exception
    {
        this.propertyStore.getParameters().put("className", "Some.Class");
        DocumentReference classReference = new DocumentReference("wiki", "Some", "Class");
        when(this.currentDocumentReferenceResolver.resolve("Some.Class")).thenReturn(classReference);

        Collection<LiveDataPropertyDescriptor> properties = this.propertyStore.get();

        String expectedJSON = "[" + getExpectedDocPropsJSON() + "]";
        assertEquals(expectedJSON, this.objectMapper.writeValueAsString(properties));

        verify(this.xcontextProvider, never()).get();
    }

    private String getExpectedDocPropsJSON() throws Exception
    {
        String docPropsJSON =
            this.objectMapper.writeValueAsString(this.defaultConfigProvider.get().getMeta().getPropertyDescriptors());
        // Remove the array wrapper because we want to concatenate other properties.
        return docPropsJSON.substring(1, docPropsJSON.length() - 1);
    }
}
