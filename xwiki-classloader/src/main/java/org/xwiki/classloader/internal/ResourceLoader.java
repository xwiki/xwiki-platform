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
 * 
 * 
 * Originally written by Dawid Kurzyniec and released to the public domain, as explained
 * at http://creativecommons.org/licenses/publicdomain
 * 
 * Source: http://dcl.mathcs.emory.edu/php/loadPage.php?content=util/features.html#classloading
 */
package org.xwiki.classloader.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.Permission;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import edu.emory.mathcs.util.classloader.ResourceHandle;
import edu.emory.mathcs.util.classloader.ResourceUtils;

/**
 * This class aids in accessing remote resources referred by URLs. The URLs are resolved into {@link ResourceHandle
 * resource handles} which can be used to access the resources directly and uniformly, regardless of the URL type. The
 * resource loader is particularly useful when dealing with resources fetched from JAR files. It maintains the cache of
 * opened JAR files (so that so that subsequent requests for resources coming from the same base Jar file can be handled
 * efficiently). It fully supports JAR class-path (references from a JAR file to other JAR files) and JAR index (JAR
 * containing information about content of other JARs). The caching policy of downloaded JAR files can be customized via
 * the constructor parameter <code>jarHandler</code>; the default policy is to use separate cache per each
 * ResourceLoader instance.
 * <p>
 * This class is particularly useful when implementing custom class loaders. It provides bottom-level resource fetching
 * functionality. By using one of the loader methods which accepts an array of URLs, it is straightforward to implement
 * class-path searching similar to that of {@link java.net.URLClassLoader}, with JAR dependencies (Class-Path) properly
 * resolved and with JAR indexes properly handled.
 * <p>
 * This class provides two set of methods: <i>get</i> methods that return {@link ResourceHandle}s (or their
 * enumerations) and <i>find</i> methods that return URLs (or their enumerations). If the resource is not found, null
 * (or empty enumeration) is returned. Resource handles represent a connection to the resource and they should be closed
 * when done processing, just like input streams. In contrast, find methods return URLs that can be used to open
 * multiple connections to the resource. In typical class loader applications, when a single retrieval is sufficient, it
 * is preferable to use <i>get</i> methods since they pose slightly smaller communication overhead.
 *
 * @version $Id$
 * @since 2.0.1
 */
public class ResourceLoader
{
    private static final String JAR_INDEX_ENTRY_NAME = "META-INF/INDEX.LIST";

    final URLStreamHandler jarHandler;

    final Map url2jarInfo = new HashMap();

    /**
     * Constructs new ResourceLoader with specified JAR file handler which can implement custom JAR caching policy.
     * 
     * @param jarHandler JAR file handler
     */
    public ResourceLoader(URLStreamHandler jarHandler)
    {
        this.jarHandler = jarHandler;
    }

    /**
     * Gets resource with given name at the given source URL. If the URL points to a directory, the name is the file
     * path relative to this directory. If the URL points to a JAR file, the name identifies an entry in that JAR file.
     * If the URL points to a JAR file, the resource is not found in that JAR file, and the JAR file has Class-Path
     * attribute, the JAR files identified in the Class-Path are also searched for the resource.
     * 
     * @param source the source URL
     * @param name the resource name
     * @return handle representing the resource, or null if not found
     */
    public ResourceHandle getResource(URL source, String name)
    {
        return getResource(source, name, new HashSet(), null);
    }

    /**
     * Gets resource with given name at the given search path. The path is searched iteratively, one URL at a time. If
     * the URL points to a directory, the name is the file path relative to this directory. If the URL points to the JAR
     * file, the name identifies an entry in that JAR file. If the URL points to the JAR file, the resource is not found
     * in that JAR file, and the JAR file has Class-Path attribute, the JAR files identified in the Class-Path are also
     * searched for the resource.
     * 
     * @param sources the source URL path
     * @param name the resource name
     * @return handle representing the resource, or null if not found
     */
    public ResourceHandle getResource(URL[] sources, String name)
    {
        Set visited = new HashSet();
        for (int i = 0; i < sources.length; i++) {
            ResourceHandle h = getResource(sources[i], name, visited, null);
            if (h != null)
                return h;
        }
        return null;
    }

    /**
     * Gets all resources with given name at the given source URL. If the URL points to a directory, the name is the
     * file path relative to this directory. If the URL points to a JAR file, the name identifies an entry in that JAR
     * file. If the URL points to a JAR file, the resource is not found in that JAR file, and the JAR file has
     * Class-Path attribute, the JAR files identified in the Class-Path are also searched for the resource.
     * <p>
     * The search is lazy, that is, "find next resource" operation is triggered by calling
     * {@link Enumeration#hasMoreElements}.
     * 
     * @param source the source URL
     * @param name the resource name
     * @return enumeration of resource handles representing the resources
     */
    public Enumeration getResources(URL source, String name)
    {
        return new ResourceEnumeration(new URL[] {source}, name, false);
    }

