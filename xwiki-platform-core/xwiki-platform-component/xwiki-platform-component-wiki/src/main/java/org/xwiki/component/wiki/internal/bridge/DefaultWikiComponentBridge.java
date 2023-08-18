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
package org.xwiki.component.wiki.internal.bridge;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.WikiComponentScope;
import org.xwiki.component.wiki.internal.WikiComponentConstants;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * A bridge between Wiki Components and the old model.
 *
 * @version $Id$
 * @since 4.3M2
 */
@Component
@Singleton
public class DefaultWikiComponentBridge implements WikiComponentConstants, WikiComponentBridge
{
    /**
     * The logger to log.
     */
    @Inject
    protected Logger logger;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * The rendering bridge.
     */
    @Inject
    private ContentParser renderingBridge;

    @Inject
    private AuthorizationManager authorization;

    @Override
    public Syntax getSyntax(DocumentReference reference) throws WikiComponentException
    {
        XWikiDocument componentDocument = this.getDocument(reference);
        return componentDocument.getSyntax();
    }

    @Override
    public Type getRoleType(DocumentReference reference) throws WikiComponentException
    {
        BaseObject componentObject = getComponentObject(reference);
        String role = componentObject.getStringValue(COMPONENT_ROLE_TYPE_FIELD);
        Type roleType;

        try {
            roleType = ReflectionUtils.unserializeType(role, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new WikiComponentException(String.format("The role type [%s] does not exist", role), e);
        }

        return roleType;
    }

    @Override
    public String getRoleHint(DocumentReference reference) throws WikiComponentException
    {
        BaseObject componentObject = getComponentObject(reference);
        return StringUtils.defaultIfEmpty(componentObject.getStringValue(COMPONENT_ROLE_HINT_FIELD), "default");
    }

    @Override
    public DocumentReference getAuthorReference(DocumentReference reference) throws WikiComponentException
    {
        XWikiDocument componentDocument = this.getDocument(reference);
        return componentDocument.getAuthorReference();
    }

    @Override
    public WikiComponentScope getScope(DocumentReference reference) throws WikiComponentException
    {
        BaseObject componentObject = getComponentObject(reference);
        return WikiComponentScope.fromString(componentObject.getStringValue(COMPONENT_SCOPE_FIELD));
    }

    @Override
    public int getRoleTypePriority(DocumentReference reference) throws WikiComponentException
    {
        BaseObject componentObject = getComponentObject(reference);
        return componentObject.getIntValue(COMPONENT_ROLE_TYPE_PRIORITY_FIELD, ComponentDescriptor.DEFAULT_PRIORITY);
    }

    @Override
    public int getRoleHintPriority(DocumentReference reference) throws WikiComponentException
    {
        BaseObject componentObject = getComponentObject(reference);
        return componentObject.getIntValue(COMPONENT_ROLE_HINT_PRIORITY_FIELD, ComponentDescriptor.DEFAULT_PRIORITY);
    }

    @Override
    public Map<String, XDOM> getHandledMethods(DocumentReference reference) throws WikiComponentException
    {
        Map<String, XDOM> handledMethods = new HashMap<String, XDOM>();
        XWikiDocument componentDocument = this.getDocument(reference);
        if (componentDocument.getObjectNumbers(METHOD_CLASS) > 0) {
            for (BaseObject method : componentDocument.getObjects(METHOD_CLASS)) {
                if (!StringUtils.isBlank(method.getStringValue(METHOD_NAME_FIELD))) {
                    handledMethods.put(method.getStringValue(METHOD_NAME_FIELD), renderingBridge.parse(
                        method.getStringValue(METHOD_CODE_FIELD), this.getSyntax(reference), reference));
                }
            }
        }
        return handledMethods;
    }

    @Override
    public List<Class< ? >> getDeclaredInterfaces(DocumentReference reference) throws WikiComponentException
    {
        List<Class< ? >> interfaces = new ArrayList<Class< ? >>();
        XWikiDocument componentDocument = this.getDocument(reference);
        if (componentDocument.getObjectNumbers(INTERFACE_CLASS) > 0) {
            for (BaseObject iface : componentDocument.getObjects(INTERFACE_CLASS)) {
                if (!StringUtils.isBlank(iface.getStringValue(INTERFACE_NAME_FIELD))) {
                    try {
                        Class< ? > implemented = Class.forName(iface.getStringValue(INTERFACE_NAME_FIELD));
                        interfaces.add(implemented);
                    } catch (Exception e) {
                        this.logger.warn("Interface [{}] not found, declared for wiki component [{}]",
                            iface.getStringValue(INTERFACE_NAME_FIELD), componentDocument.getDocumentReference());
                    }
                }
            }
        }
        return interfaces;
    }

    @Override
    public Map<String, ComponentDescriptor> getDependencies(DocumentReference reference) throws WikiComponentException
    {
        Map<String, ComponentDescriptor> dependencies = new HashMap<String, ComponentDescriptor>();
        XWikiDocument componentDocument = this.getDocument(reference);
        if (componentDocument.getObjectNumbers(DEPENDENCY_CLASS) > 0) {
            for (BaseObject dependency : componentDocument.getObjects(DEPENDENCY_CLASS)) {
                try {
                    DefaultComponentDescriptor cd = new DefaultComponentDescriptor();
                    cd.setRoleType(ReflectionUtils.unserializeType(
                        dependency.getStringValue(COMPONENT_ROLE_TYPE_FIELD), Thread.currentThread()
                            .getContextClassLoader()));
                    cd.setRoleHint(dependency.getStringValue(COMPONENT_ROLE_HINT_FIELD));
                    dependencies.put(dependency.getStringValue(DEPENDENCY_BINDING_NAME_FIELD), cd);
                } catch (Exception e) {
                    this.logger.warn("Interface [{}] not found, declared as dependency for wiki component [{}]",
                        dependency.getStringValue(COMPONENT_ROLE_TYPE_FIELD), componentDocument.getDocumentReference());
                }
            }
        }

        return dependencies;
    }

    @Override
    public boolean hasProgrammingRights(DocumentReference reference) throws WikiComponentException
    {
        XWikiDocument document = getDocument(reference);

        return this.authorization.hasAccess(Right.PROGRAM, document.getAuthorReference(), null);
    }

    /**
     * Get the main component object from a wiki component document.
     *
     * @param reference a reference to the document holding the component
     * @return the object defining the component
     * @throws WikiComponentException if the document can't be retrieved
     */
    private BaseObject getComponentObject(DocumentReference reference) throws WikiComponentException
    {
        XWikiDocument componentDocument = this.getDocument(reference);
        BaseObject componentObject = componentDocument.getObject(COMPONENT_CLASS);

        if (componentObject == null) {
            throw new WikiComponentException(String.format("No component object could be found in document [%s]",
                reference));
        }

        return componentObject;
    }

    /**
     * Get the XWikiDocument corresponding to the given Document Reference.
     *
     * @param reference the document reference
     * @return the found XWikiDocument
     * @throws WikiComponentException if an error occurs
     */
    private XWikiDocument getDocument(DocumentReference reference) throws WikiComponentException
    {
        try {
            XWikiContext xcontext = this.xcontextProvider.get();
            return xcontext.getWiki().getDocument(reference, xcontext);
        } catch (XWikiException e) {
            throw new WikiComponentException(String.format("Failed to retrieve the document [%s]", reference), e);
        }
    }
}
