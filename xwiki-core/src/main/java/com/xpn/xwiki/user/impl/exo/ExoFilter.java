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
 *
 */

package com.xpn.xwiki.user.impl.exo;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;

import javax.servlet.*;
import java.io.IOException;


public class ExoFilter implements Filter {

    public static final String EXO_CONTAINER = "portal";
    public static String portalName_ = null;

    public void init(FilterConfig filterConfig) throws ServletException {
        portalName_ = filterConfig.getInitParameter("portalName");
        if (portalName_ == null){
          portalName_ = EXO_CONTAINER;
        }
        System.out.append("init done");
        System.out.append("portal Name: " + portalName_);
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        PortalContainer pcontainer = RootContainer.getInstance().getPortalContainer(portalName_);
        System.out.append("pcontainer: " + pcontainer);

        PortalContainer.setInstance(pcontainer);
        filterChain.doFilter(servletRequest, servletResponse);
    }

    public void destroy() {
    }

}

