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
package org.xwiki.livedata.internal.livetable;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.PropertyClass;

/**
 * Utility component to read the property type from an XWiki class.
 * 
 * @version $Id$
 * @since 12.10RC1
 */
@Component(roles = PropertyTypeSupplier.class)
@Singleton
public class PropertyTypeSupplier
{
    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    /**
     * Reads the type of the specified property from the specified class.
     * 
     * @param propertyName name of the property to look for
     * @param className the class where to look for the specified property
     * @return the type of the specified property from the specified class, {@code null} if the specified property is
     *         not found
     */
    public String getPropertyType(String propertyName, String className)
    {
        DocumentReference classReference = this.currentDocumentReferenceResolver.resolve(className);
        XWikiContext xcontext = this.xcontextProvider.get();
        try {
            XWikiDocument classDocument = xcontext.getWiki().getDocument(classReference, xcontext);
            PropertyInterface property = classDocument.getXClass().get(propertyName);
            if (property instanceof PropertyClass) {
                return ((PropertyClass) property).getClassType();
            }
        } catch (XWikiException e) {
            this.logger.warn("Failed to read the type of property [{}] from [{}]. Root cause is [{}].", propertyName,
                className, ExceptionUtils.getRootCauseMessage(e));
        }
        return null;
    }
}
