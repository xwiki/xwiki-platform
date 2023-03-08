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
package com.xpn.xwiki.objects;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.store.merge.MergeManager;
import org.xwiki.store.merge.MergeManagerResult;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.doc.merge.MergeResult;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link BaseObject} class.
 *
 * @version $Id$
 */
@ReferenceComponentList
@OldcoreTest
public class BaseObjectTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    private MergeManager mergeManager;
    
    @Test
    public void testSetDocumentReference() throws Exception
    {
        BaseObject baseObject = new BaseObject();

        DocumentReference reference = new DocumentReference("wiki", "space", "page");
        baseObject.setDocumentReference(reference);

        assertEquals(reference, baseObject.getDocumentReference());
    }

    @Test
    public void testSetName() throws Exception
    {
        String database = this.oldcore.getXWikiContext().getWikiId();
        BaseObject baseObject = new BaseObject();

        baseObject.setName("space.page");

        assertEquals(database, baseObject.getDocumentReference().getWikiReference().getName());
        assertEquals("space", baseObject.getDocumentReference().getLastSpaceReference().getName());
        assertEquals("page", baseObject.getDocumentReference().getName());
    }

    @Test
    public void testSetNameAloneWithChangingContext() throws Exception
    {
        String database = this.oldcore.getXWikiContext().getWikiId();
        BaseObject baseObject = new BaseObject();

        baseObject.setName("space.page");

        try {
            this.oldcore.getXWikiContext().setWikiId("otherwiki");

            assertEquals(database, baseObject.getDocumentReference().getWikiReference().getName());
            assertEquals("space", baseObject.getDocumentReference().getLastSpaceReference().getName());
            assertEquals("page", baseObject.getDocumentReference().getName());

            baseObject.setName("otherspace.otherpage");
        } finally {
            this.oldcore.getXWikiContext().setWikiId(database);
        }

        assertEquals(database, baseObject.getDocumentReference().getWikiReference().getName());
        assertEquals("otherspace", baseObject.getDocumentReference().getLastSpaceReference().getName());
        assertEquals("otherpage", baseObject.getDocumentReference().getName());

        baseObject = new BaseObject();
        try {
            this.oldcore.getXWikiContext().setWikiId("otherwiki");
            baseObject.setName("space.page");
        } finally {
            this.oldcore.getXWikiContext().setWikiId(database);
        }

        assertEquals("otherwiki", baseObject.getDocumentReference().getWikiReference().getName());
        assertEquals("space", baseObject.getDocumentReference().getLastSpaceReference().getName());
        assertEquals("page", baseObject.getDocumentReference().getName());

        baseObject.setName("otherspace.otherpage");

        assertEquals("otherwiki", baseObject.getDocumentReference().getWikiReference().getName());
        assertEquals("otherspace", baseObject.getDocumentReference().getLastSpaceReference().getName());
        assertEquals("otherpage", baseObject.getDocumentReference().getName());
    }

    @Test
    public void getReference()
    {
        BaseObject baseObject = new BaseObject();

        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        baseObject.setDocumentReference(documentReference);
        DocumentReference classReference = new DocumentReference("wiki", "space", "class");
        baseObject.setXClassReference(classReference);

        assertEquals(new BaseObjectReference(classReference, baseObject.getNumber(), documentReference),
            baseObject.getReference());
    }

    @Test
    public void setXClassReference()
    {
        BaseObject baseObject = new BaseObject();

        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        baseObject.setDocumentReference(documentReference);
        DocumentReference classReference = new DocumentReference("otherwiki", "space", "class");
        baseObject.setXClassReference(classReference);

        assertEquals(new DocumentReference("wiki", "space", "class"), baseObject.getXClassReference());
        assertEquals(new EntityReference("class", EntityType.DOCUMENT, new EntityReference("space",
            EntityType.SPACE)), baseObject.getRelativeXClassReference());
    }

    @Test
    public void testMerge()
    {
        BaseObject previousObject = new BaseObject();
        previousObject.setStringValue("str", "value");
        BaseObject nextObject = new BaseObject();
        nextObject.setStringValue("str", "newvalue");
        BaseObject currentObject = new BaseObject();
        currentObject.setStringValue("str", "value");

        when(mergeManager.mergeCharacters(any(), any(), any(), any())).thenReturn(new MergeManagerResult<>());
        MergeManagerResult<String, String> mergeManagerResult = new MergeManagerResult<>();
        mergeManagerResult.setMergeResult("newvalue");
        mergeManagerResult.setModified(true);
        when(mergeManager.mergeObject(eq("value"), eq("newvalue"), eq("value"), any())).thenReturn(mergeManagerResult);

        MergeConfiguration mergeConfiguration = new MergeConfiguration();
        MergeResult mergeResult = new MergeResult();

        currentObject
            .merge(previousObject, nextObject, mergeConfiguration, this.oldcore.getXWikiContext(), mergeResult);

        List<LogEvent> errors = mergeResult.getLog().getLogsFrom(LogLevel.ERROR);
        if (errors.size() > 0) {
            fail("Found error or warning during the merge (" + errors.get(0) + ")");
        }

        assertEquals("newvalue", currentObject.getStringValue("str"));
    }

    @Test
    public void testHashCode()
    {
        final int number = 101;

        DocumentReference documentReference = new DocumentReference("wiki", "space", "document");
        DocumentReference classReference = new DocumentReference("wiki", "space", "class");

        BaseObject o1 = new BaseObject();
        o1.setDocumentReference(documentReference);
        o1.setXClassReference(classReference);
        BaseObject o2 = new BaseObject();
        o2.setDocumentReference(documentReference);
        o2.setXClassReference(classReference);

        o1.setNumber(number);
        o2.setNumber(number);

        assertEquals(o1.hashCode(), o2.hashCode());
    }
}
