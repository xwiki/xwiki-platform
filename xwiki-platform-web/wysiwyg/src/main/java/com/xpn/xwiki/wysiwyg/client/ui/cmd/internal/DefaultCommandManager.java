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
package com.xpn.xwiki.wysiwyg.client.ui.cmd.internal;

import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.FocusWidget;

public class DefaultCommandManager extends AbstractCommandManager
{
    public final Element target;

    public DefaultCommandManager(FocusWidget widget)
    {
        super(widget);
        target = widget.getElement();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractCommandManager#execCommandAssumingFocus(String, String)
     */
    protected native boolean execCommandAssumingFocus(String cmd, String param) /*-{
        try{
            return this.@com.xpn.xwiki.wysiwyg.client.ui.cmd.internal.DefaultCommandManager::target.contentWindow.document.execCommand(cmd, false, param);
        } catch(e) {
            return false;
        }
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractCommandManager#queryCommandEnabledAssumingFocus(String)
     */
    protected native boolean queryCommandEnabledAssumingFocus(String cmd) /*-{
        try{
            return this.@com.xpn.xwiki.wysiwyg.client.ui.cmd.internal.DefaultCommandManager::target.contentWindow.document.queryCommandEnabled(cmd);
        } catch(e) {
            return false;
        }
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractCommandManager#queryCommandIndetermAssumingFocus(String)
     */
    protected native boolean queryCommandIndetermAssumingFocus(String cmd) /*-{
        try{
            return this.@com.xpn.xwiki.wysiwyg.client.ui.cmd.internal.DefaultCommandManager::target.contentWindow.document.queryCommandIndeterm(cmd);
        } catch(e) {
            return true;
        }
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractCommandManager#queryCommandStateAssumingFocus(String)
     */
    protected native boolean queryCommandStateAssumingFocus(String cmd) /*-{
        try{
            return this.@com.xpn.xwiki.wysiwyg.client.ui.cmd.internal.DefaultCommandManager::target.contentWindow.document.queryCommandState(cmd);
        } catch(e) {
            return false;
        }
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractCommandManager#queryCommandSupportedAssumingFocus(String)
     */
    protected native boolean queryCommandSupportedAssumingFocus(String cmd) /*-{
        try{
            return this.@com.xpn.xwiki.wysiwyg.client.ui.cmd.internal.DefaultCommandManager::target.contentWindow.document.queryCommandSupported(cmd);
        } catch(e) {
            return true;
        }
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractCommandManager#queryCommandValueAssumingFocus(String)
     */
    protected native String queryCommandValueAssumingFocus(String cmd) /*-{
        try{
            return this.@com.xpn.xwiki.wysiwyg.client.ui.cmd.internal.DefaultCommandManager::target.contentWindow.document.queryCommandValue(cmd);
        } catch(e) {
            return null;
        }
    }-*/;
}
