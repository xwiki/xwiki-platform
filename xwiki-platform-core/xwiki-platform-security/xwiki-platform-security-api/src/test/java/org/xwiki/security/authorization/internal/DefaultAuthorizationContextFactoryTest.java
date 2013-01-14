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
import org.xwiki.security.authorization.ContentDocumentController;
import org.xwiki.security.authorization.GrantProgrammingRightController;
import org.xwiki.security.authorization.PrivilegedModeController;

import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextInitializer;
import org.xwiki.context.Execution;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;


import org.xwiki.test.jmock.AbstractMockingComponentTestCase;
import org.xwiki.test.jmock.annotation.MockingRequirement;

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

    private EffectiveUserUpdater effectiveUserUpdater;

    private Execution execution;

    @Before
    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        final ConfigurationSource configurationSource
            = getComponentManager().getInstance(ConfigurationSource.class, "xwikiproperties");

        registerMockComponent(ContentAuthorResolver.class,
            DefaultAuthorizationContextFactory.DEFAULT_CONTENT_AUTHOR_RESOLVER);

        getMockery().checking(new Expectations() {{
            allowing(configurationSource)
                 .getProperty(DefaultAuthorizationContextFactory.CONTENT_AUTHOR_RESOLVER_PROPERTY,
                              DefaultAuthorizationContextFactory.DEFAULT_CONTENT_AUTHOR_RESOLVER);
            will(returnValue(DefaultAuthorizationContextFactory.DEFAULT_CONTENT_AUTHOR_RESOLVER));
        }});

        this.authorizationContextFactory = getComponentManager().getInstance(ExecutionContextInitializer.class,
                                                                             "defaultAuthorizationContextFactory");

        this.contentAuthorResolver = getComponentManager().getInstance(ContentAuthorResolver.class);

        this.effectiveUserUpdater = getComponentManager().getInstance(EffectiveUserUpdater.class);

        execution = getComponentManager().getInstance(Execution.class);
        final ExecutionContext executionContext = new ExecutionContext();
        getMockery().checking(new Expectations() {{
            allowing(execution).setContext(executionContext);
            allowing(execution).getContext();   will(returnValue(executionContext));
            allowing(contentAuthorResolver).resolveContentAuthor(null); will(returnValue(null));
            allowing(effectiveUserUpdater).updateUser(with(any(DocumentReference.class)));
            allowing(effectiveUserUpdater).updateUser(null);
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

        Assert.assertTrue(authorizationContext.securityStackIsEmpty());

        Assert.assertFalse(authorizationContext.grantProgrammingRight());

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
    public void contextDocumentControl() throws Exception
    {
        final AuthorizationContext authorizationContext = getAuthorizationContext();

        final ContentDocumentController cdc = getComponentManager().getInstance(ContentDocumentController.class);

        final DocumentModelBridge document1 = getMockery().mock(DocumentModelBridge.class, "document1");
        final DocumentModelBridge document2 = getMockery().mock(DocumentModelBridge.class, "document2");

        getMockery().checking(new Expectations() {{
            allowing(contentAuthorResolver).resolveContentAuthor(document1); will(returnValue(user1));
            allowing(contentAuthorResolver).resolveContentAuthor(document2); will(returnValue(user2));
        }});

        Assert.assertTrue(authorizationContext.getContentAuthor() == null);

        cdc.pushContentDocument(document1);

        Assert.assertTrue(user1.equals(authorizationContext.getContentAuthor()));
        Assert.assertFalse(authorizationContext.securityStackIsEmpty());

        cdc.pushContentDocument(document2);

        Assert.assertTrue(user2.equals(authorizationContext.getContentAuthor()));

        cdc.popContentDocument();

        Assert.assertTrue(user1.equals(authorizationContext.getContentAuthor()));

        cdc.popContentDocument();

        Assert.assertTrue(authorizationContext.getContentAuthor() == null);
        Assert.assertTrue(authorizationContext.securityStackIsEmpty());

    }

    @Test
    public void contentAuthorControl() throws Exception
    {
        final AuthorizationContext authorizationContext = getAuthorizationContext();

        final ContentAuthorController cac = getComponentManager().getInstance(ContentAuthorController.class);

        Assert.assertTrue(authorizationContext.getContentAuthor() == null);
        Assert.assertTrue(authorizationContext.securityStackIsEmpty());

        cac.pushContentAuthor(user1);

        Assert.assertTrue(user1.equals(authorizationContext.getContentAuthor()));
        Assert.assertFalse(authorizationContext.securityStackIsEmpty());

        cac.pushContentAuthor(user2);

        Assert.assertTrue(user2.equals(authorizationContext.getContentAuthor()));

        cac.popContentAuthor();

        Assert.assertTrue(user1.equals(authorizationContext.getContentAuthor()));

        cac.popContentAuthor();

        Assert.assertTrue(authorizationContext.getContentAuthor() == null);
        Assert.assertTrue(authorizationContext.securityStackIsEmpty());
    }

    @Test
    public void grantProgrammingRightControl() throws Exception
    {
        final AuthorizationContext authorizationContext = getAuthorizationContext();

        final GrantProgrammingRightController gac = getComponentManager().getInstance(GrantProgrammingRightController.class);
        final ContentAuthorController cac = getComponentManager().getInstance(ContentAuthorController.class);
        final ContentDocumentController cdc = getComponentManager().getInstance(ContentDocumentController.class);

        final DocumentModelBridge document2 = getMockery().mock(DocumentModelBridge.class, "document2");

        Assert.assertFalse(authorizationContext.grantProgrammingRight());
        Assert.assertTrue(authorizationContext.securityStackIsEmpty());

        cac.pushContentAuthor(user1);

        Assert.assertFalse(authorizationContext.grantProgrammingRight());
        Assert.assertFalse(authorizationContext.securityStackIsEmpty());

        gac.pushGrantProgrammingRight();

        Assert.assertTrue(authorizationContext.grantProgrammingRight());
        Assert.assertFalse(authorizationContext.securityStackIsEmpty());

        cdc.pushContentDocument(document2);

        Assert.assertFalse(authorizationContext.grantProgrammingRight());
        Assert.assertFalse(authorizationContext.securityStackIsEmpty());

        cdc.popContentDocument();

        Assert.assertTrue(authorizationContext.grantProgrammingRight());
        Assert.assertFalse(authorizationContext.securityStackIsEmpty());

        gac.popGrantProgrammingRight();

        Assert.assertFalse(authorizationContext.grantProgrammingRight());
        Assert.assertEquals(user1, authorizationContext.getContentAuthor());
        Assert.assertFalse(authorizationContext.securityStackIsEmpty());

        cac.popContentAuthor();

        Assert.assertTrue(authorizationContext.securityStackIsEmpty());
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
