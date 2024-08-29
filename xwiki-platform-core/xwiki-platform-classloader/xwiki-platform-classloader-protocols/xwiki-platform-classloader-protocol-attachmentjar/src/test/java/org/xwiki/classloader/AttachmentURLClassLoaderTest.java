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
import java.io.File;
import java.net.URL;
import java.net.URLStreamHandler;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.classloader.internal.DefaultClassLoaderManager;
import org.xwiki.classloader.internal.ExtendedURLStreamHandlerFactory;
import org.xwiki.classloader.internal.JarExtendedURLStreamHandler;
import org.xwiki.classloader.internal.protocol.attachmentjar.AttachmentURLStreamHandler;
import org.xwiki.environment.Environment;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.TestEnvironment;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for checking attachment URL.
 * 
 * @version $Id$
 */
@ComponentTest
@ComponentList({AttachmentURLStreamHandler.class, ExtendedURLStreamHandlerFactory.class,
    DefaultClassLoaderManager.class, JarExtendedURLStreamHandler.class, TestEnvironment.class})
class AttachmentURLClassLoaderTest
{
    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    @Named("current")
    private AttachmentReferenceResolver<String> arf;

    @MockComponent
    private DocumentAccessBridge dab;

    /**
     * Verify that resource located in a URI with an attachmentjar protocol can be found.
     */
    @Test
    void findResource() throws Exception
    {
        NamespaceURLClassLoader cl = this.componentManager.<ClassLoaderManager>getInstance(ClassLoaderManager.class)
            .getURLClassLoader(null, false);

        URLStreamHandler attachmentURLStreamHandler =
            this.componentManager.getInstance(ExtendedURLStreamHandler.class, "attachmentjar");
        cl.addURL(new URL(null, "attachmentjar://page%40filename1", attachmentURLStreamHandler));
        cl.addURL(new URL("http://some/url"));
        cl.addURL(new URL(null, "attachmentjar://filename2", attachmentURLStreamHandler));

        assertEquals(3, cl.getURLs().length);
        assertEquals("attachmentjar://page%40filename1", cl.getURLs()[0].toString());
        assertEquals("http://some/url", cl.getURLs()[1].toString());
        assertEquals("attachmentjar://filename2", cl.getURLs()[2].toString());

        final AttachmentReference attachmentName1 =
            new AttachmentReference("filename1", new DocumentReference("wiki", "space", "page"));
        final AttachmentReference attachmentName2 =
            new AttachmentReference("filename2", new DocumentReference("wiki", "space", "page"));

        when(this.arf.resolve("page@filename1")).thenReturn(attachmentName1);
        when(this.dab.getAttachmentContent(attachmentName1))
            .thenReturn(new ByteArrayInputStream(createJarFile("/nomatch")));

        when(this.arf.resolve("filename2")).thenReturn(attachmentName2);
        when(this.dab.getAttachmentContent(attachmentName2))
            .thenReturn(new ByteArrayInputStream(createJarFile("/something")));

        assertEquals("jar:attachmentjar://filename2!/something", cl.findResource("/something").toString());

        // Make sure closing the classloader get rid of temporary files
        cl.close();
        Environment environment = this.componentManager.getInstance(Environment.class);
        File jars = new File(environment.getTemporaryDirectory(), JarExtendedURLStreamHandler.JARS_FOLDER);
        assertEquals(0, jars.list().length);
    }

    private byte[] createJarFile(String resourceName) throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JarOutputStream jos = new JarOutputStream(baos);
        JarEntry entry = new JarEntry(resourceName);
        jos.putNextEntry(entry);
        jos.write("whatever".getBytes("UTF-8"));
        jos.closeEntry();
        jos.close();
        return baos.toByteArray();
    }
}
