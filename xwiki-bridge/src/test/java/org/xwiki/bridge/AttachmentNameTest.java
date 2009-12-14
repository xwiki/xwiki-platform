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
package org.xwiki.bridge;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Unit tests for {@link AttachmentName}.
 * 
 * @version $Id$
 * @since 2.0RC1
 */
@Deprecated
public class AttachmentNameTest
{
    @Test
    public void testEquals()
    {
        AttachmentName attachmentName = new AttachmentName(new DocumentName("wiki", "space", "page"), "filename");

        Assert.assertTrue(attachmentName.equals(attachmentName));
        Assert.assertTrue(attachmentName.equals(
            new AttachmentName(new DocumentName("wiki", "space", "page"), "filename")));
        Assert.assertFalse(attachmentName.equals(
            new AttachmentName(new DocumentName("wiki", "space", "page"), "filename2")));
        Assert.assertFalse(attachmentName.equals(
            new AttachmentName(new DocumentName("wiki", "space", "page2"), "filename2")));
        Assert.assertFalse(attachmentName.equals(
            new AttachmentName(new DocumentName(null, null, null), null)));
        Assert.assertTrue(new AttachmentName(new DocumentName(null, null, null), null).equals(
            new AttachmentName(new DocumentName(null, null, null), null)));
    }

    @Test
    public void testHashCode()
    {
        AttachmentName attachmentName = new AttachmentName(new DocumentName("wiki", "space", "page"), "filename");

        Assert.assertTrue(attachmentName.hashCode() == attachmentName.hashCode());
        Assert.assertTrue(attachmentName.hashCode() == new AttachmentName(
            new DocumentName("wiki", "space", "page"), "filename").hashCode());
        Assert.assertFalse(attachmentName.hashCode() == new AttachmentName(
            new DocumentName("wiki", "space", "page"), "filename2").hashCode()); 
        Assert.assertFalse(attachmentName.hashCode() == new AttachmentName(
            new DocumentName("wiki", "space", "page2"), "filename").hashCode()); 
    }
}
