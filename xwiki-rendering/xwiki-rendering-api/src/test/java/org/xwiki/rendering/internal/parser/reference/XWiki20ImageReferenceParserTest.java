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
 * Unit tests for {@link XWiki20ImageReferenceParser}.
 *
 * @version $Id$
 * @since 2.5RC1
 */
public class XWiki20ImageReferenceParserTest extends AbstractImageReferenceParserTest
{
    @Override
    protected void registerComponents() throws Exception
    {
        // Create a Mock WikiModel implementation so that the link parser works in wiki mode
        registerMockComponent(WikiModel.class);

        this.parser = getComponentManager().lookup(ResourceReferenceParser.class, "xwiki/2.0/image");
    }

    @Test
    public void testParseImages() throws Exception
    {
        // Verify that "attach:" prefix isn't taken into account in XWiki Syntax 2.0.
        ResourceReference reference = parser.parse("attach:wiki:space.page@filename");
        Assert.assertEquals(ResourceType.ATTACHMENT, reference.getType());
        Assert.assertEquals("attach:wiki:space.page@filename", reference.getReference());
        Assert.assertFalse(reference.isTyped());
        Assert.assertEquals("Typed = [false] Type = [attach] Reference = [attach:wiki:space.page@filename]",
            reference.toString());
    }
}
