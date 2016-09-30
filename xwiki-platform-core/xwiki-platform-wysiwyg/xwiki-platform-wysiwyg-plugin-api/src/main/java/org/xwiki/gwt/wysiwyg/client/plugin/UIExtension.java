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
package org.xwiki.gwt.wysiwyg.client.plugin;

import com.google.gwt.user.client.ui.UIObject;

/**
 * User interface extension.
 * 
 * @version $Id$
 */
public interface UIExtension
{
    /**
     * Examples of features are: <em>bold</em>, <em>justifyright</em> and <em>macro</em>.
     * 
     * @return An array of supported features, depending on the underlying text area's capabilities.
     */
    String[] getFeatures();

    /**
     * It can be a button, a menu item, a label or any other {@link UIObject}-derived instance.
     * 
     * @param feature One of the features provided by the editor's plug-ins and supported by the underlying text area.
     * @return The user interface object that makes the specified feature accessible in the underlying extension point.
     */
    UIObject getUIObject(String feature);

    /**
     * @return The extension point. It could be <em>menu</em>, <em>toolbar</em>, <em>statusbar</em>,
     *         <em>contextmenu</em> and so on.
     */
    String getRole();

    /**
     * Enables or disables the given feature. This way a feature can be disabled as long as it can generate invalid
     * mark-up.
     * 
     * @param feature The feature to enable or disable.
     * @param enabled <code>true</code> if the specified feature should be enabled.
     */
    void setEnabled(String feature, boolean enabled);

    /**
     * Tells if the given feature is enabled in the current context. A feature can be temporarily disabled if it can
     * generate invalid mark-up.
     * <p>
     * See org.xwiki.gwt.wysiwyg.client.syntax.SyntaxValidator.
     * 
     * @param feature The feature whose enabled state is queried.
     * @return <code>true</code> if the specified feature is enabled.
     */
    boolean isEnabled(String feature);
}
