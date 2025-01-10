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
package org.xwiki.container.servlet;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Make threads names created by the application server more meaningful. TODO When it will be possible it would be
 * better to do this a component like a RequestInitializer component to work for any kind of container. Right now
 * component can't really access the initial URL.
 * <p>
 * While the class is much older, the since annotation was moved to 42.0.0 because it implement a completely different
 * API from Java point of view.
 * 
 * @version $Id$
 * @since 42.0.0
 */
public class SetThreadNameServletRequestListener implements ServletRequestListener
{
    /**
     * The name of the servlet request attribute holding the original name of the processing thread.
     */
    private static final String ORIGINAL_THREAD_NAME_ATTRIBUTE = "xwiki.thread.originalName";

    @Override
    public void requestInitialized(ServletRequestEvent sre)
    {
        ServletRequest servletRequest = sre.getServletRequest();

        if (servletRequest instanceof HttpServletRequest httpServletRequest) {
            String threadName = httpServletRequest.getRequestURL().toString();

            if (httpServletRequest.getQueryString() != null) {
                threadName += "?" + httpServletRequest.getQueryString();
            }

            String originalThreadName = Thread.currentThread().getName();
            // Note: we keep the original thread name since that name is usually unique in the thread pool which helps
            // in debugging multithreading issues (otherwise we cannot differentiate two logs for the same URL).
            threadName = String.format("%s - %s", originalThreadName, threadName);

            httpServletRequest.setAttribute(ORIGINAL_THREAD_NAME_ATTRIBUTE, originalThreadName);
            Thread.currentThread().setName(threadName);
        }
    }

    @Override
    public void requestDestroyed(ServletRequestEvent sre)
    {
        ServletRequest servletRequest = sre.getServletRequest();
        if (servletRequest instanceof HttpServletRequest httpServletRequest) {
            Thread.currentThread().setName("" + httpServletRequest.getAttribute(ORIGINAL_THREAD_NAME_ATTRIBUTE));
        }
    }
}
