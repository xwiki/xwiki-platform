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
package org.xwiki.action.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.action.Action;
import org.xwiki.action.ActionException;
import org.xwiki.action.ActionManager;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.url.XWikiEntityURL;
import org.xwiki.url.XWikiURL;
import org.xwiki.url.XWikiURLType;

@Component
@Singleton
public class DefaultActionManager implements ActionManager
{
    @Inject
    private ComponentManager componentManager;

    @Inject
    private Container container;

    @Inject
    @Named("error")
    private Action errorAction;

    public void handleRequest() throws ActionException
    {
        // Get the action to execute from the request URL
        XWikiURL requestURL = (XWikiURL) this.container.getRequest().getProperty(Request.XWIKI_URL);
        
        // TODO: We need to handle non document actions too.
        if (requestURL != null && requestURL.getType() == XWikiURLType.ENTITY) {
            String actionName = ((XWikiEntityURL) requestURL).getAction();
            handleRequest(actionName);
        }
    }

    /**
     * Force execution of a specific action.
     * @exception ActionException when we haven't been able to use the error action to handle
     *            the original exception
     */
    public void handleRequest(String actionName) throws ActionException
    {
        handleRequest(actionName, null);
    }

    public void handleRequest(String actionName, Object additionalData)
        throws ActionException
    {
        // Actions are registered with a role hint corresponding to the action name
        try {
            Action action = this.componentManager.getInstance(Action.class, actionName);
            action.execute(additionalData);
        } catch (Exception e) {
            this.errorAction.execute(e);
        }
    }
}
