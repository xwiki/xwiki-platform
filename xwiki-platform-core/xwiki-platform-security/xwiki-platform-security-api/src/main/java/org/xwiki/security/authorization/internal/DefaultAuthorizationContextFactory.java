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

import javax.inject.Singleton;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import java.util.Deque;
import java.util.LinkedList;
import java.lang.reflect.Type;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentRepositoryException;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.security.authorization.AuthorizationContext;
import org.xwiki.security.authorization.EffectiveUserController;
import org.xwiki.security.authorization.ContentAuthorController;
import org.xwiki.security.authorization.ContentDocumentController;
import org.xwiki.security.authorization.PrivilegedModeController;
import org.xwiki.security.authorization.GrantProgrammingRightController;
import org.xwiki.context.ExecutionContextInitializer;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.Execution;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.model.reference.DocumentReference;

/**
 * Default implementation of authorization context factory.
 *
 * @version $Id$
 * @since 4.3M1
 */
@Component
@Named("defaultAuthorizationContextFactory")
@Singleton
public class DefaultAuthorizationContextFactory implements ExecutionContextInitializer, Initializable
{
    /** Default hint. */
    static final String DEFAULT_HINT = "default";

    /** Property name for content author resolver hint. */
    static final String CONTENT_AUTHOR_RESOLVER_PROPERTY
        = "security.authorization.contentAuthorResolverHint";

    /** Default hint for the content author resolver. */
    static final String DEFAULT_CONTENT_AUTHOR_RESOLVER = DEFAULT_HINT;

    /** Execution context key for indicating that the privileged mode is disabled. */
    private static final String PRIVILEGED_MODE_DISABLED_EXECUTION_CONTEXT_KEY = "privileged_mode_disabled";
    
    /** The execution. */
    @Inject
    private Execution execution;

    /** Used by the content athor controller to set the active content author. */
    private ContentAuthorResolver contentAuthorResolver;

    /** The component manater is only used during initialization. */
    @Inject
    private ComponentManager componentManager;

    /** Obtain configuration from the xwiki.properties file. */
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    /** For backwards compliancy with old xwiki context.  */
    @Inject
    private EffectiveUserUpdater effectiveUserUpdater;

    @Override
    public void initialize(ExecutionContext executionContext)
    {
        if (!executionContext.hasProperty(AuthorizationContext.EXECUTION_CONTEXT_KEY)) {
            executionContext.newProperty(AuthorizationContext.EXECUTION_CONTEXT_KEY)
                .makeFinal().inherited().initial(new PrivateAuthorizationContext()).declare();
        }
    }

    /**
     * Add a singleton component instance to the component manager.
     *
     * We want to keep the constructors private of the controller components to enforce going through the component
     * manager, so we instantiate the singletons here.
     * 
     * @param type The role type.
     * @param instance The instance.
     * @param <T> The role type.
     * @throws ComponentRepositoryException {@see ComponentManager#registerComponent}
     */
    private <T> void addSingleton(Type type, T instance) throws ComponentRepositoryException
    {
        DefaultComponentDescriptor<T> descriptor = new DefaultComponentDescriptor<T>();
        descriptor.setInstantiationStrategy(ComponentInstantiationStrategy.SINGLETON);
        descriptor.setRoleType(type);
        descriptor.setRoleHint(DEFAULT_HINT);
        componentManager.registerComponent(descriptor, instance);
    }

    @Override
    public void initialize() throws InitializationException
    {
        String hint = configuration.getProperty(CONTENT_AUTHOR_RESOLVER_PROPERTY, DEFAULT_CONTENT_AUTHOR_RESOLVER);

        try {
            contentAuthorResolver = componentManager.getInstance(ContentAuthorResolver.class, hint);
        } catch (ComponentLookupException e) {
            throw new InitializationException(
                String.format("Failed to obtain content author resolver for hint [{}].", hint), e);
        }

        try {
            addSingleton(EffectiveUserController.class, new PrivateEffectiveUserController());
            addSingleton(ContentAuthorController.class, new PrivateContentAuthorController());
            addSingleton(ContentDocumentController.class, new PrivateContentDocumentController());
            addSingleton(GrantProgrammingRightController.class, new PrivateGrantProgrammingRightController());
            addSingleton(PrivilegedModeController.PROVIDER_TYPE, new PrivatePrivilegedModeControllerProvider());
        } catch (ComponentRepositoryException e) {
            throw new InitializationException("Failed to register authorization context controller components.", e);
        }

        this.componentManager = null;
        this.configuration = null;
    }

    /**
     * @return The current authorization context, casted to the authorization context type of this authorization context
     * factory.
     * @throws NullPointerException if the authorization context have not been set in the execution context.
     * @throws IllegalStateException if the type of the authorization context is incompatible with this authorization
     * context factory.
     */
    private PrivateAuthorizationContext currentAuthorizationContext()
    {
        Object ctx = execution.getContext().getProperty(AuthorizationContext.EXECUTION_CONTEXT_KEY);

        if (!(ctx instanceof PrivateAuthorizationContext)) {
            if (ctx == null) {
                throw new NullPointerException("The authorization context have not been set!");
            }
            throw new IllegalStateException(
                String.format("Invalid type of authorization context for this authorization context factory: [%s]",
                              ctx.getClass().getName()));
        }

        return (PrivateAuthorizationContext) ctx;
    }

    /**
     * The autorization context implementation used by this autorization context factory.
     */
    private class PrivateAuthorizationContext implements AuthorizationContext
    {

        /** @see AuthorizationContext#getEffectiveUser() */
        private DocumentReference effectiveUser;

