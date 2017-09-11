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
package org.xwiki.rest.internal;

import java.lang.reflect.ParameterizedType;

import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.util.DefaultParameterizedType;

/**
 * Give access to the {@link Servlet} used as entry point of the REST service.
 * 
 * @version $Id$
 * @since 9.8RC1
 */
@Component(roles = JaxRsServletProvider.class)
@Singleton
public class JaxRsServletProvider
{
    /**
     * The Type to use to lookup this component.
     */
    public static final ParameterizedType ROLE_TYPE = new DefaultParameterizedType(null, Provider.class, Servlet.class);

    private Servlet servlet;

    void setApplication(Servlet servlet)
    {
        this.servlet = servlet;
    }

    /**
     * @return the servlet instance
     */
    public Servlet get()
    {
        return this.servlet;
    }

    /**
     * @throws ServletException when failing to restart the Servlet
     */
    public void reload() throws ServletException
    {
        this.servlet.destroy();
        this.servlet.init(this.servlet.getServletConfig());
    }
}
