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
package org.xwiki.extension.index.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.xwiki.extension.DefaultExtensionSupportPlans;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionAuthor;
import org.xwiki.extension.ExtensionComponent;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionSupportPlan;
import org.xwiki.extension.ExtensionSupportPlans;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.index.IndexedExtension;
import org.xwiki.extension.rating.ExtensionRating;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.repository.rating.RatableExtensionRepository;
import org.xwiki.extension.version.Version;
import org.xwiki.extension.wrap.WrappingIndexedExtension;

/**
 * An extension stored in the Solr based index of extensions.
 * 
 * @version $Id$
 * @since 12.10
 */
public class SolrExtension extends WrappingIndexedExtension<Extension> implements IndexedExtension
{
    private final ExtensionRepository repository;

    private final ExtensionId extensionId;

    private List<Version> versions;

    private Set<String> compatibleNamespaces = Collections.emptySet();

    private Set<String> incompatibleNamespaces = Collections.emptySet();

    private Date indexDate;

    private boolean last;

    private int totalVotes;

    private float averageVote;

    private final ExtensionRating rating = new ExtensionRating()
    {
        @Override
        public int getTotalVotes()
        {
            return totalVotes;
        }

        @Override
        public float getAverageVote()
        {
            return averageVote;
        }

        @Override
        public RatableExtensionRepository getRepository()
        {
            return null;
        }
    };

    /**
     * @param repository the repository where this extension comes from
     * @param extensionId the extension identifier
     */
    public SolrExtension(ExtensionRepository repository, ExtensionId extensionId)
    {
        this.repository = repository;
        this.extensionId = extensionId;
    }

    @Override
    protected Extension resolveWrapped()
    {
        try {
            return this.repository.resolve(this.extensionId);
        } catch (ResolveException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    // IndexedExtension

    @Override
    public Boolean isCompatible(String namespace)
    {
        if (this.compatibleNamespaces != null && this.compatibleNamespaces.contains(namespace)) {
            return true;
        }

        if (this.incompatibleNamespaces != null && this.incompatibleNamespaces.contains(namespace)) {
            return false;
        }

        return null;
    }

    /**
     * @param compatibleNamespaces the namespaces in which the extension can be installed
     */
    public void setCompatibleNamespaces(Collection<String> compatibleNamespaces)
    {
        if (compatibleNamespaces != null) {
            this.compatibleNamespaces = new HashSet<>(compatibleNamespaces);
        } else {
            this.compatibleNamespaces = Collections.emptySet();
        }
    }

    /**
     * @param incompatibleNamespaces the namespaces in which the extension cannot be installed
     */
    public void setIncompatibleNamespaces(Collection<String> incompatibleNamespaces)
    {
        if (incompatibleNamespaces != null) {
            this.incompatibleNamespaces = new HashSet<>(incompatibleNamespaces);
        } else {
            this.incompatibleNamespaces = Collections.emptySet();
        }
    }

    // Extension

    @Override
    public ExtensionId getId()
    {
        return this.extensionId;
    }

    @Override
    public ExtensionRepository getRepository()
    {
        return this.repository;
    }

    /**
     * @param name the display name of the extension
     */
    public void setName(String name)
    {
        this.overwrites.put(FIELD_NAME, name);
    }

    /**
     * @param type the type to set
     */
    public void setType(String type)
    {
        this.overwrites.put(FIELD_TYPE, type);
    }

    /**
     * @param summary a short description of the extension
     */
    public void setSummary(String summary)
    {
        this.overwrites.put(FIELD_SUMMARY, summary);
    }

    /**
     * @param website an URL for the extension website
     */
    public void setWebsite(String website)
    {
        this.overwrites.put(FIELD_WEBSITE, website);
    }

    /**
     * @param categrory the category of the extension;
     */
    public void setCategory(String categrory)
    {
        this.overwrites.put(FIELD_CATEGORY, categrory);
    }

    /**
     * @param namespaces the namespaces where it's allowed to install this extension
     */
    public void setAllowedNamespaces(Collection<String> namespaces)
    {
        this.overwrites.put(FIELD_ALLOWEDNAMESPACES,
            namespaces != null ? Collections.unmodifiableCollection(namespaces) : null);
    }

    /**
     * @param features the {@link ExtensionId}s also provided by this extension
     */
    public void setExtensionFeatures(Collection<ExtensionId> features)
    {
        this.overwrites.put(FIELD_EXTENSIONFEATURES,
            features != null ? Collections.unmodifiableCollection(features) : null);
    }

    /**
     * @param authors the authors of the extension
     */
    public void setAuthors(Collection<? extends ExtensionAuthor> authors)
    {
        this.overwrites.put(FIELD_AUTHORS,
            authors != null ? Collections.unmodifiableCollection(authors) : Collections.emptyList());
    }

    /**
     * @param supportPlans the support plans of the extension
     */
    public void setSupportPlans(Collection<? extends ExtensionSupportPlan> supportPlans)
    {
        this.overwrites.put(FIELD_SUPPORT_PLANS,
            supportPlans != null ? new DefaultExtensionSupportPlans(supportPlans) : ExtensionSupportPlans.EMPTY);
    }

    /**
     * @param components the components provided by the extension
     * @since 13.3RC1
     */
    public void setComponents(Collection<? extends ExtensionComponent> components)
    {
        this.overwrites.put(FIELD_COMPONENTS,
            components != null ? Collections.unmodifiableCollection(components) : Collections.emptyList());
    }

    // RemoteExtension

    /**
     * @param recommended true if the extension is recommended
     * @see #isRecommended()
     */
    public void setRecommended(boolean recommended)
    {
        this.overwrites.put(FIELD_RECOMMENDED, recommended);
    }

    /**
     * @param supportPlans the support plans
     * @since 16.8.0RC1
     */
    public void setSupportPlans(List<ExtensionSupportPlan> supportPlans)
    {
        this.overwrites.put(FIELD_SUPPORT_PLANS, supportPlans);
    }

    // RatingExtension

    @Override
    public ExtensionRating getRating()
    {
        return this.rating;
    }

    /**
     * @param totalVotes the total number of votes
     */
    public void setTotalVotes(int totalVotes)
    {
        this.totalVotes = totalVotes;
    }

    /**
     * @param averageVote the average vote
     */
    public void setAverageVote(float averageVote)
    {
        this.averageVote = averageVote;
    }

    // SolrExtension

    /**
     * @return the versions available for the extension
     */
    public List<Version> getVersions()
    {
        return this.versions;
    }

    /**
     * @return the date at which the extension was indexed
     */
    public Date getIndexDate()
    {
        return this.indexDate;
    }

    /**
     * @return true if it's the last version among the extension with this id
     */
    public boolean isLast()
    {
        return this.last;
    }

    /**
     * @param last true if it's the last version among the extension with this id
     */
    public void setLast(boolean last)
    {
        this.last = last;
    }

    @Override
    public String toString()
    {
        return getId().toString();
    }
}
