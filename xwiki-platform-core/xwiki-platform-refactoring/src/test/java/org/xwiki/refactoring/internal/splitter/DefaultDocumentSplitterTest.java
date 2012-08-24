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
package org.xwiki.refactoring.internal.splitter;

import java.io.StringReader;
import java.util.List;

import org.xwiki.refactoring.WikiDocument;
import org.xwiki.refactoring.internal.AbstractRefactoringTestCase;
import org.xwiki.refactoring.splitter.DocumentSplitter;
import org.xwiki.refactoring.splitter.criterion.HeadingLevelSplittingCriterion;
import org.xwiki.refactoring.splitter.criterion.SplittingCriterion;
import org.xwiki.refactoring.splitter.criterion.naming.NamingCriterion;
import org.xwiki.refactoring.splitter.criterion.naming.PageIndexNamingCriterion;
import org.xwiki.rendering.block.XDOM;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test case for {@link DefaultDocumentSplitter}.
 * 
 * @version $Id$
 * @since 1.9M1
 */
public class DefaultDocumentSplitterTest extends AbstractRefactoringTestCase
{
    /**
     * The {@link DocumentSplitter} component.
     */
    private DocumentSplitter splitter;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        splitter = getComponentManager().getInstance(DocumentSplitter.class, "default");
    }

    /**
     * Tests document splitting by heading-levels.
     * 
     * @throws Exception
     */
    @Test
    public void testHeadingLevelDocumentSplitting() throws Exception
    {
        SplittingCriterion splittingCriterion = new HeadingLevelSplittingCriterion(new int[] {4, 1, 3});
        NamingCriterion namingCriterion = new PageIndexNamingCriterion("Main.Test", docBridge);
        StringBuffer buf = new StringBuffer();
        buf.append("=Topic1=\n");
        buf.append("Some Content\n");
        buf.append("==Topic1.1==");
        buf.append("Some Content\n");
        buf.append("===Topic1.2===");
        buf.append("Some Content\n");
        buf.append("=Topic2=\n");
        buf.append("Some Content\n");
        buf.append("====Topic2.1====");
        buf.append("Some Content\n");
        buf.append("=Topic3=\n");
        buf.append("Some Content\n");
        Assert.assertEquals(6, split("Main.Test", buf.toString(), splittingCriterion, namingCriterion).size());
    }

    /**
     * A utility method for simulating a document split operation.
     * 
     * @param masterDocumentName name of the document being split.
     * @param xwikiTwoZeroContent xwiki/2.0 content of the masterDocument.
     * @param splittingCriterion {@link SplittingCriterion}.
     * @param namingCriterion {@link NamingCriterion}.
     * @return the list of wiki documents resulting from the split operation.
     */
    private List<WikiDocument> split(String masterDocumentName, String xwikiTwoZeroContent,
        SplittingCriterion splittingCriterion, NamingCriterion namingCriterion) throws Exception
    {
        XDOM xdom = xwikiParser.parse(new StringReader(xwikiTwoZeroContent));
        WikiDocument rootDoc = new WikiDocument(masterDocumentName, xdom, null);
        return splitter.split(rootDoc, splittingCriterion, namingCriterion);
    }
}
