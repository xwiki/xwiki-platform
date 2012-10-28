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
package org.xwiki.security.authorization.internal;

import org.xwiki.security.authorization.AuthorizationContext;
import org.xwiki.security.authorization.EffectiveUserController;
import org.xwiki.security.authorization.ContentAuthorController;
import org.xwiki.security.authorization.PrivilegedModeController;

import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextInitializer;
import org.xwiki.context.Execution;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;


import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;

import org.xwiki.component.manager.ComponentManager;

import javax.inject.Provider;

import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Default authorization context factory test.
 *
 * @version $Id$
 * @since 4.3M1
 */
@MockingRequirement(value=DefaultAuthorizationContextFactory.class,
                    exceptions=ComponentManager.class)
public class DefaultAuthorizationContextFactoryTest extends AbstractMockingComponentTestCase
{
    private final SpaceReference spaceReference = new SpaceReference("space", new WikiReference("wiki"));

    private final DocumentReference user1 = new DocumentReference("user1", spaceReference);
    private final DocumentReference user2 = new DocumentReference("user2", spaceReference);

    private DefaultAuthorizationContextFactory authorizationContextFactory;

    private ContentAuthorResolver contentAuthorResolver;

    private Execution execution;

    @Before
    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        this.authorizationContextFactory = getComponentManager().getInstance(ExecutionContextInitializer.class,
                                                                             "defaultAuthorizationContextFactory");

        this.contentAuthorResolver = getComponentManager().getInstance(ContentAuthorResolver.class);

        execution = getComponentManager().getInstance(Execution.class);
        final ExecutionContext executionContext = new ExecutionContext();
        getMockery().checking(new Expectations() {{
            allowing(execution).setContext(executionContext);
            allowing(execution).getContext();   will(returnValue(executionContext));
            allowing(contentAuthorResolver).resolveContentAuthor(null); will(returnValue(null));
        }});

        authorizationContextFactory.initialize(executionContext);

    }

    private AuthorizationContext getAuthorizationContext() throws Exception
    {
        return (AuthorizationContext) execution.getContext()
            .getProperty(AuthorizationContext.EXECUTION_CONTEXT_KEY);
    }


    @Test
    public void authorizationContextInitialization() throws Exception
    {
        AuthorizationContext authorizationContext = getAuthorizationContext();

        Assert.assertNotNull(authorizationContext);

        Assert.assertTrue(authorizationContext.getEffectiveUser() == null);

        Assert.assertTrue(authorizationContext.getContentAuthor() == null);

        Assert.assertTrue(authorizationContext.isPrivileged());

    }

    @Test
    public void effectiveUserController() throws Exception
    {
        final AuthorizationContext authorizationContext = getAuthorizationContext();

        final EffectiveUserController euc = getComponentManager().getInstance(EffectiveUserController.class);

        Assert.assertTrue(authorizationContext.getEffectiveUser() == null);

        euc.setEffectiveUser(user1);

        Assert.assertTrue(user1.equals(authorizationContext.getEffectiveUser()));

        euc.setEffectiveUser(null);

        Assert.assertTrue(authorizationContext.getEffectiveUser() == null);

    }

    @Test
    public void contentAuthorControl() throws Exception
    {
        final AuthorizationContext authorizationContext = getAuthorizationContext();

        final ContentAuthorController cac = getComponentManager().getInstance(ContentAuthorController.class);

        final DocumentModelBridge document1 = getMockery().mock(DocumentModelBridge.class, "document1");
        final DocumentModelBridge document2 = getMockery().mock(DocumentModelBridge.class, "document2");

        getMockery().checking(new Expectations() {{
            allowing(contentAuthorResolver).resolveContentAuthor(document1); will(returnValue(user1));
            allowing(contentAuthorResolver).resolveContentAuthor(document2); will(returnValue(user2));
        }});

        Assert.assertTrue(authorizationContext.getContentAuthor() == null);

        cac.pushContentDocument(document1);

        Assert.assertTrue(user1.equals(authorizationContext.getContentAuthor()));

        cac.pushContentDocument(document2);
        
        Assert.assertTrue(user2.equals(authorizationContext.getContentAuthor()));

        DocumentModelBridge document = cac.popContentDocument();

        Assert.assertTrue(document == document2);
        Assert.assertTrue(user1.equals(authorizationContext.getContentAuthor()));

        document = cac.popContentDocument();

        Assert.assertTrue(document == document1);
        Assert.assertTrue(authorizationContext.getContentAuthor() == null);
        
    }

    @Test
    public void privilegedModeControl() throws Exception
    {
        final AuthorizationContext authorizationContext = getAuthorizationContext();

        final Provider<PrivilegedModeController> provider = getComponentManager()
            .getInstance(PrivilegedModeController.PROVIDER_TYPE);

        final PrivilegedModeController pmc1 = provider.get();

        final PrivilegedModeController pmc2 = provider.get();

        Assert.assertTrue(pmc1 != pmc2);

        Assert.assertTrue(authorizationContext.isPrivileged());

        pmc1.disablePrivilegedMode();

        Assert.assertFalse(authorizationContext.isPrivileged());

        pmc1. restorePrivilegedMode();

        Assert.assertTrue(authorizationContext.isPrivileged());

        pmc1.disablePrivilegedMode();

        Assert.assertFalse(authorizationContext.isPrivileged());

        pmc2.disablePrivilegedMode();

        Assert.assertFalse(authorizationContext.isPrivileged());

        pmc2.restorePrivilegedMode();

        Assert.assertFalse(authorizationContext.isPrivileged());

        pmc2.disablePrivilegedMode();

        Assert.assertFalse(authorizationContext.isPrivileged());

        pmc1.restorePrivilegedMode();

        Assert.assertTrue(authorizationContext.isPrivileged());
    }

    public void privilegedModeControlCurrentContext() throws Exception
    {
        final AuthorizationContext authorizationContext = getAuthorizationContext();

        final Provider<PrivilegedModeController> provider = getComponentManager()
            .getInstance(PrivilegedModeController.PROVIDER_TYPE);

        final PrivilegedModeController pmc = provider.get();

        Assert.assertTrue(authorizationContext.isPrivileged());

        pmc.disablePrivilegedModeInCurrentExecutionContext();

        Assert.assertFalse(authorizationContext.isPrivileged());

        execution.pushContext(new ExecutionContext());

        Assert.assertTrue(authorizationContext.isPrivileged());

        execution.popContext();

        Assert.assertFalse(authorizationContext.isPrivileged());
    }
}
