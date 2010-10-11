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
package org.xwiki.rendering;

import junit.framework.TestCase;

import org.xwiki.rendering.scaffolding.RenderingTestSuite;
import org.xwiki.rendering.wiki.WikiModel;
import org.xwiki.test.ComponentManagerTestSetup;

/**
 * Rendering tests not requiring a {@link WikiModel} implementation (ie tests that don't need the notion of Wiki to
 * run).
 * 
 * @version $Id$
 * @since 2.0M1
 */
public class SimpleRenderingTests extends TestCase
{
    public static junit.framework.Test suite() throws Exception
    {
        RenderingTestSuite suite = new RenderingTestSuite("Rendering tests not requiring the wiki notion", "simple");
        return new ComponentManagerTestSetup(suite);
    }
}
