/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * @author vmassol
 * @author sdumitriu
 */

package com.xpn.xwiki.test;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiRightService;

import java.util.ArrayList;
import java.util.List;

public class GroovyTestRightService implements XWikiRightService {
    public boolean checkAccess(String action, XWikiDocument doc, XWikiContext context) throws XWikiException {
        return true;
    }

    public boolean hasAccessLevel(String right, String username, String docname, XWikiContext context) throws XWikiException {
        return true;
    }

    public boolean hasProgrammingRights(XWikiContext context) {
        return true;
    }

    public boolean hasProgrammingRights(XWikiDocument doc, XWikiContext context) {
        return true;
    }

    public boolean hasAdminRights(XWikiContext context) {
        return true;
    }

    public List listAllLevels(XWikiContext context) throws XWikiException {
        return new ArrayList();
    }
}
