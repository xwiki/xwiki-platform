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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.classloader.ExtendedURLStreamHandler;
import org.xwiki.classloader.internal.protocol.jar.JarURLConnection.JarOpener;
import org.xwiki.component.annotation.Component;

import edu.emory.mathcs.util.classloader.ResourceUtils;

/**
 * Handler for the "jar" protocol. Note that we don't use the JDK's JAR URL Connection class since it doesn't support
 * using a URL Stream Handler Factory for handling nested protocols (the protocol inside the "jar" protocol, for
 * example: {@code jar:http://...} or {@code jar:attachmentjar://...}, etc).
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
@Component
@Named("jar")
@Singleton
public class JarURLStreamHandler extends URLStreamHandler implements ExtendedURLStreamHandler
{
    /**
     * Used to parse the string representation of a {@code URL} into a {@code URL} using the format:
     * "jar:" + url + "!" + /<path>/ + <file>? + "#"<anchor>?
     */
    private static final Pattern ABSOLUTE_JAR_URL_PATTERN =
        Pattern.compile("jar:(.*)!(/(?:.*/)?)((?:[^/#]+)?)((?:#.*)?)");

    /**
     * The JAR protocol name.
     */
    private static final String JAR_PROTOCOL = "jar";

    /**
     * Separator for JAR in a URL.
     */
    private static final String JAR_PROTOCOL_SEPARATOR = "!";

    /**
     * The actual logic to load JARs and cache them on the local filesystem.
     */
    private JarOpener opener = new JarProxy();

    /**
     * Stream factory passed to the JAR URL Connection class we create so that it can support custom
     * protocols even if they are not globally registered (the problems with global registration are
     * described at http://accu.org/index.php/journals/1434).
     */
    @Inject
    private URLStreamHandlerFactory handlerFactory;

    @Override
    public String getProtocol()
    {
        return JAR_PROTOCOL;
    }

    @Override
    public URLConnection openConnection(URL url) throws IOException
    {
        return new JarURLConnection(url, this.opener, this.handlerFactory);
    }

    /**
     * {@inheritDoc}
     * 
     * Implementation copied from Emory's Classloader Utilities. We had to copy it since we cannot extend Emory's
     * JarURLStreamHandler implementation since it extends the JDK's JAR URL Connection (see this class's description
     * to understand why we cannot use it).
     * 
     * @see URLStreamHandler#parseURL(URL, String, int, int)
     */
    @Override
    protected void parseURL(URL u, String spec, int start, int limit)
    {
        Matcher matcher = ABSOLUTE_JAR_URL_PATTERN.matcher(spec);
        if (matcher.matches()) {
            // spec is an absolute URL
            String base = matcher.group(1);
            try {
                // Verify
                new URL(base);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(e.toString());
            }
            String path = matcher.group(2) + matcher.group(3);
            path = ResourceUtils.canonizePath(path);
            String ref = matcher.group(4);
            if (ref.length() == 0) {
                ref = null;
            } else {
                ref = ref.substring(1);
            }
            setURL(u, JAR_PROTOCOL, "", -1, "", "", base + JAR_PROTOCOL_SEPARATOR + path, null, ref);
        } else {
            matcher = ABSOLUTE_JAR_URL_PATTERN.matcher(u.toString());
            if (matcher.matches()) {
                String ref = spec.substring(limit);
                if (ref.length() == 0) {
                    ref = null;
                } else {
                    ref = ref.substring(1);
                }
                String newSpec = spec.substring(start, limit);
                String base = matcher.group(1);
                String path;
                if (newSpec.length() > 0 && newSpec.charAt(0) == '/') {
                    path = newSpec;
                } else {
                    String cxtDir = matcher.group(2);
                    path = cxtDir + newSpec;
                }
                path = ResourceUtils.canonizePath(path);
                setURL(u, JAR_PROTOCOL, "", -1, "", "", base + JAR_PROTOCOL_SEPARATOR + path, null, ref);
            } else {
                throw new IllegalArgumentException("Neither URL nor the spec are valid JAR URLs");
            }
        }
    }
}
