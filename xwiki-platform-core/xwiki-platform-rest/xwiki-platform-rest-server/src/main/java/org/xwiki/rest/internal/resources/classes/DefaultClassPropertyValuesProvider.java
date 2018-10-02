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
package org.xwiki.rest.internal.resources.classes;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.ClassPropertyReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.PropertyValue;
import org.xwiki.rest.model.jaxb.PropertyValues;
import org.xwiki.rest.resources.classes.ClassPropertyValuesProvider;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

/**
 * Default {@link ClassPropertyValuesProvider} implementation that delegates the work to specific implementations.
 * 
 * @version $Id$
 * @since 9.8RC1
 */
@Component
@Singleton
public class DefaultClassPropertyValuesProvider implements ClassPropertyValuesProvider
{
    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Override
    public PropertyValues getValues(ClassPropertyReference propertyReference, int limit, Object... filterParameters)
        throws XWikiRestException
    {
        return getDedicatedProvider(getPropertyType(propertyReference)).getValues(propertyReference, limit,
            filterParameters);
    }

    @Override
    public PropertyValue getValue(ClassPropertyReference propertyReference, Object rawValue)
        throws XWikiRestException
    {
        return getDedicatedProvider(getPropertyType(propertyReference)).getValue(propertyReference, rawValue);
    }

    private ClassPropertyValuesProvider getDedicatedProvider(String propertyType) throws XWikiRestException
    {
        try {
            return this.componentManager.getInstance(ClassPropertyValuesProvider.class, propertyType);
        } catch (ComponentLookupException e) {
            throw new XWikiRestException(
                String.format("There's no value provider registered for the [%s] property type.", propertyType));
        }
    }

    private String getPropertyType(ClassPropertyReference propertyReference) throws XWikiRestException
    {
        try {
            XWikiContext xcontext = this.xcontextProvider.get();
            XWikiDocument document = xcontext.getWiki().getDocument(propertyReference.getParent(), xcontext);
            BaseClass xclass = document.getXClass();
            PropertyInterface xproperty = xclass.get(propertyReference.getName());
            if (xproperty instanceof PropertyClass) {
                return ((PropertyClass) xproperty).getClassType();
            } else {
                throw new XWikiRestException(String.format("No such property [%s].",
                    this.entityReferenceSerializer.serialize(propertyReference)));
            }
        } catch (XWikiException e) {
            throw new XWikiRestException(String.format("Failed to determine the property type for [{}].",
                this.entityReferenceSerializer.serialize(propertyReference)));
        }
    }
}
