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
 * Date: 23 nov. 2003
 * Time: 23:58:57
 */
package com.xpn.xwiki.doc;

import com.xpn.xwiki.*;
import com.xpn.xwiki.store.*;

public abstract class XWikiDefaultDoc implements XWikiDocInterface {
  private XWikiStoreInterface store;
  private XWikiDocCacheInterface docCache;

    public XWikiStoreInterface getStore() {
        return store;
    }

    public void setStore(XWikiStoreInterface store) {
        this.store = store;
    }

    public XWikiDocCacheInterface getDocCache() {
        if ((docCache==null)&&(store!=null)) {
            docCache = store.newDocCache();
        }
        return docCache;
    }

    public void setDocCache(XWikiDocCacheInterface docCache) {
        this.docCache = docCache;
    }
}
