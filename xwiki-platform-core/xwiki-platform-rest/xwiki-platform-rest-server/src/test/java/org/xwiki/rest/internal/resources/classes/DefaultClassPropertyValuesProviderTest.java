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

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.ClassPropertyReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.PropertyValues;
import org.xwiki.rest.resources.classes.ClassPropertyValuesProvider;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultClassPropertyValuesProvider}.
 *
 * @version $Id$
 * @since 9.8RC1
 */
@ComponentTest
class DefaultClassPropertyValuesProviderTest
{
    @InjectMockComponents
    private DefaultClassPropertyValuesProvider provider;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    @Named("context")
    private ComponentManager contextComponentManager;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    private final DocumentReference classReference = new DocumentReference("wiki", "Some", "Class");

    @BeforeEach
    void configure() throws Exception
    {
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
    void getValues() throws Exception
    {
        ClassPropertyReference propertyReference = new ClassPropertyReference("category", this.classReference);
        ClassPropertyValuesProvider dbListValuesProvider = mock(ClassPropertyValuesProvider.class);
        PropertyValues values = new PropertyValues();

        when(this.contextComponentManager.getInstance(ClassPropertyValuesProvider.class, "DBList"))
            .thenReturn(dbListValuesProvider);
        when(dbListValuesProvider.getValues(propertyReference, 13, "one", "two")).thenReturn(values);

        assertSame(values, this.provider.getValues(propertyReference, 13, "one", "two"));
    }

    @Test
    void getValuesForMissingProperty()
    {
        ClassPropertyReference propertyReference = new ClassPropertyReference("status", this.classReference);
        when(this.entityReferenceSerializer.serialize(propertyReference)).thenReturn("status reference");

        XWikiRestException expected = assertThrows(XWikiRestException.class,
            () -> this.provider.getValues(propertyReference, 13));
        assertEquals("No such property [status reference].", expected.getMessage());
    }

    @Test
    void getValuesWithMissingProvider() throws Exception
    {
        ClassPropertyReference propertyReference = new ClassPropertyReference("category", this.classReference);
        when(this.contextComponentManager.getInstance(ClassPropertyValuesProvider.class, "DBList"))
            .thenThrow(new ComponentLookupException("Component not found."));

        XWikiRestException expected = assertThrows(XWikiRestException.class,
            () -> this.provider.getValues(propertyReference, 13, "one"));
        assertEquals("There's no value provider registered for the [DBList] property type.", expected.getMessage());
    }
}
