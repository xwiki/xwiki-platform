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
package org.xwiki.extension.internal.validator;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.EnumUtils;
import org.xwiki.component.namespace.Namespace;
import org.xwiki.component.namespace.NamespaceUtils;
import org.xwiki.extension.Extension;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.handler.ExtensionValidator;
import org.xwiki.job.Request;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Base class helper to implement {@link ExtensionValidator}.
 * 
 * @version $Id$
 * @since 9.0RC1
 */
public abstract class AbstractExtensionValidator implements ExtensionValidator
{
    /**
     * The name of the property containing the reference of the current user.
     */
    public static final String PROPERTY_USERREFERENCE = "user.reference";

    /**
     * The name of the property containing the reference of the current author.
     */
    public static final String PROPERTY_CALLERREFERENCE = "caller.reference";

    /**
     * The name of the property indicating of rights should be checked.
     */
    public static final String PROPERTY_CHECKRIGHTS = "checkrights";

    /**
     * The name of the property indicating of rights of the user should be checked.
     * 
     * @since 10.11.10
     * @since 11.3.5
     * @since 11.8RC1
     */
    public static final String PROPERTY_CHECKRIGHTS_USER = "checkUserRights";

    /**
     * The name of the property indicating of rights of the author should be checked.
     * 
     * @since 10.11.10
     * @since 11.3.5
     * @since 11.8RC1
     */
    public static final String PROPERTY_CHECKRIGHTS_CALLER = "checkAuthorRights";

    @Inject
    protected AuthorizationManager authorization;

    @Inject
    @Named("relative")
    protected EntityReferenceResolver<String> resolver;

    protected Right entityRight = Right.PROGRAM;

    /**
     * @param property the property containing a user reference
     * @param request the request of the job currently being executed
     * @return the reference of the user found in the request
     */
    public static DocumentReference getRequestUserReference(String property, Request request)
    {
        Object obj = request.getProperty(property);

        if (obj instanceof DocumentReference) {
            return (DocumentReference) obj;
        }

        return null;
    }

    protected void checkAccess(EntityReference entityReference, Right right, Request request)
        throws AccessDeniedException
    {
        // Context author
        checkAccess(PROPERTY_CHECKRIGHTS_CALLER, PROPERTY_CALLERREFERENCE, entityReference, right, request);

        // Context user
        checkAccess(PROPERTY_CHECKRIGHTS_USER, PROPERTY_USERREFERENCE, entityReference, right, request);
    }

    private void checkAccess(String propertySetReference, String propertyReference, EntityReference entityReference,
        Right right, Request request) throws AccessDeniedException
    {
        DocumentReference userReference = getRequestUserReference(propertyReference, request);

        if (userReference != null || request.getProperty(propertySetReference) == Boolean.TRUE) {
            this.authorization.checkAccess(right, userReference, entityReference);
        }
    }

    @Override
    public void checkInstall(Extension extension, String namespace, Request request) throws InstallException
    {
        if (request.getProperty(PROPERTY_CHECKRIGHTS) == Boolean.TRUE) {
            checkInstallInternal(extension, namespace, request);
        }
    }

    protected void checkAccess(Extension extension, String namespaceString, Request request)
        throws AccessDeniedException
    {
        checkAccess(this.entityRight, namespaceString, request);
    }

    protected void checkAccess(Right entityRight, String namespaceString, Request request) throws AccessDeniedException
    {
        Namespace namespace = NamespaceUtils.toNamespace(namespaceString);

        // Root namespace
        if (namespace == null) {
            checkRootRight(entityRight, request);

            return;
        }

        if (namespace.getType() != null) {
            // User
            if (namespace.getType().equals("user")) {
                EntityReference reference = this.resolver.resolve(namespace.getValue(), EntityType.DOCUMENT);

                checkUserRight(reference, request);

                return;
            }

            // Entity
            EntityType entityType = EnumUtils.getEnum(EntityType.class, namespace.getType().toUpperCase());
            if (entityType != null) {
                EntityReference reference = this.resolver.resolve(namespace.getValue(), entityType);

                checkAccess(reference, entityRight, request);

                return;
            }
        }

        // Unknown namespace
        checkNamespaceRight(namespace, Right.PROGRAM, request);
    }

    protected void checkInstallInternal(Extension extension, String namespace, Request request) throws InstallException
    {
        try {
            checkAccess(extension, namespace, request);
        } catch (AccessDeniedException e) {
            throw new InstallException(String.format("Install of extension [%s] is not allowed", extension.getId()), e);
        }
    }

    private void checkRootRight(Right entityRight, Request request) throws AccessDeniedException
    {
        // Need programming right for root namespace
        checkAccess(null, entityRight, request);
    }

    private void checkUserRight(EntityReference entityReference, Request request) throws AccessDeniedException
    {
        // Context author
        checkUserAccess(PROPERTY_CHECKRIGHTS_CALLER, PROPERTY_CALLERREFERENCE, entityReference, request);

        // Context user
        checkUserAccess(PROPERTY_CHECKRIGHTS_USER, PROPERTY_USERREFERENCE, entityReference, request);
    }

    private void checkUserAccess(String propertySetReference, String propertyReference, EntityReference entityReference,
        Request request) throws AccessDeniedException
    {
        DocumentReference userReference = getRequestUserReference(propertyReference, request);

        if (userReference != null || request.getProperty(propertySetReference) == Boolean.TRUE) {
            // Allow a user to install an extension in its own namespace
            DocumentReference currentAuthorReference = getRequestUserReference(propertyReference, request);

            if (currentAuthorReference == null || !currentAuthorReference.equals(entityReference)) {
                // Need programming right to register an extension in another user namespace
                this.authorization.checkAccess(Right.PROGRAM, userReference, null);
            }
        }
    }

    private void checkNamespaceRight(Namespace namespace, Right program, Request request) throws AccessDeniedException
    {
        // Check programming right by default
        checkAccess(null, Right.PROGRAM, request);
    }

    @Override
    public void checkUninstall(InstalledExtension extension, String namespace, Request request)
        throws UninstallException
    {
        if (request.getProperty(PROPERTY_CHECKRIGHTS) == Boolean.TRUE) {
            checkUninstallInternal(extension, namespace, request);
        }
    }

    protected void checkUninstallInternal(InstalledExtension extension, String namespace, Request request)
        throws UninstallException
    {
        try {
            checkAccess(extension, namespace, request);
        } catch (AccessDeniedException e) {
            throw new UninstallException(String.format("Uninstall of extension [%s] is not allowed", extension.getId()),
                e);
        }
    }
}
