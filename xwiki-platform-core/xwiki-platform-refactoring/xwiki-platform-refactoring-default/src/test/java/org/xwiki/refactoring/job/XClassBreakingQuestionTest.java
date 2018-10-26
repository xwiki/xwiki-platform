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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.refactoring.job.question.EntitySelection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validate the behaviour of {@link XClassBreakingQuestion}
 *
 * @since 10.9RC1
 * @version $Id$
 */
public class XClassBreakingQuestionTest
{
    @Test
    public void markImpactedObject()
    {
        DocumentReference document1 = new DocumentReference("wiki1", "space1", "document1");
        DocumentReference document2 = new DocumentReference("wiki2", "space2", "document2");

        DocumentReference docObjDoc1 = new DocumentReference("wiki1", "space1", "documentObject1");
        DocumentReference docObjDoc2 = new DocumentReference("wiki1", "space1", "documentObject2");
        DocumentReference docObjDoc3 = new DocumentReference("wiki2", "space1", "documentObject3");

        EntitySelection selection1 = new EntitySelection(document1);
        EntitySelection selection2 = new EntitySelection(document2);

        Map<EntityReference, EntitySelection> concernedEntities = new HashMap<>();
        concernedEntities.put(document1, selection1);
        concernedEntities.put(document2, selection2);

        XClassBreakingQuestion question = new XClassBreakingQuestion(concernedEntities);

        assertTrue(question.getImpactedObjects().isEmpty());

        question.markImpactedObject(selection1, docObjDoc1);
        question.markImpactedObject(selection1, docObjDoc2);
        question.markImpactedObject(selection2, docObjDoc3);

        Map<EntityReference, Set<EntityReference>> expectedImpacted = new HashMap<>();
        HashSet<EntityReference> impact1 = new HashSet<>();
        impact1.add(docObjDoc1);
        impact1.add(docObjDoc2);
        expectedImpacted.put(document1, impact1);

        HashSet<EntityReference> impact2 = new HashSet<>();
        impact2.add(docObjDoc3);
        expectedImpacted.put(document2, impact2);

        assertEquals(expectedImpacted, question.getImpactedObjects());
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

        XClassBreakingQuestion question = new XClassBreakingQuestion(concernedEntities);

        question.selectAllFreePages();

        assertFalse(selection1.isSelected());
        assertFalse(selection2.isSelected());

        question.markAsFreePage(selection1);
        question.selectAllFreePages();

        assertTrue(selection1.isSelected());
        assertFalse(selection2.isSelected());
    }
}
