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
package org.xwiki.filter.test.ui.docker;

import org.junit.jupiter.api.Nested;
import org.xwiki.test.docker.junit5.UITest;

/**
 * All functional Filter tests. Note that XWiki is started/stopped only once during all the tests and thus they must all
 * work as independent scenarios sharing the same XWiki instance (this reproduces the behavior of the former
 * {@code PageObjectSuite}-based suite).
 *
 * @version $Id$
 */
@UITest(
    properties = {
        // The Filter Stream app's home page (Filter.WebHome) requires Programming Rights, so we need to exclude it
        // from the Programming Rights checker.
        "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:Filter\\.WebHome"
    }
)
class AllIT
{
    @Nested
    class NestedFilterIT extends FilterIT
    {
    }
}
