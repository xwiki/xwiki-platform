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
package com.xpn.xwiki.doc;

import org.jmock.Mock;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;
import com.xpn.xwiki.web.XWikiURLFactory;

/**
 * Unit tests for {@link DefaultDocumentAccessBridge}.
 * 
 * @version $Id$
 */
public class DefaultDocumentAccessBridgeTest extends AbstractBridgedXWikiComponentTestCase
{
    private DocumentAccessBridge documentAccessBridge;

    private Mock mockXWiki;

    private Mock mockURLFactory;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        this.mockXWiki = mock(XWiki.class);
        this.mockURLFactory = mock(XWikiURLFactory.class);

        getContext().setURLFactory((XWikiURLFactory) mockURLFactory.proxy());
        getContext().setWiki((XWiki) mockXWiki.proxy());

        this.documentAccessBridge = getComponentManager().getInstance(DocumentAccessBridge.class);
    }

    public void testGetUrlEmptyDocument()
    {
        getContext().setDoc(new XWikiDocument(new DocumentReference("Wiki", "Space", "Page")));
        this.mockXWiki.stubs().method("getURL").will(returnValue("/xwiki/bin/view/Main/WebHome"));

        assertEquals("/xwiki/bin/view/Main/WebHome", this.documentAccessBridge.getURL("", "view", "", ""));
        assertEquals("/xwiki/bin/view/Main/WebHome", this.documentAccessBridge.getURL(null, "view", "", ""));
    }
}
