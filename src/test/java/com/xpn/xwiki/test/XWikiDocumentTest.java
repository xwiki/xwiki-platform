/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 *
 * @author jeremi
 */
package com.xpn.xwiki.test;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

public class XWikiDocumentTest  extends HibernateTestCase {


    public void testCustomClass() throws XWikiException {
        XWikiDocument doc  = xwiki.getDocument("Test.CustomTest", context);
        doc.setCustomClass(CustomDocumentClass.class.getName());
        xwiki.saveDocument(doc, context);


        doc  = xwiki.getDocument("Test.NotCustomTest", context);
        xwiki.saveDocument(doc, context);

        doc  = xwiki.getDocument("Test.CustomTest", context);
        assertEquals(CustomDocumentClass.class.getName(), doc.getCustomClass());
        assertTrue(doc.newDocument(context) instanceof CustomDocumentClass);

        doc  = xwiki.getDocument("Test.NotCustomTest", context);
        assertNotNull(doc);
        assertTrue(doc.newDocument(context) instanceof Document);        
        assertFalse(doc.newDocument(context) instanceof CustomDocumentClass);

        doc  = xwiki.getDocument("Test.CustomTest", context);
        doc.setContent("plop");
        xwiki.saveDocument(doc, context);

        assertEquals(CustomDocumentClass.class.getName(), doc.getCustomClass());
        assertTrue(doc.newDocument(context) instanceof CustomDocumentClass);
    }
}
