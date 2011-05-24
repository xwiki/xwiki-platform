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
import com.google.gwt.i18n.client.Constants;

/**
 * String {@link Constants} used to localize the user interface.
 * 
 * @version $Id$
 */
public interface AlfrescoConstants extends Constants
{
    /**
     * An instance of this string bundle that can be used anywhere in the code to obtain i18n strings.
     */
    AlfrescoConstants INSTANCE = GWT.create(AlfrescoConstants.class);

    /**
     * @return the label of the insert Alfresco link menu
     */
    String insertLink();

    /**
     * @return the label of the edit Alfresco link menu
     */
    String editLink();

    /**
     * @return the label of the insert Alfresco image menu
     */
    String insertImage();

    /**
     * @return the label of the edit Alfresco image menu
     */
    String editImage();

    /**
     * @return the wizard title
     */
    String wizardTitle();

    /**
     * @return the title of the link selector wizard step
     */
    String linkSelectorTitle();

    /**
     * @return the label of the current path
     */
    String pathLabel();
}
