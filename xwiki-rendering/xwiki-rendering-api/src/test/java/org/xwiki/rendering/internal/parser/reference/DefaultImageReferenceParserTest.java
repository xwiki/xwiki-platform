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
import org.xwiki.rendering.wiki.WikiModel;

/**
 * Unit tests for {@link DefaultImageReferenceParser}.
 *
 * @version $Id$
 * @since 2.6M1
 */
public class DefaultImageReferenceParserTest extends AbstractImageReferenceParserTest
{
    @Override
    protected void registerComponents() throws Exception
    {
        // Create a Mock WikiModel implementation so that the link parser works in wiki mode
        registerMockComponent(WikiModel.class);

        this.parser = getComponentManager().lookup(ResourceReferenceParser.class, "image");
    }

    @Test
    public void testParseImages() throws Exception
    {
        ResourceReference reference = parser.parse("attach:wiki:space.page@filename");
        Assert.assertEquals(ResourceType.ATTACHMENT, reference.getType());
        Assert.assertEquals("wiki:space.page@filename", reference.getReference());
        Assert.assertTrue(reference.isTyped());
        Assert.assertEquals("Typed = [true] Type = [attach] Reference = [wiki:space.page@filename]",
            reference.toString());

        // Verify path: support
        reference = parser.parse("path:/some/image");
        Assert.assertEquals(ResourceType.PATH, reference.getType());
        Assert.assertEquals("/some/image", reference.getReference());
        Assert.assertTrue(reference.isTyped());
        Assert.assertEquals("Typed = [true] Type = [path] Reference = [/some/image]",
            reference.toString());

        // Verify icon: support
        reference = parser.parse("icon:name");
        Assert.assertEquals(ResourceType.ICON, reference.getType());
        Assert.assertEquals("name", reference.getReference());
        Assert.assertTrue(reference.isTyped());
        Assert.assertEquals("Typed = [true] Type = [icon] Reference = [name]", reference.toString());
    }
}
