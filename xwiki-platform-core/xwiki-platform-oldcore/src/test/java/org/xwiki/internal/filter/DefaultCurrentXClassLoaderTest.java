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
package org.xwiki.internal.filter;

import jakarta.inject.Named;
import jakarta.inject.Provider;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultCurrentXClassLoader}.
 */
@ComponentTest
class DefaultCurrentXClassLoaderTest
{
    @InjectMockComponents
    private DefaultCurrentXClassLoader defaultCurrentXClassLoader;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> stringDocumentReferenceResolver;

    @Test
    void getXClassPropertyType() throws XWikiException
    {
        DocumentReference classReference = new DocumentReference("xwiki", "Space", "MyClass");
        when(this.stringDocumentReferenceResolver.resolve("myclass")).thenReturn(classReference);

        XWikiContext context = mock(XWikiContext.class);
        when(contextProvider.get()).thenReturn(context);

        XWiki xWiki = mock(XWiki.class);
        when(context.getWiki()).thenReturn(xWiki);
        XWikiDocument document = mock(XWikiDocument.class);
        when(xWiki.getDocument(classReference, context)).thenReturn(document);
        when(document.isNew()).thenReturn(true);
        assertNull(this.defaultCurrentXClassLoader.getXClassPropertyType("myclass", "myprop"));

        when(document.isNew()).thenReturn(false);
        BaseClass baseClass = mock(BaseClass.class);
        when(document.getXClass()).thenReturn(baseClass);
        PropertyInterface property = mock(PropertyInterface.class);
        when(baseClass.safeget("myprop")).thenReturn(property);
        when(property.getPropertyType()).thenReturn("Date");
        assertEquals("Date", this.defaultCurrentXClassLoader.getXClassPropertyType("myclass", "myprop"));
    }
}