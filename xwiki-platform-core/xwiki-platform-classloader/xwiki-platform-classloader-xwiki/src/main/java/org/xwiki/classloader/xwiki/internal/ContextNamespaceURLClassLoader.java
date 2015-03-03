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
package org.xwiki.classloader.xwiki.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

import org.xwiki.classloader.ClassLoaderManager;
import org.xwiki.classloader.NamespaceURLClassLoader;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReferenceValueProvider;

/**
 * Dynamic classloader which select the current class loader based on context wiki each time it's called.
 * 
 * @version $Id$
 * @since 6.4M2
 */
public class ContextNamespaceURLClassLoader extends NamespaceURLClassLoader
{
    private final EntityReferenceValueProvider currentWikiProvider;

    private final ClassLoaderManager classLoaderManager;

    private String cachedCurrentWiki;

    private NamespaceURLClassLoader currentClassLoader;

    /**
     * @param currentWikiProvider the component used to access current wiki
     * @param classLoaderManager the component used to get the ClassLoader associated to current wiki
     */
    public ContextNamespaceURLClassLoader(EntityReferenceValueProvider currentWikiProvider,
        ClassLoaderManager classLoaderManager)
    {
        // Note: it's important to set the parent CL to be the Context CL since some third party frameworks can use
        // that information. For example the Apache Naming (JNDI) implementation will get the context CL to check if
        // there's any NamingContext instance bound to it and at Tomcat's init (for example), Tomcat binds the defined
        // DataSource to the Tomcat CL (WebappClassLoader). Thus if we loose the path from our CL to the Tomcat CL, all
        // the DataSources defined in Tomcat will fail to be usable from our Hibernate code.
        super(new URI[] {}, Thread.currentThread().getContextClassLoader(), null);

        this.currentWikiProvider = currentWikiProvider;
        this.classLoaderManager = classLoaderManager;
    }

    // TODO: Add support for context user ?
    private NamespaceURLClassLoader getCurrentClassLoader()
    {
        String currentWiki = this.currentWikiProvider.getDefaultValue(EntityType.WIKI);

        if (!Objects.equals(currentWiki, this.cachedCurrentWiki)) {
            this.currentClassLoader =
                this.classLoaderManager.getURLClassLoader(currentWiki != null ? "wiki:" + currentWiki : null, false);

            if (this.currentClassLoader == null) {
                // Fallback on system classloader in the very weird edge case where ClassLoaderManager does not return
                // any (which is already supposed to fallback on system classloader)
                this.currentClassLoader = new NamespaceURLClassLoader(new URI[] {}, getSystemClassLoader(), null);
            }

            this.cachedCurrentWiki = currentWiki;
        }

        return this.currentClassLoader;
    }

    @Override
    public String getNamespace()
    {
        return getCurrentClassLoader().getNamespace();
    }

    @Override
    public void addURL(URL url)
    {
        getCurrentClassLoader().addURL(url);
    }

    @Override
    public void addURLs(List<URL> urls)
    {
        getCurrentClassLoader().addURLs(urls);
    }

    @Override
    public URL[] getURLs()
    {
        return getCurrentClassLoader().getURLs();
    }

    @Override
    public URL getResource(String name)
    {
        return getCurrentClassLoader().getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException
    {
        return getCurrentClassLoader().getResources(name);
    }

    @Override
    public InputStream getResourceAsStream(String name)
    {
        return getCurrentClassLoader().getResourceAsStream(name);
    }

    @Override
    public URL findResource(String name)
    {
        return getCurrentClassLoader().findResource(name);
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException
    {
        return getCurrentClassLoader().findResources(name);
    }

    @Override
    public void close() throws IOException
    {
        getCurrentClassLoader().close();
    }

    @Override
    public void clearAssertionStatus()
    {
        getCurrentClassLoader().clearAssertionStatus();
    }

    @Override
    public void setClassAssertionStatus(String className, boolean enabled)
    {
        getCurrentClassLoader().setClassAssertionStatus(className, enabled);
    }

    @Override
    public void setDefaultAssertionStatus(boolean enabled)
    {
        getCurrentClassLoader().setDefaultAssertionStatus(enabled);
    }

    @Override
    public void setPackageAssertionStatus(String packageName, boolean enabled)
    {
        getCurrentClassLoader().setPackageAssertionStatus(packageName, enabled);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException
    {
        return getCurrentClassLoader().loadClass(name);
    }

    // protected

    /**
     * {@inheritDoc}
     * <p>
     * Yes it's a protected method but it's sometime called directly from {@link ClassLoader#loadClass(String, String)}.
     * 
     * @see java.lang.ClassLoader#loadClass(java.lang.String, boolean)
     */
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
    {
        return getCurrentClassLoader().loadClass(name);
    }
}
