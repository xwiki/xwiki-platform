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
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.xml.html.HTMLCleanerConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Miscellaneous cleaning tests for {@link WysiwygHTMLCleaner}.
 * 
 * @version $Id$
 * @since 1.8
 */
@ComponentTest
public class MiscWysiwygCleaningTest extends AbstractHTMLCleaningTest
{
    /**
     * Test cleaning of HTML paragraphs with namespaces specified.
     */
    @Test
    public void paragraphsWithNamespaces()
    {
        String html = header + "<w:p>paragraph</w:p>" + footer;
        HTMLCleanerConfiguration configuration = this.officeHTMLCleaner.getDefaultConfiguration();
        configuration.setParameters(Collections.singletonMap(HTMLCleanerConfiguration.NAMESPACES_AWARE, "false"));
        Document doc = wysiwygHTMLCleaner.clean(new StringReader(html), configuration);
        NodeList nodes = doc.getElementsByTagName("p");
        assertEquals(1, nodes.getLength());
    }
}
