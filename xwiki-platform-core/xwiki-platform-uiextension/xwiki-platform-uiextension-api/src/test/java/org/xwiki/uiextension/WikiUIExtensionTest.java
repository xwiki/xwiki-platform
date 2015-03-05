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
package org.xwiki.uiextension;

import org.apache.commons.collections.MapUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.wiki.WikiComponentScope;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.uiextension.internal.WikiUIExtension;

/**
 * Unit tests for {@link WikiUIExtension}.
 *
 * @version $Id$
 * @since 4.2M3
 */
public class WikiUIExtensionTest
{
    private static final DocumentReference CLASS_REF = new DocumentReference("xwiki", "XWiki", "UIExtensionClass");

    private static final DocumentReference DOC_REF = new DocumentReference("xwiki", "XWiki", "MyUIExtension");

    private static final DocumentReference AUTHOR_REFERENCE = new DocumentReference("xwiki", "XWiki", "Admin");

    private ObjectReference objectReference;

    @Before
    public void setUp() throws Exception
    {
        objectReference = new ObjectReference(CLASS_REF.toString() + "[1]", DOC_REF);
    }

    @Test
    public void createWikiUIExtension()
    {
        WikiUIExtension wikiUIX =
            new WikiUIExtension("roleHint", "id", "epId", objectReference, AUTHOR_REFERENCE, null);
        wikiUIX.setScope(WikiComponentScope.WIKI);

        Assert.assertEquals("roleHint", wikiUIX.getRoleHint());
        Assert.assertEquals("id", wikiUIX.getId());
        Assert.assertEquals("epId", wikiUIX.getExtensionPointId());
        Assert.assertEquals(AUTHOR_REFERENCE, wikiUIX.getAuthorReference());
        Assert.assertEquals(UIExtension.class, wikiUIX.getRoleType());
        Assert.assertEquals(WikiComponentScope.WIKI, wikiUIX.getScope());
        Assert.assertEquals(new WordBlock(""), wikiUIX.execute());
        Assert.assertEquals(MapUtils.EMPTY_MAP, wikiUIX.getParameters());
    }
}
