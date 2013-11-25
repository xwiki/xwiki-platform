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
package org.xwiki.linkchecker.internal;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.*;
import org.xwiki.rendering.transformation.linkchecker.LinkCheckerTransformationConfiguration;
import org.xwiki.test.ComponentManagerRule;
import org.xwiki.test.annotation.ComponentList;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link org.xwiki.linkchecker.internal.XWikiLinkCheckerTransformationConfiguration}.
 *
 * @version $Id$
 * @since 5.3RC1
 */
@ComponentList({
    XWikiLinkCheckerTransformationConfiguration.class
})
public class XWikiLinkCheckerTransformationConfigurationTest
{
    @Rule
    public ComponentManagerRule componentManager = new ComponentManagerRule();

    @Test
    public void getExcludedReferencePatterns() throws Exception
    {
        LinkCheckerTransformationConfiguration configuration =
            this.componentManager.getInstance(LinkCheckerTransformationConfiguration.class);
        List<Pattern> patterns = configuration.getExcludedReferencePatterns();
        assertEquals(1, patterns.size());
        assertTrue(patterns.get(0).matcher("xwiki:XWiki.ExternalLinksJSON").matches());
        assertFalse(patterns.get(0).matcher("xwiki:SomeSpace.SomePage").matches());
    }
}
