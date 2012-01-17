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
package org.xwiki.extension.internal.reference;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;

/**
 * Represents a reference to an extension file.
 * 
 * @version $Id$
 */
public class ExtensionResourceReference extends ResourceReference
{
    /**
     * The type for extension resources.
     */
    public static final ResourceType TYPE = new ResourceType("extension");

    /**
     * The name of the parameter representing the id of the repository from were to get the extension file.
     */
    public static final String PARAM_REPOSITORYID = "rid";

    /**
     * The name of the parameter representing the type of the repository from were to get the extension file.
     */
    public static final String PARAM_REPOSITORYTYPE = "rtype";

    /**
     * The name of the parameter representing the uri of the repository from were to get the extension file.
     */
    public static final String PARAM_REPOSITORYURI = "ruri";

    /**
     * The encoding used to encode/decode the reference.
     */
    private static final String URLENCODING = "UTF-8";

    /**
     * @see #getExtensionId()
     */
    private String extensionId;

    /**
     * @see #getExtensionId()
     */
    private String extensionVersion;

    /**
     * @see #getRepositoryId()
     */
    private String repositoryId;

    /**
     * @see #getRepositoryType()
     */
    private String repositoryType;

    /**
     * @see #getRepositoryUri()
     */
    private URI repositoryURI;

    /**
     * @param id the id of the extension
     * @param version the version of the extension
     * @param repositoryId the id of the repository from where to get the extension file
     */
    public ExtensionResourceReference(String id, String version, String repositoryId)
    {
        super(encode(id) + '/' + encode(version)
            + (repositoryId != null ? '?' + PARAM_REPOSITORYID + '=' + encode(repositoryId) : ""), TYPE);

        this.extensionId = id;
        this.extensionVersion = version;

        this.repositoryId = repositoryId;
    }

    /**
     * @param id the id of the extension
     * @param version the version of the extension
     * @param repositoryType the type of the repository from where to get the extension file
     * @param repositoryURI the URI of the repository from where to get the extension file
     */
    public ExtensionResourceReference(String id, String version, String repositoryType, URI repositoryURI)
    {
        super(encode(id)
            + '/'
            + encode(version)
            + (repositoryType != null && repositoryURI != null ? '?' + PARAM_REPOSITORYTYPE + '='
                + encode(repositoryType) + '&' + PARAM_REPOSITORYURI + '=' + encode(repositoryURI.toString()) : ""),
            TYPE);

        this.extensionId = id;
        this.extensionVersion = version;

        this.repositoryType = repositoryType;
        this.repositoryURI = repositoryURI;
    }

    /**
     * @param reference the reference
     */
    public ExtensionResourceReference(String reference)
    {
        super(reference, TYPE);

        // Parameters
        int queryStringIndex = reference.indexOf('?');

        if (queryStringIndex != -1) {
            String[] parameters = StringUtils.split(reference.substring(queryStringIndex + 1), '&');
            for (String parameter : parameters) {
                int equalIndex = parameter.indexOf('=');

                String parameterName = parameter.substring(0, equalIndex);
                String parameterValue = parameter.substring(equalIndex + 1);

                if (PARAM_REPOSITORYID.equals(parameterName)) {
                    this.repositoryId = parameterValue;
                } else if (PARAM_REPOSITORYTYPE.equals(parameterName)) {
                    this.repositoryType = parameterValue;
                } else if (PARAM_REPOSITORYURI.equals(parameterName)) {
                    try {
                        this.repositoryURI = new URI(parameterValue);
                    } catch (URISyntaxException e) {
                        // Ignore invalid repository URI
                    }
                }
            }
        } else {
            queryStringIndex = reference.length();
        }

        // Id and version
        int index = reference.indexOf('/');

        if (index == -1) {
            this.extensionId = decode(reference.substring(0, queryStringIndex));
        } else {
            this.extensionId = decode(reference.substring(0, index));
            this.extensionVersion = decode(reference.substring(index + 1, queryStringIndex));
        }
    }

    /**
     * @param str the string to encode
     * @return the encoded string
     */
    private static String encode(String str)
    {
        try {
            return URLEncoder.encode(str, URLENCODING);
        } catch (UnsupportedEncodingException e) {
            // Should never happen
            throw new RuntimeException("UTF-8 encoding is not supported");
        }
    }

    /**
     * @param str the string to decode
     * @return the decoded string
     */
    private static String decode(String str)
    {
        try {
            return URLDecoder.decode(str, URLENCODING);
        } catch (UnsupportedEncodingException e) {
            // Should never happen
            throw new RuntimeException("UTF-8 dencoding is not supported");
        }
    }

    /**
     * @return the id of the extension
     */
    public String getExtensionId()
    {
        return this.extensionId;
    }

    /**
     * @return the version of the extension
     */
    public String getExtensionVersion()
    {
        return this.extensionVersion;
    }

    /**
     * @return the id of the repository from were to get the extension file
     */
    public String getRepositoryId()
    {
        return this.repositoryId;
    }

    /**
     * @return the (optional) type of the repository from were to get the extension file
     */
    public String getRepositoryType()
    {
        return this.repositoryType;
    }

    /**
     * @return the (optional) uri of the repository from were to get the extension file
     */
    public URI getRepositoryURI()
    {
        return this.repositoryURI;
    }
}
