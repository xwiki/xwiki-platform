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

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Base class for all User client tests. It returns the name of the module in {@link #getModuleName()} so you don't have
 * to do it in each test.
 * 
 * @version $Id$
 */
public class UserTestCase extends GWTTestCase
{
    /**
     * The name of this GWT module.
     */
    public static final String MODULE_NAME = "org.xwiki.gwt.user.UserTest";

    @Override
    public String getModuleName()
    {
        return MODULE_NAME;
    }
}
