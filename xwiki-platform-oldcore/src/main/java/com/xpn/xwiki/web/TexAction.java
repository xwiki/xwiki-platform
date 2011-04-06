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

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.formula.ImageData;
import org.xwiki.formula.ImageStorage;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.util.Util;

/**
 * Returns rendered mathematical formulae to the client. The formulae are images rendered by the
 * {@link org.xwiki.formula.FormulaRenderer} component, and stored inside an {@link ImageStorage}.
 * 
 * @version $Id$
 * @since 2.0M3
 */
public class TexAction extends XWikiAction
{
    /** Logging helper object */
    private static final Log LOG = LogFactory.getLog(TexAction.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        String path = request.getRequestURI();
        // Expected /xwiki/bin/tex/Current/Document/image_identifier
        String filename = Util.decodeURI(path.substring(path.lastIndexOf("/") + 1), context);
        ImageStorage storage = Utils.getComponent(ImageStorage.class);
        ImageData image = storage.get(filename);
        if (image == null) {
            return "docdoesnotexist";
        }
        response.setContentLength(image.getData().length);
        response.setContentType(image.getMimeType());
        try {
            response.getOutputStream().write(image.getData());
        } catch (IOException e) {
            LOG.info("Failed to send image to the client");
        }

        return null;
    }
}
