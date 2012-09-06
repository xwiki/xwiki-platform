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
package org.xwiki.model;

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Unit tests for {@link Content}.
 *
 * @version $Id$
 */
public class ContentTest
{
    @Test
    public void equalsAndHashcode()
    {
        Content content1 = new Content("text", Syntax.XWIKI_2_0);
        Content content2 = new Content("other text", Syntax.HTML_4_01);
        Content content3 = new Content("text", Syntax.XWIKI_2_0);
        Content content4 = new Content("text", Syntax.HTML_4_01);

        Assert.assertEquals(content1, content3);
        Assert.assertEquals(content1.hashCode(), content3.hashCode());

        // Different text, different syntax
        Assert.assertFalse(content1.equals(content2));

        // Same text, different syntax
        Assert.assertFalse(content1.equals(content4));

        // Different text, same syntax
        Assert.assertFalse(content2.equals(content4));
    }
}
