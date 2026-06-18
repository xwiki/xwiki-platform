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
package org.xwiki.rest.test;

import org.junit.jupiter.api.Nested;
import org.xwiki.test.docker.junit5.UITest;

/**
 * All functional REST tests. Note that XWiki is started/stopped only once during all the tests and thus they must all
 * work as independent scenarios sharing the same XWiki instance (this reproduces the behavior of the former
 * {@code PageObjectSuite}-based suite).
 *
 * @version $Id$
 */
@UITest(
    properties = {
        // Allow the script-executing test pages (e.g. the {{groovy}} pages used to tweak the search configuration in
        // WikisResourceIT) to run without being blocked by the programming rights checker.
        "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:Test\\.Execute\\..*"
    }
)
class AllIT
{
    @Nested
    class NestedRootResourceIT extends RootResourceIT
    {
    }

    @Nested
    class NestedWikisResourceIT extends WikisResourceIT
    {
    }

    @Nested
    class NestedPagesResourceIT extends PagesResourceIT
    {
    }

    @Nested
    class NestedPageResourceIT extends PageResourceIT
    {
    }

    @Nested
    class NestedPageTranslationResourceIT extends PageTranslationResourceIT
    {
    }

    @Nested
    class NestedSpacesResourceIT extends SpacesResourceIT
    {
    }

    @Nested
    class NestedClassesResourceIT extends ClassesResourceIT
    {
    }

    @Nested
    class NestedObjectsResourceIT extends ObjectsResourceIT
    {
    }

    @Nested
    class NestedAttachmentsResourceIT extends AttachmentsResourceIT
    {
    }

    @Nested
    class NestedCommentsResourceIT extends CommentsResourceIT
    {
    }

    @Nested
    class NestedTagsResourceIT extends TagsResourceIT
    {
    }

    @Nested
    class NestedWikisIT extends WikisIT
    {
    }
}