    /**
     * Gets all resources with given name at the given search path. If the URL points to a directory, the name is the
     * file path relative to this directory. If the URL points to a JAR file, the name identifies an entry in that JAR
     * file. If the URL points to a JAR file, the resource is not found in that JAR file, and the JAR file has
     * Class-Path attribute, the JAR files identified in the Class-Path are also searched for the resource.
     * <p>
     * The search is lazy, that is, "find next resource" operation is triggered by calling
     * {@link Enumeration#hasMoreElements}.
     * 
     * @param sources the source URL path
     * @param name the resource name
     * @return enumeration of resource handles representing the resources
     */
    public Enumeration getResources(URL[] sources, String name)
    {
        return new ResourceEnumeration((URL[]) sources.clone(), name, false);
    }

    private ResourceHandle getResource(final URL source, String name, Set visitedJars, Set skip)
    {

        name = ResourceUtils.canonizePath(name);
        if (isDir(source)) {
            // plain resource
            final URL url;
            try {
                // escape spaces etc. to make sure url is well-formed
                URI relUri = new URI(null, null, null, -1, name, null, null);
                url = new URL(source, relUri.getRawPath());
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Illegal resource name: " + name);
            } catch (MalformedURLException e) {
                return null;
            }

            if (skip != null && skip.contains(url))
                return null;
            final URLConnection conn;
            try {
                conn = url.openConnection();
                conn.getInputStream();
            } catch (IOException e) {
                return null;
            }
            final String finalName = name;
            return new ResourceHandle()
            {
                public String getName()
                {
                    return finalName;
                }

                public URL getURL()
                {
                    return url;
                }

                public URL getCodeSourceURL()
                {
                    return source;
                }

                public InputStream getInputStream() throws IOException
                {
                    return conn.getInputStream();
                }

                public int getContentLength()
                {
                    return conn.getContentLength();
                }

                public void close()
                {
                    try {
                        getInputStream().close();
                    } catch (IOException e) {
                    }
                }
            };
        } else {
            // we deal with a JAR file here
            try {
                return getJarInfo(source).getResource(name, visitedJars, skip);
            } catch (MalformedURLException e) {
                return null;
            }
        }
    }

    /**
     * Fined resource with given name at the given source URL. If the URL points to a directory, the name is the file
     * path relative to this directory. If the URL points to a JAR file, the name identifies an entry in that JAR file.
     * If the URL points to a JAR file, the resource is not found in that JAR file, and the JAR file has Class-Path
     * attribute, the JAR files identified in the Class-Path are also searched for the resource.
     * 
     * @param source the source URL
     * @param name the resource name
     * @return URL of the resource, or null if not found
     */
    public URL findResource(URL source, String name)
    {
        return findResource(source, name, new HashSet(), null);
    }

    /**
     * Finds resource with given name at the given search path. The path is searched iteratively, one URL at a time. If
     * the URL points to a directory, the name is the file path relative to this directory. If the URL points to the JAR
     * file, the name identifies an entry in that JAR file. If the URL points to the JAR file, the resource is not found
     * in that JAR file, and the JAR file has Class-Path attribute, the JAR files identified in the Class-Path are also
     * searched for the resource.
     * 
     * @param sources the source URL path
     * @param name the resource name
     * @return URL of the resource, or null if not found
     */
    public URL findResource(URL[] sources, String name)
    {
        Set visited = new HashSet();
        for (int i = 0; i < sources.length; i++) {
            URL url = findResource(sources[i], name, visited, null);
            if (url != null)
                return url;
        }
        return null;
    }

    /**
     * Finds all resources with given name at the given source URL. If the URL points to a directory, the name is the
     * file path relative to this directory. If the URL points to a JAR file, the name identifies an entry in that JAR
     * file. If the URL points to a JAR file, the resource is not found in that JAR file, and the JAR file has
     * Class-Path attribute, the JAR files identified in the Class-Path are also searched for the resource.
     * <p>
     * The search is lazy, that is, "find next resource" operation is triggered by calling
     * {@link Enumeration#hasMoreElements}.
     * 
     * @param source the source URL
     * @param name the resource name
     * @return enumeration of URLs of the resources
     */
    public Enumeration findResources(URL source, String name)
    {
        return new ResourceEnumeration(new URL[] {source}, name, true);
    }

