/**
 * ===================================================================
 *
 * Copyright (c) 2005 Artem Melentev, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.

 * Created by
 * User: Artem Melentev
 * Date: 16.08.2005
 * Time: 05:52
 */
package com.xpn.xwiki.plugin.query;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.NamespaceException;

import org.apache.jackrabbit.Constants;
import org.apache.jackrabbit.core.SearchManager;
import org.apache.jackrabbit.name.NamespaceResolver;

/** XWiki NamespaceResolver for JackRabbits. Singleton */
public class XWikiNamespaceResolver implements NamespaceResolver, Constants {
    static Map prefixToURI = new HashMap();
    static Map uriToPrefix = new HashMap();
    public static final String
    	NS_FLEX_PREFFIX = "f",
    	NS_FLEX_URI = "flex",
    	NS_DOC_PREFFIX = "doc",
    	NS_DOC_URI = "XWikiDocument",
    	NS_OBJ_PREFFIX = "obj",
    	NS_OBJ_URI = "BaseObject";
    private static void addnamespace(String pref, String uri) {
    	prefixToURI.put(pref, uri);
    	uriToPrefix.put(uri, pref);
    }

    static {
        // default namespace (if no prefix is specified)
    	addnamespace(NS_EMPTY_PREFIX, NS_DEFAULT_URI);
        // declare the predefined mappings
        // rep:
    	addnamespace(NS_REP_PREFIX, NS_REP_URI);
        // jcr:
    	addnamespace(NS_JCR_PREFIX, NS_JCR_URI);
        // nt:
        addnamespace(NS_NT_PREFIX, NS_NT_URI);
        // mix:
        addnamespace(NS_MIX_PREFIX, NS_MIX_URI);
        // sv:
        addnamespace(NS_SV_PREFIX, NS_SV_URI);
        // xml:
        addnamespace(NS_XML_PREFIX, NS_XML_URI);
        // fn:
        addnamespace("fn", SearchManager.NS_FN_URI);
        // xs:
        addnamespace("xs", SearchManager.NS_XS_URI);
        
        // XWiki namespaces
        addnamespace(NS_FLEX_PREFFIX, NS_FLEX_URI);
        addnamespace(NS_DOC_PREFFIX, NS_DOC_URI);
        addnamespace(NS_OBJ_PREFFIX, NS_OBJ_URI);
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
