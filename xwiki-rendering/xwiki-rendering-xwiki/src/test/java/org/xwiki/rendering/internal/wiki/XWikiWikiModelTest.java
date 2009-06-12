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

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentName;
import org.xwiki.bridge.DocumentNameSerializer;
import org.xwiki.component.internal.ReflectionUtils;

/**
 * Unit tests for {@link XWikiWikiModel}.
 * 
 * @version $Id$
 * @since 2.0M1
 */
public class XWikiWikiModelTest extends MockObjectTestCase
{
    public void testGetDocumentEditURLWhenNoQueryStringSpecified() throws Exception
    {
        XWikiWikiModel model = new XWikiWikiModel();

        Mock mockDocumentNameSerializer = mock(DocumentNameSerializer.class);
        mockDocumentNameSerializer.expects(once()).method("serialize").will(returnValue("wiki:Space.Page\u20AC"));
        ReflectionUtils.setFieldValue(model, "documentNameSerializer", mockDocumentNameSerializer.proxy());
        
        Mock mockDocumentAccessBridge = mock(DocumentAccessBridge.class);
        mockDocumentAccessBridge.expects(once()).method("getCurrentDocumentName").will(returnValue(
            new DocumentName("wiki", "Space", "Page")));

        // The test is here: we verify that getURL is called with the query string already encoded since getURL()
        // doesn't encode it.
        mockDocumentAccessBridge.expects(once()).method("getURL").with(eq("Space.Page\u20AC"), eq("edit"),
            eq("parent=wiki%3ASpace.Page%E2%82%AC"), eq("anchor"));
        ReflectionUtils.setFieldValue(model, "documentAccessBridge", mockDocumentAccessBridge.proxy());

        model.getDocumentEditURL("Space.Page\u20AC", "anchor", null);
    }
}
