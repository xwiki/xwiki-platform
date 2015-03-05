/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
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
 */
package com.xpn.xwiki.web;

import com.xpn.xwiki.util.Util;

/**
 * Struts form for {@link DeleteVersionsAction}.
 *
 * @version $Id$
 */
public class DeleteVersionsForm extends XWikiForm
{
    /** from revision. */
    private String rev1;

    /** to revision. */
    private String rev2;

    /** single version. */
    private String rev;

    /** document language. */
    private String language;

    /** is action confirmed. */
    private boolean confirm;

    @Override
    public void readRequest()
    {
        XWikiRequest request = getRequest();
        this.rev1 = request.getParameter("rev1");
        this.rev2 = request.getParameter("rev2");
        this.rev = request.getParameter("rev");
        this.language = Util.normalizeLanguage(request.getParameter("language"));
        this.confirm = request.getParameter("confirm") != null;
    }

    /**
     * @return from revision
     */
    public String getRev1()
    {
        return this.rev1;
    }

    /**
     * @return to revision
     */
    public String getRev2()
    {
        return this.rev2;
    }

    /**
     * @return single revision
     */
    public String getRev()
    {
        return this.rev;
    }

    /**
     * @return document language
     */
    public String getLanguage()
    {
        return this.language;
    }

    /**
     * @return is action confirmed
     */
    public boolean isConfirmed()
    {
        return this.confirm;
    }
}
