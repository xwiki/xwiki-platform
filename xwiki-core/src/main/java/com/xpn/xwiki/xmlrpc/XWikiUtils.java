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
 *
 */
package com.xpn.xwiki.xmlrpc;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;

import org.apache.xmlrpc.XmlRpcException;
import org.xwiki.xmlrpc.model.XWikiExtendedId;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * This is an helper class containing some utility method for handling and setting up the XWiki and XMLRPC data objects
 * needed to serve XMLRPC requests.
 * 
 * @version $Id$
 */
public class XWikiUtils
{
    public static Map getTokens(XWikiContext context)
    {
        Map tokens = (Map) context.getEngineContext().getAttribute("xmlrpc_tokens");
        if (tokens == null) {
            tokens = new HashMap();
            context.getEngineContext().setAttribute("xmlrpc_tokens", tokens);
        }

        return tokens;
    }

    public static XWikiXmlRpcUser checkToken(String token, XWikiContext context) throws Exception
    {
        XWikiXmlRpcUser user = null;
        String ip = context.getRequest().getRemoteAddr();

        /* Check if we must grant guest access when no token is provided. Default is true. */        
        boolean allowGuest = context.getWiki().ParamAsLong("xwiki.xmlrpc.allowGuest", 1) != 0;

        if (token != null) {
            if (token.equals("")) {
                /* If no token is provided, then grant guest access or refuse it, depending on the current configuration */
                if (allowGuest) {
                    user = new XWikiXmlRpcUser(XWikiRightService.GUEST_USER_FULLNAME, ip);
                } else {
                    throw new Exception(String.format("[Guest access denied from IP '%s']", ip));
                }
            } else {
                user = (XWikiXmlRpcUser) getTokens(context).get(token);
            }
        }

        if ((user == null) || (!user.getRemoteIp().equals(ip))) {
            throw new Exception(String.format("[Access Denied: authentication token '%s' for IP '%s' is invalid]",
                token, ip));
        }

        context.setUser(user.getName());

        return user;
    }

    /**
     * <p>
     * Gets a document. This method can be used to retrieve a specific translation or a version of a page by using
     * extended ids in the form of Space.Page[?language=l&version=v&minorVersion=mv] where all parameters are optional.
     * </p>
     * <p>
     * For example:
     * <ul>
     * <li><code>Main.WebHome</code>: retrieves Main.WebHome at its latest version in the default language</li>
     * <li><code>Main.WebHome?language=fr</code>: retrieves Main.WebHome in its french translation</li>
     * <li><code>Main.WebHome?version=3</code>: retrieves Main.WebHome at version 3.1 in the default language</li>
     * <li><code>Main.WebHome?language=fr&version=2</code>: retrieves the version 2.1 of the french translation</li>
     * </ul>
     * </p>
     * 
     * @param xwikiApi The api object for accessing XWiki functionalities.
     * @param extendedPageId The extended page id
     * @param failIfDoesntExist True is an exception has to be raised if the page doesn't exist.
     * @return Always returns a document if success. Otherwise an exception is raised. Never returns null.
     * @throws Exception
     * @throws XmlRpcException An exception is thrown if the requested document doesn't exist or cannot be accessed or
     *             if there has been a problem with the underlying XWiki infrastructure.
     * @throws Exception
     */
    public static Document getDocument(com.xpn.xwiki.api.XWiki xwikiApi, String extendedPageId,
        boolean failIfDoesntExist) throws Exception
    {
        XWikiExtendedId id = new XWikiExtendedId(extendedPageId);

        String pageId = id.getBasePageId();

        String language = id.getParameter(XWikiExtendedId.LANGUAGE_PARAMETER);

        String versionString = id.getParameter(XWikiExtendedId.VERSION_PARAMETER);
        int version = versionString != null ? Integer.parseInt(versionString) : 0;

        String minorVersionString = id.getParameter(XWikiExtendedId.MINOR_VERSION_PARAMETER);
        int minorVersion = minorVersionString != null ? Integer.parseInt(minorVersionString) : 1;

        if (failIfDoesntExist) {
            /* Check if the page exists */
            if (!xwikiApi.exists(pageId)) {
                throw new Exception(String.format("[Page '%s' doesn't exist]", pageId));
            }
        }

        /* Get the base doc */
        Document doc = xwikiApi.getDocument(pageId);
        if (doc == null) {
            throw new Exception(String.format("[Page '%s' cannot be accessed]", pageId));
        }

        /* In case the language is specified, get the translated document */
        if (language != null && !language.equals("") && doc.getTranslationList().contains(language)) {
            doc = doc.getTranslatedDocument(language);
            if (doc == null) {
                throw new Exception(String.format("[Page '%s' at translation '%s' cannot be accessed]", pageId,
                    language));
            }
        }

        /* Get the specific version of the document */
        if (version != 0) {
            doc = doc.getDocumentRevision(String.format("%d.%d", version, minorVersion));
            if (doc == null) {
                throw new Exception(String.format("[Page '%s' at version '%d.%d' cannot be accessed (language '%s')]",
                    pageId, version, minorVersion, language != null ? language : "default"));
            }
        }

        return doc;
    }

    public static com.xpn.xwiki.api.Object getObjectByGuid(Document doc, String guid)
    {
        Map<String, Vector<com.xpn.xwiki.api.Object>> classToObjectsMap = doc.getxWikiObjects();
        for (Entry<String, Vector<com.xpn.xwiki.api.Object>> classObjects : classToObjectsMap.entrySet()) {
            for (com.xpn.xwiki.api.Object object : classObjects.getValue()) {
                if (guid.equals(object.getGuid())) {
                    return object;
                }
            }
        }

        return null;
    }

    public static Object xmlRpcConvert(Object object)
    {
        if (object == null) {
            return "__NULL__VALUE__";
        }

        if (object.getClass().isArray()) {
            Object[] objects = (Object[]) object;
            List result = new ArrayList();
            for (int i = 0; i < objects.length; i++) {
                result.add(xmlRpcConvert(objects[i]));
            }

            return result;
        } else if (object instanceof List) {
            List list = (List) object;
            List result = new ArrayList();
            for (int i = 0; i < list.size(); i++) {
                result.add(xmlRpcConvert(list.get(i)));
            }

            return result;
        } else if (object instanceof Map) {
            Map map = (Map) object;
            Map result = new HashMap();
            for (Object objEntry : map.entrySet()) {
                Map.Entry entry = (Map.Entry) objEntry;
                result.put(entry.getKey(), xmlRpcConvert(entry.getValue()));
            }

            return result;
        } else if (object instanceof Date) {
            return object; // dateFormat.format((Date) object);
        }

        return object.toString();
    }
}
