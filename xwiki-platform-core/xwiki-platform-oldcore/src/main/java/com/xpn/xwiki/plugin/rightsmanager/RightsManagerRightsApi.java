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

package com.xpn.xwiki.plugin.rightsmanager;

import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * API for managing rights and inheritance.
 *
 * @version $Id$
 * @since 1.1.2
 * @since 1.2M2
 */
public class RightsManagerRightsApi extends Api
{
    /**
     * Field name of the last error code inserted in context.
     */
    public static final String CONTEXT_LASTERRORCODE = RightsManagerPluginApi.CONTEXT_LASTERRORCODE;

    /**
     * Field name of the last api exception inserted in context.
     */
    public static final String CONTEXT_LASTEXCEPTION = RightsManagerPluginApi.CONTEXT_LASTEXCEPTION;

    /**
     * The logging toolkit.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(RightsManagerRightsApi.class);

    /**
     * Create an instance of RightsManageRightsApi.
     *
     * @param context the XWiki context.
     */
    public RightsManagerRightsApi(XWikiContext context)
    {
        super(context);
    }

    /**
     * Log error and register {@link #CONTEXT_LASTERRORCODE} and {@link #CONTEXT_LASTEXCEPTION}.
     *
     * @param comment the comment to use with {@link #LOGGER}.
     * @param e the exception.
     */
    private void logError(String comment, XWikiException e)
    {
        LOGGER.error(comment, e);

        this.context.put(CONTEXT_LASTERRORCODE, e.getCode());
        this.context.put(CONTEXT_LASTEXCEPTION, e);
    }

    // Inheritance

    /**
     * Get the document containing inherited rights of provided document.
     *
     * @param spaceOrPage the space of page where to get XWikiRights. If null get wiki rights.
     * @return the document containing inherited rights of provided document.
     * @throws XWikiException error when browsing rights preferences.
     */
    public Document getParentPreference(String spaceOrPage) throws XWikiException
    {
        Document parent = null;

        try {
            XWikiDocument xdoc = RightsManager.getInstance().getParentPreference(spaceOrPage, this.context);

            parent = convert(xdoc);
        } catch (RightsManagerException e) {
            logError(
                MessageFormat.format("Try to get parent rights preference for [{0}]", new Object[] { spaceOrPage }), e);
        }

        return parent;
    }
}
