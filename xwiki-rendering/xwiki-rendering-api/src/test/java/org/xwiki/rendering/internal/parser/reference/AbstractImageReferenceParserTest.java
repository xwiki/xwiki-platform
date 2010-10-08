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
package org.xwiki.rendering.internal.parser.reference;

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.parser.ResourceReferenceParser;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Common tests for Image implementations of {@link org.xwiki.rendering.parser.ResourceReferenceParser} for 
 * XWiki Syntax 2.0 and 2.1.
 *
 * @version $Id$
 * @since 2.5RC1
 */
public abstract class AbstractImageReferenceParserTest extends AbstractComponentTestCase
{
    protected ResourceReferenceParser parser;

    @Test
    public void testParseImagesCommon() throws Exception
    {
        // Verify that non-typed image referencing an attachment works.
        ResourceReference reference = parser.parse("wiki:space.page@filename");
        Assert.assertEquals(ResourceType.ATTACHMENT, reference.getType());
        Assert.assertEquals("wiki:space.page@filename", reference.getReference());
        Assert.assertEquals("Typed = [false] Type = [attach] Reference = [wiki:space.page@filename]",
            reference.toString());
        Assert.assertFalse(reference.isTyped());

        // Verify that non-typed image referencing a URL works.
        reference = parser.parse("http://server/path/to/image");
        Assert.assertEquals(ResourceType.URL, reference.getType());
        Assert.assertEquals("http://server/path/to/image", reference.getReference());
        Assert.assertEquals("Typed = [false] Type = [url] Reference = [http://server/path/to/image]",
            reference.toString());
        Assert.assertFalse(reference.isTyped());

    }
}
