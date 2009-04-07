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
 *
 */
package com.xpn.xwiki.web;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.sx.AbstractSxAction;
import com.xpn.xwiki.web.sx.CssExtension;
import com.xpn.xwiki.web.sx.Extension;
import com.xpn.xwiki.web.sx.SxDocumentSource;
import com.xpn.xwiki.web.sx.SxResourceSource;
import com.xpn.xwiki.web.sx.SxSource;

/**
 * <p>
 * Action for serving css skin extensions.
 * </p>
 * 
 * @version $Id$
 * @since 1.4M2
 */
public class SsxAction extends AbstractSxAction
{
    /** Logging helper. */
    private static final Log LOG = LogFactory.getLog(SsxAction.class);

    /**
     * {@inheritDoc}
     * 
     * @see XWikiAction#render(XWikiContext)
     */
    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        SxSource sxSource;

        Extension sxType = new CssExtension();

        if (context.getRequest().getParameter("resource") != null) {
            sxSource = new SxResourceSource(context.getRequest().getParameter("resource"));
        }

        else {
            if (context.getDoc().isNew()) {
                context.getResponse().setStatus(HttpServletResponse.SC_NOT_FOUND);
                return "docdoesnotexist";
            }
            sxSource = new SxDocumentSource(context, sxType);
        }

        try {
            super.renderExtension(sxSource, sxType, context);
        } catch (IllegalArgumentException e) {
            // Simply set a 404 status code and return null, so that no unneeded bytes are transfered
            context.getResponse().setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
        return null;
    }

    @Override
    protected Log getLog()
    {
        return LOG;
    }

}
