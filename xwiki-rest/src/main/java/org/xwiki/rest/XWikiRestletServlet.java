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
package org.xwiki.rest;

import java.util.Enumeration;

import org.restlet.Application;
import org.restlet.Context;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;

import com.noelios.restlet.ext.servlet.ServerServlet;
import com.noelios.restlet.ext.servlet.ServletContextAdapter;

/**
 * @version $Id$
 */
public class XWikiRestletServlet extends ServerServlet
{
    private static final long serialVersionUID = -8292963474366330847L;

    @Override
    protected Application createApplication(Context context)
    {
        ComponentManager componentManager =
            (ComponentManager) getServletContext().getAttribute(
                org.xwiki.component.manager.ComponentManager.class.getName());

        final String applicationClass =
            getInitParameter("org.restlet.application", XWikiRestApplication.class.getName());

        XWikiRestApplication restApplication = null;

        try {
            restApplication = (XWikiRestApplication) componentManager.lookup(applicationClass);

            restApplication.setName(getServletConfig().getServletName());
            restApplication.setContext(new ServletContextAdapter(this, context));
            restApplication.getContext().setLogger(restApplication.getClass().getName());

            final Context applicationContext = restApplication.getContext();

            // Copy all the servlet parameters into the context
            String initParam;

            // Copy all the Servlet component initialization parameters
            final javax.servlet.ServletConfig servletConfig = getServletConfig();
            for (final Enumeration<String> enum1 = servletConfig.getInitParameterNames(); enum1.hasMoreElements();) {
                initParam = enum1.nextElement();
                applicationContext.getParameters().add(initParam, servletConfig.getInitParameter(initParam));
            }

            // Copy all the Servlet application initialization parameters
            for (final Enumeration<String> enum1 = getServletContext().getInitParameterNames(); enum1.hasMoreElements();) {
                initParam = enum1.nextElement();
                applicationContext.getParameters().add(initParam, getServletContext().getInitParameter(initParam));
            }

        } catch (ComponentLookupException e) {
            e.printStackTrace();
        }

        return restApplication;
    }
}
