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

import org.junit.Test;
import org.xwiki.model.EntityType;

import junit.framework.Assert;

/**
 * Unit tests for {@link AttachmentReference}.
 *
 * @version $Id$
 * @since 2.2M1
 */
public class AttachmentReferenceTest
{
    @Test
    public void testInvalidType()
    {
        try {
            new AttachmentReference(new EntityReference("filename", EntityType.DOCUMENT));
            Assert.fail("Should have thrown an exception here");
        } catch (IllegalArgumentException expected) {
            Assert.assertEquals("Invalid type [DOCUMENT] for an attachment reference", expected.getMessage());
        }
    }

    @Test
    public void testInvalidNullParent()
    {
        try {
            new AttachmentReference("filename", null);
            Assert.fail("Should have thrown an exception here");
        } catch (IllegalArgumentException expected) {
            Assert.assertEquals("Invalid parent reference [null] in an attachment reference", expected.getMessage());
        }
    }

    @Test
    public void testInvalidParentType()
    {
        try {
            new AttachmentReference(new EntityReference("filename", EntityType.ATTACHMENT, new WikiReference("wiki")));
            Assert.fail("Should have thrown an exception here");
        } catch (IllegalArgumentException expected) {
            Assert.assertEquals("Invalid parent reference [name = [wiki], type = [WIKI], parent = [null]] in an "
                + "attachment reference", expected.getMessage());
        }
    }
}
