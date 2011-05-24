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
package org.xwiki.gwt.wysiwyg.client.plugin.alfresco;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

/**
 * The collection of images used by the Alfresco plugin.
 * 
 * @version $Id$
 */
public interface AlfrescoImages extends ClientBundle
{
    /**
     * An instance of this client bundle that can be used anywhere in the code to extract images.
     */
    AlfrescoImages INSTANCE = GWT.create(AlfrescoImages.class);

    /**
     * @return the Alfresco icon, used on the Alfresco link and image wizards
     */
    @Source("alfresco.gif")
    ImageResource alfrescoIcon();
}
