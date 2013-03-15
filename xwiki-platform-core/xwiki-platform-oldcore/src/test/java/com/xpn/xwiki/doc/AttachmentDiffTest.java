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
package com.xpn.xwiki.doc;

import org.junit.Test;

import junit.framework.Assert;

/**
 * Unit test for {@link AttachmentDiff}.
 *
 * @version $Id$
 * @since 5.0M2
 */
public class AttachmentDiffTest
{
    @Test
    public void toStringWhenVersionsNotNull()
    {
        AttachmentDiff diff = new AttachmentDiff("filename", "1.1", "1.2");
        Assert.assertEquals("filename: 1.1 -&gt; 1.2", diff.toString());
    }

    @Test
    public void toStringWhenNoOriginalVersion()
    {
        AttachmentDiff diff = new AttachmentDiff("filename", null, "1.1");
        Assert.assertEquals("filename: () -&gt; 1.1", diff.toString());
    }

    @Test
    public void toStringWhenNoNewVersion()
    {
        AttachmentDiff diff = new AttachmentDiff("filename", "1.1", null);
        Assert.assertEquals("filename: 1.1 -&gt; ()", diff.toString());
    }

    @Test
    public void toStringWhenNoVersions()
    {
        AttachmentDiff diff = new AttachmentDiff("filename", null, null);
        Assert.assertEquals("filename: () -&gt; ()", diff.toString());
    }
}
