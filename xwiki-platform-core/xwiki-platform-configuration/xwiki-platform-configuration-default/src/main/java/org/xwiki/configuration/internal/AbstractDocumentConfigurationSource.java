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
package org.xwiki.configuration.internal;

import java.util.List;

import javax.inject.Inject;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelConfiguration;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

/**
 * Common features for all Document sources (ie configuration data coming from wiki pages).
 * 
 * @version $Id$
 * @since 2.0M2
 */
public abstract class AbstractDocumentConfigurationSource extends AbstractConfigurationSource
{
    /**
     * @see #getDocumentAccessBridge()
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /** @see #getCurrentWikiReference() */
    @Inject
    private ModelContext modelContext;

    /** @see #getCurrentWikiReference() */
    @Inject
    private ModelConfiguration modelConfig;

    /**
     * @return the document reference of the document containing an XWiki Object with configuration data or null if
     *         there no such document in which case this configuration source will be skipped
     */
    protected abstract DocumentReference getDocumentReference();

    /**
     * @return the XWiki Class reference of the XWiki Object containing the configuration properties
     */
    protected abstract DocumentReference getClassReference();

    /**
     * @return the bridge used to access Object properties
     */
    protected DocumentAccessBridge getDocumentAccessBridge()
    {
        return this.documentAccessBridge;
    }

    /**
     * @return the reference pointing to the current wiki
     */
    protected WikiReference getCurrentWikiReference()
    {
        if (this.modelContext.getCurrentEntityReference() != null) {
            return (WikiReference) this.modelContext.getCurrentEntityReference().extractReference(EntityType.WIKI);
        }

        return new WikiReference(this.modelConfig.getDefaultReferenceValue(EntityType.WIKI));
    }

    @Override
    public boolean containsKey(String key)
    {
        return getPropertyObject(key) != null;
    }

    @Override
    public List<String> getKeys()
    {
        return null;
    }

    @Override
    public <T> T getProperty(String key, T defaultValue)
    {
        T result = getProperty(key);

        if (result == null) {
            result = defaultValue;
        }

        return result;
    }

    @Override
    public <T> T getProperty(String key, Class<T> valueClass)
    {
        T result = getProperty(key);

        // Make sure we don't return null values for List and Properties (they must return empty elements
        // when using the typed API).
        if (result == null) {
            result = getDefault(valueClass);
        }

        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key)
    {
        return (T) getPropertyObject(key);
    }

    private Object getPropertyObject(String key)
    {
        Object result;

        DocumentReference documentReference = getFailsafeDocumentReference();
        DocumentReference classReference = getFailsafeClassReference();
        if (documentReference != null && classReference != null) {
            result = getDocumentAccessBridge().getProperty(documentReference, classReference, key);
        } else {
            result = null;
        }

        return result;
    }

    @Override
    public boolean isEmpty()
    {
        return getKeys().isEmpty();
    }

    private DocumentReference getFailsafeDocumentReference()
    {
        DocumentReference documentReference;

        try {
            documentReference = getDocumentReference();
        } catch (Exception e) {
            // We verify that no error has happened and if one happened then we skip this configuration source. This
            // ensures the system will continue to work even if this source has a problem.
            documentReference = null;
        }

        return documentReference;
    }

    private DocumentReference getFailsafeClassReference()
    {
        DocumentReference classReference;

        try {
            classReference = getClassReference();
        } catch (Exception e) {
            // We verify that no error has happened and if one happened then we skip this configuration source. This
            // ensures the system will continue to work even if this source has a problem.
            classReference = null;
        }

        return classReference;
    }
}
