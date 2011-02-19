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
package org.xwiki.gwt.user.client.ui.rta.cmd;

import org.xwiki.gwt.dom.client.JavaScriptObject;

/**
 * This class exposes a {@link CommandManager} to the native JavaScript code.
 * 
 * @version $Id$
 */
public final class CommandManagerApi extends JavaScriptObject
{
    /**
     * Default constructor. Overlay types always have protected, zero-arguments constructors.
     */
    protected CommandManagerApi()
    {
    }

    /**
     * We need this static native method because we can't instantiate JavaScript objects directly in Java code.
     * 
     * @param cm the command manager to be exposed in JavaScript code
     * @return a JavaScript object that exposes the given command manager
     */
    public static native CommandManagerApi newInstance(CommandManager cm)
    /*-{
        return new $wnd.CommandManager(cm);
    }-*/;

    /**
     * Publishes the JavaScript API that can be used to play with a {@link CommandManager}.
     */
    public static native void publish()
    /*-{
        $wnd.CommandManager = function(cm) {
            this.cm = cm;
        }
        $wnd.CommandManager.prototype.execute = function(commandName, parameter) {
            var command = @org.xwiki.gwt.user.client.ui.rta.cmd.Command::new(Ljava/lang/String;)('' + commandName);
            return this.cm.@org.xwiki.gwt.user.client.ui.rta.cmd.CommandManager::execute(Lorg/xwiki/gwt/user/client/ui/rta/cmd/Command;Ljava/lang/String;)(command, '' + parameter);
        }
        $wnd.CommandManager.prototype.getValue = function(commandName) {
            var command = @org.xwiki.gwt.user.client.ui.rta.cmd.Command::new(Ljava/lang/String;)('' + commandName);
            return this.cm.@org.xwiki.gwt.user.client.ui.rta.cmd.CommandManager::getStringValue(Lorg/xwiki/gwt/user/client/ui/rta/cmd/Command;)(command);
        }
        $wnd.CommandManager.prototype.isEnabled = function(commandName) {
            var command = @org.xwiki.gwt.user.client.ui.rta.cmd.Command::new(Ljava/lang/String;)('' + commandName);
            return this.cm.@org.xwiki.gwt.user.client.ui.rta.cmd.CommandManager::isEnabled(Lorg/xwiki/gwt/user/client/ui/rta/cmd/Command;)(command);
        }
        $wnd.CommandManager.prototype.isExecuted = function(commandName) {
            var command = @org.xwiki.gwt.user.client.ui.rta.cmd.Command::new(Ljava/lang/String;)('' + commandName);
            return this.cm.@org.xwiki.gwt.user.client.ui.rta.cmd.CommandManager::isExecuted(Lorg/xwiki/gwt/user/client/ui/rta/cmd/Command;)(command);
        }
        $wnd.CommandManager.prototype.isSupported = function(commandName) {
            var command = @org.xwiki.gwt.user.client.ui.rta.cmd.Command::new(Ljava/lang/String;)('' + commandName);
            return this.cm.@org.xwiki.gwt.user.client.ui.rta.cmd.CommandManager::isSupported(Lorg/xwiki/gwt/user/client/ui/rta/cmd/Command;)(command);
        }
    }-*/;
}
