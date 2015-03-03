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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;

import javax.inject.Provider;
import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;

import org.apache.velocity.VelocityContext;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.skin.Skin;
import org.xwiki.skin.SkinManager;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.velocity.VelocityConfiguration;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityFactory;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.XWikiContext;

/**
 * Unit tests for {@link DefaultVelocityManager}.
 * 
 * @version $Id$
 */
public class DefaultVelocityManagerTest
{
    @Rule
    public final MockitoComponentMockingRule<VelocityManager> mocker =
        new MockitoComponentMockingRule<VelocityManager>(DefaultVelocityManager.class);

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

        XWikiContext mockContext = mock(XWikiContext.class);
        Provider<XWikiContext> xcontextProvider = this.mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(mockContext);

        this.mocker.getComponentUnderTest().getVelocityContext();

        verify(mockContext).put("vcontext", velocityContext);
    }

    @Test
    public void getVelocityEngineWhenNoVelocityEngineInCache() throws Exception
    {
        Execution execution = this.mocker.getInstance(Execution.class);
        ExecutionContext executionContext = new ExecutionContext();
        when(execution.getContext()).thenReturn(executionContext);

        SkinManager skinManager = this.mocker.registerMockComponent(SkinManager.class);
        Skin skin = mock(Skin.class);
        when(skin.getId()).thenReturn("testskin");
        when(skinManager.getCurrentSkin(true)).thenReturn(skin);

        XWikiContext xwikiContext = mock(XWikiContext.class);
        Provider<XWikiContext> xcontextProvider = this.mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(xwikiContext);
        com.xpn.xwiki.XWiki xwiki = mock(com.xpn.xwiki.XWiki.class);
        when(xwikiContext.getWiki()).thenReturn(xwiki);

        VelocityFactory velocityFactory = this.mocker.getInstance(VelocityFactory.class);
        when(velocityFactory.hasVelocityEngine("default")).thenReturn(false);

        VelocityConfiguration velocityConfiguration = this.mocker.getInstance(VelocityConfiguration.class);
        when(velocityConfiguration.getProperties()).thenReturn(new Properties());

        VelocityEngine velocityEngine = mock(VelocityEngine.class);
        when(velocityFactory.createVelocityEngine(eq("default"), any(Properties.class))).thenReturn(velocityEngine);

        this.mocker.registerMockComponent(TemplateManager.class);

        Assert.assertSame(velocityEngine, this.mocker.getComponentUnderTest().getVelocityEngine());
    }
}
