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
package org.xwiki.classloader.internal.protocol.jar;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLStreamHandlerFactory;
import java.security.Permission;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * URL Connection that knows how to get a JAR file with any custom protocol specified (in the form {@code jar:<custom
 * protocol>://<path to jar file>!<path inside jar file>}. Note that we don't extend the JDK's JarURLConnection since it
 * doesn't know how to use a custom URL Stream Handler Factory to handle custom protocols.
 * 
 * <p>
 * Originally written by Dawid Kurzyniec and released to the public domain, as explained
 * at http://creativecommons.org/licenses/publicdomain
 * </p>
 * <p>
 * Source: http://dcl.mathcs.emory.edu/php/loadPage.php?content=util/features.html#classloading
 * </p>
 *
 * @version $Id$
 * @since 2.0.1
 */
public class JarURLConnection extends java.net.URLConnection implements org.xwiki.classloader.internal.JarURLConnection
{
    private URLStreamHandlerFactory handlerFactory;

    final JarOpener opener;

    boolean connected;

    JarFile jfile;

    JarEntry jentry;

    private URL jarFileURL;

    private String entryName;

    public JarURLConnection(URL url, JarOpener opener, URLStreamHandlerFactory handlerFactory) throws IOException
    {
        super(url);
        this.opener = opener;
        this.handlerFactory = handlerFactory;
        parseSpecs(url);
    }

    public synchronized void connect() throws IOException
    {
        if (connected)
            return;
        jfile = opener.openJarFile(this);
        if (jfile != null && getEntryName() != null) {
            jentry = jfile.getJarEntry(getEntryName());
            if (jentry == null) {
                throw new FileNotFoundException("Entry " + getEntryName() + " not found in " + getJarFileURL());
            }
        }
        connected = true;
    }

    public synchronized JarFile getJarFile() throws IOException
    {
        connect();
        return jfile;
    }

    public synchronized JarEntry getJarEntry() throws IOException
    {
        connect();
        return jentry;
    }

    public synchronized InputStream getInputStream() throws IOException
    {
        connect();
        return jfile.getInputStream(jentry);
    }

    public Permission getPermission() throws IOException
    {
        return getJarFileURL().openConnection().getPermission();
    }

    /*
     * get the specs for a given url out of the cache, and compute and cache them if they're not there.
     */
    private void parseSpecs(URL url) throws MalformedURLException
    {
        String spec = url.getFile();

        int separator = spec.indexOf('!');
        /*
         * REMIND: we don't handle nested JAR URLs
         */
        if (separator == -1) {
            throw new MalformedURLException("no ! found in url spec:" + spec);
        }

        // Get the protocol
        String protocol = spec.substring(0, spec.indexOf(":"));

        // This is the important part: we use a URL Stream Handler Factory to find the URL Stream handler to use for
        // the nested protocol.
        jarFileURL =
            new URL(null, spec.substring(0, separator++), this.handlerFactory.createURLStreamHandler(protocol));
        entryName = null;

        /* if ! is the last letter of the innerURL, entryName is null */
        if (++separator != spec.length()) {
            entryName = spec.substring(separator, spec.length());
            try {
                // Note: we decode using UTF8 since it's the W3C recommendation.
                // See http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars
                entryName = URLDecoder.decode(entryName, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                // Not supporting UTF-8 as a valid encoding for some reasons. We consider XWiki cannot work
                // without that encoding.
                throw new RuntimeException("Failed to URL decode [" + entryName + "] using UTF-8.", e);
            }
        }
    }

    /**
     * Returns the URL for the Jar file for this connection.
     * 
     * @return the URL for the Jar file for this connection.
     */
    public URL getJarFileURL()
    {
        return jarFileURL;
    }

    /**
     * Return the entry name for this connection. This method returns null if the JAR file URL corresponding to this
     * connection points to a JAR file and not a JAR file entry.
     * 
     * @return the entry name for this connection, if any.
     */
    public String getEntryName()
    {
        return entryName;
    }

    /**
     * Returns the Manifest for this connection, or null if none.
     * 
     * @return the manifest object corresponding to the JAR file object for this connection.
     * @exception IOException if getting the JAR file for this connection causes an IOException to be trown.
     * @see #getJarFile
     */
    public Manifest getManifest() throws IOException
    {
        return getJarFile().getManifest();
    }

    /**
     * Return the Attributes object for this connection if the URL for it points to a JAR file entry, null otherwise.
     * 
     * @return the Attributes object for this connection if the URL for it points to a JAR file entry, null otherwise.
     * @exception IOException if getting the JAR entry causes an IOException to be thrown.
     * @see #getJarEntry
     */
    public Attributes getAttributes() throws IOException
    {
        JarEntry e = getJarEntry();
        return e != null ? e.getAttributes() : null;
    }

    /**
     * Returns the main Attributes for the JAR file for this connection.
     * 
     * @return the main Attributes for the JAR file for this connection.
     * @exception IOException if getting the manifest causes an IOException to be thrown.
     * @see #getJarFile
     * @see #getManifest
     */
    public Attributes getMainAttributes() throws IOException
    {
        Manifest man = getManifest();
        return man != null ? man.getMainAttributes() : null;
    }

    /**
     * Return the Certificate object for this connection if the URL for it points to a JAR file entry, null otherwise.
     * This method can only be called once the connection has been completely verified by reading from the input stream
     * until the end of the stream has been reached. Otherwise, this method will return <code>null</code>
     * 
     * @return the Certificate object for this connection if the URL for it points to a JAR file entry, null otherwise.
     * @exception IOException if getting the JAR entry causes an IOException to be thrown.
     * @see #getJarEntry
     */
    public java.security.cert.Certificate[] getCertificates() throws IOException
    {
        JarEntry e = getJarEntry();
        return e != null ? e.getCertificates() : null;
    }

    /**
     * Abstraction of JAR opener which allows to implement various caching policies. The opener receives URL pointing to
     * the JAR file, along with other meta-information, as a JarURLConnection instance. Then it has to download the file
     * (if it is remote) and open it.
     * 
     * @author Dawid Kurzyniec
     * @version 1.0
     */
    public static interface JarOpener
    {
        /**
         * Given the URL connection (not yet connected), return JarFile representing the resource. This method is
         * invoked as a part of the {@link #connect} method in JarURLConnection.
         * 
         * @param conn the connection for which the JAR file is required
         * @return opened JAR file
         * @throws IOException if I/O error occurs
         */
        public JarFile openJarFile(JarURLConnection conn) throws IOException;
    }
}
