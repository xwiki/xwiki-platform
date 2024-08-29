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

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.BooleanClass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PropertyTypeSupplier}.
 * 
 * @version $Id$
 * @since 12.10
 */
@ComponentTest
public class PropertyTypeSupplierTest
{
    @InjectMockComponents
    private PropertyTypeSupplier propertyTypeSupplier;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @Mock
    private XWikiContext xcontext;

    @MockComponent
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    private DocumentReference classReference = new DocumentReference("test", Arrays.asList("Path", "To"), "Class");

    @Mock
    private XWikiDocument classDocument;

    @Mock
    private BaseClass xclass;

    @BeforeEach
    void before() throws Exception
    {
        XWiki wiki = mock(XWiki.class);
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getWiki()).thenReturn(wiki);

        when(this.currentDocumentReferenceResolver.resolve("Path.To.Class")).thenReturn(this.classReference);
        when(this.xcontext.getWiki().getDocument(this.classReference, this.xcontext)).thenReturn(this.classDocument);
        when(this.classDocument.getXClass()).thenReturn(this.xclass);
    }

    @Test
    void getPropertyType()
    {
        BooleanClass booleanProperty = new BooleanClass();
        when(this.xclass.get("foo")).thenReturn(booleanProperty);

        assertEquals("Boolean", this.propertyTypeSupplier.getPropertyType("foo", "Path.To.Class"));
    }

    @Test
    void getPropertyTypeForMissingProperty()
    {
        assertNull(this.propertyTypeSupplier.getPropertyType("foo", "Path.To.Class"));
    }

    @Test
    void getPropertyTypeWithFailure() throws Exception
    {
        when(this.xcontext.getWiki().getDocument(this.classReference, this.xcontext))
            .thenThrow(new XWikiException(0, 0, "expected failure"));

        assertNull(this.propertyTypeSupplier.getPropertyType("foo", "Path.To.Class"));

        assertEquals(
            "Failed to read the type of property [foo] from [Path.To.Class]. "
                + "Root cause is [XWikiException: Error number 0 in 0: expected failure].",
            this.logCapture.getMessage(0));
    }
}
