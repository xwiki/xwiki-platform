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

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xwiki.test.junit5.mockito.ComponentTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test case for filtering redundant html tags in {@link OfficeHTMLCleaner}.
 * 
 * @version $Id$
 * @since 1.8
 */
@ComponentTest
public class RedundantTagOfficeCleaningTest extends AbstractHTMLCleaningTest
{
    /**
     * Test filtering of those tags which doesn't have any attributes set.
     */
    @Test
    public void filterIfZeroAttributes()
    {
        String htmlTemplate = header + "<p>Test%sRedundant%sFiltering</p>" + footer;
        String[] filterIfZeroAttributesTags = new String[] {"span", "div"};
        for (String tag : filterIfZeroAttributesTags) {
            String startTag = "<" + tag + ">";
            String endTag = "</" + tag + ">";
            String html = String.format(htmlTemplate, startTag, endTag);
            Document doc = officeHTMLCleaner.clean(new StringReader(html));
            NodeList nodes = doc.getElementsByTagName(tag);
            assertEquals(0, nodes.getLength());
        }
    }

    /**
     * Test filtering of those tags which doesn't have any textual content in them.
     */
    @Test
    public void filterIfNoContent()
    {
        String htmlTemplate = header + "<p>Test%sRedundant%s%s%sFiltering</p>" + footer;
        String[] filterIfNoContentTags =
            new String[] {"em", "strong", "dfn", "code", "samp", "kbd", "var", "cite", "abbr", "acronym", "address",
                "blockquote", "q", "pre", "h1", "h2", "h3", "h4", "h5", "h6"};
        for (String tag : filterIfNoContentTags) {
            String startTag = "<" + tag + ">";
            String endTag = "</" + tag + ">";
            String html = String.format(htmlTemplate, startTag, endTag, startTag, endTag);
            Document doc = officeHTMLCleaner.clean(new StringReader(html));
            NodeList nodes = doc.getElementsByTagName(tag);
            assertEquals(1, nodes.getLength());
        }
    }
}
