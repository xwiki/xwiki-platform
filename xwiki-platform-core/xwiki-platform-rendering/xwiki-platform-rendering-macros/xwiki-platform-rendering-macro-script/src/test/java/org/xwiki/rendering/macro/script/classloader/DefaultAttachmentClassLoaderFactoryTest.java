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
package org.xwiki.rendering.macro.script.classloader;

import java.net.URL;

import junit.framework.Assert;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.classloader.ExtendedURLClassLoader;
import org.xwiki.classloader.URIClassLoader;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.rendering.internal.macro.script.AttachmentClassLoaderFactory;
import org.xwiki.rendering.internal.macro.script.DefaultAttachmentClassLoaderFactory;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Unit tests for {@link DefaultAttachmentClassLoaderFactory}.
 *  
 * @version $Id$
 * @since 2.0.1
 */
public class DefaultAttachmentClassLoaderFactoryTest extends AbstractComponentTestCase
{
    private AttachmentClassLoaderFactory factory;
    
    private AttachmentReferenceResolver<String> arf;
    
    private DocumentAccessBridge dab;

    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        this.arf = registerMockComponent(AttachmentReferenceResolver.TYPE_STRING, "current");
        this.dab = registerMockComponent(DocumentAccessBridge.class);

        this.factory = getComponentManager().getInstance(AttachmentClassLoaderFactory.class);
    }
    
    @org.junit.Test
    public void testCreateAttachmentClassLoader() throws Exception
    {
        URIClassLoader cl = (URIClassLoader) factory.createAttachmentClassLoader(
            "attach:page@filename1, http://some/url, attach:filename2", null);

        Assert.assertEquals(3, cl.getURLs().length);
        Assert.assertEquals("attachmentjar://page%40filename1", cl.getURLs()[0].toString());
        Assert.assertEquals("http://some/url", cl.getURLs()[1].toString());
        Assert.assertEquals("attachmentjar://filename2", cl.getURLs()[2].toString());
    }
    
    @org.junit.Test
    public void testExtendClassLoaderLoader() throws Exception
    {
        ExtendedURLClassLoader cl = new ExtendedURLClassLoader(new URL[0]);
        factory.extendAttachmentClassLoader("attach:page@filename, http://some/url", cl);
        
        Assert.assertEquals("attachmentjar://page%40filename", cl.getURLs()[0].toString());
        Assert.assertEquals("http://some/url", cl.getURLs()[1].toString());
    }
}
