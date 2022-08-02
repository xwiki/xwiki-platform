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
package org.xwiki.refactoring.job;

import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.junit5.mockito.ComponentTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test of {@link OverwriteQuestion}.
 *
 * @version $Id$
 * @since 12.8RC1
 */
@ComponentTest
class OverwriteQuestionTest
{
    @Test
    void getSource()
    {
        EntityReference from = new EntityReference("from", EntityType.DOCUMENT);
        EntityReference to = new EntityReference("to", EntityType.DOCUMENT);
        OverwriteQuestion overwriteQuestion = new OverwriteQuestion(from, to);
        assertEquals(from, overwriteQuestion.getSource());
    }

    @Test
    void getDestination()
    {
        EntityReference from = new EntityReference("from", EntityType.DOCUMENT);
        EntityReference to = new EntityReference("to", EntityType.DOCUMENT);
        OverwriteQuestion overwriteQuestion = new OverwriteQuestion(from, to);
        assertEquals(to, overwriteQuestion.getDestination());
    }

    @Test
    void isOverwrite()
    {
        EntityReference from = new EntityReference("from", EntityType.DOCUMENT);
        EntityReference to = new EntityReference("to", EntityType.DOCUMENT);
        OverwriteQuestion overwriteQuestion = new OverwriteQuestion(from, to);
        assertTrue(overwriteQuestion.isOverwrite());
        overwriteQuestion.setOverwrite(false);
        assertFalse(overwriteQuestion.isOverwrite());
        overwriteQuestion.setOverwrite(true);
        assertTrue(overwriteQuestion.isOverwrite());
    }

    @Test
    void isAskAgain()
    {
        EntityReference from = new EntityReference("from", EntityType.DOCUMENT);
        EntityReference to = new EntityReference("to", EntityType.DOCUMENT);
        OverwriteQuestion overwriteQuestion = new OverwriteQuestion(from, to);
        assertTrue(overwriteQuestion.isAskAgain());
        overwriteQuestion.setAskAgain(false);
        assertFalse(overwriteQuestion.isAskAgain());
        overwriteQuestion.setAskAgain(true);
        assertTrue(overwriteQuestion.isAskAgain());
    }
}