        /** @see AuthorizationContext#getContentAuthor() */
        private final Deque<SecurityStackEntry> securityStack = new LinkedList<SecurityStackEntry>();

        /** @see AuthorizationContext#isPrivileged(). */
        private PrivilegedModeController privilegedModeDisabled;

        @Override
        public DocumentReference getEffectiveUser()
        {
            return effectiveUser;
        }

        @Override
        public DocumentReference getContentAuthor()
        {
            if (securityStack.isEmpty()) {
                return effectiveUser;
            }
            return securityStack.peek().getContentAuthor();
        }

        @Override
        public boolean securityStackIsEmpty()
        {
            return securityStack.isEmpty();
        }

        @Override
        public boolean isPrivileged()
        {
            return privilegedModeDisabled == null
                && !execution.getContext().hasProperty(PRIVILEGED_MODE_DISABLED_EXECUTION_CONTEXT_KEY);
        }

        @Override
        public boolean grantProgrammingRight()
        {
            return !securityStack.isEmpty() && securityStack.peek().grantProgrammingRight();
        }

    }

    /**
     * The effective user controller implementation used by this authorization context factory implementation.
     */
    private final class PrivateEffectiveUserController implements EffectiveUserController
    {
        /** Make constructor private. */
        private PrivateEffectiveUserController()
        {
        }

        @Override
        public void setEffectiveUser(DocumentReference user)
        {
            PrivateAuthorizationContext ctx = currentAuthorizationContext();

            if (user != ctx.effectiveUser || (user != null && !user.equals(ctx.effectiveUser))) {
                if (user == null) {
                    ctx.effectiveUser = null;
                } else {
                    ctx.effectiveUser = new DocumentReference(user);
                }

                effectiveUserUpdater.updateUser(user);
            }
        }
    }

    /**
     * The content document controller implementation used by this authorization context factory implementation.
     */
    private final class PrivateContentDocumentController implements ContentDocumentController
    {

        /** Make constructor private. */
        private PrivateContentDocumentController()
        {
        }


        @Override
        public void pushContentDocument(DocumentModelBridge contentDocument)
        {
            PrivateAuthorizationContext ctx = currentAuthorizationContext();

            ctx.securityStack.addFirst(new DocumentSecurityStackEntry(contentDocument, contentAuthorResolver));
        }

        @Override
        public void popContentDocument()
        {
            PrivateAuthorizationContext ctx = currentAuthorizationContext();

            ctx.securityStack.removeFirst();
        }
    }

    /**
     * The content author controller implementation used by this authorization context factory implementation.
     */
    private final class PrivateContentAuthorController implements ContentAuthorController
    {

        /** Make constructor private. */
        private PrivateContentAuthorController()
        {
        }


        @Override
        public void pushContentAuthor(DocumentReference userReference)
        {
            PrivateAuthorizationContext ctx = currentAuthorizationContext();

            ctx.securityStack.addFirst(new UserSecurityStackEntry(userReference));
        }

        @Override
        public void popContentAuthor()
        {
            PrivateAuthorizationContext ctx = currentAuthorizationContext();

            ctx.securityStack.removeFirst();
        }
    }

    /**
     * The grant programming right controller implementation used by this authorization context factory implementation.
     */
    private final class PrivateGrantProgrammingRightController implements GrantProgrammingRightController
    {

        /** There is no need to allocate multiple instances of the grant all entry. */
        private final GrantProgrammingRightSecurityStackEntry grantProgrammingRightEntry
            = new GrantProgrammingRightSecurityStackEntry();

        /** Make constructor private. */
        private PrivateGrantProgrammingRightController()
        {
        }


        @Override
        public void pushGrantProgrammingRight()
        {
            PrivateAuthorizationContext ctx = currentAuthorizationContext();

            ctx.securityStack.addFirst(grantProgrammingRightEntry);
        }

        @Override
        public void popGrantProgrammingRight()
        {
            PrivateAuthorizationContext ctx = currentAuthorizationContext();

            ctx.securityStack.removeFirst();
        }
    }


    /**
     * Control the privileged mode of the authorization context.
     */
    private final class PrivatePrivilegedModeController implements PrivilegedModeController
    {

        /** Make the constructor private. */
        private PrivatePrivilegedModeController()
        {
        }

        @Override
        public void disablePrivilegedMode()
        {
            PrivateAuthorizationContext ctx = currentAuthorizationContext();

            if (ctx.privilegedModeDisabled == null) {
                ctx.privilegedModeDisabled = this;
            }
        }

        @Override
        public void restorePrivilegedMode()
        {
            PrivateAuthorizationContext ctx = currentAuthorizationContext();

            if (ctx.privilegedModeDisabled == this) {

                ctx.privilegedModeDisabled = null;
            }
        }

        @Override
        public void disablePrivilegedModeInCurrentExecutionContext()
        {
            if (!execution.getContext().hasProperty(PRIVILEGED_MODE_DISABLED_EXECUTION_CONTEXT_KEY)) {
                execution.getContext().newProperty(PRIVILEGED_MODE_DISABLED_EXECUTION_CONTEXT_KEY)
                    .makeFinal().initial((Boolean) true).declare();
            }
        }
    }

    /**
     * We want to keep the constructors private for the controller classes to enforce going through the component
     * manager to get an instance.  So, we have to use a provider for the privileged mode controller, as it is not a
     * singleton.
     */
    private final class PrivatePrivilegedModeControllerProvider implements Provider<PrivilegedModeController>
    {

        /** Hide constructor. */
        private PrivatePrivilegedModeControllerProvider()
        {
        }

        @Override
        public PrivilegedModeController get()
        {
            return new PrivatePrivilegedModeController();
        }
    }
}