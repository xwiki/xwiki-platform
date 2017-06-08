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
package org.xwiki.test.misc;

import junit.framework.TestCase;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Test that the suggest service works without programming rights on {@code Main.WebHome}.
 *
 * @see <a href="https://jira.xwiki.org/browse/XE-539">XE-539</a>
 * @version $Id$
 * @since 4.5M1
 */
public class SuggestTest extends TestCase
{

    public void testSuggestWithNonexistingContextDocument() throws Exception
    {
        URL url = new URL("http://localhost:8080/xwiki/bin/view/NonExistingSpace/NonexistingPage"
            + "?xpage=suggest&classname=XWiki.TagClass&fieldname=tags&firCol=%2D&secCol=%2D&input=test");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        InputStream is = connection.getInputStream();

        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = documentBuilder.parse(is);

        assertNotNull(document);

        Element results = document.getDocumentElement();

        assertNotNull(results);
        assertEquals("results", results.getTagName());

        // The tag suggestion use a hibernate query to fetch the suggestions, which requires programming rights.  If
        // programming rights are denied, the result type will be "8" and there will be no results.  If programming
        // rights are granted, and the query succeeds the result type will be "3".
        assertEquals("3", results.getAttribute("type"));
    }
}
