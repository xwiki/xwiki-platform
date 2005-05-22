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
 * Date: 26 nov. 2003
 * Time: 13:52:39
 */
package com.xpn.xwiki.store;

import com.xpn.xwiki.XWikiContext;

public interface XWikiCacheStoreInterface extends XWikiStoreInterface {
    public XWikiStoreInterface getStore();
    public void setStore(XWikiStoreInterface store);
    public void flushCache();
    public void setCacheCapacity(int capacity);
    public void setPageExistCacheCapacity(int capacity);
    public void initCache(int capacity, int pageExistCapacity, XWikiContext context);
}
