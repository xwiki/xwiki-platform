/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.
 *
 * Created by
 * User: Ludovic Dubost
 * Date: 24 nov. 2003
 * Time: 00:58:55
 */

package com.xpn.xwiki.store;

import com.xpn.xwiki.doc.XWikiDocInterface;
import com.xpn.xwiki.XWikiException;
import org.apache.commons.jrcs.rcs.Version;

import java.util.List;

public interface XWikiStoreInterface {
    public void saveXWikiDoc(XWikiDocInterface doc) throws XWikiException;
    public void loadXWikiDoc(XWikiDocInterface doc) throws XWikiException;
    public void loadXWikiDoc(XWikiDocInterface doc, String version) throws XWikiException;
    public Version[] getXWikiDocVersions(XWikiDocInterface doc) throws XWikiException;
    public XWikiDocCacheInterface newDocCache();
    public List getClassList() throws XWikiException;
    public List searchDocuments(String wheresql) throws XWikiException;
    public List searchDocuments(String wheresql, int nb, int start) throws XWikiException;
}
