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
package org.xwiki.gwt.user.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ImageBundle;

/**
 * This {@link ImageBundle} contains all the images used in the User module. Using an image bundle allows all of these
 * images to be packed into a single image, which saves a lot of HTTP requests, drastically improving startup time.
 * 
 * @version $Id$
 */
public interface Images extends ImageBundle
{
    /**
     * An instance of this image bundle that can be used anywhere in the code to extract images.
     */
    Images INSTANCE = (Images) GWT.create(Images.class);

    /**
     * The icon representing the action of closing a dialog box.
     * 
     * @return a prototype of this image
     */
    @Resource("close.gif")
    AbstractImagePrototype close();
}
