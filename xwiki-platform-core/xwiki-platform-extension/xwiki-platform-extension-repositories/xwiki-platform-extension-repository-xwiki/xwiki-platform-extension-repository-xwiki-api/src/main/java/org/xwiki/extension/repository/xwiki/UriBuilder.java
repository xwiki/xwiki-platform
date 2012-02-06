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
package org.xwiki.extension.repository.xwiki;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Generate a URI to use to request a REST server.
 * <p>
 * Support javax.ws.rs.Path style URIs.
 * 
 * @version $Id$
 */
public class UriBuilder implements Cloneable
{
    private String scheme;

    private String host;

    private Integer port;

    private String userInfo;

    private CharSequence path;

    private CharSequence query;

    private String fragment;

    public UriBuilder(URI base, String path)
    {
        uri(base);
        path(path);
    }

    public UriBuilder(String base, String path)
    {
        try {
            uri(new URI(base));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid base URI [" + base + "]", e);
        }

        path(path);
    }

    private void uri(URI uri) throws IllegalArgumentException
    {
        if (uri == null) {
            throw new IllegalArgumentException("The URI must not be null");
        }

        if (uri.getScheme() != null) {
            this.scheme = uri.getScheme();
        }
        if (uri.getHost() != null) {
            this.host = uri.getHost();
        }
        if (uri.getPort() > 0) {
            this.port = uri.getPort();
        }
        if (uri.getRawUserInfo() != null) {
            this.userInfo = uri.getRawUserInfo();
        }
        if (uri.getRawPath() != null) {
            this.path = uri.getRawPath();
        }
        if (uri.getRawQuery() != null) {
            this.query = uri.getRawQuery();
        }
        if (uri.getRawFragment() != null) {
            this.fragment = uri.getRawFragment();
        }
    }

    private void path(String path)
    {
        if (path != null && !path.isEmpty()) {
            StringBuilder stringBuilder;
            if (this.path != null) {
                if (this.path instanceof String) {
                    stringBuilder = new StringBuilder(this.path);
                    this.path = stringBuilder;
                } else {
                    stringBuilder = (StringBuilder) this.path;
                }
            } else {
                stringBuilder = new StringBuilder();
                this.path = stringBuilder;
            }

            if (this.path.length() == 0 || this.path.charAt(this.path.length() - 1) != '/') {
                if (path.charAt(0) != '/') {
                    stringBuilder.append('/');
                }

                stringBuilder.append(path);
            } else {
                int i = 0;
                for (; i < path.length() && path.charAt(i) == '/'; ++i)
                    ;

                if (i > 0) {
                    stringBuilder.append(path.substring(i));
                } else {
                    stringBuilder.append(path);
                }
            }
        }
    }

    public static String encode(String toEncode)
    {
        String result = null;

        if (toEncode != null) {
            try {
                result = java.net.URLEncoder.encode(toEncode, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                // Should never happen
            }
        }

        return result;
    }

    public UriBuilder queryParam(String name, Object... values) throws IllegalArgumentException
    {
        if (values == null) {
            throw new IllegalArgumentException("The values must not be null");
        }

        String encodedName = encode(name);

        StringBuilder queryBuilder;
        if (this.query == null) {
            queryBuilder = new StringBuilder();
            this.query = queryBuilder;
        } else if (this.query instanceof StringBuilder) {
            queryBuilder = (StringBuilder) this.query;
        } else {
            queryBuilder = new StringBuilder(this.query);
            this.query = queryBuilder;
        }

        for (Object value : values) {
            if (queryBuilder.length() > 0) {
                queryBuilder.append('&');
            }
            queryBuilder.append(encodedName);
            queryBuilder.append('=');
            queryBuilder.append(encode(value.toString()));
        }

        return this;
    }

    public URI build(Object... values)
    {
        final StringBuilder stb = new StringBuilder();
        if (this.scheme != null) {
            stb.append(this.scheme);
            stb.append("://");
        }
        if (this.userInfo != null) {
            stb.append(this.userInfo);
            stb.append('@');
        }
        if (this.host != null) {
            stb.append(this.host);
        }
        if (this.port != null) {
            stb.append(':');
            stb.append(this.port);
        }

        String resolvePath = formatPath(values);
        if (resolvePath != null) {
            if (stb.length() > 0) {
                if (resolvePath.length() == 0 || resolvePath.charAt(0) != '/') {
                    stb.append('/');
                }
            }
            stb.append(resolvePath);
        }

        String query = this.query != null ? this.query.toString() : null;
        if (query != null) {
            stb.append('?');
            stb.append(query);
        }
        if (this.fragment != null) {
            stb.append('#');
            stb.append(this.fragment);
        }

        try {
            return new URI(stb.toString());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Failed to build the URI", e);
        }
    }

    public static boolean isUnreserved(char character)
    {
        return Character.isLetter(character) || Character.isDigit(character) || (character == '-')
            || (character == '.') || (character == '_') || (character == '~');
    }

    private String formatPath(Object[] values)
    {
        final StringBuilder result = new StringBuilder();

        StringBuilder varBuffer = null;
        char c;
        boolean inVariable = false;
        final int patternLength = this.path.length();
        int valueId = 0;
        for (int i = 0; i < patternLength; i++) {
            c = this.path.charAt(i);

            if (inVariable) {
                if (isUnreserved(c)) {
                    // Append to the variable name
                    varBuffer.append(c);
                } else if (c == '}') {
                    // End of variable detected
                    if (varBuffer.length() == 0) {
                        // TODO: log ?
                    } else {
                        Object varValue = values[valueId++];

                        String varValueString = (varValue == null) ? null : varValue.toString();

                        result.append(encode(varValueString));

                        // Reset the variable name buffer
                        varBuffer = new StringBuilder();
                    }
                    inVariable = false;
                } else {
                    // TODO: log ?
                }
            } else {
                if (c == '{') {
                    inVariable = true;
                    varBuffer = new StringBuilder();
                } else if (c == '}') {
                    // TODO: log ?
                } else {
                    result.append(c);
                }
            }
        }

        return result.toString();
    }

    // Object

    @Override
    public String toString()
    {
        return build().toString();
    }

    @Override
    public UriBuilder clone()
    {
        UriBuilder clone = null;

        try {
            clone = (UriBuilder) super.clone();
        } catch (CloneNotSupportedException e) {
            // Should never happen
        }

        return clone;
    }
}
