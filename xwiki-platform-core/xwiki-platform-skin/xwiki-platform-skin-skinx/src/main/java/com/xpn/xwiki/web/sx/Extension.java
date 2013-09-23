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

/**
 * Representation of a type of extension.
 * 
 * @version $Id$
 * @since 1.7M2
 */
public interface Extension
{
    /**
     * @return the full name of the wiki document holding the XWiki class defining this type of extension. For example,
     *         'XWiki.JavaScriptExtension' for javascript extensions.
     */
    String getClassName();

    /**
     * @return the contentType for that extension, as it will be written as the response content type.
     */
    String getContentType();

    /**
     * @return a compressor associated with the extension.
     */
    SxCompressor getCompressor();
}
