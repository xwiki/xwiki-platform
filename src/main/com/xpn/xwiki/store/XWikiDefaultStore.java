/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
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
 *
 * Created by
 * User: Ludovic Dubost
 * Date: 24 nov. 2003
 * Time: 00:59:50
 */

package com.xpn.xwiki.store;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

import java.util.List;

public abstract class XWikiDefaultStore implements XWikiStoreInterface {

    public List searchDocumentsNames(String wheresql, XWikiContext context) throws XWikiException {
        return searchDocumentsNames(wheresql, 0, 0, "", context);
    }

    public List searchDocumentsNames(String wheresql, int nb, int start, XWikiContext context) throws XWikiException {
        return searchDocumentsNames(wheresql, nb, start, "", context);
    }

    public List searchDocuments(String wheresql, XWikiContext context) throws XWikiException {
        return searchDocuments(wheresql, true, 0, 0, context);
    }

    public List searchDocuments(String wheresql, boolean distinctbylanguage, XWikiContext context) throws XWikiException {
        return searchDocuments(wheresql, distinctbylanguage, 0, 0, context);
    }

    public List searchDocuments(String wheresql, int nb, int start, XWikiContext context) throws XWikiException {
        return searchDocuments(wheresql, true, nb, start, context);
    }
}
