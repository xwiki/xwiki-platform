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
package org.xwiki.url;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.resource.CreateResourceReferenceException;
import org.xwiki.stability.Unstable;

/**
 * Extend a {@link URL} by providing access to the URL path segments (URL-decoded).
 *
 * @version $Id$
 * @since 6.1M2
 */
@Unstable
public class ExtendedURL implements Cloneable
{
    /**
     * URL path separator character.
     */
    private static final String URL_SEPARATOR = "/";

    private static final String UTF8 = "UTF-8";

    /**
     * @see #getURI()
     */
    private URI uri;

    /**
     * @see #getSegments()
     */
    private List<String> segments;

    /**
     * Populate the Extended URL with a list of path segments.
     *
     * @param segments the path segments of the URL
     */
    public ExtendedURL(List<String> segments)
    {
        this.segments = new ArrayList<>(segments);
    }

    /**
     * @param url the URL being wrapped
     * @throws org.xwiki.resource.CreateResourceReferenceException if the passed URL is invalid which can happen if it
     *         has incorrect encoding
     */
    public ExtendedURL(URL url) throws CreateResourceReferenceException
    {
        this(url, null);
    }

    /**
     * @param url the URL being wrapped
     * @param ignorePrefix the ignore prefix must start with "/" (eg "/xwiki"). It can be empty or null too in which
     *        case it's not used
     * @throws org.xwiki.resource.CreateResourceReferenceException if the passed URL is invalid which can happen if it
     *         has incorrect encoding
     */
    public ExtendedURL(URL url, String ignorePrefix) throws CreateResourceReferenceException
    {
        // Convert the URL to a URI since URI performs correctly decoding.
        // Note that this means that this method only accepts valid URLs (with proper encoding)
        URI internalURI;
        try {
            internalURI = url.toURI();
        } catch (URISyntaxException e) {
            throw new CreateResourceReferenceException(String.format("Invalid URL [%s]", url), e);
        }
        this.uri = internalURI;

        String rawPath = getURI().getRawPath();
        if (!StringUtils.isEmpty(ignorePrefix)) {

            // Allow the passed ignore prefix to not contain the leading "/"
            String normalizedIgnorePrefix = ignorePrefix;
            if (!ignorePrefix.startsWith(URL_SEPARATOR)) {
                normalizedIgnorePrefix = URL_SEPARATOR + ignorePrefix;
            }

            if (!getURI().getPath().startsWith(normalizedIgnorePrefix)) {
                throw new CreateResourceReferenceException(
                    String.format("URL Path [%s] doesn't start with [%s]", getURI().getPath(), ignorePrefix));
            }
            // Note: We also remove the leading "/" after the context path.
            rawPath = rawPath.substring(ignorePrefix.length() + 1);
        }

        // Remove leading "/" if any
        rawPath = StringUtils.removeStart(rawPath, URL_SEPARATOR);

        this.segments = extractPathSegments(rawPath);
    }

    /**
     * @return the path segments (each part of the URL separated by the path separator character)
     */
    public List<String> getSegments()
    {
        return this.segments;
    }

    /**
     * @return the URI corresponding to the passed URL that this instance wraps, provided as a helper feature
     */
    public URI getURI()
    {
        return this.uri;
    }

    /**
     * Extract segments between "/" characters in the passed path. Also remove any path parameters (i.e. content
     * after ";" in a path segment; for ex ";jsessionid=...") since we don't want to have these params in the
     * segments we return and act on (otherwise we would get them in document names for example).
     * <p/>
     * Note that we only remove ";" characters when they are not URL-encoded. We want to allow the ";" character to be
     * in document names for example.
     *
     * @param rawPath the path from which to extract the segments
     * @return the extracted path segments
     */
    private List<String> extractPathSegments(String rawPath)
    {
        List<String> urlSegments = new ArrayList<String>();

        // Note that we use -1 in the call below in order to get empty segments too. This is needed since in our URL
        // scheme a tailing "/" can have a meaning (for example "bin/view/Page" can represent a Page while
        // "bin/view/Space/" can represents a Space).
        for (String pathSegment : rawPath.split(URL_SEPARATOR, -1)) {

            // Remove path parameters
            String normalizedPathSegment = pathSegment.split(";", 2)[0];

            // Now let's decode it
            String decodedPathSegment;
            try {
                // Note: we decode using UTF-8 since the URI javadoc says:
                // "A sequence of escaped octets is decoded by replacing it with the sequence of characters that it
                // represents in the UTF-8 character set. UTF-8 contains US-ASCII, hence decoding has the effect of
                // de-quoting any quoted US-ASCII characters as well as that of decoding any encoded non-US-ASCII
                // characters."
                decodedPathSegment = URLDecoder.decode(normalizedPathSegment, UTF8);
            } catch (UnsupportedEncodingException e) {
                // Not supporting UTF-8 as a valid encoding for some reasons. We consider XWiki cannot work
                // without that encoding.
                throw new RuntimeException(
                    String.format("Failed to URL decode [%s] using UTF-8.", normalizedPathSegment), e);
            }

            urlSegments.add(decodedPathSegment);
        }

        return urlSegments;
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(7, 7)
            .append(getURI())
            .append(getSegments())
            .toHashCode();
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == null) {
            return false;
        }
        if (object == this) {
            return true;
        }
        if (object.getClass() != getClass()) {
            return false;
        }
        ExtendedURL rhs = (ExtendedURL) object;
        return new EqualsBuilder()
            .append(getURI(), rhs.getURI())
            .append(getSegments(), rhs.getSegments())
            .isEquals();
    }

    /**
     * @return the serialized segments as a relative URL with URL-encoded path segments
     */
    public String serialize()
    {
        List<String> encodedSegments = new ArrayList<>();
        for (String path : getSegments()) {
            try {
                encodedSegments.add(URLEncoder.encode(path, UTF8));
            } catch (UnsupportedEncodingException e) {
                // Not supporting UTF-8 as a valid encoding for some reasons. We consider XWiki cannot work
                // without that encoding.
                throw new RuntimeException(String.format("Failed to URL encode [%s] using UTF-8.", path), e);
            }
        }

        return StringUtils.join(encodedSegments, URL_SEPARATOR);
    }

    @Override
    public String toString()
    {
        return serialize();
    }
}
