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
package com.xpn.xwiki.web.sx;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yahoo.platform.yui.compressor.CssCompressor;

/**
 * StyleSheet extension.
 * 
 * @version $Id$
 * @since 1.7M2
 */
public class CssExtension implements Extension
{
    /** Logging helper. */
    private static final Logger LOGGER = LoggerFactory.getLogger(CssExtension.class);

    @Override
    public String getClassName()
    {
        return "XWiki.StyleSheetExtension";
    }

    @Override
    public String getContentType()
    {
        return "text/css; charset=UTF-8";
    }

    @Override
    public SxCompressor getCompressor()
    {
        return new SxCompressor()
        {
            @Override
            public String compress(String source)
            {
                try {
                    CssCompressor compressor = new CssCompressor(new StringReader(source));
                    StringWriter out = new StringWriter();
                    compressor.compress(out, -1);
                    return out.toString();
                } catch (IOException ex) {
                    LOGGER.warn("Exception compressing SSX code", ex);
                }
                return source;
            }
        };
    }

}
