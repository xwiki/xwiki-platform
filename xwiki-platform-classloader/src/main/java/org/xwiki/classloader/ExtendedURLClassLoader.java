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

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.List;

/**
 * Implementation that allows adding URLs on demand (the default {@link URLClassLoader} only allows
 * adding URLs in the constructor).
 *  
 * @version $Id$
 * @since 2.0.1
 */
public class ExtendedURLClassLoader extends URLClassLoader
{
    /**
     * See {@link URLClassLoader#URLClassLoader(URL[], ClassLoader, URLStreamHandlerFactory)}.
     * 
     * @param urls the URLs from which to load classes and resources 
     * @param parent the parent class loader for delegation
     * @param factory the URLStreamHandlerFactory to use when creating URLs 
     */
    public ExtendedURLClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory)
    {
        super(urls, parent, factory);
    }

    /**
     * See {@link URLClassLoader#URLClassLoader(URL[], ClassLoader)}.
     * 
     * @param urls the URLs from which to load classes and resources 
     * @param parent the parent class loader for delegation
     */
    public ExtendedURLClassLoader(URL[] urls, ClassLoader parent)
    {
        super(urls, parent);
    }

    /**
     * See {@link URLClassLoader#URLClassLoader(URL[])}.
     * 
     * @param urls the URLs from which to load classes and resources 
     */
    public ExtendedURLClassLoader(URL[] urls)
    {
        super(urls);
    }
    
    public ExtendedURLClassLoader(ClassLoader parent, URLStreamHandlerFactory factory)
    {
        this(new URL[0], parent, factory);
    }

    /**
     * @param url the JAR URL to add
     */
    @Override
    public void addURL(URL url)
    {
        super.addURL(url);
    }
    
    /**
     * @param urls the JAR URLs to add
     */
    public void addURLs(List<URL> urls)
    {
        for (URL url : urls) {
            addURL(url);
        }
    }
}
