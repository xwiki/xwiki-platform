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
package org.xwiki.resource.test.internal;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;

import com.xpn.xwiki.XWikiContext;

/**
 * Entity Resource Action Handler registered for the whole farm (this module is installed as a root extension), and
 * thus routed by the {@code RoutingFilter} to the {@code resourceReferenceHandler} Servlet.
 *
 * @version $Id$
 */
@Component
@Named(TestFarmEntityResourceActionHandler.ACTION)
@Singleton
public class TestFarmEntityResourceActionHandler extends AbstractTestEntityResourceActionHandler
{
    /**
     * The handled action.
     */
    public static final String ACTION = "testfarmaction";

    @Inject
    private Container container;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Default constructor.
     */
    public TestFarmEntityResourceActionHandler()
    {
        super(ACTION);
    }

    @Override
    protected Container getContainer()
    {
        return this.container;
    }

    @Override
    protected Provider<XWikiContext> getXWikiContextProvider()
    {
        return this.xcontextProvider;
    }
}
