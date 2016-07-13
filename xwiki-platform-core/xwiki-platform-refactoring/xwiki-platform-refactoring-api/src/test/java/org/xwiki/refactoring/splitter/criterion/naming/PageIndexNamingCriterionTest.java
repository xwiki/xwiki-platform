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
package org.xwiki.refactoring.splitter.criterion.naming;

import java.io.StringReader;

import org.xwiki.refactoring.internal.AbstractRefactoringTestCase;
import org.xwiki.rendering.block.XDOM;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test case for {@link PageIndexNamingCriterion}.
 * 
 * @version $Id$
 * @since 1.9M1
 */
public class PageIndexNamingCriterionTest extends AbstractRefactoringTestCase
{
    /**
     * Tests document names generated.
     * 
     * @throws Exception
     */
    @Test
    public void testDocumentNamesGeneration() throws Exception
    {
        XDOM xdom = xwikiParser.parse(new StringReader("=Test="));
        NamingCriterion namingCriterion = new PageIndexNamingCriterion("Main.Test", docBridge);
        for (int i = 1; i < 10; i++) {
            Assert.assertEquals("Main.Test-" + i, namingCriterion.getDocumentName(xdom));
        }
    }
}