    /**
     * Finds all resources with given name at the given search path. If the URL points to a directory, the name is the
     * file path relative to this directory. If the URL points to a JAR file, the name identifies an entry in that JAR
     * file. If the URL points to a JAR file, the resource is not found in that JAR file, and the JAR file has
     * Class-Path attribute, the JAR files identified in the Class-Path are also searched for the resource.
     * <p>
     * The search is lazy, that is, "find next resource" operation is triggered by calling
     * {@link Enumeration#hasMoreElements}.
     * 
     * @param sources the source URL path
     * @param name the resource name
     * @return enumeration of URLs of the resources
     */
    public Enumeration findResources(URL[] sources, String name)
    {
        return new ResourceEnumeration((URL[]) sources.clone(), name, true);
    }

    private URL findResource(final URL source, String name, Set visitedJars, Set skip)
    {
        URL url;
        name = ResourceUtils.canonizePath(name);
        if (isDir(source)) {
            // plain resource
            try {
                url = new URL(source, name);
            } catch (MalformedURLException e) {
                return null;
            }
            if (skip != null && skip.contains(url))
                return null;
            final URLConnection conn;
            try {
                conn = url.openConnection();
                if (conn instanceof HttpURLConnection) {
                    HttpURLConnection httpConn = (HttpURLConnection) conn;
                    httpConn.setRequestMethod("HEAD");
                    if (httpConn.getResponseCode() >= 400)
                        return null;
                } else {
                    conn.getInputStream().close();
                }
            } catch (IOException e) {
                return null;
            }
            return url;
        } else {
            // we deal with a JAR file here
            try {
                ResourceHandle rh = getJarInfo(source).getResource(name, visitedJars, skip);
                return (rh != null) ? rh.getURL() : null;
            } catch (MalformedURLException e) {
                return null;
            }
        }

    }

    /**
     * Test whether given URL points to a directory. URL is deemed to point to a directory if has non-null "file"
     * component ending with "/".
     * 
     * @param url the URL to test
     * @return true if the URL points to a directory, false otherwise
     */
    protected static boolean isDir(URL url)
    {
        String file = url.getFile();
        return (file != null && file.endsWith("/"));
    }

    private static class JarInfo
    {
        final ResourceLoader loader;

        final URL source; // "real" jar file path

        final URL base; // "jar:{base}!/"

        JarFile jar;

        boolean resolved;

        Permission perm;

        URL[] classPath;

        String[] index;

        Map package2url;

        JarInfo(ResourceLoader loader, URL source) throws MalformedURLException
        {
            this.loader = loader;
            this.source = source;
            this.base = new URL("jar", "", -1, source + "!/", loader.jarHandler);
        }

        public ResourceHandle getResource(String name)
        {
            return getResource(name, new HashSet());
        }

        ResourceHandle getResource(String name, Set visited)
        {
            return getResource(name, visited, null);
        }

        ResourceHandle getResource(String name, Set visited, Set skip)
        {
            visited.add(source);
            URL url;
            try {
                // escape spaces etc. to make sure url is well-formed
                URI relUri = new URI(null, null, null, -1, name, null, null);
                url = new URL(base, relUri.getRawPath());
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Illegal resource name: " + name);
            } catch (MalformedURLException e) {
                return null;
            }
            try {
                JarFile jfile = getJarFileIfPossiblyContains(name);
                if (jfile != null) {
                    JarEntry jentry = jar.getJarEntry(name);
                    if (jentry != null && (skip == null || !skip.contains(url))) {
                        return new JarResourceHandle(jfile, jentry, url, source);
                    }
                }
            } catch (IOException e) {
                return null;
            }

            // not in here, but check also the dependencies
            URL[] dependencies;
            synchronized (this) {
                if (package2url != null) {
                    int idx = name.lastIndexOf("/");
                    String prefix = (idx > 0) ? name.substring(0, idx) : name;
                    dependencies = (URL[]) package2url.get(prefix);
                } else {
                    // classpath might be null only if it was a dependency of
                    // an indexed JAR with out-of-date index (the index brought
                    // us here but resource was not found in the JAR). But this
                    // (out-of-sync index) should be captured by
                    // getJarFileIfPossiblyContains.
                    assert classPath != null;
                    dependencies = classPath;
                }
            }

            if (dependencies == null)
                return null;

            for (int i = 0; i < dependencies.length; i++) {
                URL cpUrl = dependencies[i];
                if (visited.contains(cpUrl))
                    continue;
                JarInfo depJInfo;
                try {
                    depJInfo = loader.getJarInfo(cpUrl);
                    ResourceHandle rh = depJInfo.getResource(name, visited, skip);
                    if (rh != null)
                        return rh;
                } catch (MalformedURLException e) {
                    // continue with other URLs
                }
            }

            // not found
            return null;
        }

