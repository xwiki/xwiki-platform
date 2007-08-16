/*
 * Copyright 2007, XpertNet SARL, and individual contributors.
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

/**
 * Struts form for {@link DeleteVersionsAction}.
 * @version $Id: $
 */
public class DeleteVersionsForm extends XWikiForm
{
    /** from revision. */
    private String rev1;
    /** to revision. */
    private String rev2;
    /** document language. */
    private String language;
    /** is action confirmed. */
    private boolean confirm;
    /** {@inheritDoc} */
    public void readRequest()
    {
        XWikiRequest request = getRequest();
        rev1 = request.getParameter("rev1");
        rev2 = request.getParameter("rev2");
        language = request.getParameter("language");
        confirm = request.getParameter("confirm") != null;
    }
    /**
     * @return from revision
     */
    public String getRev1()
    {
        return rev1;
    }
    /**
     * @return to revision
     */
    public String getRev2()
    {
        return rev2;
    }
    /**
     * @return document language
     */
    public String getLanguage()
    {
        return language;
    }
    /**
     * @return is action confirmed
     */
    public boolean isConfirmed()
    {
        return confirm;
    }
}
