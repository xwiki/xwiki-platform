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
package org.xwiki.classloader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLStreamHandlerFactory;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import junit.framework.Assert;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;
import org.xwiki.model.AttachmentName;
import org.xwiki.model.AttachmentNameFactory;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Unit tests for {@link URIClassLoader}.
 * 
 * @version $Id$
 * @since 2.0.1
 */
public class URIClassLoaderTest extends AbstractComponentTestCase
{
    private Mockery mockery = new Mockery();

    private AttachmentNameFactory anf;

    private DocumentAccessBridge dab;

    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        this.anf = this.mockery.mock(AttachmentNameFactory.class);
        DefaultComponentDescriptor<AttachmentNameFactory> descriptorANF =
            new DefaultComponentDescriptor<AttachmentNameFactory>();
        descriptorANF.setRole(AttachmentNameFactory.class);
        getComponentManager().registerComponent(descriptorANF, this.anf);

        this.dab = this.mockery.mock(DocumentAccessBridge.class);
        DefaultComponentDescriptor<DocumentAccessBridge> descriptorDAB =
            new DefaultComponentDescriptor<DocumentAccessBridge>();
        descriptorDAB.setRole(DocumentAccessBridge.class);
        getComponentManager().registerComponent(descriptorDAB, this.dab);
    }

    /**
     * Verify that resource located in a URI with an attachmentjar protocol can be found.
     */
    @Test
    public void testFindResource() throws Exception
    {
        URIClassLoader cl =
            new URIClassLoader(new URI[] {new URI("attachmentjar://page%40filename1"), new URI("http://some/url"),
            new URI("attachmentjar://filename2")}, getComponentManager().lookup(URLStreamHandlerFactory.class));

        Assert.assertEquals(3, cl.getURLs().length);
        Assert.assertEquals("attachmentjar://page%40filename1", cl.getURLs()[0].toString());
        Assert.assertEquals("http://some/url", cl.getURLs()[1].toString());
        Assert.assertEquals("attachmentjar://filename2", cl.getURLs()[2].toString());

        final AttachmentName attachmentName1 = new AttachmentName("wiki", "space", "page", "filename1");
        final AttachmentName attachmentName2 = new AttachmentName("wiki", "space", "page", "filename2");

        mockery.checking(new Expectations() {{
            allowing(anf).createAttachmentName("page@filename1");
            will(returnValue(attachmentName1));
            oneOf(dab).getAttachmentContent(attachmentName1);
            will(returnValue(new ByteArrayInputStream(createJarFile("/nomatch"))));
            allowing(anf).createAttachmentName("filename2");
            will(returnValue(attachmentName2));
            oneOf(dab).getAttachmentContent(attachmentName2);
            will(returnValue(new ByteArrayInputStream(createJarFile("/something"))));
        }});

        Assert.assertEquals("jar:attachmentjar://filename2!/something", cl.findResource("/something").toString());
    }

    private byte[] createJarFile(String resourceName) throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JarOutputStream jos = new JarOutputStream(baos);
        JarEntry entry = new JarEntry(resourceName);
        jos.putNextEntry(entry);
        jos.write("whatever".getBytes());
        jos.closeEntry();
        jos.close();
        return baos.toByteArray();
    }
}
