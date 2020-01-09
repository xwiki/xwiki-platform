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
package org.xwiki.namestrategies;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbstractEntityReferenceNameStrategyTest
{
    private class OnlyNameNameStrategyTestClass extends AbstractEntityReferenceNameStrategy
    {
        private String toBeRenamed = null;
        private String transformedName = null;

        OnlyNameNameStrategyTestClass(String toBeRenamed, String transformedName)
        {
            this.toBeRenamed = toBeRenamed;
            this.transformedName = transformedName;
        }

        @Override
        public String transform(String name)
        {
            return (name.equals(toBeRenamed)) ? transformedName : name;
        }

        @Override
        public boolean isValid(String name)
        {
            return !name.equals(toBeRenamed);
        }
    }

    private OnlyNameNameStrategyTestClass onlyNameValidatorTestClass = new OnlyNameNameStrategyTestClass("foo", "bar");

    @Test
    public void transformation()
    {
        WikiReference wikiReference = new WikiReference("wiki");
        assertEquals(wikiReference, onlyNameValidatorTestClass.transform(wikiReference));

        wikiReference = new WikiReference("foo");
        WikiReference transformedWikiReference = new WikiReference("bar");
        assertEquals(transformedWikiReference, onlyNameValidatorTestClass.transform(wikiReference));

        SpaceReference spaceReference = new SpaceReference("wiki", Arrays.asList("Space1", "Space2"));
        assertEquals(spaceReference, onlyNameValidatorTestClass.transform(spaceReference));

        spaceReference = new SpaceReference("foo", Arrays.asList("Space1", "Space2"));
        SpaceReference  transformedSpaceReference = new SpaceReference("bar", Arrays.asList("Space1", "Space2"));
        assertEquals(transformedSpaceReference, onlyNameValidatorTestClass.transform(spaceReference));

        spaceReference = new SpaceReference("wiki", Arrays.asList("Space1", "foo"));
        transformedSpaceReference = new SpaceReference("wiki", Arrays.asList("Space1", "bar"));
        assertEquals(transformedSpaceReference, onlyNameValidatorTestClass.transform(spaceReference));

        spaceReference = new SpaceReference("wiki", Arrays.asList("foo", "Space2"));
        transformedSpaceReference = new SpaceReference("wiki", Arrays.asList("bar", "Space2"));
        assertEquals(transformedSpaceReference, onlyNameValidatorTestClass.transform(spaceReference));

        spaceReference = new SpaceReference("wiki", Arrays.asList("foo", "foo"));
        transformedSpaceReference = new SpaceReference("wiki", Arrays.asList("bar", "bar"));
        assertEquals(transformedSpaceReference, onlyNameValidatorTestClass.transform(spaceReference));

        spaceReference = new SpaceReference("foo", Arrays.asList("Space1", "foo"));
        transformedSpaceReference = new SpaceReference("bar", Arrays.asList("Space1", "bar"));
        assertEquals(transformedSpaceReference, onlyNameValidatorTestClass.transform(spaceReference));

        spaceReference = new SpaceReference("foo", Arrays.asList("foo", "foo"));
        transformedSpaceReference = new SpaceReference("bar", Arrays.asList("bar", "bar"));
        assertEquals(transformedSpaceReference, onlyNameValidatorTestClass.transform(spaceReference));

        DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("Space1", "Space2"), "Page");
        assertEquals(documentReference, onlyNameValidatorTestClass.transform(documentReference));

        documentReference = new DocumentReference("wiki", Arrays.asList("Space1", "Space2"), "foo");
        DocumentReference transformedDocumentReference =
            new DocumentReference("wiki", Arrays.asList("Space1", "Space2"), "bar");
        assertEquals(transformedDocumentReference, onlyNameValidatorTestClass.transform(documentReference));

        documentReference = new DocumentReference("wiki", Arrays.asList("Space1", "foo"), "Page");
        transformedDocumentReference =
            new DocumentReference("wiki", Arrays.asList("Space1", "bar"), "Page");
        assertEquals(transformedDocumentReference, onlyNameValidatorTestClass.transform(documentReference));

        spaceReference = new SpaceReference("foo", Arrays.asList("Space1", "Space2"));
        Map<String, Serializable> parameters = new HashMap<>();
        parameters.put("foo", "value");
        parameters.put("key", "foo");
        documentReference = new DocumentReference("foo", spaceReference, parameters);

        transformedSpaceReference = new SpaceReference("bar", Arrays.asList("Space1", "Space2"));
        transformedDocumentReference = new DocumentReference("bar", transformedSpaceReference, parameters);
        assertEquals(transformedDocumentReference, onlyNameValidatorTestClass.transform(documentReference));

        documentReference = new DocumentReference("wiki", Arrays.asList("Space1", "foo"), "foo");
        transformedDocumentReference =
            new DocumentReference("wiki", Arrays.asList("Space1", "bar"), "bar");
        assertEquals(transformedDocumentReference, onlyNameValidatorTestClass.transform(documentReference));

        documentReference = new DocumentReference("wiki", Arrays.asList("Space1", "Space2"), "Page");
        AttachmentReference attachmentReference = new AttachmentReference("filename", documentReference);
        assertEquals(attachmentReference, onlyNameValidatorTestClass.transform(attachmentReference));

        attachmentReference = new AttachmentReference("foo", documentReference);
        AttachmentReference transformedAttachmentReference = new AttachmentReference("bar", documentReference);
        assertEquals(transformedAttachmentReference, onlyNameValidatorTestClass.transform(attachmentReference));

        documentReference = new DocumentReference("foo", Arrays.asList("Space1", "Space2"), "foo");
        transformedDocumentReference = new DocumentReference("bar", Arrays.asList("Space1", "Space2"), "bar");
        attachmentReference = new AttachmentReference("filename", documentReference);
        transformedAttachmentReference = new AttachmentReference("filename", transformedDocumentReference);
        assertEquals(transformedAttachmentReference, onlyNameValidatorTestClass.transform(attachmentReference));
    }

    @Test
    public void isValid()
    {
        WikiReference wikiReference = new WikiReference("wiki");
        assertTrue(onlyNameValidatorTestClass.isValid(wikiReference));

        wikiReference = new WikiReference("foo");
        assertFalse(onlyNameValidatorTestClass.isValid(wikiReference));

        SpaceReference spaceReference = new SpaceReference("wiki", Arrays.asList("Space1", "Space2"));
        assertTrue(onlyNameValidatorTestClass.isValid(spaceReference));

        spaceReference = new SpaceReference("foo", Arrays.asList("Space1", "Space2"));
        assertFalse(onlyNameValidatorTestClass.isValid(spaceReference));

        spaceReference = new SpaceReference("wiki", Arrays.asList("Space1", "foo"));
        assertFalse(onlyNameValidatorTestClass.isValid(spaceReference));

        spaceReference = new SpaceReference("wiki", Arrays.asList("foo", "Space2"));
        assertFalse(onlyNameValidatorTestClass.isValid(spaceReference));

        spaceReference = new SpaceReference("wiki", Arrays.asList("foo", "foo"));
        assertFalse(onlyNameValidatorTestClass.isValid(spaceReference));

        spaceReference = new SpaceReference("foo", Arrays.asList("Space1", "foo"));
        assertFalse(onlyNameValidatorTestClass.isValid(spaceReference));

        spaceReference = new SpaceReference("foo", Arrays.asList("foo", "foo"));
        assertFalse(onlyNameValidatorTestClass.isValid(spaceReference));

        spaceReference = new SpaceReference("bar", Arrays.asList("Space1", "Space2"));
        assertTrue(onlyNameValidatorTestClass.isValid(spaceReference));

        spaceReference = new SpaceReference("wiki", Arrays.asList("Space1", "bar"));
        assertTrue(onlyNameValidatorTestClass.isValid(spaceReference));

        spaceReference = new SpaceReference("wiki", Arrays.asList("bar", "Space2"));
        assertTrue(onlyNameValidatorTestClass.isValid(spaceReference));

        spaceReference = new SpaceReference("wiki", Arrays.asList("bar", "bar"));
        assertTrue(onlyNameValidatorTestClass.isValid(spaceReference));

        spaceReference = new SpaceReference("bar", Arrays.asList("Space1", "bar"));
        assertTrue(onlyNameValidatorTestClass.isValid(spaceReference));

        spaceReference = new SpaceReference("bar", Arrays.asList("bar", "bar"));
        assertTrue(onlyNameValidatorTestClass.isValid(spaceReference));

        DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("Space1", "Space2"), "Page");
        assertTrue(onlyNameValidatorTestClass.isValid(documentReference));

        documentReference = new DocumentReference("wiki", Arrays.asList("Space1", "Space2"), "foo");
        assertFalse(onlyNameValidatorTestClass.isValid(documentReference));

        documentReference = new DocumentReference("wiki", Arrays.asList("Space1", "foo"), "Page");
        assertFalse(onlyNameValidatorTestClass.isValid(documentReference));

        documentReference = new DocumentReference("wiki", Arrays.asList("Space1", "Space2"), "bar");
        assertTrue(onlyNameValidatorTestClass.isValid(documentReference));

        documentReference = new DocumentReference("wiki", Arrays.asList("Space1", "bar"), "Page");
        assertTrue(onlyNameValidatorTestClass.isValid(documentReference));

        spaceReference = new SpaceReference("foo", Arrays.asList("Space1", "Space2"));
        Map<String, Serializable> parameters = new HashMap<>();
        parameters.put("foo", "value");
        parameters.put("key", "foo");
        documentReference = new DocumentReference("foo", spaceReference, parameters);
        assertFalse(onlyNameValidatorTestClass.isValid(documentReference));

        spaceReference = new SpaceReference("bar", Arrays.asList("Space1", "Space2"));
        documentReference = new DocumentReference("bar", spaceReference, parameters);
        assertTrue(onlyNameValidatorTestClass.isValid(documentReference));

        documentReference = new DocumentReference("wiki", Arrays.asList("Space1", "foo"), "foo");
        assertFalse(onlyNameValidatorTestClass.isValid(documentReference));

        documentReference = new DocumentReference("wiki", Arrays.asList("Space1", "bar"), "bar");
        assertTrue(onlyNameValidatorTestClass.isValid(documentReference));

        documentReference = new DocumentReference("wiki", Arrays.asList("Space1", "Space2"), "Page");
        AttachmentReference attachmentReference = new AttachmentReference("filename", documentReference);
        assertTrue(onlyNameValidatorTestClass.isValid(attachmentReference));

        attachmentReference = new AttachmentReference("foo", documentReference);
        assertFalse(onlyNameValidatorTestClass.isValid(attachmentReference));

        documentReference = new DocumentReference("foo", Arrays.asList("Space1", "Space2"), "foo");
        attachmentReference = new AttachmentReference("filename", documentReference);
        assertFalse(onlyNameValidatorTestClass.isValid(attachmentReference));

        documentReference = new DocumentReference("bar", Arrays.asList("Space1", "Space2"), "bar");
        attachmentReference = new AttachmentReference("bar", documentReference);
        assertTrue(onlyNameValidatorTestClass.isValid(attachmentReference));

        documentReference = new DocumentReference("bar", Arrays.asList("Space1", "Space2"), "bar");
        attachmentReference = new AttachmentReference("filename", documentReference);
        assertTrue(onlyNameValidatorTestClass.isValid(attachmentReference));
    }
}
