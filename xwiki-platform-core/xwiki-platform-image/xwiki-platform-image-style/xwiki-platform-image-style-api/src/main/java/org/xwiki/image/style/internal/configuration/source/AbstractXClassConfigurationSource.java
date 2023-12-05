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
package org.xwiki.image.style.internal.configuration.source;

import java.util.Locale;

import org.xwiki.configuration.internal.AbstractDocumentConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.SpaceReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

/**
 * Base class for class based configuration.
 *
 * @version $Id$
 * @since 14.3RC1
 */
public abstract class AbstractXClassConfigurationSource extends AbstractDocumentConfigurationSource
{
    @Override
    protected String getCacheKeyPrefix()
    {
        DocumentReference currentDocumentReference = getDocumentReference();
        if (currentDocumentReference != null) {
            return this.referenceSerializer.serialize(currentDocumentReference.getParent());
        }

        return null;
    }
    
    @Override
    protected DocumentReference getDocumentReference()
    {
        return new DocumentReference(getClassReference().getName(), new SpaceReference(getClassReference().getParent(),
            getCurrentWikiReference()));
    }

    protected BaseObject getBaseObject(String language) throws XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        if (xcontext != null && xcontext.getWiki() != null) {
            DocumentReference documentReference = getFailsafeDocumentReference();
            LocalDocumentReference classReference = getFailsafeClassReference();

            if (documentReference != null && classReference != null) {
                XWikiDocument document = xcontext.getWiki().getDocument(getDocumentReference(), xcontext);

                return getBaseObject(document, language);
            }
        }

        return null;
    }

    protected BaseObject getBaseObject(XWikiDocument document, String language)
    {
        if (language != null) {
            return document.getXObject(getClassReference(), "default_language", language, true);
        } else {
            return document.getXObject(getClassReference());
        }
    }

    @Override
    protected BaseObject getBaseObject() throws XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        if (xcontext != null && xcontext.getWiki() != null) {
            DocumentReference documentReference = getFailsafeDocumentReference();
            LocalDocumentReference classReference = getFailsafeClassReference();

            if (documentReference != null && classReference != null) {
                XWikiDocument document = xcontext.getWiki().getDocument(getDocumentReference(), xcontext);

                // First we try to get a translated preference object
                BaseObject object = getBaseObject(document, safeGetLanguage(xcontext));

                if (object != null) {
                    return object;
                }

                return document.getXObject(classReference);
            }
        }

        return null;
    }

    protected Object getBaseProperty(String propertyName, String language, boolean text) throws XWikiException
    {
        // First we try to get a translated preference object
        BaseObject baseObject = getBaseObject(language);

        if (baseObject != null) {
            BaseProperty property = (BaseProperty) baseObject.getField(propertyName);

            if (property != null) {
                if (text) {
                    return property.toText();
                }
                return property.getValue();
            } else {
                return null;
            }
        }

        return null;
    }

    @Override
    protected Object getBaseProperty(String propertyName, boolean text) throws XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        // First we try to get a translated preference object
        Object propertyValue = getBaseProperty(propertyName, safeGetLanguage(xcontext), text);

        // If empty we take it from the default pref object
        if (propertyValue == null || isEmpty(propertyValue)) {
            propertyValue = getBaseProperty(propertyName, null, text);
        }

        // TODO: In the future we would need the notion of initialized/not-initialized property values in the wiki.
        // When this is implemented modify the code below.
        if (isEmpty(propertyValue)) {
            propertyValue = null;
        }

        return propertyValue;
    }

    private String safeGetLanguage(XWikiContext xcontext)
    {
        Locale locale = xcontext.getLocale();
        if (locale != null) {
            return locale.getLanguage();
        } else {
            return null;
        }
    }
}
