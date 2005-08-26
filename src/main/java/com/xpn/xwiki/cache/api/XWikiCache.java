/**
 * ===================================================================
 *
 * Copyright (c) 2003,2004 Ludovic Dubost, All rights reserved.
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

 * Created by
 * User: Ludovic Dubost
 * Date: 4 avr. 2005
 * Time: 00:52:19
 */
package com.xpn.xwiki.cache.api;


public interface XWikiCache {
    void setCapacity(int capacity);
    void flushEntry(String key);
    void putInCache(String key, Object obj);
    Object getFromCache(String key) throws XWikiCacheNeedsRefreshException;
    Object getFromCache(String key, int refeshPeriod) throws XWikiCacheNeedsRefreshException;
    void cancelUpdate(String key);
    void flushAll();
}
