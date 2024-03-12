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
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.localization.ContextualLocalizationManager;
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

    @MockComponent
    private ContextualLocalizationManager localizationManager;

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

        // We only mock this one to check the fallback
        when(this.localizationManager.getTranslationPlain("rightsmanager.edit")).thenReturn("Edit right");

        when(xclass.getEnabledProperties()).thenReturn(Arrays.asList(dateField, computedField, listField, levelsField));

        LiveDataPropertyDescriptor descriptor0 = new LiveDataPropertyDescriptor();
        descriptor0.setId("doc.title");

        LiveDataPropertyDescriptor descriptor1 = new LiveDataPropertyDescriptor();
        descriptor1.setId("birthdate");
        descriptor1.setName("Birthdate");
        descriptor1.setDescription("The date when you were born.");
        descriptor1.setType("Date");
        LiveDataPropertyDescriptor.DisplayerDescriptor displayer1 =
            new LiveDataPropertyDescriptor.DisplayerDescriptor();
        displayer1.setId("xObjectProperty");
        descriptor1.setDisplayer(displayer1);
        LiveDataPropertyDescriptor.FilterDescriptor filter1 = new LiveDataPropertyDescriptor.FilterDescriptor();
        filter1.setId("date");
        filter1.setParameter("dateFormat", "h:mm a");
        descriptor1.setFilter(filter1);

        LiveDataPropertyDescriptor descriptor2 = new LiveDataPropertyDescriptor();
        descriptor2.setId("total");
        descriptor2.setName("Total");
        descriptor2.setDescription("The computed total amount.");
        descriptor2.setType("Computed");
        LiveDataPropertyDescriptor.DisplayerDescriptor displayer2 =
            new LiveDataPropertyDescriptor.DisplayerDescriptor();
        displayer2.setId("xObjectProperty");
        descriptor2.setDisplayer(displayer2);

        LiveDataPropertyDescriptor descriptor3 = new LiveDataPropertyDescriptor();
        descriptor3.setId("status");
        descriptor3.setName("Status");
        descriptor3.setDescription("The status.");
        descriptor3.setType("List");
        descriptor3.setSortable(false);
        LiveDataPropertyDescriptor.DisplayerDescriptor displayer3 =
            new LiveDataPropertyDescriptor.DisplayerDescriptor();
        displayer3.setId("xObjectProperty");
        descriptor3.setDisplayer(displayer3);
        LiveDataPropertyDescriptor.FilterDescriptor filter3 = new LiveDataPropertyDescriptor.FilterDescriptor();
        filter3.setId("list");
        LiveDataPropertyDescriptor.OperatorDescriptor operator1 = new LiveDataPropertyDescriptor.OperatorDescriptor();
        operator1.setId("empty");
        LiveDataPropertyDescriptor.OperatorDescriptor operator2 = new LiveDataPropertyDescriptor.OperatorDescriptor();
        operator2.setId("equals");
        filter3.setOperators(List.of(operator1, operator2));
        filter3.setParameter("searchURL",
            "/xwiki/rest/wikis/wiki/classes/Some.Class/properties/status/values?fp={encodedQuery}");
        descriptor3.setFilter(filter3);

        LiveDataPropertyDescriptor descriptor4 = new LiveDataPropertyDescriptor();
        descriptor4.setId("levels");
        descriptor4.setType("Levels");
        LiveDataPropertyDescriptor.DisplayerDescriptor displayer4 =
            new LiveDataPropertyDescriptor.DisplayerDescriptor();
        displayer4.setId("xObjectProperty");
        descriptor4.setDisplayer(displayer4);
        LiveDataPropertyDescriptor.FilterDescriptor filter4 = new LiveDataPropertyDescriptor.FilterDescriptor();
        filter4.setId("list");
        filter4.setParameter("options", List.of(
            Map.of("value", "edit", "label", "Edit right"),
            Map.of("value", "delete", "label", "delete")
        ));
        descriptor4.setFilter(filter4);
        Collection<LiveDataPropertyDescriptor> properties = this.propertyStore.get();

        assertEquals(List.of(descriptor0, descriptor1, descriptor2, descriptor3, descriptor4), properties);
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
