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
package org.xwiki.rendering.listener;

import java.util.Map;

/**
 * Contains callback events for Images,called when a document has been parsed and when it needs to be modified 
 * or rendered.
 * 
 * @version $Id$
 * @since 1.8RC3
 * @see Listener
 */
public interface ImageListener
{
    /**
     * An image.
     * 
     * @param image the image definition (location, attachment name)
     * @param isFreeStandingURI if true then the image is defined directly as a URI in the text
     * @param parameters a generic list of parameters. Example: style="background-color: blue"
     */
    void onImage(Image image, boolean isFreeStandingURI, Map<String, String> parameters);
}
