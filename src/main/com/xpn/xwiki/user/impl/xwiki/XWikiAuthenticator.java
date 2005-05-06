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
 * Date: 6 mai 2005
 * Time: 22:47:16
 */
package com.xpn.xwiki.user.impl.xwiki;

import org.securityfilter.authenticator.Authenticator;
import org.securityfilter.filter.SecurityRequestWrapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.xpn.xwiki.XWikiContext;

import java.io.IOException;

public interface XWikiAuthenticator extends Authenticator {
    public boolean processLogin(SecurityRequestWrapper request, HttpServletResponse response, XWikiContext context) throws Exception;
    public void showLogin(HttpServletRequest request, HttpServletResponse response, XWikiContext context) throws IOException;
}
