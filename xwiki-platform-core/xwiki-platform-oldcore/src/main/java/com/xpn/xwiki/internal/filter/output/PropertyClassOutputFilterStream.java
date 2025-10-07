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
package com.xpn.xwiki.internal.filter.output;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.internal.objects.classes.PropertyClassProvider;
import com.xpn.xwiki.internal.objects.meta.PropertyMetaClassInterface;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

/**
 * @version $Id$
 * @since 9.0RC1
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class PropertyClassOutputFilterStream extends AbstractEntityOutputFilterStream<PropertyClass>
{
    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private Logger logger;

    private BaseClass currentXClass;

    private PropertyMetaClassInterface currentClassPropertyMeta;

    /**
     * @param currentXClass the current {@link BaseClass}
     */
    public void setCurrentXClass(BaseClass currentXClass)
    {
        this.currentXClass = currentXClass;
    }

    // Events

    @Override
    public void beginWikiClassProperty(String name, String type, FilterEventParameters parameters)
        throws FilterException
    {
        if (this.enabled) {
            ComponentManager componentManager = this.componentManagerProvider.get();

            this.currentClassPropertyMeta = null;

            PropertyClassProvider provider;

            // First try to use the specified class type as hint.
            try {
                if (componentManager.hasComponent(PropertyClassProvider.class, type)) {
                    provider = componentManager.getInstance(PropertyClassProvider.class, type);
                } else {
                    // In previous versions the class type was the full Java class name of the property class
                    // implementation. Extract the hint by removing the Java package prefix and the Class suffix.
                    String classType = StringUtils.removeEnd(StringUtils.substringAfterLast(type, "."), "Class");
                    if (componentManager.hasComponent(PropertyClassProvider.class, classType)) {
                        provider = componentManager.getInstance(PropertyClassProvider.class, classType);
                    } else {
                        this.logger.warn("Unknown property type [{}]", type);

                        return;
                    }
                }
            } catch (ComponentLookupException e) {
                throw new FilterException(
                    String.format("Failed to get instance of the property class provider for type [%s]", type), e);
            }

            this.currentClassPropertyMeta = provider.getDefinition();

            if (this.entity == null) {
                // We should use PropertyClassInterface (instead of PropertyClass, its default implementation) but it
                // doesn't have the set methods and adding them would breaks the backwards compatibility. We make the
                // assumption that all property classes extend PropertyClass.
                this.entity = (PropertyClass) provider.getInstance();
            }

            this.entity.setName(name);
            this.entity.setObject(this.currentXClass);
            // The object should not be dirty there.
            this.entity.setDirty(false);
        }
    }

    @Override
    public void endWikiClassProperty(String name, String type, FilterEventParameters parameters) throws FilterException
    {
        if (this.enabled) {
            this.currentClassPropertyMeta = null;
        }
    }

    @Override
    public void onWikiClassPropertyField(String name, String value, FilterEventParameters parameters)
        throws FilterException
    {
        if (this.entity != null) {
            Object classReference = this.entity.getObject().getName();
            if (classReference == null) {
                classReference = this.entity.getObject().getDocumentReference();
            }

            PropertyClass propertyClass;
            try {
                propertyClass = (PropertyClass) this.currentClassPropertyMeta.get(name);
            } catch (XWikiException e) {
                throw new FilterException(
                    String.format("Failed to get definition of field [%s] for property type [%s] in class [%s]", name,
                        this.entity.getClassType(), classReference),
                    e);
            }

            // Make sure the property is known
            if (propertyClass == null) {
                this.logger.warn("{} - Unknown property meta class field [{}] for property type [{}] in class [{}]",
                    this.currentEntityReference, name, this.entity.getClassType(), classReference);

                return;
            }

            BaseProperty<?> field = null;
            try {
                field = propertyClass.fromString(value);
            } catch (XWikiException e) {
                throw new FilterException(
                    String.format("Failed to parse value [%s] for field [%s] in class reference [%s]",
                    value, name, classReference), e);
            }

            this.entity.safeput(name, field);
            // The object should not be dirty there.
            this.entity.setDirty(false);
        }
    }
}
