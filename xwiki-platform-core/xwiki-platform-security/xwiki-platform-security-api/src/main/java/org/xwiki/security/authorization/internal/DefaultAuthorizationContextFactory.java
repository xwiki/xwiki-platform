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
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.security.authorization.AuthorizationContext;
import org.xwiki.security.authorization.EffectiveUserController;
import org.xwiki.security.authorization.ContentAuthorController;
import org.xwiki.security.authorization.PrivilegedModeController;
import org.xwiki.context.ExecutionContextInitializer;
import org.xwiki.context.ExecutionContextProperty;
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

    /** Execution context key for indicating that the privileged mode is disabled. */
    private static final String PRIVILEGED_MODE_DISABLED_EXECUTION_CONTEXT_KEY = "privileged_mode_disabled";

    /** The execution. */
    @Inject
    private Execution execution;

    /** Used by the content athor controller to set the active content author. */
    @Inject
    private ContentAuthorResolver contentAuthorResolver;

    /** The component manater is only used during initialization. */
    @Inject
    private ComponentManager componentManager;

    @Override
    public void initialize(ExecutionContext executionContext)
    {
        if (!executionContext.hasProperty(AuthorizationContext.EXECUTION_CONTEXT_KEY)) {
            ExecutionContextProperty property
                = new ExecutionContextProperty(AuthorizationContext.EXECUTION_CONTEXT_KEY);
            property.setFinal(true);
            property.setInherited(true);
            property.setValue(new PrivateAuthorizationContext());
            executionContext.declareProperty(property);
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
        descriptor.setRoleHint("default");
        componentManager.registerComponent(descriptor, instance);
    }

    @Override
    public void initialize() throws InitializationException
    {
        try {
            addSingleton(EffectiveUserController.class, new PrivateEffectiveUserController());
            addSingleton(ContentAuthorController.class, new PrivateContentAuthorController());
            addSingleton(PrivilegedModeController.PROVIDER_TYPE, new PrivatePrivilegedModeControllerProvider());
        } catch (ComponentRepositoryException e) {
            throw new InitializationException("Failed to register authorization context controller components.", e);
        }
        
        this.componentManager = null;
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
        private final Deque<DocumentModelBridge> contentDocuments = new LinkedList<DocumentModelBridge>();

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
            return contentAuthorResolver.resolveContentAuthor(contentDocuments.peekFirst());
        }

        @Override
        public boolean isPrivileged()
        {
            return privilegedModeDisabled == null
                && !execution.getContext().hasProperty(PRIVILEGED_MODE_DISABLED_EXECUTION_CONTEXT_KEY);
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

            ctx.effectiveUser = user;
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
        public void pushContentDocument(DocumentModelBridge contentDocument)
        {
            PrivateAuthorizationContext ctx = currentAuthorizationContext();

            ctx.contentDocuments.addFirst(contentDocument);
        }

        @Override
        public DocumentModelBridge popContentDocument()
        {
            PrivateAuthorizationContext ctx = currentAuthorizationContext();

            return ctx.contentDocuments.removeFirst();
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
                ExecutionContextProperty p 
                    = new ExecutionContextProperty(PRIVILEGED_MODE_DISABLED_EXECUTION_CONTEXT_KEY);
                p.setValue((Boolean) true);
                p.setFinal(true);
                execution.getContext().declareProperty(p);
            }
        }
    }

    /**
     * We want to keep the constructors private for the controller classes to enforce going through the component
     * manager to get an instance.  So, we have to use a provider for the privileged mode controller, as it is not a
     * singleton.
     */
    public final class PrivatePrivilegedModeControllerProvider implements Provider<PrivilegedModeController>
    {

        @Override
        public PrivilegedModeController get()
        {
            return new PrivatePrivilegedModeController();
        }
    }
}