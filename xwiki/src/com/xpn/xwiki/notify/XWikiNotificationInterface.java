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

 * Created by
 * User: Ludovic Dubost
 * Date: 23 janv. 2004
 * Time: 18:00:36
 */
package com.xpn.xwiki.notify;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocInterface;

public interface XWikiNotificationInterface {
    public static int EVENT_CHANGE = 0;
    public static int EVENT_NEW = 1;
    public static int EVENT_DELETE = 2;
    public static int EVENT_UPDATE_CONTENT = 3;
    public static int EVENT_UPDATE_OBJECT = 4;
    public static int EVENT_UPDATE_CLASS = 5;

    public void notify(XWikiNotificationRule rule, XWikiDocInterface newdoc, XWikiDocInterface olddoc, int event, XWikiContext context);
}
