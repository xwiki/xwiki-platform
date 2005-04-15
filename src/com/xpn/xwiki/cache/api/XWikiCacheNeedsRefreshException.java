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
 * Time: 00:54:37
 */
package com.xpn.xwiki.cache.api;

import com.opensymphony.oscache.base.NeedsRefreshException;

public class XWikiCacheNeedsRefreshException extends Exception {
    protected Exception e;

    public XWikiCacheNeedsRefreshException(NeedsRefreshException e) {
        this.e = e;
    }


    /**
     * Returns the detail message string of this throwable.
     *
     * @return the detail message string of this <tt>Throwable</tt> instance
     *         (which may be <tt>null</tt>).
     */
    public String getMessage() {
        return e.getMessage();
    }
}
