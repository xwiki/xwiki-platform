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
 * Date: 4 juin 2004
 * Time: 11:47:00
 */
package com.xpn.xwiki.user.api;

public class XWikiRightNotFoundException extends Exception {

    /**
     * Could not find the item you were looking for you
     */
    public XWikiRightNotFoundException() {
        super();
    }

    /**
     * Could not find the item you were looking for you
     *
     * @param msg message you want to send
     */
    public XWikiRightNotFoundException(String msg) {
        super(msg);
    }
}