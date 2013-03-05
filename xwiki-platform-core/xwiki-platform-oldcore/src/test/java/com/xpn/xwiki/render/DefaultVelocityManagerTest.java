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
import java.util.Properties;

import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;

import org.apache.velocity.VelocityContext;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.velocity.VelocityConfiguration;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityFactory;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.Utils;

import junit.framework.Assert;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultVelocityManager}.
 *
 * @version $Id$
 */
public class DefaultVelocityManagerTest
{
    @Rule
    public final MockitoComponentMockingRule<VelocityManager> mocker =
        new MockitoComponentMockingRule(DefaultVelocityManager.class);

    /**
     * Tests that the Execution Context and the XWiki Context share the same reference of the Velocity Context after a
     * call to {@link VelocityManager#getVelocityContext()}. There is old code that accesses the Velocity Context from
     * the XWiki Context.
     */
    @Test
    public void getVelocityContextUpdatesXContext() throws Exception
    {
        Execution execution = this.mocker.getInstance(Execution.class);
        ExecutionContext executionContext = new ExecutionContext();
        when(execution.getContext()).thenReturn(executionContext);

        ScriptContextManager scriptContextManager = this.mocker.getInstance(ScriptContextManager.class);
        ScriptContext scriptContext = new SimpleScriptContext();
        when(scriptContextManager.getScriptContext()).thenReturn(scriptContext);

        VelocityContext velocityContext = new VelocityContext();
        executionContext.newProperty("velocityContext").initial(velocityContext).inherited().cloneValue().declare();

        Map<String, Object> xwikiContext = new HashMap<String, Object>();
        executionContext.setProperty("xwikicontext", xwikiContext);

        this.mocker.getComponentUnderTest().getVelocityContext();
        Assert.assertEquals(velocityContext, xwikiContext.get("vcontext"));
    }

    @Test
    public void getVelocityEngineWhenNoVelocityEngineInCache() throws Exception
    {
        Execution execution = this.mocker.getInstance(Execution.class);
        ExecutionContext executionContext = new ExecutionContext();
        when(execution.getContext()).thenReturn(executionContext);

        XWikiContext xwikiContext = mock(XWikiContext.class);
        executionContext.setProperty("xwikicontext", xwikiContext);
        com.xpn.xwiki.XWiki xwiki = mock(com.xpn.xwiki.XWiki.class);
        when(xwikiContext.getWiki()).thenReturn(xwiki);
        when(xwiki.getSkin(any(XWikiContext.class))).thenReturn("testskin");

        ComponentManager componentManager = mock(ComponentManager.class);
        Utils.setComponentManager(componentManager);
        VelocityFactory velocityFactory = mock(VelocityFactory.class);
        when(componentManager.getInstance(VelocityFactory.class, "default")).thenReturn(velocityFactory);

        when(velocityFactory.hasVelocityEngine("default")).thenReturn(false);

        VelocityConfiguration velocityConfiguration = mock(VelocityConfiguration.class);
        when(componentManager.getInstance(VelocityConfiguration.class, "default")).thenReturn(velocityConfiguration);
        Properties properties = new Properties();
        when(velocityConfiguration.getProperties()).thenReturn(properties);

        VelocityEngine velocityEngine = mock(VelocityEngine.class);
        when(velocityFactory.createVelocityEngine(eq("default"), any(Properties.class))).thenReturn(velocityEngine);

        Assert.assertSame(velocityEngine, this.mocker.getComponentUnderTest().getVelocityEngine());
    }
}
