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

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.ClassPropertyReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.PropertyValues;
import org.xwiki.rest.resources.classes.ClassPropertyValuesProvider;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DefaultClassPropertyValuesProvider}.
 * 
 * @version $Id$
 * @since 9.8RC1
 */
public class DefaultClassPropertyValuesProviderTest
{
    @Rule
    public MockitoComponentMockingRule<ClassPropertyValuesProvider> mocker =
        new MockitoComponentMockingRule<ClassPropertyValuesProvider>(DefaultClassPropertyValuesProvider.class);

    private Provider<XWikiContext> xcontextProvider;

    private DocumentReference classReference = new DocumentReference("wiki", "Some", "Class");

    @Before
    public void configure() throws Exception
    {
        this.xcontextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        XWikiContext xcontext = mock(XWikiContext.class);
        XWiki xwiki = mock(XWiki.class);
        XWikiDocument classDocument = mock(XWikiDocument.class);
        BaseClass xclass = mock(BaseClass.class);
        PropertyClass propertyClass = mock(PropertyClass.class);

        when(this.xcontextProvider.get()).thenReturn(xcontext);
        when(xcontext.getWiki()).thenReturn(xwiki);
        when(xwiki.getDocument((EntityReference) this.classReference, xcontext)).thenReturn(classDocument);
        when(classDocument.getXClass()).thenReturn(xclass);
        when(xclass.get("category")).thenReturn(propertyClass);
        when(propertyClass.getClassType()).thenReturn("DBList");
    }

    @Test
    public void getValues() throws Exception
    {
        ClassPropertyReference propertyReference = new ClassPropertyReference("category", this.classReference);
        ClassPropertyValuesProvider dbListValuesProvider = mock(ClassPropertyValuesProvider.class);
        ComponentManager contextComponentManager = this.mocker.getInstance(ComponentManager.class, "context");
        PropertyValues values = new PropertyValues();

        when(contextComponentManager.getInstance(ClassPropertyValuesProvider.class, "DBList"))
            .thenReturn(dbListValuesProvider);
        when(dbListValuesProvider.getValues(propertyReference, 13, "one", "two")).thenReturn(values);

        assertSame(values, this.mocker.getComponentUnderTest().getValues(propertyReference, 13, "one", "two"));
    }

    @Test
    public void getValuesForMissingProperty() throws Exception
    {
        ClassPropertyReference propertyReference = new ClassPropertyReference("status", this.classReference);
        EntityReferenceSerializer<String> entityReferenceSerializer =
            this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);
        when(entityReferenceSerializer.serialize(propertyReference)).thenReturn("status reference");

        try {
            this.mocker.getComponentUnderTest().getValues(propertyReference, 13);
            fail();
        } catch (XWikiRestException e) {
            assertEquals("No such property [status reference].", e.getMessage());
        }
    }

    @Test
    public void getValuesWithMissingProvider() throws Exception
    {
        ClassPropertyReference propertyReference = new ClassPropertyReference("category", this.classReference);
        ComponentManager contextComponentManager = this.mocker.getInstance(ComponentManager.class, "context");
        when(contextComponentManager.getInstance(ClassPropertyValuesProvider.class, "DBList"))
            .thenThrow(new ComponentLookupException("Component not found."));

        try {
            this.mocker.getComponentUnderTest().getValues(propertyReference, 13, "one");
            fail();
        } catch (XWikiRestException e) {
            assertEquals("There's no value provider registered for the [DBList] property type.", e.getMessage());
        }
    }
}
