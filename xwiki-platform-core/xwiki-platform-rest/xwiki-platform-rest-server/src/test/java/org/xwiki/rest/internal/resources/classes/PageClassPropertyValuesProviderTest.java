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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rest.model.jaxb.PropertyValues;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.objects.classes.PageClass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.xwiki.rest.internal.resources.classes.AbstractClassPropertyValuesProvider.META_DATA_ICON_META_DATA;

/**
 * Unit tests for {@link PageClassPropertyValuesProvider}.
 *
 * @version $Id$
 * @since 10.6
 */
@ComponentTest
public class PageClassPropertyValuesProviderTest extends AbstractListClassPropertyValuesProviderTest
{
    @InjectMockComponents
    private PageClassPropertyValuesProvider provider;

    @MockComponent
    private PageClass pageClass;

    @BeforeEach
    public void configure() throws Exception
    {
        super.configure();

        when(this.pageClass.getOwnerDocument()).thenReturn(this.classDocument);
        when(this.xcontext.getWiki().getDocument(any(DocumentReference.class), eq(this.xcontext)))
            .thenReturn(this.classDocument);
        when(this.classDocument.getRenderedTitle(Syntax.PLAIN_1_0, this.xcontext)).thenReturn("Document");
    }

    @Test
    public void getValues() throws Exception
    {
        List<String> spaces = Arrays.asList("space1", "space2");
        DocumentReference documentReference = new DocumentReference("wiki", spaces, "page");

        Query query = mock(Query.class);
        when(query.execute()).thenReturn(Collections.singletonList(documentReference));

        PropertyValues values = this.provider.getValues(query, 3, "", this.pageClass);
        assertEquals(1, values.getPropertyValues().size());

        Map<String, Object> metadata = values.getPropertyValues().get(0).getMetaData();
        assertEquals("space1 / space2", metadata.get("hint"));
        assertEquals("Document", metadata.get("label"));
        assertTrue(metadata.containsKey(META_DATA_ICON_META_DATA));
    }

    @Test
    public void getValuesWithNonTerminalPage() throws Exception
    {
        List<String> spaces = Arrays.asList("space1", "space2");
        DocumentReference documentReference1 = new DocumentReference("wiki", spaces, "page");
        DocumentReference documentReference2 = new DocumentReference("wiki", spaces, XWiki.DEFAULT_SPACE_HOMEPAGE);

        Query query = mock(Query.class);
        when(query.execute()).thenReturn(Arrays.asList(documentReference1, documentReference2));

        PropertyValues values = this.provider.getValues(query, 3, "", this.pageClass);
        assertEquals(2, values.getPropertyValues().size());

        Map<String, Object> metadata = values.getPropertyValues().get(0).getMetaData();
        assertEquals("space1 / space2", metadata.get("hint"));
        assertEquals("Document", metadata.get("label"));
        assertTrue(metadata.containsKey(META_DATA_ICON_META_DATA));

        metadata = values.getPropertyValues().get(1).getMetaData();
        assertEquals("space1", metadata.get("hint"));
        assertEquals("Document", metadata.get("label"));
        assertTrue(metadata.containsKey(META_DATA_ICON_META_DATA));
    }
}
