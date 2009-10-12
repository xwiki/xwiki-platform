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

import org.apache.commons.io.IOUtils;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentName;
import org.xwiki.bridge.DocumentNameSerializer;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.rendering.scaffolding.RenderingTestSuite;
import org.xwiki.test.ComponentManagerTestSetup;

/**
 * Rendering tests for the chart macro.
 *
 * @version $Id$
 * @since 2.0M1
 */
public class RenderingTests extends TestCase
{
    /**
     * Builds and returns a new {@link ComponentManagerTestSetup}.
     * 
     * @return a {@link ComponentManagerTestSetup}.
     * @throws Exception if an error occurs while building the test setup.
     */
    public static Test suite() throws Exception
    {
        RenderingTestSuite suite = new RenderingTestSuite("Test Chart Macro");

        suite.addTestsFromResource("macrochart1", true);        
        suite.addTestsFromResource("macrochart2", true);
        suite.addTestsFromResource("macrochart3", true);
        suite.addTestsFromResource("macrochart4", true);

        ComponentManagerTestSetup testSetup = new ComponentManagerTestSetup(suite);
        setUpMocks(testSetup.getComponentManager());

        return testSetup;
    }
    
    public static void setUpMocks(EmbeddableComponentManager componentManager) throws Exception
    {
        Mockery context = new Mockery();
        
        // Document Access Bridge Mock
        final DocumentAccessBridge mockDocumentAccessBridge = context.mock(DocumentAccessBridge.class);
        DefaultComponentDescriptor<DocumentAccessBridge> descriptorDAB =
            new DefaultComponentDescriptor<DocumentAccessBridge>();
        descriptorDAB.setRole(DocumentAccessBridge.class);
        componentManager.registerComponent(descriptorDAB, mockDocumentAccessBridge);

        final DocumentName documentName = new DocumentName(null, "Test", "Test");
        
        context.checking(new Expectations() {{
            allowing(mockDocumentAccessBridge).getURL(with(any(String.class)), with(any(String.class)), 
                with(any(String.class)), with(any(String.class))); will(returnValue("http://localhost/charts"));
            allowing(mockDocumentAccessBridge).getCurrentDocumentName(); will(returnValue(documentName));
            allowing(mockDocumentAccessBridge).exists(with(any(String.class))); will(returnValue(true));
            allowing(mockDocumentAccessBridge).getDocumentSyntaxId(with(any(String.class))); will(returnValue("xwiki/2.0"));
            allowing(mockDocumentAccessBridge).getDocumentContent("Test.Test");
                will(returnValue(IOUtils.toString(RenderingTests.class.getClassLoader().getResourceAsStream("wiki.txt"))));
        }});
        
        // Document Name Serializer Mock
        final DocumentNameSerializer mockDocumentNameSerializer = context.mock(DocumentNameSerializer.class);
        DefaultComponentDescriptor<DocumentNameSerializer> descriptorDNS =
            new DefaultComponentDescriptor<DocumentNameSerializer>();
        descriptorDNS.setRole(DocumentNameSerializer.class);
        componentManager.registerComponent(descriptorDNS, mockDocumentNameSerializer);

        context.checking(new Expectations() {{
            allowing(mockDocumentNameSerializer).serialize(documentName); will(returnValue("Test.Test"));
        }});
    }    
}