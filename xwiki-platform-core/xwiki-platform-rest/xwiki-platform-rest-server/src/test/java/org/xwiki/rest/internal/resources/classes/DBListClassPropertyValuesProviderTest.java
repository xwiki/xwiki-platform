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
package org.xwiki.rest.internal.resources.classes;

import java.util.Arrays;
import java.util.HashMap;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.ClassPropertyReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryBuilder;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryParameter;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.PropertyValue;
import org.xwiki.rest.model.jaxb.PropertyValues;
import org.xwiki.rest.resources.classes.ClassPropertyValuesProvider;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.DBListClass;
import com.xpn.xwiki.objects.classes.DateClass;
import com.xpn.xwiki.objects.classes.ListClass;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DBListClassPropertyValuesProvider}.
 * 
 * @version $Id$
 * @since 9.8RC1
 */
public class DBListClassPropertyValuesProviderTest
{
    @Rule
    public MockitoComponentMockingRule<ClassPropertyValuesProvider> mocker =
        new MockitoComponentMockingRule<ClassPropertyValuesProvider>(DBListClassPropertyValuesProvider.class);

    private Provider<XWikiContext> xcontextProvider;

    private DocumentReference classReference = new DocumentReference("wiki", "Some", "Class");

    private DBListClass dbListClass = new DBListClass();

    private EntityReferenceSerializer<String> entityReferenceSerializer;

    private AuthorExecutor authorExecutor;

    private QueryBuilder<ListClass> usedValuesQueryBuilder;

    @Before
    public void configure() throws Exception
    {
        this.xcontextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        XWikiContext xcontext = mock(XWikiContext.class);
        XWiki xwiki = mock(XWiki.class);
        XWikiDocument classDocument = mock(XWikiDocument.class);
        BaseClass xclass = mock(BaseClass.class);

        when(this.xcontextProvider.get()).thenReturn(xcontext);
        when(xcontext.getWiki()).thenReturn(xwiki);
        when(xwiki.getDocument(new ClassPropertyReference("status", this.classReference), xcontext))
            .thenReturn(classDocument);
        when(xwiki.getDocument(new ClassPropertyReference("date", this.classReference), xcontext))
            .thenReturn(classDocument);
        when(xwiki.getDocument(new ClassPropertyReference("category", this.classReference), xcontext))
            .thenReturn(classDocument);
        when(classDocument.getXClass()).thenReturn(xclass);
        when(xclass.get("category")).thenReturn(dbListClass);
        when(xclass.get("date")).thenReturn(new DateClass());

        XWikiDocument ownerDocument = mock(XWikiDocument.class);
        DocumentReference authorReference = new DocumentReference("wiki", "Users", "alice");
        dbListClass.setOwnerDocument(ownerDocument);
        when(ownerDocument.getAuthorReference()).thenReturn(authorReference);

        this.entityReferenceSerializer = this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);
        this.authorExecutor = this.mocker.getInstance(AuthorExecutor.class);
        DefaultParameterizedType listQueryBuilderType =
            new DefaultParameterizedType(null, QueryBuilder.class, ListClass.class);
        this.usedValuesQueryBuilder = this.mocker.getInstance(listQueryBuilderType, "usedValues");
    }

    @Test
    public void getValuesForMissingProperty() throws Exception
    {
        ClassPropertyReference propertyReference = new ClassPropertyReference("status", this.classReference);
        when(this.entityReferenceSerializer.serialize(propertyReference)).thenReturn("status reference");
        try {
            this.mocker.getComponentUnderTest().getValues(propertyReference, 0);
            fail();
        } catch (XWikiRestException expected) {
            assertEquals("Property [status reference] not found.", expected.getMessage());
        }
    }

    @Test
    public void getValuesForWrongProperty() throws Exception
    {
        ClassPropertyReference propertyReference = new ClassPropertyReference("date", this.classReference);
        when(this.entityReferenceSerializer.serialize(propertyReference)).thenReturn("status reference");
        try {
            this.mocker.getComponentUnderTest().getValues(propertyReference, 0);
            fail();
        } catch (XWikiRestException expected) {
            assertEquals("This [status reference] is not a Database List property.", expected.getMessage());
        }
    }

    @Test
    public void getValuesAllowed() throws Exception
    {
        ClassPropertyReference propertyReference = new ClassPropertyReference("category", this.classReference);
        DocumentReference authorReference = this.dbListClass.getOwnerDocument().getAuthorReference();
        PropertyValues values = new PropertyValues();
        when(this.authorExecutor.call(any(), eq(authorReference))).thenReturn(values);

        assertSame(values, this.mocker.getComponentUnderTest().getValues(propertyReference, 3));

        assertSame(values, this.mocker.getComponentUnderTest().getValues(propertyReference, 0, "text"));
    }

    @Test
    public void getValuesMixedWithoutUsed() throws Exception
    {
        ClassPropertyReference propertyReference = new ClassPropertyReference("category", this.classReference);
        DocumentReference authorReference = this.dbListClass.getOwnerDocument().getAuthorReference();
        PropertyValues values = new PropertyValues();
        values.getPropertyValues().add(new PropertyValue());
        when(this.authorExecutor.call(any(), eq(authorReference))).thenReturn(values);

        assertSame(values, this.mocker.getComponentUnderTest().getValues(propertyReference, 1, "foo"));
        assertEquals(1, values.getPropertyValues().size());

        verify(this.usedValuesQueryBuilder, never()).build(any());
    }

    @Test
    public void getValuesMixedWithUsed() throws Exception
    {
        ClassPropertyReference propertyReference = new ClassPropertyReference("category", this.classReference);
        DocumentReference authorReference = this.dbListClass.getOwnerDocument().getAuthorReference();

        PropertyValues values = new PropertyValues();
        PropertyValue red = new PropertyValue();
        red.setValue("red");
        red.setMetaData(new HashMap<String, Object>());
        red.getMetaData().put("label", "Red");
        values.getPropertyValues().add(red);
        when(this.authorExecutor.call(any(), eq(authorReference))).thenReturn(values);

        Query query = mock(Query.class);
        QueryParameter queryParameter = mock(QueryParameter.class);
        when(this.usedValuesQueryBuilder.build(dbListClass)).thenReturn(query);
        when(query.bindValue("text")).thenReturn(queryParameter);
        when(queryParameter.anyChars()).thenReturn(queryParameter);
        when(queryParameter.literal("bar")).thenReturn(queryParameter);
        when(query.execute()).thenReturn(Arrays.asList(new Object[] {"blue", 21L}, new Object[] {"red", 17L}));

        assertSame(values, this.mocker.getComponentUnderTest().getValues(propertyReference, 3, "bar"));

        verify(query).setLimit(2);
        verify(query).addFilter(this.mocker.getInstance(QueryFilter.class, "text"));
        verify(queryParameter, times(2)).anyChars();

        assertEquals(2, values.getPropertyValues().size());
        assertEquals("red", values.getPropertyValues().get(0).getValue());
        assertEquals(17L, values.getPropertyValues().get(0).getMetaData().get("count"));
        assertEquals("blue", values.getPropertyValues().get(1).getValue());
        assertEquals(21L, values.getPropertyValues().get(1).getMetaData().get("count"));
    }
}
