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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.web.sx.AbstractSxAction;
import com.xpn.xwiki.web.sx.CssExtension;
import com.xpn.xwiki.web.sx.Extension;

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
    /** The extension type of this action. */
    public static final CssExtension CSSX = new CssExtension();

    /** Logging helper. */
    private static final Log LOG = LogFactory.getLog(SsxAction.class);

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSxAction#getExtensionType()
     */
    @Override
    public Extension getExtensionType()
    {
        return CSSX;
    }

    /**
     * @return the logging object for this class.
     */
    @Override
    protected Log getLog()
    {
        return LOG;
    }
}
