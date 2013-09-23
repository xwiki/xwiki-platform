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

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Wraps a JavaScript function and calls it whenever this action handler is executed.
 * 
 * @version $Id$
 */
public class NativeActionHandler implements ActionHandler
{
    /**
     * The JavaScript function that is called whenever this action handler is executed.
     */
    private final JavaScriptObject jsHandler;

    /**
     * Creates a new action handler that wraps the given JavaScript function and calls it whenever an action is caught.
     * 
     * @param jsHandler the JavaScript to call when an action occurs
     */
    public NativeActionHandler(JavaScriptObject jsHandler)
    {
        this.jsHandler = jsHandler;
    }

    @Override
    public native void onAction(ActionEvent event)
    /*-{
        var jsHandler = this.@org.xwiki.gwt.user.client.NativeActionHandler::jsHandler;
        if (typeof jsHandler == 'function') {
            jsHandler(event.@org.xwiki.gwt.user.client.ActionEvent::getActionName()());
        }
    }-*/;
}
