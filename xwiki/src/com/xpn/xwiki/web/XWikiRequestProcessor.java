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
 * Date: 25 nov. 2003
 * Time: 23:46:34
 */
package com.xpn.xwiki.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class XWikiRequestProcessor extends org.apache.struts.action.RequestProcessor {
    protected String processPath(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        String result = super.processPath(httpServletRequest, httpServletResponse);
        String res = result.substring(0,result.indexOf("/",1)+1);
        return res;
    }
}
