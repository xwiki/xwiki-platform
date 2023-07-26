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
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.solr.common.SolrInputDocument;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.namespace.Namespace;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.internal.ExtensionFactory;
import org.xwiki.extension.internal.converter.ExtensionIdConverter;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.search.solr.SolrUtils;

import static org.xwiki.extension.InstalledExtension.FIELD_INSTALLED_NAMESPACES;
import static org.xwiki.search.solr.SolrUtils.ATOMIC_UPDATE_MODIFIER_SET;

/**
 * Various helpers around the Solr API for the Extension Index.
 *
 * @version $Id$
 * @since 15.6RC1
 * @since 15.5.1
 */
@Component(roles = ExtensionIndexSolrUtil.class)
@Singleton
public class ExtensionIndexSolrUtil
{
    private static final String ROOT_NAMESPACE = "{root}";

    @Inject
    private InstalledExtensionRepository installedExtensions;

    @Inject
    private SolrUtils utils;

    @Inject
    private ExtensionFactory factory;

    /**
     * Update the installed state of a given extensionId. The provided {@link SolrInputDocument} is updated but need to
     * be added and committed afterward.
     *
     * @param extensionId the extension id of the extension to update
     * @param document the document storing the updated version of the extension
     */
    public void updateInstalledState(ExtensionId extensionId, SolrInputDocument document)
    {
        InstalledExtension installedExtension = this.installedExtensions.getInstalledExtension(extensionId);
        if (installedExtension != null) {
            List<String> installedNamespaces;
            if (installedExtension.getNamespaces() == null) {
                installedNamespaces = Collections.singletonList(toStoredNamespace((String) null));
            } else {
                installedNamespaces = installedExtension.getNamespaces().stream().map(this::toStoredNamespace)
                    .collect(Collectors.toList());
            }
            this.utils.setAtomic(ATOMIC_UPDATE_MODIFIER_SET, FIELD_INSTALLED_NAMESPACES,
                installedNamespaces, document);

            // We can already set those extensions as "incompatible" with those namespaces
            this.utils.set(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_INCOMPATIBLE_NAMESPACES, installedNamespaces,
                document);
        } else {
            this.utils.setAtomic(ATOMIC_UPDATE_MODIFIER_SET, FIELD_INSTALLED_NAMESPACES, null, document);
        }
    }

    /**
     * Convert a namespace to a solr string representation. In particular, a {@code null} namespace is converted to
     * {@link #ROOT_NAMESPACE}.
     *
     * @param namespace the namespace to convert
     * @return the solr string representation of the namespace
     */
    public String toStoredNamespace(Namespace namespace)
    {
        return namespace != null ? toStoredNamespace(namespace.toString()) : ROOT_NAMESPACE;
    }

    /**
     * Convert a namespace to a solr string representation. In particular, a {@code null} namespace is converted to
     * {@link #ROOT_NAMESPACE}.
     *
     * @param namespace the namespace to convert
     * @return the solr string representation of the namespace
     */
    public String toStoredNamespace(String namespace)
    {
        if (namespace == null) {
            return ROOT_NAMESPACE;
        }

        return namespace;
    }

    /**
     * Convert a collection of solr namespaces and convert them to a list of namespaces that can be used by  a
     * {@link SolrExtension}. Elements of the collection matching {@link #ROOT_NAMESPACE} are converted to null, the
     * rest of the elements stays unchanged
     *
     * @param solrNamespaces the namespaces to convert
     * @return the converted list
     */
    public List<String> fromStoredNamespaces(Collection<String> solrNamespaces)
    {
        if (solrNamespaces == null) {
            return List.of();
        }

        return solrNamespaces.stream()
            .map(this::fromStoredNamespace)
            .collect(Collectors.toList());
    }

    /**
     * @param solrId the identifier of the Solr document holding the extension
     * @return the extension id
     */
    public ExtensionId fromSolrId(String solrId)
    {
        return ExtensionIdConverter.toExtensionId(solrId, null, this.factory);
    }

    /**
     * @param extensionId the extension id
     * @return the identifier of the Solr document holding the extension
     */
    public String toSolrId(ExtensionId extensionId)
    {
        return ExtensionIdConverter.toString(extensionId);
    }

    /**
     * Convert a solr string representation to a string representation that can be stored in a {@link SolrExtension}.
     *
     * @param storedNamespace the solr string representation of the namespace
     */
    private String fromStoredNamespace(String storedNamespace)
    {
        if (storedNamespace == null || storedNamespace.equals(ROOT_NAMESPACE)) {
            return null;
        }

        return storedNamespace;
    }
}
