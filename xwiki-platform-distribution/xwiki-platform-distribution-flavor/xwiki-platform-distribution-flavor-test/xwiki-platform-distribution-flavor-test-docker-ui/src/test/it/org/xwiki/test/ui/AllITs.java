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
package org.xwiki.test.ui;

import org.junit.jupiter.api.Nested;
import org.xwiki.test.docker.junit5.UITest;

/**
 * TODO: Please document me.
 *
 * @version $Id$
 * @since 12.8RC1
 */
@UITest
public class AllITs
{
    @Nested
    class NestedBacklinksTest extends BacklinksTest
    {
    }

    @Nested
    class NestedCommentAsAdminTest extends CommentAsAdminTest
    {
    }

    @Nested
    class NestedCommentAsGuestTest extends CommentAsGuestTest
    {
    }

    @Nested
    class NestedCompareVersionsTest extends CompareVersionsTest
    {
    }

    @Nested
    class NestedCopyPageTest extends CopyPageTest
    {
    }

    @Nested
    class NestedEditClassTest extends EditClassTest
    {
    }

    @Nested
    class NestedEditInlineTest extends EditInlineTest
    {
    }
}
