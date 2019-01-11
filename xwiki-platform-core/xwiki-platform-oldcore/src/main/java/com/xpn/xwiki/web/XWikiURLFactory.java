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
package com.xpn.xwiki.web;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import com.xpn.xwiki.XWikiContext;

public interface XWikiURLFactory
{
    void init(XWikiContext context);

    /**
     * @param spaces a serialized space reference which can contain one or several spaces (e.g. "space1.space2"). If
     *        a space name contains a dot (".") it must be passed escaped as in "space1\.with\.dot.space2"
     */
    URL createURL(String spaces, String name, XWikiContext context);

    /**
     * @param spaces a serialized space reference which can contain one or several spaces (e.g. "space1.space2"). If
     *        a space name contains a dot (".") it must be passed escaped as in "space1\.with\.dot.space2"
     */
    URL createURL(String spaces, String name, String action, XWikiContext context);

    /**
     * @param spaces a serialized space reference which can contain one or several spaces (e.g. "space1.space2"). If
     *        a space name contains a dot (".") it must be passed escaped as in "space1\.with\.dot.space2"
     */
    URL createURL(String spaces, String name, String action, boolean redirect, XWikiContext context);

    /**
     * @param spaces a serialized space reference which can contain one or several spaces (e.g. "space1.space2"). If
     *        a space name contains a dot (".") it must be passed escaped as in "space1\.with\.dot.space2"
     * @param querystring the URL-encoded Query String. It's important to realize that the implementation of this
     *        method cannot encode it automatically since the Query String is passed as a String (and it's not possible
     *        to differentiate between a '=' character that should be encoded and one that shouldn't. Imagine an input
     *        of 'a=&amp;b=c' which can be understood either as 'a' = '&amp;b=c' or as 'a' = '' and 'b' = 'c'). Ideally
     *        we would need an API signature that accepts a {@code Map&lt;String, String&gt;} for the Query String, for
     *        example
     */
    URL createURL(String spaces, String name, String action, String querystring, String anchor, XWikiContext context);

    /**
     * @param spaces a serialized space reference which can contain one or several spaces (e.g. "space1.space2"). If
     *        a space name contains a dot (".") it must be passed escaped as in "space1\.with\.dot.space2"
     * @param querystring the URL-encoded Query String. It's important to realize that the implementation of this
     *        method cannot encode it automatically since the Query String is passed as a String (and it's not possible
     *        to differentiate between a '=' character that should be encoded and one that shouldn't. Imagine an input
     *        of 'a=&amp;b=c' which can be understood either as 'a' = '&amp;b=c' or as 'a' = '' and 'b' = 'c'). Ideally
     *        we would need an API signature that accepts a {@code Map&lt;String, String&gt;} for the Query String, for
     *        example
     */
    URL createExternalURL(String spaces, String name, String action, String querystring, String anchor,
        XWikiContext context);

    /**
     * @param spaces a serialized space reference which can contain one or several spaces (e.g. "space1.space2"). If
     *        a space name contains a dot (".") it must be passed escaped as in "space1\.with\.dot.space2"
     * @param querystring the URL-encoded Query String. It's important to realize that the implementation of this
     *        method cannot encode it automatically since the Query String is passed as a String (and it's not possible
     *        to differentiate between a '=' character that should be encoded and one that shouldn't. Imagine an input
     *        of 'a=&amp;b=c' which can be understood either as 'a' = '&amp;b=c' or as 'a' = '' and 'b' = 'c'). Ideally
     *        we would need an API signature that accepts a {@code Map&lt;String, String&gt;} for the Query String, for
     *        example
     */
    URL createURL(String spaces, String name, String action, String querystring, String anchor, String xwikidb,
        XWikiContext context);

    /**
     * @param spaces a serialized space reference which can contain one or several spaces (e.g. "space1.space2"). If
     *        a space name contains a dot (".") it must be passed escaped as in "space1\.with\.dot.space2"
     * @param querystring the URL-encoded Query String. It's important to realize that the implementation of this
     *        method cannot encode it automatically since the Query String is passed as a String (and it's not possible
     *        to differentiate between a '=' character that should be encoded and one that shouldn't. Imagine an input
     *        of 'a=&amp;b=c' which can be understood either as 'a' = '&amp;b=c' or as 'a' = '' and 'b' = 'c'). Ideally
     *        we would need an API signature that accepts a {@code Map&lt;String, String&gt;} for the Query String, for
     *        example
     */
    URL createExternalURL(String spaces, String name, String action, String querystring, String anchor,
        String xwikidb, XWikiContext context);

