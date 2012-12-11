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
package com.xpn.xwiki.render;

import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;

import junit.framework.Assert;

import org.apache.velocity.VelocityContext;
import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.test.jmock.AbstractMockingComponentTestCase;
import org.xwiki.test.jmock.annotation.MockingRequirement;
import org.xwiki.velocity.VelocityManager;

/**
 * Unit tests for {@link DefaultVelocityManager}.
 * 
 * @version $Id$
 */
@MockingRequirement(DefaultVelocityManager.class)
public class DefaultVelocityManagerTest extends AbstractMockingComponentTestCase
{
    /**
     * The component being tested.
     */
    private VelocityManager velocityManager;

    @Before
    public void configure() throws Exception
    {
        this.velocityManager = getComponentManager().getInstance(VelocityManager.class);
    }

    /**
     * Tests that the Execution Context and the XWiki Context share the same reference of the Velocity Context after a
     * call to {@link VelocityManager#getVelocityContext()}. There is old code that accesses the Velocity Context from
     * the XWiki Context.
     */
    @Test
    public void testGetVelocityContextUpdatesXContext() throws Exception
    {
        final Execution execution = getComponentManager().getInstance(Execution.class);
        final ExecutionContext executionContext = new ExecutionContext();

        final ScriptContextManager scriptContextManager = getComponentManager().getInstance(ScriptContextManager.class);
        final ScriptContext scriptContext = new SimpleScriptContext();

        final VelocityContext velocityContext = new VelocityContext();
        executionContext.newProperty("velocityContext").initial(velocityContext).inherited().cloneValue().declare();

        Map<String, Object> xwikiContext = new HashMap<String, Object>();
        executionContext.setProperty("xwikicontext", xwikiContext);

        getMockery().checking(new Expectations()
        {
            {
                allowing(execution).getContext();
                will(returnValue(executionContext));

                oneOf(scriptContextManager).getScriptContext();
                will(returnValue(scriptContext));
            }
        });

        velocityManager.getVelocityContext();
        Assert.assertEquals(velocityContext, xwikiContext.get("vcontext"));
    }
}
