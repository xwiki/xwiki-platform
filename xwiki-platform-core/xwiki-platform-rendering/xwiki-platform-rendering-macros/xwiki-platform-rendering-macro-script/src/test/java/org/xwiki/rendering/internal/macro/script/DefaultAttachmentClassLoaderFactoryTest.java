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
package org.xwiki.rendering.internal.macro.script;

import java.net.URL;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.classloader.ExtendedURLClassLoader;
import org.xwiki.classloader.internal.ExtendedURLStreamHandlerFactory;
import org.xwiki.classloader.internal.protocol.attachmentjar.AttachmentURLStreamHandler;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link DefaultAttachmentClassLoaderFactory}.
 *  
 * @version $Id$
 * @since 2.0.1
 */
@ComponentTest
@ComponentList({ AttachmentURLStreamHandler.class, ExtendedURLStreamHandlerFactory.class })
public class DefaultAttachmentClassLoaderFactoryTest
{
    @InjectMockComponents
    private DefaultAttachmentClassLoaderFactory factory;

    @MockComponent
    @Named("current")
    private AttachmentReferenceResolver<String> arf;

    @MockComponent
    private DocumentAccessBridge dab;
    
    @Test
    void createAttachmentClassLoader() throws Exception
    {
        ExtendedURLClassLoader cl = factory.createAttachmentClassLoader(
            "attach:page@filename1, http://some/url, attach:filename2", null);

        assertEquals(3, cl.getURLs().length);
        assertEquals("attachmentjar://page%40filename1", cl.getURLs()[0].toString());
        assertEquals("http://some/url", cl.getURLs()[1].toString());
        assertEquals("attachmentjar://filename2", cl.getURLs()[2].toString());
    }
    
    @Test
    void extendClassLoaderLoader() throws Exception
    {
        ExtendedURLClassLoader cl = new ExtendedURLClassLoader(new URL[0]);
        factory.extendAttachmentClassLoader("attach:page@filename, http://some/url", cl);
        
        assertEquals("attachmentjar://page%40filename", cl.getURLs()[0].toString());
        assertEquals("http://some/url", cl.getURLs()[1].toString());
    }
}
