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
package org.xwiki.internal.filter;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.filter.xar.internal.input.CurrentXClassLoader;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.DateClass;
import com.xpn.xwiki.objects.classes.ListClass;

/**
 * Default implementation of {@link CurrentXClassLoader}.
 *
 * @version $Id$
 * @since 18.8.0RC1
 * @since 18.4.5
 * @since 17.10.13
 */
@Component
@Singleton
public class DefaultCurrentXClassLoader implements CurrentXClassLoader
{
    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> stringDocumentReferenceResolver;

    @Override
    public String getXClassPropertyType(String xclassName, String propertyName)
    {
        String result = null;
        XWikiContext context = contextProvider.get();
        DocumentReference xclassReference = this.stringDocumentReferenceResolver.resolve(xclassName);
        try {
            XWikiDocument document = context.getWiki().getDocument(xclassReference, context);
            if (!document.isNew()) {
                BaseClass baseClass = document.getXClass();
                if (baseClass != null) {
                    PropertyInterface propertyInterface = baseClass.safeget(propertyName);
                    if (propertyInterface instanceof DateClass) {
                        result = "Date";
                    } else if (propertyInterface instanceof ListClass) {
                        // We don't really care here if it's a StaticList, a DBList etc:
                        // we only want to have the proper hint for ListXarObjectPropertySerializer.
                        result = "StaticList";
                    }
                }
            }
        } catch (XWikiException e) {
            this.logger.error("Error while trying to load current xclass [{}] to read type of property [{}]",
                xclassName, propertyName, e);
        }
        return result;
    }
}
