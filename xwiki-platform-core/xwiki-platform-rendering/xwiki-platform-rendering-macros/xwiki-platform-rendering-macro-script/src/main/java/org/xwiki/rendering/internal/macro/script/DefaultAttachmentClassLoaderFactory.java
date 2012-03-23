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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.URLStreamHandlerFactory;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringTokenizer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.classloader.ExtendedURLClassLoader;
import org.xwiki.classloader.ExtendedURLStreamHandler;
import org.xwiki.classloader.URIClassLoader;
import org.xwiki.component.annotation.Component;

/**
 * Supports the following syntax for JARs attached to wiki pages:
 * {@code attach:(wiki):(space).(page)@(attachment)}.
 *  
 * @version $Id$
 * @since 2.0.1
 */
@Component
@Singleton
public class DefaultAttachmentClassLoaderFactory implements AttachmentClassLoaderFactory
{
    /**
     * The prefix to specify a JAR attached to a wiki page. This is because we also support {@code http://} prefixes
     * to load JARs from remote locations. 
     */
    private static final String ATTACHMENT_PREFIX = "attach:";

    /**
     * The Stream handler factory to use in the created classloader in order to be able to load our custom 
     * {@code attachmentjar} custom protocol.
     */
    @Inject
    private URLStreamHandlerFactory streamHandlerFactory;

    /**
     * The stream handler for our custom {@code attachmentjar} protocol. We use it to get access to the protocol 
     * name and to transform from URI to URL.
     */
    @Inject
    @Named("attachmentjar")
    private ExtendedURLStreamHandler attachmentJarHandler;
    
    @Override
    public ExtendedURLClassLoader createAttachmentClassLoader(String jarURLs, ClassLoader parent) throws Exception
    {
        URI[] uris = extractURIs(jarURLs).toArray(new URI[0]);
        return new URIClassLoader(uris, parent, this.streamHandlerFactory);
    }
    
    @Override
    public void extendAttachmentClassLoader(String jarURLs, ExtendedURLClassLoader source) throws Exception
    {
        for (URI uri : extractURIs(jarURLs)) {
            if (uri.getScheme().equalsIgnoreCase(this.attachmentJarHandler.getProtocol())) {
                source.addURL(new URL(null, uri.toString(), 
                    this.streamHandlerFactory.createURLStreamHandler(uri.getScheme())));
            } else {
                source.addURL(uri.toURL());
            }
        }
    }
    
    /**
     * @param jarURLs the comma-separated list of JARs locations, specified using either an already registered
     *        protocol (such as {@code http}) or using the format {@code attach:(wiki):(space).(page)@(filename)}.
     * @return the list of URIs
     * @throws URISyntaxException in case of an invalid URI 
     */
    private Set<URI> extractURIs(String jarURLs) throws URISyntaxException
    {
        // Parse the passed JAR URLs to tokenize it.
        Set<URI> uris = new LinkedHashSet<URI>();
        if (StringUtils.isNotEmpty(jarURLs)) {
            StringTokenizer tokenizer = new StringTokenizer(jarURLs, ",");
            while (tokenizer.hasMoreElements()) {
                String token = tokenizer.nextToken().trim();
                if (token.startsWith(ATTACHMENT_PREFIX)) {
                    uris.add(createURI(token));
                } else {
                    uris.add(new URI(token));
                }
            }
        }

        return uris;
    }

    /**
     * @param attachmentReference the attachment reference in the form {@code attach:(wiki):(space).(page)@(filename)}
     * @return a URI of the form {@code attachmentjar://(wiki):(space).(page)@(filename)}. 
     *         The {@code (wiki):(space).(page)@(filename)} part is URL-encoded
     * @throws URISyntaxException in case of an invalid URI 
     */
    private URI createURI(String attachmentReference) throws URISyntaxException
    {
        String uriBody = attachmentReference.substring(ATTACHMENT_PREFIX.length());

        try {
            // Note: we encode using UTF8 since it's the W3C recommendation.
            // See http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars
            // TODO: Once the xwiki-url module is usable, refactor this code to use it and remove the need to
            // perform explicit encoding here.
            uriBody = URLEncoder.encode(uriBody, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Not supporting UTF-8 as a valid encoding for some reasons. We consider XWiki cannot work
            // without that encoding.
            throw new RuntimeException("Failed to URL encode [" + uriBody + "] using UTF-8.", e);
        }

        return new URI(this.attachmentJarHandler.getProtocol() + "://" + uriBody);
    }
}
