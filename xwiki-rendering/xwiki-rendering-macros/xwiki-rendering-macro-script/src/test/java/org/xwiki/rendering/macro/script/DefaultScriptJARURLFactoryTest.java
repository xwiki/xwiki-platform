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
package org.xwiki.rendering.macro.script;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.jmock.Expectations;
import org.junit.Test;
import org.xwiki.bridge.AttachmentName;
import org.xwiki.bridge.DocumentName;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Unit tests for {@link DefaultScriptJARURLFactoryTest}.
 * 
 * @version $Id$
 * @since 2.0RC1
 */
public class DefaultScriptJARURLFactoryTest extends AbstractComponentTestCase
{
    private ScriptMockSetup mockSetup;

    private ScriptJARURLFactory factory;
    
    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();
        this.mockSetup = new ScriptMockSetup(getComponentManager());

        this.factory = getComponentManager().lookup(ScriptJARURLFactory.class);
    }

    @Test
    public void testExtraJarLocatedAtURL() throws Exception
    {
        List<URL> urls = this.factory.createJARURLs("http://path/to/some.jar");
        Assert.assertEquals(1, urls.size());
        Assert.assertTrue(urls.contains(new URL("http://path/to/some.jar")));
    }

    @Test
    public void testExtraJarsLocatedAtURL() throws Exception
    {
        // Note: we test with spaces between urls too below.
        List<URL> urls = this.factory.createJARURLs(
            "http://path1/to/some.jar,http://path2/to/some.jar,   http://path3/to/some.jar");
        Assert.assertEquals(3, urls.size());
        Assert.assertTrue(urls.contains(new URL("http://path1/to/some.jar")));
        Assert.assertTrue(urls.contains(new URL("http://path2/to/some.jar")));
        Assert.assertTrue(urls.contains(new URL("http://path3/to/some.jar")));
    }

    @Test
    public void testExtraJarLocatedInSpecifiedDocument() throws Exception
    {
        final AttachmentName attachmentName = new AttachmentName(
            new DocumentName("wiki", "space", "page"), "some.jar");
            
        this.mockSetup.mockery.checking(new Expectations() {{
            oneOf(mockSetup.attachmentNameFactory).createAttachmentName("wiki:space.page@some.jar");
                will(returnValue(attachmentName));
            oneOf(mockSetup.bridge).getAttachmentURL(with(same(attachmentName)), with(equal(true)));
                will(returnValue("http://path/to/some.jar"));
        }});

        List<URL> urls = this.factory.createJARURLs("attach:wiki:space.page@some.jar");
        Assert.assertTrue(urls.contains(new URL("http://path/to/some.jar")));
    }

    @Test
    public void testExtraJarLocatedInCurrentDocument() throws Exception
    {
        final DocumentName documentName = new DocumentName("wiki", "space", "page"); 
        this.mockSetup.mockery.checking(new Expectations() {{
            oneOf(mockSetup.bridge).getCurrentDocumentName(); will(returnValue(documentName));
            oneOf(mockSetup.bridge).getAttachmentURL(new AttachmentName(documentName, "some.jar"), true);
                will(returnValue("http://path/to/some.jar"));
        }});

        List<URL> urls = this.factory.createJARURLs("attach:some.jar");
        Assert.assertTrue(urls.contains(new URL("http://path/to/some.jar")));
    }
    
    @Test
    public void testAllJarsLocatedInSpecifiedDocument() throws Exception
    {
        final DocumentName documentName = new DocumentName("wiki", "space", "page"); 
        this.mockSetup.mockery.checking(new Expectations() {{
            oneOf(mockSetup.documentNameFactory).createDocumentName("wiki:space.page");
                will(returnValue(documentName));
            oneOf(mockSetup.bridge).getAttachmentURLs(documentName, true);
            will(returnValue(Arrays.asList(
                "http://path/to/some1.jar", "http://path/to/notajar.txt", "http://path/to/some2.jar")));
        }});
        
        List<URL> urls = this.factory.createJARURLs("attach:wiki:space.page");
        Assert.assertEquals(2, urls.size());
        Assert.assertTrue(urls.contains(new URL("http://path/to/some1.jar")));
        Assert.assertTrue(urls.contains(new URL("http://path/to/some2.jar")));
    }
}
