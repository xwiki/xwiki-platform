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
package com.xpn.xwiki.test;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.Utils;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;

/**
 * Extension of {@link com.xpn.xwiki.test.AbstractXWikiComponentTestCase} that sets up a bridge between
 * the new Execution Context and the old XWikiContext. This allows code that uses XWikiContext to be
 * tested using this Test Case class.
 *
 * @version $Id: AbstractBridgedXWikiComponentTestCase.java 11544 2008-07-29 14:43:19Z amelentev $
 * @since 1.6M1 
 */
public abstract class AbstractBridgedXWikiComponentTestCase extends AbstractXWikiComponentTestCase
{
    private XWikiContext context;

    protected void setUp() throws Exception
    {
        super.setUp();

        this.context = new XWikiContext();

        // We need to initialize the Component Manager so that the components can be looked up
        getContext().put(ComponentManager.class.getName(), getComponentManager());

        // Statically store the component manager in {@link Utils} to be able to access it without
        // the context.
        Utils.setComponentManager(getComponentManager());

        // Bridge with old XWiki Context, required for old code.
        Execution execution = (Execution) getComponentManager().lookup(Execution.ROLE);
        execution.getContext().setProperty("xwikicontext", this.context);
    }

    protected void tearDown() throws Exception
    {
        Utils.setComponentManager(null);
        super.tearDown();
    }

    public XWikiContext getContext()
    {
        return this.context;
    }
}
