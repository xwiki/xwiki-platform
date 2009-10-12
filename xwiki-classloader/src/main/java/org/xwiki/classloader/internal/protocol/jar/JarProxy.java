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
package org.xwiki.classloader.internal.protocol.jar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import edu.emory.mathcs.util.classloader.ResourceUtils;
import edu.emory.mathcs.util.io.RedirectibleInput;
import edu.emory.mathcs.util.io.RedirectingInputStream;

/**
 * Implementation of {@link JarURLConnection.JarOpener} that caches downloaded JAR files in a local file system.
 * 
 * @see JarURLConnection
 * @see JarURLStreamHandler
 * @version $Id$
 * @since 2.0.1
 */
public class JarProxy implements JarURLConnection.JarOpener
{
    private final Map cache = new HashMap();

    public JarFile openJarFile(JarURLConnection conn) throws IOException
    {
        URL url = conn.getJarFileURL();
        CachedJarFile result;
        synchronized (cache) {
            result = (CachedJarFile) cache.get(url);
        }
        if (result != null) {
            SecurityManager security = System.getSecurityManager();
            if (security != null) {
                security.checkPermission(result.perm);
            }
            return result;
        }

        // we have to download and open the JAR; yet it may be a local file
        try {
            URI uri = new URI(url.toString());
            if (ResourceUtils.isLocalFile(uri)) {
                File file = new File(uri);
                Permission perm = new FilePermission(file.getAbsolutePath(), "read");
                result = new CachedJarFile(file, perm, false);
            }
        } catch (URISyntaxException e) {
            // apparently not a local file
        }

        if (result == null) {
            final URLConnection jarconn = url.openConnection();

            // set up the properties based on the JarURLConnection
            jarconn.setAllowUserInteraction(conn.getAllowUserInteraction());
            jarconn.setDoInput(conn.getDoInput());
            jarconn.setDoOutput(conn.getDoOutput());
            jarconn.setIfModifiedSince(conn.getIfModifiedSince());

            Map map = conn.getRequestProperties();
            for (Iterator itr = map.entrySet().iterator(); itr.hasNext();) {
                Map.Entry entry = (Map.Entry) itr.next();
                jarconn.setRequestProperty((String) entry.getKey(), (String) entry.getValue());
            }

            jarconn.setUseCaches(conn.getUseCaches());

            final InputStream in = getJarInputStream(jarconn);

            try {
                result = (CachedJarFile) AccessController.doPrivileged(new PrivilegedExceptionAction()
                {
                    public Object run() throws IOException
                    {
                        File file = File.createTempFile("jar_cache", "");
                        FileOutputStream out = new FileOutputStream(file);
                        try {
                            RedirectibleInput r = new RedirectingInputStream(in, false, false);
                            int len = r.redirectAll(out);
                            out.flush();
                            if (len == 0) {
                                // e.g. HttpURLConnection: "NOT_MODIFIED"
                                return null;
                            }
                        } finally {
                            out.close();
                        }
                        return new CachedJarFile(file, jarconn.getPermission(), true);

                    }
                });
            } catch (PrivilegedActionException pae) {
                throw (IOException) pae.getException();
            } finally {
                in.close();
            }
        }

        // if no input came (e.g. due to NOT_MODIFIED), do not cache
        if (result == null)
            return null;

        // optimistic locking
        synchronized (cache) {
            CachedJarFile asyncResult = (CachedJarFile) cache.get(url);
            if (asyncResult != null) {
                // some other thread already retrieved the file; return w/o
                // security check since we already succeeded in getting past it
                result.closeCachedFile();
                return asyncResult;
            }
            cache.put(url, result);
            return result;
        }
    }

    protected InputStream getJarInputStream(URLConnection conn) throws IOException
    {
        return conn.getInputStream();
    }

    protected void clear()
    {
        Map cache;
        synchronized (this.cache) {
            cache = new HashMap(this.cache);
            this.cache.clear();
        }
        for (Iterator itr = cache.values().iterator(); itr.hasNext();) {
            CachedJarFile jfile = (CachedJarFile) itr.next();
            try {
                jfile.closeCachedFile();
            } catch (IOException e) {
                // best-effort
            }
        }
    }

    protected void finalize()
    {
        clear();
    }

    private static class CachedJarFile extends JarFile
    {
        final Permission perm;

        CachedJarFile(File file, Permission perm, boolean tmp) throws IOException
        {
            super(file, true, JarFile.OPEN_READ | (tmp ? JarFile.OPEN_DELETE : 0));
            this.perm = perm;
        }

        public Manifest getManifest() throws IOException
        {
            Manifest orig = super.getManifest();
            if (orig == null)
                return null;
            // make sure the original manifest is not modified
            Manifest man = new Manifest();
            man.getMainAttributes().putAll(orig.getMainAttributes());
            for (Map.Entry<String, Attributes> entry : orig.getEntries().entrySet()) {
                man.getEntries().put(entry.getKey(), new Attributes((Attributes) entry.getValue()));
            }
            return man;
        }

        public ZipEntry getEntry(String name)
        {
            // super.getJarEntry() would result in stack overflow
            return super.getEntry(name);
        }

        protected void finalize() throws IOException
        {
            closeCachedFile();
        }

        protected void closeCachedFile() throws IOException
        {
            super.close();
        }

        public void close() throws IOException
        {
            // no op; do NOT close file while still in cache
        }

        private static class Entry extends JarEntry
        {
            JarEntry jentry;

            Entry(JarEntry jentry)
            {
                super(jentry);
                this.jentry = jentry;
            }

            public Certificate[] getCertificates()
            {
                Certificate[] certs = jentry.getCertificates();
                return (certs == null ? null : (Certificate[]) certs.clone());
            }

            public Attributes getAttributes() throws IOException
            {
                Attributes attr = jentry.getAttributes();
                return (attr == null ? null : new Attributes(attr));
            }
        }
    }
}