        synchronized void setIndex(List newIndex)
        {
            if (jar != null) {
                // already loaded; no need for index
                return;
            }
            if (index != null) {
                // verification - previously declared content must remain there
                Set violating = new HashSet(Arrays.asList(index));
                violating.removeAll(newIndex);
                if (!violating.isEmpty()) {
                    throw new RuntimeException("Invalid JAR index: "
                        + "the following entries were previously declared, but "
                        + "they are not present in the new index: " + violating.toString());
                }
            }
            this.index = (String[]) newIndex.toArray(new String[newIndex.size()]);
            Arrays.sort(this.index);
        }

        public JarFile getJarFileIfPossiblyContains(String name) throws IOException
        {
            Map indexes;
            synchronized (this) {
                if (jar != null) {
                    // make sure we would be allowed to load it ourselves
                    SecurityManager security = System.getSecurityManager();
                    if (security != null) {
                        security.checkPermission(perm);
                    }

                    // other thread may still be updating indexes of dependent
                    // JAR files
                    try {
                        while (!resolved)
                            wait();
                    } catch (InterruptedException e) {
                        throw new IOException("Interrupted");
                    }
                    return jar;
                }

                if (index != null) {
                    // we may be able to respond negatively w/o loading the JAR
                    int pos = name.lastIndexOf('/');
                    if (pos > 0)
                        name = name.substring(0, pos);
                    if (Arrays.binarySearch(index, name) < 0)
                        return null;
                }

                // load the JAR
                URLConnection connection = base.openConnection();
                this.perm = connection.getPermission();
                
                JarFile jar;
                if (connection instanceof org.xwiki.classloader.internal.protocol.jar.JarURLConnection) {
                    jar =  ((org.xwiki.classloader.internal.protocol.jar.JarURLConnection)connection).getJarFile();
                } else {
                    jar =  ((java.net.JarURLConnection)connection).getJarFile();
                }

                // conservatively check if index is accurate, that is, does not
                // contain entries which are not in the JAR file
                if (index != null) {
                    Set indices = new HashSet(Arrays.asList(index));
                    Enumeration entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = (JarEntry) entries.nextElement();
                        String indexEntry = entry.getName();
                        // for non-top, find the package name
                        int pos = indexEntry.lastIndexOf('/');
                        if (pos > 0) {
                            indexEntry = indexEntry.substring(0, pos);
                        }
                        indices.remove(indexEntry);
                    }
                    if (!indices.isEmpty()) {
                        throw new RuntimeException("Invalid JAR index: " + "the following entries not found in JAR: "
                            + indices);
                    }
                }
                this.jar = jar;

                this.classPath = parseClassPath(jar, source);

                indexes = parseJarIndex(this.source, jar);
                indexes.remove(this.source.toExternalForm());

                if (!indexes.isEmpty()) {
                    this.package2url = package2url(indexes);
                }
            }
            // just loaded the JAR - need to resolve the index
            try {
                for (Iterator itr = indexes.entrySet().iterator(); itr.hasNext();) {
                    Map.Entry entry = (Map.Entry) itr.next();
                    URL url = (URL) entry.getKey();
                    if (url.toExternalForm().equals(this.source.toExternalForm())) {
                        continue;
                    }
                    List index = (List) entry.getValue();
                    loader.getJarInfo(url).setIndex(index);
                }
            } finally {
                synchronized (this) {
                    this.resolved = true;
                    notifyAll();
                }
            }
            return jar;
        }
    }

    private static Map package2url(Map indexes)
    {
        Map prefix2url = new HashMap();
        for (Iterator i = indexes.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            URL url = (URL) entry.getKey();
            List idx = (List) entry.getValue();
            for (Iterator j = idx.iterator(); j.hasNext();) {
                String prefix = (String) j.next();
                List prefixList = (List) prefix2url.get(prefix);
                if (prefixList == null) {
                    prefixList = new ArrayList();
                    prefix2url.put(prefix, prefixList);
                }
                prefixList.add(url);
            }
        }

        // replace lists with arrays
        for (Iterator i = prefix2url.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            List list = (List) entry.getValue();
            entry.setValue(list.toArray(new URL[list.size()]));
        }
        return prefix2url;
    }

    private JarInfo getJarInfo(URL url) throws MalformedURLException
    {
        JarInfo jinfo;
        synchronized (url2jarInfo) {
            // fix: no longer use url.equals, since it distinguishes between
            // "" and null in the host part of file URLs. The ""-type urls are
            // correct but "null"-type ones come from file.toURI().toURL()
            // on 1.4.1. (It is fixed in 1.4.2)
            jinfo = (JarInfo) url2jarInfo.get(url.toExternalForm());
            if (jinfo == null) {
                jinfo = new JarInfo(this, url);
                url2jarInfo.put(url.toExternalForm(), jinfo);
            }
        }
        return jinfo;
    }

    private static class JarResourceHandle extends ResourceHandle
    {
        final JarFile jar;

        final JarEntry jentry;

        final URL url;

        final URL codeSource;

        JarResourceHandle(JarFile jar, JarEntry jentry, URL url, URL codeSource)
        {
            this.jar = jar;
            this.jentry = jentry;
            this.url = url;
            this.codeSource = codeSource;
        }

        public String getName()
        {
            return jentry.getName();
        }

        public URL getURL()
        {
            return url;
        }

        public URL getCodeSourceURL()
        {
            return codeSource;
        }

        public InputStream getInputStream() throws IOException
        {
            return jar.getInputStream(jentry);
        }

        public int getContentLength()
        {
            return (int) jentry.getSize();
        }

        public Manifest getManifest() throws IOException
        {
            return jar.getManifest();
        }

        public Attributes getAttributes() throws IOException
        {
            return jentry.getAttributes();
        }

        public Certificate[] getCertificates()
        {
            return jentry.getCertificates();
        }

        public void close()
        {
        }
    }

    private static Map parseJarIndex(URL cxt, JarFile jar) throws IOException
    {
        JarEntry entry = jar.getJarEntry(JAR_INDEX_ENTRY_NAME);
        if (entry == null)
            return Collections.EMPTY_MAP;
        InputStream is = jar.getInputStream(entry);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

        Map result = new LinkedHashMap();

        String line;

        // skip version-info
        do {
            line = reader.readLine();
        } while (line != null && line.trim().length() > 0);

        URL currentURL;
        List currentList = null;
        while (true) {
            // skip the blank line
            line = reader.readLine();
            if (line == null) {
                return result;
            }

            currentURL = new URL(cxt, line);
            currentList = new ArrayList();
            result.put(currentURL, currentList);

            while (true) {
                line = reader.readLine();
                if (line == null || line.trim().length() == 0)
                    break;
                currentList.add(line);
            }
        }
    }

    private static URL[] parseClassPath(JarFile jar, URL source) throws IOException
    {
        Manifest man = jar.getManifest();
        if (man == null)
            return new URL[0];
        Attributes attr = man.getMainAttributes();
        if (attr == null)
            return new URL[0];
        String cp = attr.getValue(Attributes.Name.CLASS_PATH);
        if (cp == null)
            return new URL[0];
        StringTokenizer tokenizer = new StringTokenizer(cp);
        List cpList = new ArrayList();
        URI sourceURI = URI.create(source.toString());
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            try {
                try {
                    URI uri = new URI(token);
                    if (!uri.isAbsolute()) {
                        uri = sourceURI.resolve(uri);
                    }
                    cpList.add(uri.toURL());
                } catch (URISyntaxException e) {
                    // tolerate malformed URIs for backward-compatibility
                    URL url = new URL(source, token);
                    cpList.add(url);
                }
            } catch (MalformedURLException e) {
                throw new IOException(e.getMessage());
            }
        }
        return (URL[]) cpList.toArray(new URL[cpList.size()]);
    }

    private class ResourceEnumeration implements Enumeration
    {
        final URL[] urls;

        final String name;

        final boolean findOnly;

        int idx;

        Object next;

        Set previousURLs = new HashSet();

        ResourceEnumeration(URL[] urls, String name, boolean findOnly)
        {
            this.urls = urls;
            this.name = name;
            this.findOnly = findOnly;
            this.idx = 0;
        }

        public boolean hasMoreElements()
        {
            fetchNext();
            return (next != null);
        }

        public Object nextElement()
        {
            fetchNext();
            if (next == null)
                throw new NoSuchElementException();
            
            Object nextElement = next;
            next = null;
            
            return nextElement;
        }

        private void fetchNext()
        {
            if (next != null)
                return;
            while (idx < urls.length) {
                Object found;
                if (findOnly) {
                    URL url = findResource(urls[idx], name, new HashSet(), previousURLs);
                    if (url != null) {
                        previousURLs.add(url);
                        next = url;
                        return;
                    }
                } else {
                    ResourceHandle h = getResource(urls[idx], name, new HashSet(), previousURLs);
                    if (h != null) {
                        previousURLs.add(h.getURL());
                        next = h;
                        return;
                    }
                }
                idx++;
            }
        }
    }
}
