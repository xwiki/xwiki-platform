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
import com.xpn.xwiki.wysiwyg.client.ui.cmd.Executable;

public abstract class AbstractExecutable implements Executable
{
    protected native boolean execute(Element target, String command, String parameter) /*-{
        try{
            return target.contentWindow.document.execCommand(command, false, parameter);
        } catch(e) {
            return false;
        }
    }-*/;

    protected native String getParameter(Element target, String command) /*-{
        try{
            return target.contentWindow.document.queryCommandValue(command);
        } catch(e) {
            return null;
        }
    }-*/;

    protected native boolean isEnabled(Element target, String command) /*-{
        try{
            return target.contentWindow.document.queryCommandEnabled(command);
        } catch(e) {
            return false;
        }
    }-*/;

    protected native boolean isExecuted(Element target, String command) /*-{
        try{
            return target.contentWindow.document.queryCommandState(command);
        } catch(e) {
            return false;
        }
    }-*/;

    protected native boolean isSupported(Element target, String command) /*-{
        try{
            return target.contentWindow.document.queryCommandSupported(command);
        } catch(e) {
            return true;
        }
    }-*/;
}