    URL createSkinURL(String filename, String skin, XWikiContext context);

    URL createSkinURL(String filename, String spaces, String name, XWikiContext context);

    URL createSkinURL(String filename, String spaces, String name, String xwikidb, XWikiContext context);

    URL createResourceURL(String filename, boolean forceSkinAction, XWikiContext context);

    URL createResourceURL(String filename, boolean forceSkinAction, XWikiContext context,
        Map<String, String> queryParameters);

    /**
     * @param spaces a serialized space reference which can contain one or several spaces (e.g. "space1.space2"). If
     *        a space name contains a dot (".") it must be passed escaped as in "space1\.with\.dot.space2"
     * @param querystring the URL-encoded Query String. It's important to realize that the implementation of this
     *        method cannot encode it automatically since the Query String is passed as a String (and it's not possible
     *        to differentiate between a '=' character that should be encoded and one that shouldn't. Imagine an input
     *        of 'a=&amp;b=c' which can be understood either as 'a' = '&amp;b=c' or as 'a' = '' and 'b' = 'c'). Ideally
     *        we would need an API signature that accepts a {@code Map&lt;String, String&gt;} for the Query String, for
     *        example
     */
    URL createAttachmentURL(String filename, String spaces, String name, String action, String querystring,
        XWikiContext context);

    /**
     * @param spaces a serialized space reference which can contain one or several spaces (e.g. "space1.space2"). If
     *        a space name contains a dot (".") it must be passed escaped as in "space1\.with\.dot.space2"
     * @param querystring the URL-encoded Query String. It's important to realize that the implementation of this
     *        method cannot encode it automatically since the Query String is passed as a String (and it's not possible
     *        to differentiate between a '=' character that should be encoded and one that shouldn't. Imagine an input
     *        of 'a=&amp;b=c' which can be understood either as 'a' = '&amp;b=c' or as 'a' = '' and 'b' = 'c'). Ideally
     *        we would need an API signature that accepts a {@code Map&lt;String, String&gt;} for the Query String, for
     *        example
     */
    URL createAttachmentURL(String filename, String spaces, String name, String action, String querystring, String xwikidb,
        XWikiContext context);

    /**
     * @param spaces a serialized space reference which can contain one or several spaces (e.g. "space1.space2"). If
     *        a space name contains a dot (".") it must be passed escaped as in "space1\.with\.dot.space2"
     * @param querystring the URL-encoded Query String. It's important to realize that the implementation of this
     *        method cannot encode it automatically since the Query String is passed as a String (and it's not possible
     *        to differentiate between a '=' character that should be encoded and one that shouldn't. Imagine an input
     *        of 'a=&amp;b=c' which can be understood either as 'a' = '&amp;b=c' or as 'a' = '' and 'b' = 'c'). Ideally
     *        we would need an API signature that accepts a {@code Map&lt;String, String&gt;} for the Query String, for
     *        example
     */
    URL createAttachmentRevisionURL(String filename, String spaces, String name, String revision, String querystring,
        XWikiContext context);

    /**
     * @param spaces a serialized space reference which can contain one or several spaces (e.g. "space1.space2"). If
     *        a space name contains a dot (".") it must be passed escaped as in "space1\.with\.dot.space2"
     * @param querystring the URL-encoded Query String. It's important to realize that the implementation of this
     *        method cannot encode it automatically since the Query String is passed as a String (and it's not possible
     *        to differentiate between a '=' character that should be encoded and one that shouldn't. Imagine an input
     *        of 'a=&amp;b=c' which can be understood either as 'a' = '&amp;b=c' or as 'a' = '' and 'b' = 'c'). Ideally
     *        we would need an API signature that accepts a {@code Map&lt;String, String&gt;} for the Query String, for
     *        example
     */
    URL createAttachmentRevisionURL(String filename, String spaces, String name, String revision, String querystring,
        String xwikidb, XWikiContext context);

    URL getRequestURL(XWikiContext context);

    /**
     * Converts a URL to a string representation. It's up to the implementation to decide whether to perform
     * transformations or not on the URL. For example some implementations will convert the URL to a relative URL if the
     * URL is an internal XWiki URL.
     *
     * @param url the URL to convert
     * @return the converted URL as a string
     */
    String getURL(URL url, XWikiContext context);

    String getURL(URL url, XWikiContext context, Map<String, String> queryParameters);

    /**
     * Generate the base external URL to access this server.
     *
     * @param context the XWiki context.
     * @return the URL of the server.
     * @throws MalformedURLException error when creating the {@link URL}.
     */
    URL getServerURL(XWikiContext context) throws MalformedURLException;
}
