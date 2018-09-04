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
package org.xwiki.extension.xar.internal.delete.question;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.xwiki.extension.xar.internal.delete.question.ExtensionBreakingQuestion;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.refactoring.job.question.EntitySelection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validate {@link ExtensionBreakingQuestion}.
 * 
 * @version $Id$
 */
public class ExtensionBreakingQuestionTest
{
    @Test
    public void get()
    {
        DocumentReference document1 = new DocumentReference("wiki1", "space1", "document1");
        DocumentReference document2 = new DocumentReference("wiki2", "space2", "document2");

        Map<EntityReference, EntitySelection> concernedEntities = new HashMap<>();
        concernedEntities.put(document1, new EntitySelection(document1));
        concernedEntities.put(document2, new EntitySelection(document2));

        ExtensionBreakingQuestion question = new ExtensionBreakingQuestion(concernedEntities);

        assertEquals(document1, question.get(document1).getEntityReference());
        assertTrue(question.get(document1).isSelected());
        assertEquals(document2, question.get(document2).getEntityReference());
        assertTrue(question.get(document2).isSelected());
    }

    @Test
    public void selectAllFreePages()
    {
        DocumentReference document1 = new DocumentReference("wiki1", "space1", "document1");
        DocumentReference document2 = new DocumentReference("wiki2", "space2", "document2");

        EntitySelection selection1 = new EntitySelection(document1);
        selection1.setSelected(false);
        EntitySelection selection2 = new EntitySelection(document2);
        selection2.setSelected(false);
        Map<EntityReference, EntitySelection> concernedEntities = new HashMap<>();
        concernedEntities.put(document1, selection1);
        concernedEntities.put(document2, selection2);

        ExtensionBreakingQuestion question = new ExtensionBreakingQuestion(concernedEntities);

        assertEquals(document1, question.get(document1).getEntityReference());
        assertFalse(question.get(document1).isSelected());
        assertEquals(document2, question.get(document2).getEntityReference());
        assertFalse(question.get(document2).isSelected());

        question.selectAllFreePages();

        assertFalse(question.get(document1).isSelected());
        assertFalse(question.get(document2).isSelected());

        question.markAsFreePage(question.get(document1));        
        question.selectAllFreePages();

        assertTrue(question.get(document1).isSelected());
        assertFalse(question.get(document2).isSelected());
    }
}
