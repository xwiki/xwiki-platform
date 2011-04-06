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

package com.xpn.xwiki.plugin.query;

import org.apache.jackrabbit.core.SearchManager;
import org.apache.jackrabbit.name.AbstractNamespaceResolver;
import org.apache.jackrabbit.name.QName;

import javax.jcr.NamespaceException;
import java.util.HashMap;
import java.util.Map;

/** XWiki NamespaceResolver for JackRabbits. Singleton */
public class XWikiNamespaceResolver extends AbstractNamespaceResolver {
    static Map prefixToURI = new HashMap();
    static Map uriToPrefix = new HashMap();
    public static final String
    	NS_XWIKI_PROPERTY_PREFFIX = "xp",
    	NS_XWIKI_PROPERTY_URI = "http://www.xwiki.org/property",
    	NS_DOC_PREFFIX = "doc",
    	NS_DOC_URI = "XWikiDocument",
    	NS_OBJ_PREFFIX = "obj",
    	NS_OBJ_URI = "BaseObject",
    	NS_XWIKI_PREFFIX	= "xwiki",
    	NS_XWIKI_URI		= "http://www.xwiki.org/";
    private static void addnamespace(String pref, String uri) {
    	prefixToURI.put(pref, uri);
    	uriToPrefix.put(uri, pref);
    }

    static {
        // default namespace (if no prefix is specified)
    	addnamespace(QName.NS_EMPTY_PREFIX, QName.NS_DEFAULT_URI);
        // declare the predefined mappings
        // rep:
    	addnamespace(QName.NS_REP_PREFIX, QName.NS_REP_URI);
        // jcr:
    	addnamespace(QName.NS_JCR_PREFIX, QName.NS_JCR_URI);
        // nt:
        addnamespace(QName.NS_NT_PREFIX, QName.NS_NT_URI);
        // mix:
        addnamespace(QName.NS_MIX_PREFIX, QName.NS_MIX_URI);
        // sv:
        addnamespace(QName.NS_SV_PREFIX, QName.NS_SV_URI);
        // xml:
        addnamespace(QName.NS_XML_PREFIX, QName.NS_XML_URI);
        // fn:
        addnamespace("fn", SearchManager.NS_FN_URI);
        // xs:
        addnamespace("xs", SearchManager.NS_XS_URI);
        
        // XWiki namespaces
        addnamespace(NS_XWIKI_PROPERTY_PREFFIX, NS_XWIKI_PROPERTY_URI);
        addnamespace(NS_DOC_PREFFIX, NS_DOC_URI);   // XXX: hibernate-specific
        addnamespace(NS_OBJ_PREFFIX, NS_OBJ_URI);   // XXX: hibernate-specific
        addnamespace(NS_XWIKI_PREFFIX, NS_XWIKI_URI);
        //addnamespace(NS_XWIKI_PROPERTY_PREFFIX, NS_XWIKI_PROPERTY_URI);
    }
	
	public String getURI(String prefix) throws NamespaceException {
		if (!prefixToURI.containsKey(prefix)) {
            throw new NamespaceException(prefix + ": is not a registered namespace prefix.");
        }
        return (String) prefixToURI.get(prefix);
	}
	public String getPrefix(String uri) throws NamespaceException {
		if (!uriToPrefix.containsKey(uri)) {
            throw new NamespaceException(uri + ": is not a registered namespace uri.");
        }
        return (String) uriToPrefix.get(uri);
	}
	private static XWikiNamespaceResolver _instance;
	public static XWikiNamespaceResolver getInstance() {
		if (_instance==null)
			_instance = new XWikiNamespaceResolver();
		return _instance;
	}
}
