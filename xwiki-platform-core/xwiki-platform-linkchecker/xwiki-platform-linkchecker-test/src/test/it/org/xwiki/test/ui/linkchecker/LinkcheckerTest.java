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
package org.xwiki.test.ui.linkchecker;

import org.junit.Test;
import org.xwiki.test.ui.AbstractTest;

/**
 * UI tests for the Link Checker feature.
 *
 * @version $Id$
 * @since 3.4M1
 */
public class LinkcheckerTest extends AbstractTest
{
    @Test
    public void testLinkChecker()
    {
        // Navigate to the page listing the state of all links
        getUtil().gotoPage("XWiki", "ExternalLinks");

        // TODO: Continue test once we have moved the AllDocs page to a platform module.
    }
}
