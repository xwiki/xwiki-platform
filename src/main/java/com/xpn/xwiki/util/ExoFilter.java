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

package com.xpn.xwiki.util;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;
import org.exoplatform.services.database.HibernateService;

public class ExoFilter implements Filter {

    public static final String WSRP_CONTAINER = "portal";
    private HibernateService hservice_;
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException
    {
        PortalContainer pcontainer = RootContainer.getInstance().getPortalContainer(WSRP_CONTAINER);
        PortalContainer.setInstance(pcontainer);
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        }
        finally {
            getHibernateService(pcontainer).closeSession() ;
        }
    }

    public void destroy() {
    }

    private HibernateService getHibernateService(PortalContainer pcontainer){
      if(hservice_ == null){
        hservice_ = (HibernateService) pcontainer.getComponentInstanceOfType(HibernateService.class) ;
      }
      return hservice_;
    }
  }
