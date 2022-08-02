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
package org.xwiki.store.filesystem.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentContent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link TemporaryAttachmentSession}.
 *
 * @version $Id$
 * @since 14.3RC1
 */
class TemporaryAttachmentSessionTest
{
    private TemporaryAttachmentSession temporaryAttachmentSession;

    private DocumentReference docRef1;
    private DocumentReference docRef2;
    private DocumentReference docRef3;
    private XWikiAttachment fooAttachment1;
    private XWikiAttachment barAttachment1;
    private XWikiAttachment fooAttachment2;


    @BeforeEach
    void setup()
    {
        this.temporaryAttachmentSession = new TemporaryAttachmentSession("session");

        this.docRef1 = mock(DocumentReference.class);
        this.docRef2 = mock(DocumentReference.class);
        this.docRef3 = mock(DocumentReference.class);

        this.fooAttachment1 = mock(XWikiAttachment.class);
        this.fooAttachment2 = mock(XWikiAttachment.class);
        this.barAttachment1 = mock(XWikiAttachment.class);

        // Editions Map fixture:
        // DocRef1:
        //   - Foo1 -> FooAttachment1
        //   - Bar1 -> BarAttachment1
        // DocRef2: <empty>
        // DocRef3:
        //   - Foo2 -> FooAttachment2
        Map<DocumentReference, Map<String, XWikiAttachment>> editionsMap =
            this.temporaryAttachmentSession.getEditionsMap();

        HashMap<String, XWikiAttachment> map = new HashMap<>();
        editionsMap.put(this.docRef1, map);
        map.put("foo1", this.fooAttachment1);
        map.put("bar1", barAttachment1);

        map = new HashMap<>();
        editionsMap.put(this.docRef2, map);

        map = new HashMap<>();
        editionsMap.put(this.docRef3, map);
        map.put("foo2", fooAttachment2);
    }

    @Test
    void dispose()
    {
        Map<DocumentReference, Map<String, XWikiAttachment>> editionsMap =
            this.temporaryAttachmentSession.getEditionsMap();

        XWikiAttachmentContent contentFoo1 = mock(XWikiAttachmentContent.class);
        XWikiAttachmentContent contentBar1 = mock(XWikiAttachmentContent.class);
        XWikiAttachmentContent contentFoo2 = mock(XWikiAttachmentContent.class);

        when(this.fooAttachment1.getAttachment_content()).thenReturn(contentFoo1);
        when(this.barAttachment1.getAttachment_content()).thenReturn(contentBar1);
        when(this.fooAttachment2.getAttachment_content()).thenReturn(contentFoo2);

        this.temporaryAttachmentSession.dispose();
        verify(contentFoo1).dispose();
        verify(contentBar1).dispose();
        verify(contentFoo2).dispose();

        assertTrue(this.temporaryAttachmentSession.getEditionsMap().isEmpty());
    }

    @Test
    void addAttachment()
    {
        XWikiAttachment newAttachment = mock(XWikiAttachment.class);
        when(newAttachment.getFilename()).thenReturn("buz");

        this.temporaryAttachmentSession.addAttachment(this.docRef1, newAttachment);
        assertEquals(3, this.temporaryAttachmentSession.getEditionsMap().get(this.docRef1).size());
        assertSame(newAttachment, this.temporaryAttachmentSession.getEditionsMap().get(this.docRef1).get("buz"));
    }

    @Test
    void getFilenames()
    {
        assertEquals(new HashSet<>(Arrays.asList("foo1", "bar1")),
            this.temporaryAttachmentSession.getFilenames(this.docRef1));
        assertEquals(Collections.emptySet(),
            this.temporaryAttachmentSession.getFilenames(this.docRef2));
        assertEquals(Collections.singleton("foo2"),
            this.temporaryAttachmentSession.getFilenames(this.docRef3));
    }

    @Test
    void getAttachment()
    {
        assertEquals(
            Optional.of(this.fooAttachment1), this.temporaryAttachmentSession.getAttachment(this.docRef1, "foo1"));
        assertEquals(Optional.empty(),
            this.temporaryAttachmentSession.getAttachment(mock(DocumentReference.class), "something"));
        assertEquals(Optional.empty(),
            this.temporaryAttachmentSession.getAttachment(this.docRef2, "something"));
    }

    @Test
    void getAttachments()
    {
        assertEquals(new HashSet<>(Arrays.asList(this.fooAttachment1, this.barAttachment1)),
            this.temporaryAttachmentSession.getAttachments(this.docRef1));
        assertEquals(Collections.emptySet(), this.temporaryAttachmentSession.getAttachments(this.docRef2));
    }

    @Test
    void removeAttachment()
    {
        XWikiAttachmentContent attachmentContent = mock(XWikiAttachmentContent.class);
        when(this.fooAttachment2.getAttachment_content()).thenReturn(attachmentContent);

        assertTrue(this.temporaryAttachmentSession.removeAttachment(this.docRef3, "foo2"));
        verify(attachmentContent).dispose();
        assertTrue(this.temporaryAttachmentSession.getEditionsMap().get(this.docRef3).isEmpty());

        assertFalse(this.temporaryAttachmentSession.removeAttachment(this.docRef3, "another"));
    }

    @Test
    void removeAttachments()
    {
        XWikiAttachmentContent attachmentContent1 = mock(XWikiAttachmentContent.class);
        when(this.fooAttachment1.getAttachment_content()).thenReturn(attachmentContent1);

        XWikiAttachmentContent attachmentContent2 = mock(XWikiAttachmentContent.class);
        when(this.barAttachment1.getAttachment_content()).thenReturn(attachmentContent2);

        assertTrue(this.temporaryAttachmentSession.removeAttachments(this.docRef1));
        verify(attachmentContent1).dispose();
        verify(attachmentContent2).dispose();
        assertTrue(this.temporaryAttachmentSession.getEditionsMap().get(this.docRef1).isEmpty());

        assertFalse(this.temporaryAttachmentSession.removeAttachments(this.docRef1));
    }

}
