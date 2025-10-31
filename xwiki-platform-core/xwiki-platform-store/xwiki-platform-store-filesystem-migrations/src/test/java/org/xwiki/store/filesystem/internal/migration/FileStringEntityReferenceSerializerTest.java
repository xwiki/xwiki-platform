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
package org.xwiki.store.filesystem.internal.migration;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link FileStringEntityReferenceSerializer}.
 * 
 * @version $Id$
 */
class FileStringEntityReferenceSerializerTest
{
    private FileStringEntityReferenceSerializer serializer = new FileStringEntityReferenceSerializer();

    private void testAttachment(String wiki, String space, String page, String attachment,
        String expectedSerializedString)
    {
        testAttachment(wiki, space, page, attachment, expectedSerializedString, false);
    }

    private void testAttachment(String wiki, String space, String page, String attachment,
        String expectedSerializedString, boolean caseInsensitive)
    {
        test(new AttachmentReference(attachment, new DocumentReference(wiki, space, page)), expectedSerializedString,
            caseInsensitive);
    }

    private void test(EntityReference reference, String expectedSerializedString, boolean caseInsensitive)
    {
        String serialized = this.serializer.serialize(reference, caseInsensitive);

        assertEquals(expectedSerializedString, serialized);
    }

    // Tests

    @Test
    void simple()
    {
        testAttachment("wiki", "space", "page", "attachment", "wiki/space/page/attachment");
    }

    @Test
    void forbiddenCharacters()
    {
        testAttachment("wi+%<>:\"/\\|?*ki", "Spa+%<>:\"/\\|?*ce", "Pa+%<>:\"/\\|?*ge", "Att+%<>:\"/\\|?*achement",
            "wi%2B%25%3C%3E%3A%22%2F%5C%7C%3F%2Aki/" + "Spa%2B%25%3C%3E%3A%22%2F%5C%7C%3F%2Ace/"
                + "Pa%2B%25%3C%3E%3A%22%2F%5C%7C%3F%2Age/" + "Att%2B%25%3C%3E%3A%22%2F%5C%7C%3F%2Aachement");
    }

    @Test
    void dotAtTheBeginning()
    {
        testAttachment(".wiki", ".space", ".page", ".attachment", "%2Ewiki/%2Espace/%2Epage/%2Eattachment");
    }

    @Test
    void dotAtTheEnd()
    {
        testAttachment("wiki.", "space.", "page.", "attachment.", "wiki%2E/space%2E/page%2E/attachment%2E");
    }

    @Test
    void whiteSpaceAtTheBeginning()
    {
        testAttachment(" wiki", " space", " page", " attachment", "+wiki/+space/+page/+attachment");
    }

    @Test
    void whiteSpaceAtTheEnd()
    {
        testAttachment("wiki ", "space ", "page ", "attachment ", "wiki+/space+/page+/attachment+");
    }

    @Test
    void file()
    {
        testAttachment("wiki", "file.ext", "file.ext", "file.ext", "wiki/file.ext/file.ext/file.ext");
    }

    @Test
    void caseInsensitive()
    {
        testAttachment("wiKi", "Spa.ce", "Pa.ge", "FiLe.ext", "wi%4Bi/%53pa.ce/%50a.ge/%46i%4Ce.ext", true);
    }
}
