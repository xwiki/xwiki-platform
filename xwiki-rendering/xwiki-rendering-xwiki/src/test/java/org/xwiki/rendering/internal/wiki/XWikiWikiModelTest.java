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
package org.xwiki.rendering.internal.wiki;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.reference.EntityReferenceSerializer;

/**
 * Unit tests for {@link XWikiWikiModel}.
 * 
 * @version $Id$
 * @since 2.0M1
 */
public class XWikiWikiModelTest
{
    @org.junit.Test
    public void testGetDocumentEditURLWhenNoQueryStringSpecified() throws Exception
    {
        Mockery mockery = new Mockery();
        XWikiWikiModel wikiModel = new XWikiWikiModel();

        final EntityReferenceSerializer mockEntityReferenceSerializer = mockery.mock(EntityReferenceSerializer.class);
        ReflectionUtils.setFieldValue(wikiModel, "entityReferenceSerializer", mockEntityReferenceSerializer);

        final DocumentAccessBridge mockDocumentAccessBridge = mockery.mock(DocumentAccessBridge.class);
        ReflectionUtils.setFieldValue(wikiModel, "documentAccessBridge", mockDocumentAccessBridge);

        final DocumentReference docReference = new DocumentReference("wiki", "Space", "Page");
        mockery.checking(new Expectations() {{
            oneOf(mockDocumentAccessBridge).getCurrentDocumentReference(); will(returnValue(docReference));
            oneOf(mockEntityReferenceSerializer).serialize(docReference); will(returnValue("wiki:Space.Page\u20AC"));

            // The test is here: we verify that getURL is called with the query string already encoded since getURL()
            // doesn't encode it.
            oneOf(mockDocumentAccessBridge).getURL("Space.Page\u20AC", "create", "parent=wiki%3ASpace.Page%E2%82%AC",
                "anchor");
        }});

        wikiModel.getDocumentEditURL("Space.Page\u20AC", "anchor", null);
    }
}
