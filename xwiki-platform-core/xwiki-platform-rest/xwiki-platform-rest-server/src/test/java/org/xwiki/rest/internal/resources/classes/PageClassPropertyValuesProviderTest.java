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
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rest.model.jaxb.PropertyValues;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.PageClass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.xwiki.rest.internal.resources.classes.AbstractClassPropertyValuesProvider.META_DATA_ICON;

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
    }

    @Test
    public void getValues() throws Exception
    {
        WikiReference wikiRef = new WikiReference("wiki");
        SpaceReference space1Ref = new SpaceReference("space1", wikiRef);
        SpaceReference space2Ref = new SpaceReference("space2", space1Ref);
        EntityReference pageRef = new DocumentReference("page", space2Ref);

        XWikiDocument space1Doc = mock(XWikiDocument.class);
        XWikiDocument space2Doc = mock(XWikiDocument.class);
        XWikiDocument pageDoc = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(space1Ref, this.xcontext)).thenReturn(space1Doc);
        when(this.xwiki.getDocument(space2Ref, this.xcontext)).thenReturn(space2Doc);
        when(this.xwiki.getDocument(pageRef, this.xcontext)).thenReturn(pageDoc);
        when(space1Doc.getRenderedTitle(Syntax.PLAIN_1_0, this.xcontext)).thenReturn("Space 1");
        when(space2Doc.getRenderedTitle(Syntax.PLAIN_1_0, this.xcontext)).thenReturn("Space 2");
        when(pageDoc.getRenderedTitle(Syntax.PLAIN_1_0, this.xcontext)).thenReturn("Page");

        Query query = mock(Query.class);
        when(query.execute()).thenReturn(Collections.singletonList(pageRef));

        PropertyValues values = this.provider.getValues(query, 3, "", this.pageClass);
        assertEquals(1, values.getPropertyValues().size());

        Map<String, Object> metadata = values.getPropertyValues().get(0).getMetaData();
        assertEquals("Space 1 / Space 2", metadata.get("hint"));
        assertEquals("Page", metadata.get("label"));
        assertTrue(metadata.containsKey(META_DATA_ICON));
    }

    @Test
    public void getValuesWithNonTerminalPage() throws Exception
    {
        WikiReference wikiRef = new WikiReference("wiki");
        SpaceReference space1Ref = new SpaceReference("space1", wikiRef);
        SpaceReference space2Ref = new SpaceReference("space2", space1Ref);
        EntityReference page1Ref = new DocumentReference("page", space2Ref);
        EntityReference page2Ref = new DocumentReference(XWiki.DEFAULT_SPACE_HOMEPAGE, space2Ref);

        XWikiDocument space1Doc = mock(XWikiDocument.class);
        XWikiDocument space2Doc = mock(XWikiDocument.class);
        XWikiDocument page1Doc = mock(XWikiDocument.class);
        XWikiDocument page2Doc = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(space1Ref, this.xcontext)).thenReturn(space1Doc);
        when(this.xwiki.getDocument(space2Ref, this.xcontext)).thenReturn(space2Doc);
        when(this.xwiki.getDocument(page1Ref, this.xcontext)).thenReturn(page1Doc);
        when(this.xwiki.getDocument(page2Ref, this.xcontext)).thenReturn(page2Doc);
        when(space1Doc.getRenderedTitle(Syntax.PLAIN_1_0, this.xcontext)).thenReturn("Space 1");
        when(space2Doc.getRenderedTitle(Syntax.PLAIN_1_0, this.xcontext)).thenReturn("Space 2");
        when(page1Doc.getRenderedTitle(Syntax.PLAIN_1_0, this.xcontext)).thenReturn("Page 1");
        when(page2Doc.getRenderedTitle(Syntax.PLAIN_1_0, this.xcontext)).thenReturn("Page 2");

        Query query = mock(Query.class);
        when(query.execute()).thenReturn(Arrays.asList(page1Ref, page2Ref));

        PropertyValues values = this.provider.getValues(query, 3, "", this.pageClass);
        assertEquals(2, values.getPropertyValues().size());

        Map<String, Object> metadata = values.getPropertyValues().get(0).getMetaData();
        assertEquals("Space 1 / Space 2", metadata.get("hint"));
        assertEquals("Page 1", metadata.get("label"));
        assertTrue(metadata.containsKey(META_DATA_ICON));

        metadata = values.getPropertyValues().get(1).getMetaData();
        assertEquals("Space 1", metadata.get("hint"));
        assertEquals("Space 2", metadata.get("label"));
        assertTrue(metadata.containsKey(META_DATA_ICON));
    }
}
