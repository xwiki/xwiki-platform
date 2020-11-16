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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.namespace.Namespace;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionNotFoundException;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.index.ExtensionIndex;
import org.xwiki.extension.index.ExtensionIndexStatus;
import org.xwiki.extension.index.IndexedExtension;
import org.xwiki.extension.index.internal.job.ExtensionIndexJobScheduler;
import org.xwiki.extension.repository.AbstractAdvancedSearchableExtensionRepository;
import org.xwiki.extension.repository.DefaultExtensionRepositoryDescriptor;
import org.xwiki.extension.repository.internal.RepositoryUtils;
import org.xwiki.extension.repository.result.CollectionIterableResult;
import org.xwiki.extension.repository.result.IterableResult;
import org.xwiki.extension.repository.search.ExtensionQuery;
import org.xwiki.extension.repository.search.SearchException;
import org.xwiki.extension.version.Version;
import org.xwiki.extension.version.internal.VersionUtils;
import org.xwiki.job.JobException;

/**
 * The default implementation of {@link ExtensionIndex}, based on Solr.
 * 
 * @version $Id$
 * @since 12.10RC1
 */
@Component
@Singleton
public class DefaultExtensionIndex extends AbstractAdvancedSearchableExtensionRepository
    implements ExtensionIndex, Initializable
{
    private static final String ID = "index";

    @Inject
    private ExtensionIndexStore store;

    @Inject
    private ExtensionIndexJobScheduler scheduler;

    @Inject
    private Logger logger;

    @Override
    public void initialize() throws InitializationException
    {
        setDescriptor(new DefaultExtensionRepositoryDescriptor(ID, ID, null));
    }

    @Override
    public ExtensionIndexStatus getStatus(Namespace namespace)
    {
        return this.scheduler.getStatus(namespace);
    }

    @Override
    public ExtensionIndexStatus index(Namespace namespace) throws JobException
    {
        return this.scheduler.index(namespace);
    }

    // ExtensionRepository

    @Override
    public IterableResult<Extension> search(ExtensionQuery query) throws SearchException
    {
        return this.store.search(query);
    }

    private SolrExtension getSolrExtension(ExtensionId extensionId)
    {
        try {
            return this.store.getSolrExtension(extensionId);
        } catch (Exception e) {
            this.logger.warn("Failed to get the extension [{}] from the index: {}", extensionId,
                ExceptionUtils.getRootCauseMessage(e));
        }

        return null;
    }

    @Override
    public IndexedExtension resolve(ExtensionId extensionId) throws ResolveException
    {
        IndexedExtension extension = getSolrExtension(extensionId);

        if (extension != null) {
            return extension;
        }

        throw new ExtensionNotFoundException("No extension could be found in the index for id [" + extensionId + "]");
    }

    @Override
    public IndexedExtension resolve(ExtensionDependency extensionDependency) throws ResolveException
    {
        // Search in the index if the constraint is a unique version
        Version uniqueVersion = VersionUtils.getUniqueVersion(extensionDependency.getVersionConstraint());
        if (uniqueVersion != null) {
            ExtensionId extensionId = new ExtensionId(extensionDependency.getId(), uniqueVersion);

            IndexedExtension extension = getSolrExtension(extensionId);

            if (extension != null) {
                return extension;
            }
        }

        throw new ExtensionNotFoundException(
            "No dependency could be found in the index for id [" + extensionDependency + "]");
    }

    @Override
    public boolean exists(ExtensionId extensionId)
    {
        try {
            return this.store.exists(extensionId);
        } catch (Exception e) {
            this.logger.error("Failed to check existance of extension [{}]", extensionId, e);

            return false;
        }
    }

    @Override
    public IterableResult<Version> resolveVersions(String id, int offset, int nb) throws ResolveException
    {
        Collection<Version> versions;
        try {
            versions = this.store.getIndexedVersions(id);
        } catch (Exception e) {
            throw new ResolveException("Failed to search for extension versions", e);
        }

        if (versions == null) {
            throw new ExtensionNotFoundException("Can't find extension with id [" + id + "]");
        }

        if (nb == 0 || offset >= versions.size()) {
            return new CollectionIterableResult<>(versions.size(), offset, Collections.<Version>emptyList());
        }

        return RepositoryUtils.getIterableResult(offset, nb, versions);
    }

    @Override
    public boolean isFilterable()
    {
        return true;
    }

    @Override
    public boolean isSortable()
    {
        return true;
    }
}
