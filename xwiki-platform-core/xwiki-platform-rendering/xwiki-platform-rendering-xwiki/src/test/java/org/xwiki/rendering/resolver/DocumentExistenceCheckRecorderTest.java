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
package org.xwiki.rendering.resolver;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link DocumentExistenceCheckRecorder}.
 *
 * @version $Id$
 */
class DocumentExistenceCheckRecorderTest
{
    private static final DocumentReference TERMINAL = new DocumentReference("wiki", "Space", "Page");

    private static final DocumentReference HOME = new DocumentReference("wiki", List.of("Space", "Page"), "WebHome");

    @Test
    void add()
    {
        DocumentExistenceCheckRecorder recorder = new DocumentExistenceCheckRecorder();

        assertEquals(Set.of(), recorder.getDocuments());

        recorder.add(TERMINAL);
        recorder.add(HOME);
        recorder.add(TERMINAL);

        assertEquals(List.of(TERMINAL, HOME), List.copyOf(recorder.getDocuments()));
    }

    @Test
    void addIgnoresLocale()
    {
        DocumentExistenceCheckRecorder recorder = new DocumentExistenceCheckRecorder();

        recorder.add(new DocumentReference(TERMINAL, Locale.FRENCH));
        recorder.add(TERMINAL);

        assertEquals(Set.of(TERMINAL), recorder.getDocuments());
    }

    @Test
    void getDocumentsIsReadOnly()
    {
        Set<DocumentReference> documents = new DocumentExistenceCheckRecorder().getDocuments();

        assertThrows(UnsupportedOperationException.class, () -> documents.add(TERMINAL));
    }
}
