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
package org.xwiki.rest;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiUser;

/**
 * <p>
 * A class containing utility methods used in the REST subsystem.
 * </p>
 * 
 * @version $Id$
 */
public class Utils
{
    /**
     * <p>
     * Get the page id given its components.
     * </p>
     * 
     * @param wikiName
     * @param spaceName
     * @param pageName
     * @return The page id.
     */
    public static String getPageId(String wikiName, String spaceName, String pageName)
    {
        XWikiDocument xwikiDocument = new XWikiDocument();
        xwikiDocument.setDatabase(wikiName);
        xwikiDocument.setName(pageName);
        xwikiDocument.setSpace(spaceName);

        Document document = new Document(xwikiDocument, null);

        return document.getPrefixedFullName();
    }

    /**
     * <p>
     * Get the page full name given its components.
     * </p>
     * 
     * @param wikiName
     * @param spaceName
     * @param pageName
     * @return The page full name.
     */
    public static String getPageFullName(String wikiName, String spaceName, String pageName)
    {
        XWikiDocument xwikiDocument = new XWikiDocument();
        xwikiDocument.setDatabase(wikiName);
        xwikiDocument.setName(pageName);
        xwikiDocument.setSpace(spaceName);

        Document document = new Document(xwikiDocument, null);

        return document.getFullName();
    }

    /**
     * <p>
     * Get the object id given its components.
     * </p>
     * 
     * @param wikiName
     * @param spaceName
     * @param pageName
     * @param className
     * @param objectNumber
     * @return The object id.
     */
    public static String getObjectId(String wikiName, String spaceName, String pageName, String className,
        int objectNumber)
    {
        XWikiDocument xwikiDocument = new XWikiDocument();
        xwikiDocument.setDatabase(wikiName);
        xwikiDocument.setName(pageName);
        xwikiDocument.setSpace(spaceName);

        Document document = new Document(xwikiDocument, null);

        return String.format("%s:%s[%d]", document.getPrefixedFullName(), className, objectNumber);
    }

    /**
     * <p>
     * Get the page id given its components.
     * </p>
     * @return The page id.
     */
    public static String getPageId(String wikiName, String pageFullName)
    {
        XWikiDocument xwikiDocument = new XWikiDocument();
        xwikiDocument.setDatabase(wikiName);
        xwikiDocument.setFullName(pageFullName);

        Document document = new Document(xwikiDocument, null);

        return document.getPrefixedFullName();
    }

    /**
     * <p>
     * Get parent document for a given document.
     * </p>
     * 
     * @param doc document to get the parent from.
     * @param xwikiApi the xwiki api.
     * @return parent of the given document, null if none is specified.
     * @throws XWikiException if getting the parent document has failed.
     */
    public static Document getParentDocument(Document doc, com.xpn.xwiki.api.XWiki xwikiApi) throws XWikiException
    {
        if (StringUtils.isEmpty(doc.getParent())) {
            return null;
        }
        /*
         * This is ugly but we have to mimic the behavior of link generation: if the parent does not specify its space,
         * use the current document space.
         */
        String parentName = doc.getParent();
        if (!parentName.contains(".")) {
            parentName = doc.getSpace() + "." + parentName;
        }
        return xwikiApi.getDocument(parentName);
    }

    /**
     * <p>
     * Retrieve the XWiki context from the current execution context
     * </p>
     * 
     * @param componentManager The component manager to be used to retrieve the execution context.
     * @return The XWiki context.
     * @throws RuntimeException If there was an error retrieving the context.
     */
    public static XWikiContext getXWikiContext(ComponentManager componentManager)
    {
        Execution execution;
        XWikiContext xwikiContext;
        try {
            execution = componentManager.getInstance(Execution.class);
            xwikiContext = (XWikiContext) execution.getContext().getProperty("xwikicontext");
            return xwikiContext;
        } catch (Exception e) {
            throw new RuntimeException("Unable to get XWiki context", e);
        }
    }

    /**
     * <p>
     * Retrieve the XWiki private API object
     * </p>
     * 
     * @param componentManager The component manager to be used to retrieve the execution context.
     * @return The XWiki private API object.
     */
    public static com.xpn.xwiki.XWiki getXWiki(ComponentManager componentManager)
    {
        return getXWikiContext(componentManager).getWiki();
    }

    /**
     * <p>
     * Retrieve the XWiki public API object
     * </p>
     * 
     * @param componentManager The component manager to be used to retrieve the execution context.
     * @return The XWiki public API object.
     * @throws RuntimeException If there was an error while initializing the XWiki public API object.
     */
    public static com.xpn.xwiki.api.XWiki getXWikiApi(ComponentManager componentManager)
    {
        return new com.xpn.xwiki.api.XWiki(getXWiki(componentManager), getXWikiContext(componentManager));
    }

    /**
     * <p>
     * Retrieve the XWiki user associated to the current XWiki context
     * </p>
     * 
     * @param componentManager The component manager to be used to retrieve the execution context.
     * @return The user associated to the current XWiki context.
     */
    public static String getXWikiUser(ComponentManager componentManager)
    {
        XWikiUser user = getXWikiContext(componentManager).getXWikiUser();
        if (user == null) {
            return "XWiki.Guest";
        }

        return user.getUser();
    }
}
