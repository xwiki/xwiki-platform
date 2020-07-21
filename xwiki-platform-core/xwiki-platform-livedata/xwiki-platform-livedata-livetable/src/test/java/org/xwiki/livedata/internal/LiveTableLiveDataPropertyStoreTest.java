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
package org.xwiki.livedata.internal;

import java.util.Arrays;
import java.util.Collection;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
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
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.ComputedFieldClass;
import com.xpn.xwiki.objects.classes.DateClass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LiveTableLiveDataPropertyStore}.
 * 
 * @version $Id$
 * @since 12.6RC1
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
    private ContextualLocalizationManager l10n;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private ContextualAuthorizationManager authorization;

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

        this.objectMapper.setSerializationInclusion(Include.NON_DEFAULT);
        this.propertyStore.getParameters().clear();
    }

    @Test
    void getAll() throws Exception
    {
        this.propertyStore.getParameters().put("className", "Some.Class");

        DocumentReference classReference = new DocumentReference("wiki", "Some", "Class");
        when(this.currentDocumentReferenceResolver.resolve("Some.Class")).thenReturn(classReference);
        when(this.authorization.hasAccess(Right.VIEW, classReference)).thenReturn(true);

        XWikiDocument classDocument = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(classReference, this.xcontext)).thenReturn(classDocument);

        BaseClass xclass = mock(BaseClass.class);
        when(classDocument.getXClass()).thenReturn(xclass);

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

        when(xclass.getEnabledProperties()).thenReturn(Arrays.asList(dateField, computedField));

        StringBuilder expectedClassProps = new StringBuilder();
        expectedClassProps.append("{'id':'birthdate','name':'Birthdate','description':'The date when you were born.'"
            + ",'type':'Date','sortable':true,'displayer':{'id':'html'}},");
        expectedClassProps.append("{'id':'total','name':'Total','description':'The computed total amount.',"
            + "'type':'Computed','displayer':{'id':'html'}}");

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

    @Test
    void getWithTranslationPrefix() throws Exception
    {
        this.propertyStore.getParameters().put("translationPrefix", "test.");
        when(this.l10n.getTranslationPlain("test.doc.title")).thenReturn("Title");
        when(this.l10n.getTranslationPlain("test.doc.title.hint")).thenReturn("The page title");

        LiveDataPropertyDescriptor property = this.propertyStore.get("doc.title").get();

        assertEquals("Title", property.getName());
        assertEquals("The page title", property.getDescription());
    }

    private String getExpectedDocPropsJSON()
    {
        StringBuilder json = new StringBuilder();
        json.append("{'id':'doc.name','type':'String','sortable':true,'displayer':{'id':'link'}},");
        json.append("{'id':'doc.title','type':'String','sortable':true,'displayer':{'id':'link'}},");
        json.append("{'id':'doc.space','type':'String','sortable':true,"
            + "'displayer':{'propertyHref':'doc.space_url','id':'link'}},");
        json.append("{'id':'doc.location','type':'String','sortable':true,'displayer':{'id':'html'}},");
        json.append("{'id':'doc.fullName','type':'String','sortable':true,'displayer':{'id':'link'}},");
        json.append("{'id':'doc.creationDate','type':'Date','sortable':true,'displayer':{'id':'text'}},");
        json.append("{'id':'doc.date','type':'Date','sortable':true,'displayer':{'id':'text'}},");
        json.append("{'id':'doc.creator','type':'Users','sortable':true,'displayer':{'id':'text'}},");
        json.append("{'id':'doc.author','type':'Users','sortable':true,"
            + "'displayer':{'propertyHref':'doc.author_url','id':'link'}},");
        json.append("{'id':'doc.objectCount','type':'Number','filter':{'id':''}},");
        json.append("{'id':'_images','type':'String','displayer':{'id':'html'},'filter':{'id':''}},");
        json.append("{'id':'_attachments','type':'String','displayer':{'id':'html'},'filter':{'id':''}},");
        json.append("{'id':'_actions','type':'String','displayer':{'id':'actions','actions':['edit','delete']},"
            + "'filter':{'id':''}},");
        json.append("{'id':'_avatar','type':'String','displayer':{'id':'html'},'filter':{'id':''}}");
        return json.toString().replace('\'', '"');
    }
}
