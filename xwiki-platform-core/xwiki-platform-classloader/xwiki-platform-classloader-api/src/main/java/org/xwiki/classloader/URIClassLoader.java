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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;

import org.xwiki.classloader.internal.ResourceLoader;

import edu.emory.mathcs.util.classloader.ResourceFinder;
import edu.emory.mathcs.util.classloader.ResourceHandle;

/**
 * Equivalent of java.net.URLClassloader but without bugs related to ill-formed URLs and with customizable JAR caching
 * policy. The standard URLClassLoader accepts URLs containing spaces and other characters which are forbidden in the
 * URI syntax, according to the RFC 2396. As a workaround to this problem, Java escapes and un-escapes URLs in various
 * arbitrary places; however, this is inconsistent and leads to numerous problems with URLs referring to local files
 * with spaces in the path. SUN acknowledges the problem, but refuses to modify the behavior for compatibility reasons;
 * see Java Bug Parade 4273532, 4466485.
 * <p>
 * Additionally, the JAR caching policy used by URLClassLoader is system-wide and inflexible: once downloaded JAR files
 * are never re-downloaded, even if one creates a fresh instance of the class loader that happens to have the same URL
 * in its search path. In fact, that policy is a security vulnerability: it is possible to crash any URL class loader,
 * thus affecting potentially separate part of the system, by creating URL connection to one of the URLs of that class
 * loader search path and closing the associated JAR file. See Java Bug Parade 4405789, 4388666, 4639900.
 * <p>
 * This class avoids these problems by 1) using URIs instead of URLs for the search path (thus enforcing strict syntax
 * conformance and defining precise escaping semantics), and 2) using custom URLStreamHandler which ensures
 * per-classloader JAR caching policy.
 * 
 * <p>
 * Originally written by Dawid Kurzyniec and released to the public domain, as explained
 * at http://creativecommons.org/licenses/publicdomain
 * </p>
 * <p>
 * Source: http://dcl.mathcs.emory.edu/php/loadPage.php?content=util/features.html#classloading
 * </p>
 *
 * @see java.io.File#toURL
 * @see java.io.File#toURI
 * @version $Id$d
 * @since 2.0.1
 */
public class URIClassLoader extends ExtendedURLClassLoader
{
    final URIResourceFinder finder;

    final AccessControlContext acc;

    /**
     * Creates URIClassLoader with the specified search path.
     * 
     * @param uris the search path
     */
    public URIClassLoader(URI[] uris)
    {
        this(uris, (URLStreamHandlerFactory) null);
    }

    /**
     * Creates URIClassLoader with the specified search path.
     * 
     * @param uris the search path
     * @param handlerFactory the URLStreamHandlerFactory to use when creating URLs
     */
    public URIClassLoader(URI[] uris, URLStreamHandlerFactory handlerFactory)
    {
        super(new URL[0]);
        this.finder = new URIResourceFinder(uris, handlerFactory);
        this.acc = AccessController.getContext();
    }

    /**
     * Creates URIClassLoader with the specified search path and parent class loader.
     * 
     * @param uris the search path
     * @param parent the parent class loader.
     */
    public URIClassLoader(URI[] uris, ClassLoader parent)
    {
        this(uris, parent, null);
    }

    /**
     * Creates URIClassLoader with the specified search path and parent class loader.
     * 
     * @param uris the search path
     * @param parent the parent class loader.
     * @param handlerFactory the URLStreamHandlerFactory to use when creating URLs
     */
    public URIClassLoader(URI[] uris, ClassLoader parent, URLStreamHandlerFactory handlerFactory)
    {
        super(new URL[0], parent);
        this.finder = new URIResourceFinder(uris, handlerFactory);
        this.acc = AccessController.getContext();
    }

    /**
     * Add specified URI at the end of the search path.
     * 
     * @param uri the URI to add
     */
    protected void addURI(URI uri)
    {
        this.finder.addURI(uri);
    }

    /**
     * Add specified URL at the end of the search path.
     * 
     * @param url the URL to add
     */
    @Override
    public void addURL(URL url)
    {
        this.finder.addURI(URI.create(url.toExternalForm()));
    }

    /**
     * Add specified URLs at the end of the search path.
     * 
     * @param urls the URLs to add
     */
    @Override
    public void addURLs(List<URL> urls)
    {
        for (URL url : urls) {
            addURL(url);
        }
    }

    @Override
    public URL[] getURLs()
    {
        return this.finder.getUrls().clone();
    }

