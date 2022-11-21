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
package org.xwiki.model.reference;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DocumentVersionReference}
 *
 * @version $Id$
 * @since 14.8RC1
 */
class DocumentVersionReferenceTest
{
    private DocumentReference documentReference;
    private DocumentVersionReference documentVersionReference;

    @BeforeEach
    void setup()
    {
        this.documentReference = new DocumentReference("xwiki", "MySpace", "MyPage");
        this.documentVersionReference = new DocumentVersionReference(documentReference, "4.5");
    }

    @Test
    void getVersion()
    {

        assertEquals("4.5", documentVersionReference.getVersion());
    }

    @Test
    void getParameters()
    {
        assertEquals(Collections.singletonMap("version", "4.5"), documentVersionReference.getParameters());
    }

    @Test
    void removeVersion()
    {
        assertEquals(documentReference, documentVersionReference.removeVersion());
    }
}