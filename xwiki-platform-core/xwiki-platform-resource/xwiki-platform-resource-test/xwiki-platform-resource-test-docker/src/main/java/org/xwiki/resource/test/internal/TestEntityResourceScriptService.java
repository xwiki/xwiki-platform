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
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.container.Container;
import org.xwiki.resource.ResourceReferenceHandler;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;

/**
 * Registers {@link TestWikiEntityResourceActionHandler} in the Component Manager of the current wiki, like an Extension
 * installed on that wiki would.
 *
 * @version $Id$
 */
@Component
@Named("testentityresource")
@Singleton
public class TestEntityResourceScriptService implements ScriptService
{
    @Inject
    @Named("wiki")
    private ComponentManager wikiComponentManager;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    private Container container;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Registers the Handler of {@value TestWikiEntityResourceActionHandler#ACTION} in the current wiki.
     *
     * @throws Exception if the current author doesn't have programming right or if the registration fails
     */
    public void registerWikiHandler() throws Exception
    {
        this.authorization.checkAccess(Right.PROGRAM);

        DefaultComponentDescriptor<ResourceReferenceHandler<EntityResourceAction>> descriptor =
            new DefaultComponentDescriptor<>();
        descriptor.setRoleType(
            new DefaultParameterizedType(null, ResourceReferenceHandler.class, EntityResourceAction.class));
        descriptor.setRoleHint(TestWikiEntityResourceActionHandler.ACTION);
        descriptor.setImplementation(TestWikiEntityResourceActionHandler.class);

        this.wikiComponentManager.registerComponent(descriptor,
            new TestWikiEntityResourceActionHandler(this.container, this.xcontextProvider));
    }
}
