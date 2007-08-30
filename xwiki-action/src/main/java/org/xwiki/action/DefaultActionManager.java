/*
 * Copyright 2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 *
 */
package org.xwiki.action;

import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Composable;
import org.xwiki.container.Container;
import org.xwiki.url.XWikiURL;

public class DefaultActionManager implements ActionManager, Composable
{
    private ComponentManager componentManager;

    private Action errorAction;

    public void compose(ComponentManager componentManager)
    {
        this.componentManager = componentManager;
    }

    public void handleRequest(Container container) throws ActionException
    {
        // Get the action to execute from the request URL
        XWikiURL requestURL = container.getRequest().getURL();
        String actionName = requestURL.getAction();
        handleRequest(container, actionName);
    }

    /**
     * Force execution of a specific action.
     * @exception ActionException when we haven't been able to use the error action to handle
     *            the original exception
     */
    public void handleRequest(Container container, String actionName) throws ActionException
    {
        handleRequest(container, actionName, null);
    }

    public void handleRequest(Container container, String actionName, Object additionalData)
        throws ActionException
    {
        // Actions are registered with a role hint correponding to the action name
        try {
            Action action = (Action) this.componentManager.lookup(Action.ROLE, actionName);
            action.execute(container, additionalData);
        } catch (Exception e) {
            this.errorAction.execute(container, e);
        }
    }
}
