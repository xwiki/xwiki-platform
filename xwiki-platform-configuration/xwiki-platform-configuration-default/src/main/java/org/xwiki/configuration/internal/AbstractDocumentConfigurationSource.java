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

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;

/**
 * Common features for all Document sources (ie configuration data coming from wiki pages).
 *  
 * @version $Id$
 * @since 2.0M2
 */
public abstract class AbstractDocumentConfigurationSource implements ConfigurationSource
{
    /**
     * @see #getDocumentAccessBridge()
     */
    @Requirement
    private DocumentAccessBridge documentAccessBridge; 

    /**
     * @return the document reference of the document containing an XWiki Object with configuration data or null
     *         if there no such document in which case this configuration source will be skipped
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
     * {@inheritDoc}
     * @see ConfigurationSource#containsKey(String)
     */
    public boolean containsKey(String key)
    {
        DocumentReference documentReference = getFailsafeDocumentReference();
        DocumentReference classReference = getFailsafeClassReference();
        return (documentReference != null && classReference != null)
            && getDocumentAccessBridge().getProperty(documentReference, classReference, key) != null;
    }

    /**
     * {@inheritDoc}
     * @see ConfigurationSource#getKeys()
     */
    public List<String> getKeys()
    {
        return null;
    }

    /**
     * {@inheritDoc}
     * @see ConfigurationSource#getProperty(String, Object)
     */
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, T defaultValue)
    {
        return getProperty(key, defaultValue, (Class<T>) defaultValue.getClass());
    }

    /**
     * {@inheritDoc}
     * @see ConfigurationSource#getProperty(String, Class)
     */
    public <T> T getProperty(String key, Class<T> valueClass)
    {
        T result = null;

        DocumentReference documentReference = getFailsafeDocumentReference();
        DocumentReference classReference = getFailsafeClassReference();
        if (documentReference != null && classReference != null) {
            result = (T) getDocumentAccessBridge().getProperty(documentReference, classReference, key);
        
            // Make sure we don't return null values for List and Properties (they must return empty elements
            // when using the typed API).
            if (result == null) {
                if (List.class.getName().equals(valueClass.getName())) {
                    result = (T) Collections.emptyList();
                } else if (Properties.class.getName().equals(valueClass.getName())) {
                    result = (T) new Properties();
                }
            }
        }

        return result;
    }

    /**
     * {@inheritDoc}
     * @see ConfigurationSource#getProperty(String)
     */
    public <T> T getProperty(String key)
    {
        T result = null;
        DocumentReference documentReference = getFailsafeDocumentReference();
        DocumentReference classReference = getFailsafeClassReference();
        if (documentReference != null && classReference != null) {
            result = (T) getDocumentAccessBridge().getProperty(documentReference, classReference, key);
        }

        return result; 
    }

    /**
     * {@inheritDoc}
     * @see ConfigurationSource#isEmpty()
     */
    public boolean isEmpty()
    {
        return getKeys().isEmpty();
    }

    private <T> T getProperty(String key, T defaultValue, Class<T> valueClass)
    {
        T result = getProperty(key, valueClass);
        if (result == null) {
            result = defaultValue; 
        }
        return result;
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
            classReference = getDocumentReference();
        } catch (Exception e) {
            // We verify that no error has happened and if one happened then we skip this configuration source. This
            // ensures the system will continue to work even if this source has a problem.
            classReference = null;
        }
        return classReference;
    }
}
