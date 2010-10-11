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

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.xwiki.bridge.SkinAccessBridge;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.rendering.scaffolding.RenderingTestSuite;
import org.xwiki.test.ComponentManagerTestSetup;

/**
 * All Rendering integration tests defined in text files using a special format.
 * 
 * @version $Id$
 * @since 1.8RC1
 */
public class RenderingTests extends TestCase
{
    public static Test suite() throws Exception
    {
        RenderingTestSuite suite = new RenderingTestSuite("Test RSS Macro");
        ComponentManagerTestSetup testSetup = new ComponentManagerTestSetup(suite);
        setUpMocks(testSetup.getComponentManager());

        return testSetup;
    }
    
    public static void setUpMocks(EmbeddableComponentManager componentManager) throws Exception
    {
        Mockery context = new Mockery();

        final SkinAccessBridge mockSkinAccessBridge = context.mock(SkinAccessBridge.class);
        DefaultComponentDescriptor<SkinAccessBridge> descriptorSAB = new DefaultComponentDescriptor<SkinAccessBridge>();
        descriptorSAB.setRole(SkinAccessBridge.class);
        componentManager.registerComponent(descriptorSAB, mockSkinAccessBridge);

        context.checking(new Expectations() {{
            allowing(mockSkinAccessBridge).getSkinFile(with(any(String.class)));
                will(returnValue("/xwiki/skins/albatross/icons/black-rss.png"));
        }});
    }    
}
