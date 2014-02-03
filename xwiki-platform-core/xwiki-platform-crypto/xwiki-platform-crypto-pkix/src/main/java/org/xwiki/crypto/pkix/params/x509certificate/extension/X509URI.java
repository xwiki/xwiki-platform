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
package org.xwiki.crypto.pkix.params.x509certificate.extension;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.x509.GeneralName;
import org.xwiki.crypto.pkix.internal.extension.BcGeneralName;
import org.xwiki.stability.Unstable;

/**
 * Uniform Resource Identifier general name.
 *
 * @version $Id$
 * @since 5.4
 */
@Unstable
public class X509URI implements X509StringGeneralName, BcGeneralName
{
    private final String str;
    private final URI uri;
    private final URL url;

    /**
     * Constructs a uniform resource locator general name by parsing the given string.
     *
     * @param str the string to be parsed into a URI
     */
    public X509URI(String str)
    {
        String newStr = null;
        URI newUri = null;
        URL newUrl = null;

        try {
            newUri = new URI(str);
            newUrl = newUri.toURL();
            newStr = newUrl.toString();
            newUri = newUrl.toURI();
        } catch (URISyntaxException e) {
            try {
                newUrl = new URL(str);
                newStr = newUrl.toString();
            } catch (MalformedURLException e1) {
                newStr = str;
            }
        } catch (MalformedURLException e) {
            newStr = newUri.toASCIIString();
        }

        this.str = newStr;
        this.uri = newUri;
        this.url = newUrl;
    }

    /**
     * Construct a uniform resource locator general name from an URL.
     *
     * @param url the url.
     */
    public X509URI(URL url)
    {
        this.str = url.toString();
        this.url = url;

        URI newUri = null;
        try {
            newUri = url.toURI();
        } catch (URISyntaxException e) {
            // Ignored
        }

        this.uri = newUri;
    }

    /**
     * Create a new instance from a Bouncy Castle general name.
     *
     * @param name the Bouncy Castle general name.
     */
    public X509URI(GeneralName name)
    {
        this(DERIA5String.getInstance(name.getName()).getString());

        if (name.getTagNo() != GeneralName.uniformResourceIdentifier) {
            throw new IllegalArgumentException("Incompatible general name: " + name.getTagNo());
        }
    }

    /**
     * @return the URL represented by this general name, or null if this name is malformed URL or the protocol handler
     *         is missing.
     */
    public URL getURL()
    {
        return url;
    }

    /**
     * @return the URI represented by this general name, or null if this name violates RFC 2396.
     */
    public URI getURI()
    {
        return uri;
    }

    @Override
    public String getName()
    {
        return str;
    }

    @Override
    public GeneralName getGeneralName()
    {
        return new GeneralName(GeneralName.uniformResourceIdentifier, str);
    }
}
