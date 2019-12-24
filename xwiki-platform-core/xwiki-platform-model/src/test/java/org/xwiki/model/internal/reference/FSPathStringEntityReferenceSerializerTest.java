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
package org.xwiki.model.internal.reference;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link FSPathStringEntityReferenceSerializer}.
 *
 * @version $Id$
 * @since 8.4.6
 * @sice 9.4RC1
 */
@ComponentTest
public class FSPathStringEntityReferenceSerializerTest
{
    @InjectMockComponents
    private FSPathStringEntityReferenceSerializer serializer;

    @Test
    public void serialize()
    {
        DocumentReference documentReference = new DocumentReference("wik.i", "spac.e", "pag.e");
        AttachmentReference attachmentReference = new AttachmentReference("image.png", documentReference);

        String result = this.serializer.serialize(attachmentReference);
        assertEquals("wik%2Ei/spac%2Ee/pag%2Ee/image.png", result);
    }
}
