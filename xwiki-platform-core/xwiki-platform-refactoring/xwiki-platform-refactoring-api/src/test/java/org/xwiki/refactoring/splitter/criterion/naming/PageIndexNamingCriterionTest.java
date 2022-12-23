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

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.refactoring.internal.AbstractRefactoringTestCase;
import org.xwiki.rendering.block.XDOM;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test case for {@link PageIndexNamingCriterion}.
 * 
 * @version $Id$
 * @since 1.9M1
 */
public class PageIndexNamingCriterionTest extends AbstractRefactoringTestCase
{
    @Test
    public void getDocumentReference() throws Exception
    {
        XDOM xdom = xwikiParser.parse(new StringReader("=Test="));
        DocumentReference documentReference = new DocumentReference("test", "Some", "Page");
        NamingCriterion namingCriterion = new PageIndexNamingCriterion(documentReference, this.docBridge);
        for (int i = 1; i < 10; i++) {
            assertEquals(new DocumentReference("test", "Some", "Page-" + i),
                namingCriterion.getDocumentReference(xdom));
        }
    }
}
