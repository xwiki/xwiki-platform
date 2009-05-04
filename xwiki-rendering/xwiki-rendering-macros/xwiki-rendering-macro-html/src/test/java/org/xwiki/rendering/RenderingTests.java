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

import junit.framework.Test;
import junit.framework.TestCase;

import org.xwiki.rendering.scaffolding.RenderingPlexusTestSetup;
import org.xwiki.rendering.scaffolding.RenderingTestSuite;

/**
 * All Rendering integration tests defined in text files using a special format.
 * 
 * @version $Id$
 * @since 1.7M3
 */
public class RenderingTests extends TestCase
{
    public static Test suite() throws Exception
    {
        RenderingTestSuite suite = new RenderingTestSuite("Test the HTML Macro");

/*        suite.addTestsFromResource("macrohtml1", true);
        suite.addTestsFromResource("macrohtml2", true);
        suite.addTestsFromResource("macrohtml3", true);
        suite.addTestsFromResource("macrohtml4", true);
        suite.addTestsFromResource("macrohtml5", true);
        suite.addTestsFromResource("macrohtml6", true);
        suite.addTestsFromResource("macrohtml7", true);
        suite.addTestsFromResource("macrohtml8", true);
        suite.addTestsFromResource("macrohtml9", true);
        suite.addTestsFromResource("macrohtml10", true);
        suite.addTestsFromResource("macrohtml11", true);
        suite.addTestsFromResource("macrohtml12", true);*/
        suite.addTestsFromResource("macrohtml13", true);

        return new RenderingPlexusTestSetup(suite);
    }
}
