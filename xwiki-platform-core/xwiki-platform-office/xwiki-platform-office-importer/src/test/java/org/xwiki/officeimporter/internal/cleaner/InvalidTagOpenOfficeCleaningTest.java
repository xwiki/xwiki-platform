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
package org.xwiki.officeimporter.internal.cleaner;

import java.io.StringReader;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Test case for tag removing in {@link OpenOfficeHTMLCleaner}.
 * 
 * @version $Id$
 * @since 1.8
 */
public class InvalidTagOpenOfficeCleaningTest extends AbstractHTMLCleaningTest
{
    /**
     * {@code STYLE} tags should be stripped from HTML content.
     */
    @Test
    public void testStyleTagRemoving()
    {
        String html =
            "<html><head><title>Title</title>" + "<style type=\"text/css\">h1 {color:red} p {color:blue} </style>"
                + "</head><body>" + footer;
        Document doc = openOfficeHTMLCleaner.clean(new StringReader(html));
        NodeList nodes = doc.getElementsByTagName("style");
        Assert.assertEquals(0, nodes.getLength());
    }

    /**
     * {@code STYLE} tags should be stripped from HTML content.
     */
    @Test
    public void testScriptTagRemoving()
    {
        String html = header + "<script type=\"text/javascript\">document.write(\"Hello World!\")</script>" + footer;
        Document doc = openOfficeHTMLCleaner.clean(new StringReader(html));
        NodeList nodes = doc.getElementsByTagName("script");
        Assert.assertEquals(0, nodes.getLength());
    }
}
