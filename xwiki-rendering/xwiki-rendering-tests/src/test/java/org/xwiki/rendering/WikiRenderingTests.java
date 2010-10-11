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

import org.xwiki.rendering.configuration.RenderingConfiguration;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfiguration;
import org.xwiki.rendering.scaffolding.MockWikiModel;
import org.xwiki.rendering.scaffolding.RenderingTestSuite;
import org.xwiki.rendering.wiki.WikiModel;
import org.xwiki.test.ComponentManagerTestSetup;

/**
 * Rendering tests requiring a {@link WikiModel} implementation (ie tests that must have the notion of a wiki to run
 * fine).
 * 
 * @version $Id$
 * @since 2.0M1
 */
public class WikiRenderingTests extends TestCase
{
    public static junit.framework.Test suite() throws Exception
    {
        RenderingTestSuite suite = new RenderingTestSuite("Rendering tests requiring the wiki notion", "wiki");

        ComponentManagerTestSetup testSetup = new ComponentManagerTestSetup(suite);
        testSetup.addComponentDescriptor(MockWikiModel.getComponentDescriptor());

        // Add InterWiki Definition for links28 test
        DefaultRenderingConfiguration renderingConfiguration = 
            (DefaultRenderingConfiguration) testSetup.getComponentManager().lookup(RenderingConfiguration.class);
        renderingConfiguration.addInterWikiDefinition("knownalias", "http://server/common/url/");

        return testSetup;
    }
}
