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
package org.xwiki.gwt.wysiwyg.client.plugin.image.internal;

import org.xwiki.gwt.user.client.StringUtils;

import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Style.Unit;

/**
 * Overwrites {@link IEImageConfigDOMWriter} in order to fix some IE6 bugs.
 * 
 * @version $Id$
 */
public class IE6ImageConfigDOMWriter extends IEImageConfigDOMWriter
{
    /**
     * {@inheritDoc}
     * 
     * @see IEImageConfigDOMWriter#updateDimension(ImageElement, String, String)
     */
    @Override
    protected void updateDimension(ImageElement image, String dimension, String value)
    {
        // Use the style attribute because IE fails to update the image size when the width/height attributes are used.
        if (StringUtils.isEmpty(value)) {
            image.removeAttribute(dimension);
            image.getStyle().clearProperty(dimension);
        } else {
            String computedValue = image.getPropertyString(dimension);
            if (!value.equals(computedValue) && !value.equals(computedValue + Unit.PX.getType())) {
                image.removeAttribute(dimension);
                image.getStyle().setProperty(dimension, value);
            }
        }
    }
}
