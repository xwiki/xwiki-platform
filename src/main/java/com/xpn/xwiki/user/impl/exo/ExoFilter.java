/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
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
 * @author sdumitriu
 */

package com.xpn.xwiki.user.impl.exo;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;

import javax.servlet.*;
import java.io.IOException;


public class ExoFilter implements Filter {

    public static final String EXO_CONTAINER = "portal";
    public static FilterConfig filterCongif_ = null;

    public void init(FilterConfig filterConfig) throws ServletException {
        if (filterConfig != null) {
            filterCongif_ = filterConfig;
        } else {
            PortalContainer manager = RootContainer.getInstance().getPortalContainer(EXO_CONTAINER);
            filterCongif_ = (FilterConfig) manager.getComponentInstanceOfType(FilterConfig.class);
        }
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        PortalContainer pcontainer = RootContainer.getInstance().getPortalContainer(EXO_CONTAINER);
        PortalContainer.setInstance(pcontainer);
        filterChain.doFilter(servletRequest, servletResponse);
    }

    public void destroy() {
    }

}