    /**
     * Finds and loads the class with the specified name.
     * 
     * @param name the name of the class
     * @return the resulting class
     * @exception ClassNotFoundException if the class could not be found
     */
    @Override
    protected Class< ? > findClass(final String name) throws ClassNotFoundException
    {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Class< ? >>()
            {
                public Class< ? > run() throws ClassNotFoundException
                {
                    String path = name.replace('.', '/').concat(".class");
                    ResourceHandle h = URIClassLoader.this.finder.getResource(path);
                    if (h != null) {
                        try {
                            return defineClass(name, h);
                        } catch (IOException e) {
                            throw new ClassNotFoundException(name, e);
                        }
                    } else {
                        throw new ClassNotFoundException(name);
                    }
                }
            }, this.acc);
        } catch (java.security.PrivilegedActionException pae) {
            throw (ClassNotFoundException) pae.getException();
        }
    }

    protected Class< ? > defineClass(String name, ResourceHandle h) throws IOException
    {
        int i = name.lastIndexOf('.');
        URL url = h.getCodeSourceURL();
        if (i != -1) { // check package
            String pkgname = name.substring(0, i);
            // check if package already loaded
            Package pkg = getPackage(pkgname);
            Manifest man = h.getManifest();
            if (pkg != null) {
                // package found, so check package sealing
                boolean ok;
                if (pkg.isSealed()) {
                    // verify that code source URLs are the same
                    ok = pkg.isSealed(url);
                } else {
                    // make sure we are not attempting to seal the package
                    // at this code source URL
                    ok = (man == null) || !isSealed(pkgname, man);
                }
                if (!ok) {
                    throw new SecurityException("sealing violation: " + name);
                }
            } else { // package not yet defined
                if (man != null) {
                    definePackage(pkgname, man, url);
                } else {
                    definePackage(pkgname, null, null, null, null, null, null, null);
                }
            }
        }

        // now read the class bytes and define the class
        byte[] b = h.getBytes();
        java.security.cert.Certificate[] certs = h.getCertificates();
        CodeSource cs = new CodeSource(url, certs);
        return defineClass(name, b, 0, b.length, cs);
    }

    /**
     * returns true if the specified package name is sealed according to the given manifest.
     */
    private boolean isSealed(String name, Manifest man)
    {
        String path = name.replace('.', '/').concat("/");
        Attributes attr = man.getAttributes(path);
        String sealed = null;
        if (attr != null) {
            sealed = attr.getValue(Name.SEALED);
        }
        if (sealed == null) {
            if ((attr = man.getMainAttributes()) != null) {
                sealed = attr.getValue(Name.SEALED);
            }
        }
        return "true".equalsIgnoreCase(sealed);
    }

    /**
     * Finds the resource with the specified name.
     * 
     * @param name the name of the resource
     * @return a <code>URL</code> for the resource, or <code>null</code> if the resource could not be found.
     */
    @Override
    public URL findResource(final String name)
    {
        return AccessController.doPrivileged(new PrivilegedAction<URL>()
        {
            public URL run()
            {
                return URIClassLoader.this.finder.findResource(name);
            }
        }, this.acc);
    }

    /**
     * Returns an Enumeration of URLs representing all of the resources having the specified name.
     * 
     * @param name the resource name
     * @exception IOException if an I/O exception occurs
     * @return an <code>Enumeration</code> of <code>URL</code>s
     */
    @Override
    public Enumeration<URL> findResources(final String name) throws IOException
    {
        return AccessController.doPrivileged(new PrivilegedAction<Enumeration<URL>>()
        {
            public Enumeration<URL> run()
            {
                return URIClassLoader.this.finder.findResources(name);
            }
        }, this.acc);
    }

    /**
     * Returns the absolute path name of a native library. The VM invokes this method to locate the native libraries
     * that belong to classes loaded with this class loader. If this method returns <code>null</code>, the VM searches
     * the library along the path specified as the <code>java.library.path</code> property. This method invoke
     * {@link #getLibraryHandle} method to find handle of this library. If the handle is found and its URL protocol is
     * "file", the system-dependent absolute library file path is returned. Otherwise this method returns null.
     * <p>
     * Subclasses can override this method to provide specific approaches in library searching.
     * 
     * @param libname the library name
     * @return the absolute path of the native library
     * @see java.lang.System#loadLibrary(java.lang.String)
     * @see java.lang.System#mapLibraryName(java.lang.String)
     */
    @Override
    protected String findLibrary(String libname)
    {
        ResourceHandle md = getLibraryHandle(libname);
        if (md == null) {
            return null;
        }
        URL url = md.getURL();
        if (!"file".equals(url.getProtocol())) {
            return null;
        }
        return new File(URI.create(url.toString())).getPath();
    }

    /**
     * Finds the ResourceHandle object for the class with the specified name. Unlike <code>findClass()</code>, this
     * method does not load the class.
     * 
     * @param name the name of the class
     * @return the ResourceHandle of the class
     */
    protected ResourceHandle getClassHandle(final String name)
    {
        String path = name.replace('.', '/').concat(".class");
        return getResourceHandle(path);
    }

    /**
     * Finds the ResourceHandle object for the resource with the specified name.
     * 
     * @param name the name of the resource
     * @return the ResourceHandle of the resource
     */
    protected ResourceHandle getResourceHandle(final String name)
    {
        return AccessController.doPrivileged(new PrivilegedAction<ResourceHandle>()
        {
            public ResourceHandle run()
            {
                return URIClassLoader.this.finder.getResource(name);
            }
        }, this.acc);
    }

    /**
     * Finds the ResourceHandle object for the native library with the specified name. The library name must be
     * '/'-separated path. The last part of this path is substituted by its system-dependent mapping (using
     * {@link System#mapLibraryName(String)} method). Next, the <code>ResourceFinder</code> is used to look for the
     * library as it was ordinary resource.
     * <p>
     * Subclasses can override this method to provide specific approaches in library searching.
     * 
     * @param name the name of the library
     * @return the ResourceHandle of the library
     */
    protected ResourceHandle getLibraryHandle(final String name)
    {
        int idx = name.lastIndexOf('/');
        String path;
        String simplename;
        if (idx == -1) {
            path = "";
            simplename = name;
        } else if (idx == name.length() - 1) { // name.endsWith('/')
            throw new IllegalArgumentException(name);
        } else {
            path = name.substring(0, idx + 1); // including '/'
            simplename = name.substring(idx + 1);
        }
        return getResourceHandle(path + System.mapLibraryName(simplename));
    }

    /**
     * Returns an Enumeration of ResourceHandle objects representing all of the resources having the specified name.
     * 
     * @param name the name of the resource
     * @return the ResourceHandle of the resource
     */
    protected Enumeration<ResourceHandle> getResourceHandles(final String name)
    {
        return AccessController.doPrivileged(new PrivilegedAction<Enumeration<ResourceHandle>>()
        {
            public Enumeration<ResourceHandle> run()
            {
                return URIClassLoader.this.finder.getResources(name);
            }
        }, this.acc);
    }

    private static class URIResourceFinder implements ResourceFinder
    {
        URL[] urls;

        final ResourceLoader loader;

        final URLStreamHandlerFactory handlerFactory;

        public URIResourceFinder(URI[] uris, URLStreamHandlerFactory handlerFactory)
        {
            this.handlerFactory = handlerFactory;
            try {
                this.loader =
                    new ResourceLoader(handlerFactory != null ? handlerFactory.createURLStreamHandler("jar") : null);
                URL[] urls = new URL[uris.length];
                for (int i = 0; i < uris.length; i++) {
                    urls[i] = new URL(null, uris[i].toString(),
                        handlerFactory != null ? handlerFactory.createURLStreamHandler(uris[i].getScheme()) : null);
                }
                this.urls = urls;
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }

        public synchronized void addURI(URI uri)
        {
            try {
                URL url = new URL(null, uri.toString(), this.handlerFactory != null ? this.handlerFactory
                    .createURLStreamHandler(uri.getScheme()) : null);
                int len = this.urls.length;
                URL[] urls = new URL[len + 1];
                System.arraycopy(this.urls, 0, urls, 0, len);
                urls[len] = url;
                this.urls = urls;
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }

        private synchronized URL[] getUrls()
        {
            return this.urls;
        }

        public ResourceHandle getResource(String name)
        {
            return this.loader.getResource(getUrls(), name);
        }

        public Enumeration<ResourceHandle> getResources(String name)
        {
            return this.loader.getResources(getUrls(), name);
        }

        public URL findResource(String name)
        {
            return this.loader.findResource(getUrls(), name);
        }

        public Enumeration<URL> findResources(String name)
        {
            return this.loader.findResources(getUrls(), name);
        }
    }
}
