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
package com.xpn.xwiki.util;

import java.io.Reader;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.web.Utils;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * Add a backward compatibility layer to the {@link com.xpn.xwiki.util.Util} class.
 * 
 * @version $Id$
 */
public privileged aspect UtilCompatibilityAspect
{
    /**
     * @deprecated use {@link #getUniqueMatches(String, String, int)} instead
     */
    @Deprecated
    public List<String> Util.getMatches(String content, String spattern, int group)
        throws MalformedPatternException
    {
        return this.getUniqueMatches(content, spattern, group);
    }

    /** @deprecated Use {@link org.apache.commons.io.FileUtils#readFileToString(File, String)} */
    @Deprecated
    public static String Util.getFileContent(File file) throws IOException
    {
        ContextualAuthorizationManager authorization = Utils.getComponent(ContextualAuthorizationManager.class);
        if (!authorization.hasAccess(Right.PROGRAM)) {
            throw new IOException("Access denied.");
        }
        return FileUtils.readFileToString(file, XWiki.DEFAULT_ENCODING);
    }

    /** @deprecated Use {@link org.apache.commons.io.IOUtils#toString(Reader)} */
    @Deprecated
    public static String Util.getFileContent(Reader reader) throws IOException
    {
        ContextualAuthorizationManager authorization = Utils.getComponent(ContextualAuthorizationManager.class);
        if (!authorization.hasAccess(Right.PROGRAM)) {
            throw new IOException("Access denied.");
        }
        return IOUtils.toString(reader);
    }

    /** @deprecated Use {@link org.apache.commons.io.FileUtils#readFileToByteArray(File)} */
    @Deprecated
    public static byte[] Util.getFileContentAsBytes(File file) throws IOException
    {
        ContextualAuthorizationManager authorization = Utils.getComponent(ContextualAuthorizationManager.class);
        if (!authorization.hasAccess(Right.PROGRAM)) {
            throw new IOException("Access denied.");
        }
        return FileUtils.readFileToByteArray(file);
    }

    /** @deprecated Use {@link org.apache.commons.io.IOUtils#toByteArray(InputStream)} */
    @Deprecated
    public static byte[] Util.getFileContentAsBytes(InputStream is) throws IOException
    {
        ContextualAuthorizationManager authorization = Utils.getComponent(ContextualAuthorizationManager.class);
        if (!authorization.hasAccess(Right.PROGRAM)) {
            throw new IOException("Access denied.");
        }
        return IOUtils.toByteArray(is);
    }

    /**
     * API to obtain a DOM document for the specified string
     *
     * @param str The parsed text
     * @return A DOM document element corresponding to the string, or null on error
     * @deprecated use {@code $services.xml.parse()} instead
     */
    @Deprecated(since = "14.10")
    public org.w3c.dom.Document Util.getDOMForString(String str)
    {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            // Prevent XXE attacks
            dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return dbFactory.newDocumentBuilder().parse(new InputSource(new StringReader(str)));
        } catch (SAXException ex) {
            LOGGER.warn("Cannot parse string:" + str, ex);
        } catch (IOException ex) {
            LOGGER.warn("Cannot parse string:" + str, ex);
        } catch (ParserConfigurationException ex) {
            LOGGER.warn("Cannot parse string:" + str, ex);
        }

        return null;
    }

    /**
     * API to get a new DOM document
     *
     * @return a new DOM document element, or null on error
     * @deprecated use {@code XMLScriptService#createDOMDocument()} instead
     */
    @Deprecated(since = "14.10")
    public org.w3c.dom.Document Util.getDOMDocument()
    {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException ex) {
            LOGGER.warn("Cannot create DOM tree", ex);
        }

        return null;
    }
}
