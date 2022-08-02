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
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.inject.Inject;
import javax.inject.Provider;

import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Abstract composite configuration source that looks in the current space and all its parent spaces.
 *
 * @version $Id$
 * @since 8.2RC1
 */
public abstract class AbstractSpacesConfigurationSource extends AbstractCompositeConfigurationSource
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    private class SpaceIterator implements Iterator<ConfigurationSource>
    {
        private SpaceReference reference;

        SpaceIterator(SpaceReference reference)
        {
            this.reference = reference;
        }

        @Override
        public boolean hasNext()
        {
            return this.reference != null;
        }

        @Override
        public ConfigurationSource next()
        {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            SpaceReference next = this.reference;
            // Move reference to parent
            if (this.reference.getParent() instanceof SpaceReference) {
                this.reference = (SpaceReference) this.reference.getParent();
            } else {
                this.reference = null;
            }

            // Return wrapped space configuration associated to the space reference
            return new SpaceConfigurationSource(next);
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }

    private class SpaceConfigurationSource implements ConfigurationSource
    {
        private SpaceReference reference;

        private XWikiDocument document;

        SpaceConfigurationSource(SpaceReference reference)
        {
            this.reference = reference;
        }

        private XWikiDocument setCurrentDocument(XWikiDocument document)
        {
            XWikiContext xcontext = xcontextProvider.get();
            if (xcontext != null) {
                XWikiDocument currentDocument = xcontext.getDoc();
                xcontext.setDoc(document);
                return currentDocument;
            }

            return null;
        }

        public XWikiDocument getDocument()
        {
            if (this.document == null) {
                this.document = new XWikiDocument(
                    new DocumentReference(SpacePreferencesConfigurationSource.DOCUMENT_NAME, this.reference));
            }

            return this.document;
        }

        @Override
        public <T> T getProperty(String key, T defaultValue)
        {
            XWikiDocument currentDocument = setCurrentDocument(getDocument());
            try {
                return getSpaceConfigurationSource().getProperty(key, defaultValue);
            } finally {
                setCurrentDocument(currentDocument);
            }
        }

        @Override
        public <T> T getProperty(String key, Class<T> valueClass)
        {
            XWikiDocument currentDocument = setCurrentDocument(getDocument());
            try {
                return getSpaceConfigurationSource().getProperty(key, valueClass);
            } finally {
                setCurrentDocument(currentDocument);
            }
        }

        @Override
        public <T> T getProperty(String key)
        {
            XWikiDocument currentDocument = setCurrentDocument(getDocument());
            try {
                return getSpaceConfigurationSource().getProperty(key);
            } finally {
                setCurrentDocument(currentDocument);
            }
        }

        @Override
        public List<String> getKeys()
        {
            XWikiDocument currentDocument = setCurrentDocument(getDocument());
            try {
                return getSpaceConfigurationSource().getKeys();
            } finally {
                setCurrentDocument(currentDocument);
            }
        }

        @Override
        public boolean containsKey(String key)
        {
            XWikiDocument currentDocument = setCurrentDocument(getDocument());
            try {
                return getSpaceConfigurationSource().containsKey(key);
            } finally {
                setCurrentDocument(currentDocument);
            }
        }

        @Override
        public boolean isEmpty()
        {
            XWikiDocument currentDocument = setCurrentDocument(getDocument());
            try {
                return getSpaceConfigurationSource().isEmpty();
            } finally {
                setCurrentDocument(currentDocument);
            }
        }
    }

    @Override
    public Iterator<ConfigurationSource> iterator()
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        if (xcontext != null) {
            XWikiDocument currentDocument = xcontext.getDoc();
            if (currentDocument != null) {
                return new SpaceIterator(currentDocument.getDocumentReference().getLastSpaceReference());
            }
        }

        return Collections.<ConfigurationSource>emptyList().iterator();
    }

    /**
     * @return the space configuration source to use at each level
     */
    protected abstract ConfigurationSource getSpaceConfigurationSource();
}
