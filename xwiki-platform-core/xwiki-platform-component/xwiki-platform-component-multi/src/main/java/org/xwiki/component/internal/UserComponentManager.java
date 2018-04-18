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
package org.xwiki.component.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.namespace.DocumentNamespace;
import org.xwiki.model.namespace.UserNamespace;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

/**
 * Proxy Component Manager that creates and queries individual Component Managers specific to the current user in the
 * Execution Context. These Component Managers are created on the fly the first time a component is registered for the
 * current user.
 * 
 * @version $Id$
 * @since 2.1RC1
 */
@Component
@Named(UserNamespace.TYPE)
@Singleton
public class UserComponentManager extends AbstractEntityComponentManager implements Initializable
{
    /**
     * The prefix of user namespace.
     * 
     * @since 8.4RC1
     */
    public static final String NAMESPACE_PREFIX = UserNamespace.TYPE + ':';

    /**
     * Used to access the current user in the Execution Context.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Used to serialize the user reference.
     */
    @Inject
    private EntityReferenceSerializer<String> referenceSerializer;

    /**
     * The Component Manager to be used as parent when a component is not found in the current Component Manager.
     */
    @Inject
    @Named(DocumentNamespace.TYPE)
    private ComponentManager documentComponentManager;

    @Override
    public void initialize() throws InitializationException
    {
        // Set the parent to the Document Component Manager since if a component isn't found for a particular user
        // we want to check if it's available for the current document and if not then in the Document Component
        // Manager's parent.
        setInternalParent(this.documentComponentManager);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Override {@link AbstractEntityComponentManager#getKey()} because the prefix is not the reference type here.
     * </p>
     * 
     * @see org.xwiki.component.internal.AbstractEntityComponentManager#getKey()
     */
    @Override
    protected String getKey()
    {
        DocumentReference userReference = getCurrentReference();

        return userReference != null ? NAMESPACE_PREFIX + this.referenceSerializer.serialize(userReference) : null;
    }

    @Override
    protected DocumentReference getCurrentReference()
    {
        return this.documentAccessBridge.getCurrentUserReference();
    }
}
