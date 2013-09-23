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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.web.sx.AbstractSxAction;
import com.xpn.xwiki.web.sx.Extension;
import com.xpn.xwiki.web.sx.JsExtension;

/**
 * <p>
 * Action for serving javascript skin extensions.
 * </p>
 * 
 * @version $Id$
 * @since 1.4M2
 */
public class JsxAction extends AbstractSxAction
{
    /** The extension type of this action. */
    public static final JsExtension JSX = new JsExtension();

    /** Logging helper. */
    private static final Logger LOGGER = LoggerFactory.getLogger(JsxAction.class);

    @Override
    public Extension getExtensionType()
    {
        return JSX;
    }

    @Override
    protected Logger getLogger()
    {
        return LOGGER;
    }
}
