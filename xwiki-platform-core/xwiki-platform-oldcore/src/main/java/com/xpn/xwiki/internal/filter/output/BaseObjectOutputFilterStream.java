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
import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.event.model.WikiObjectFilter;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

/**
 * @version $Id$
 * @since 9.0RC1
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class BaseObjectOutputFilterStream extends AbstractEntityOutputFilterStream<BaseObject> implements Initializable
{
    @Inject
    private EntityOutputFilterStream<BaseClass> classFilter;

    @Inject
    private EntityOutputFilterStream<BaseProperty> propertyFilter;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    private BaseObject externalEntity;

    private BaseClass databaseXClass;

    @Override
    public void initialize() throws InitializationException
    {
        initialize(this.classFilter, this.propertyFilter);
    }

    @Override
    public void setEntity(BaseObject entity)
    {
        super.setEntity(entity);

        this.externalEntity = entity;
    }

    private BaseClassOutputFilterStream getBaseClassOutputFilterStream()
    {
        return (BaseClassOutputFilterStream) this.classFilter;
    }

    private BasePropertyOutputFilterStream getBasePropertyOutputFilterStream()
    {
        return (BasePropertyOutputFilterStream) this.propertyFilter;
    }

    private void setCurrentXClass(BaseClass xclass)
    {
        getBasePropertyOutputFilterStream().setCurrentXClass(xclass);
    }

    private BaseClass getCurrentXClass()
    {
        return getBasePropertyOutputFilterStream().getCurrentXClass();
    }

    // Events

    @Override
    public void endWikiClass(FilterEventParameters parameters) throws FilterException
    {
        if (this.entity != null) {
            BaseClass xclass = getBaseClassOutputFilterStream().getEntity();

            if (xclass != null) {
                // Re-create the object instance if not already provided and if there is a custom class
                if (this.externalEntity == null && StringUtils.isNotEmpty(xclass.getCustomClass())) {
                    BaseObject customObject;
                    try {
                        customObject = xclass.newCustomClassInstance(true);
                        customObject.setDocumentReference(this.entity.getDocumentReference());
                        customObject.setXClassReference(this.entity.getXClassReference());
                        customObject.setOwnerDocument(this.entity.getOwnerDocument());
                        customObject.setGuid(this.entity.getGuid());

                        // Pass false as an optimization since there is nothing to clean on a new object
                        customObject.apply(this.entity, false);

                        this.entity = customObject;
                    } catch (XWikiException e) {
                        // TODO: should probably log a warning
                    }
                }

                setCurrentXClass(xclass);
                getBaseClassOutputFilterStream().setEntity(null);
            }
        }
    }

    @Override
    public void beginWikiObject(String name, FilterEventParameters parameters) throws FilterException
    {
        super.beginWikiObject(name, parameters);

        if (this.enabled) {
            if (this.entity == null) {
                this.entity = new BaseObject();
            }

            if (parameters.containsKey(WikiObjectFilter.PARAMETER_NAME)) {
                this.entity
                    .setDocumentReference(getDocumentReference(WikiObjectFilter.PARAMETER_NAME, parameters, null));
            }

            int number = getInt(WikiObjectFilter.PARAMETER_NUMBER, parameters, -1);

            EntityReference classReference =
                getEntityReference(WikiObjectFilter.PARAMETER_CLASS_REFERENCE, parameters, null);
            if (classReference == null) {
                BaseObjectReference reference = new BaseObjectReference(this.currentEntityReference);

                classReference = reference.getXClassReference();

                if (number < 0 && reference.getObjectNumber() != null) {
                    number = reference.getObjectNumber();
                }
            }
            this.entity.setXClassReference(classReference);

            // Set database class as current class if any exist
            checkDatabaseClass();

            this.entity.setNumber(number);

            this.entity.setGuid(getString(WikiObjectFilter.PARAMETER_GUID, parameters, null));

            getBaseClassOutputFilterStream().enable();
            getBasePropertyOutputFilterStream().enable();
        }
    }

    private void checkDatabaseClass()
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        if (xcontext != null && xcontext.getWiki() != null) {
            try {
                this.databaseXClass = xcontext.getWiki().getXClass(this.entity.getXClassReference(), xcontext);
                if (!this.databaseXClass.getOwnerDocument().isNew()) {
                    setCurrentXClass(this.databaseXClass);
                } else {
                    this.databaseXClass = null;
                }
            } catch (XWikiException e) {
                // TODO: log something ?
            }
        }
    }

    @Override
    public void endWikiObject(String name, FilterEventParameters parameters) throws FilterException
    {
        // Add missing properties from the class
        if (this.entity != null) {
            BaseClass xclass = getCurrentXClass();
            if (xclass != null) {
                addMissingProperties(xclass);
            }
            if (this.databaseXClass != null && this.databaseXClass != xclass) {
                addMissingProperties(this.databaseXClass);
            }
        }

        super.endWikiObject(name, parameters);

        getBaseClassOutputFilterStream().disable();
        getBaseClassOutputFilterStream().disable();
    }

    private void addMissingProperties(BaseClass xclass)
    {        
        for (String key : xclass.getPropertyList()) {
            if (this.entity.safeget(key) == null) {
                PropertyClass classProperty = (PropertyClass) xclass.getField(key);
                BaseProperty property = classProperty.newProperty();
                if (property != null) {
                    this.entity.safeput(key, property);
                }
            }
        }
    }
    
    @Override
    public void onWikiObjectProperty(String name, Object value, FilterEventParameters parameters) throws FilterException
    {
        if (this.enabled) {
            if (getBasePropertyOutputFilterStream().getEntity() != null) {
                this.entity.safeput(name, getBasePropertyOutputFilterStream().getEntity());

                getBasePropertyOutputFilterStream().setEntity(null);
            }
        }
    }
}
