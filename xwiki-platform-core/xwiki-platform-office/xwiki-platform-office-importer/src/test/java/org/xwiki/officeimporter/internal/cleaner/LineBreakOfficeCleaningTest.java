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

import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.xml.html.HTMLCleanerConfiguration;

/**
 * Test case for cleaning html {@code<br/>} elements in {@link OfficeHTMLCleaner}.
 * 
 * @version $Id$
 * @since 1.8
 */
public class LineBreakOfficeCleaningTest extends AbstractHTMLCleaningTest
{
    /**
     * {@code <br/>} elements placed next to paragraph elements should be converted to {@code<div
     * class="wikikmodel-emptyline"/>} elements.
     */
    @Test
    public void testLineBreaksNextToParagraphElements()
    {
        checkLineBreakReplacements("<div><br/><br/><p>para</p></div>", 0, 2);
        checkLineBreakReplacements("<div><p>para</p><br/><br/></div>", 0, 2);
        checkLineBreakReplacements("<div><p>para</p><br/><br/><p>para</p></div>", 0, 2);
    }

    /**
     * {@code <br/>} elements placed next to list elements should be converted to {@code<div
     * class="wikikmodel-emptyline"/>} elements.
     */
    @Test
    public void testLineBreaksNextToListElements()
    {
        checkLineBreakReplacements("<div><br/><br/><ol><li>para</li></ol></div>", 0, 2);
        checkLineBreakReplacements("<div><ol><li>para</li></ol><br/><br/></div>", 0, 2);
        checkLineBreakReplacements("<div><ol><li>para</li></ol><br/><br/><ol><li>para</li></ol></div>", 0, 2);

        checkLineBreakReplacements("<div><br/><br/><ul><li>para</li></ul></div>", 0, 2);
        checkLineBreakReplacements("<div><ul><li>para</li></ul><br/><br/></div>", 0, 2);
        checkLineBreakReplacements("<div><ul><li>para</li></ul><br/><br/><ul><li>para</li></ul></div>", 0, 2);
    }

    /**
     * {@code <br/>} elements placed next to html heading elements should be converted to {@code<div
     * class="wikikmodel-emptyline"/>} elements.
     */
    @Test
    public void testLineBreaksNextToHeadingElements()
    {
        checkLineBreakReplacements("<div><br/><br/><h1>test</h1></div>", 0, 2);
        checkLineBreakReplacements("<div><h1>test</h1><br/><br/></div>", 0, 2);
        checkLineBreakReplacements("<div><h1>test</h1><br/><br/><h1>test</h1></div>", 0, 2);

        checkLineBreakReplacements("<div><br/><br/><h2>test</h2></div>", 0, 2);
        checkLineBreakReplacements("<div><h2>test</h2><br/><br/></div>", 0, 2);
        checkLineBreakReplacements("<div><h2>test</h2><br/><br/><h2>test</h2></div>", 0, 2);

        checkLineBreakReplacements("<div><br/><br/><h3>test</h3></div>", 0, 2);
        checkLineBreakReplacements("<div><h3>test</h3><br/><br/></div>", 0, 2);
        checkLineBreakReplacements("<div><h3>test</h3><br/><br/><h3>test</h3></div>", 0, 2);

        checkLineBreakReplacements("<div><br/><br/><h4>test</h4></div>", 0, 2);
        checkLineBreakReplacements("<div><h4>test</h4><br/><br/></div>", 0, 2);
        checkLineBreakReplacements("<div><h4>test</h4><br/><br/><h4>test</h4></div>", 0, 2);

        checkLineBreakReplacements("<div><br/><br/><h5>test</h5></div>", 0, 2);
        checkLineBreakReplacements("<div><h5>test</h5><br/><br/></div>", 0, 2);
        checkLineBreakReplacements("<div><h5>test</h5><br/><br/><h5>test</h5></div>", 0, 2);

        checkLineBreakReplacements("<div><br/><br/><h6>test</h6></div>", 0, 2);
        checkLineBreakReplacements("<div><h6>test</h6><br/><br/></div>", 0, 2);
        checkLineBreakReplacements("<div><h6>test</h6><br/><br/><h6>test</h6></div>", 0, 2);
    }

    /**
     * {@code <br/>} elements placed next to html table elements should be converted to {@code<div
     * class="wikikmodel-emptyline"/>} elements.
     */
    @Test
    public void testLineBreaksNextToTableElements()
    {
        checkLineBreakReplacements("<div><br/><br/><table><tr><td>test</td></tr></table></div>", 0, 2);
        checkLineBreakReplacements("<div><table><tr><td>test</td></tr></table><br/><br/></div>", 0, 2);
        checkLineBreakReplacements(
            "<div><table><tr><td>test</td></tr></table><br/><br/><table><tr><td>test</td></tr></table></div>", 0, 2);
    }

    /**
     * Utility methods for checking if {@code <br/>} elements are properly converted to {@code<div
     * class="wikikmodel-emptyline"/>} elements.
     * 
     * @param html the html content.
     * @param expectedBrCount expected count of {@code <br/>} elements after cleaning.
     * @param expectedDivCount expected count of {@code<div class="wikikmodel-emptyline"/>} elements after cleaning.
     */
    private void checkLineBreakReplacements(String html, int expectedBrCount, int expectedDivCount)
    {
        getMockery().checking(new Expectations()
        {
            {
                allowing(mockDocumentReferenceResolver).resolve("Import.Test");
                will(returnValue(new DocumentReference("wiki", "Import", "Test")));
            }
        });

        HTMLCleanerConfiguration configuration = this.officeHTMLCleaner.getDefaultConfiguration();
        configuration.setParameters(Collections.singletonMap("targetDocument", "Import.Test"));
        Document doc = officeHTMLCleaner.clean(new StringReader(header + html + footer), configuration);
        NodeList lineBreaks = doc.getElementsByTagName("br");
        Assert.assertEquals(expectedBrCount, lineBreaks.getLength());
        NodeList divs = doc.getElementsByTagName("div");
        Assert.assertEquals(expectedDivCount, divs.getLength());
    }
}
