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
package org.xwiki.extension.xar.internal.doc;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.SpaceReference;

/**
 * Provides information about the hierarchy of installed extension documents.
 * 
 * @version $Id$
 * @since 11.10
 */
@Component(roles = InstalledExtensionDocumentTree.class)
@Singleton
public class InstalledExtensionDocumentTree
{
    private static final class InstalledExtensionDocumentTreeNode
    {
        public Set<Locale> customizedLocales;

        public Set<DocumentReference> children = Collections.newSetFromMap(new ConcurrentHashMap<>());
    }

    private final Map<EntityReference, InstalledExtensionDocumentTreeNode> nodes = new ConcurrentHashMap<>();

    @Inject
    private EntityReferenceProvider defaultEntityReferenceProvider;

    /**
     * @param parentReference the parent reference
     * @return the set of direct child pages of the specified parent node
     */
    public Set<DocumentReference> getChildren(EntityReference parentReference)
    {
        InstalledExtensionDocumentTreeNode parentNode = this.nodes.get(parentReference);
        return parentNode == null ? Collections.emptySet() : parentNode.children;
    }

    /**
     * @param documentReference a document reference
     * @return {@code true} if the specified page belongs to an installed extension
     */
    public boolean isExtensionPage(DocumentReference documentReference)
    {
        InstalledExtensionDocumentTreeNode node = this.nodes.get(documentReference);
        return node != null && node.customizedLocales != null;
    }

    /**
     * @param documentReference a document reference
     * @return {@code true} if the specified page is an extension page that has customizations
     */
    public boolean isCustomizedExtensionPage(DocumentReference documentReference)
    {
        InstalledExtensionDocumentTreeNode node = this.nodes.get(documentReference);
        return node != null && node.customizedLocales != null && !node.customizedLocales.isEmpty();
    }

    /**
     * @param rootReference the root reference
     * @return the set of extension pages nested under the specified root node
     */
    public Set<DocumentReference> getNestedExtensionPages(EntityReference rootReference)
    {
        Set<DocumentReference> documentReferences = new HashSet<>();
        Queue<DocumentReference> queue = new LinkedList<>(getChildren(rootReference));
        while (!queue.isEmpty()) {
            DocumentReference parentReference = queue.remove();
            queue.addAll(getChildren(parentReference));
            if (isExtensionPage(parentReference)) {
                documentReferences.add(parentReference);
            }
        }
        return documentReferences;
    }

    /**
     * @param rootReference the root reference
     * @return the set of extension pages nested under the specified root node and that have customizations
     */
    public Set<DocumentReference> getNestedCustomizedExtensionPages(EntityReference rootReference)
    {
        return getNestedExtensionPages(rootReference).stream().filter(this::isCustomizedExtensionPage)
            .collect(Collectors.toSet());
    }

    protected void setCustomizedExtensionPage(DocumentReference documentReference, boolean customized)
    {
        DocumentReference documentReferenceWithoutLocale = new DocumentReference(documentReference, (Locale) null);
        InstalledExtensionDocumentTreeNode node = this.nodes.get(documentReferenceWithoutLocale);
        if (node != null && node.customizedLocales != null) {
            if (customized) {
                node.customizedLocales.add(documentReference.getLocale());
            } else {
                node.customizedLocales.remove(documentReference.getLocale());
            }
        }
    }

    protected void addExtensionPage(DocumentReference documentReference)
    {
        InstalledExtensionDocumentTreeNode node = this.nodes.get(documentReference);
        if (node != null) {
            if (node.customizedLocales == null) {
                node.customizedLocales = new CopyOnWriteArraySet<>();
            }
        } else {
            node = addNode(documentReference);
            node.customizedLocales = new CopyOnWriteArraySet<>();
        }
    }

    private InstalledExtensionDocumentTreeNode addNode(EntityReference entityReference)
    {
        EntityReference parentReference = getParentReference(entityReference);
        if (parentReference != null) {
            InstalledExtensionDocumentTreeNode parentNode = this.nodes.get(parentReference);
            if (parentNode == null) {
                parentNode = addNode(parentReference);
            }
            parentNode.children.add((DocumentReference) entityReference);
        }
        InstalledExtensionDocumentTreeNode node = new InstalledExtensionDocumentTreeNode();
        this.nodes.put(entityReference, node);
        return node;
    }

    protected void removeExtensionPage(DocumentReference documentReference)
    {
        InstalledExtensionDocumentTreeNode node = this.nodes.get(documentReference);
        if (node != null) {
            if (node.children.isEmpty()) {
                removeNode(documentReference);
            } else {
                // This is not an extension page anymore.
                node.customizedLocales = null;
            }
        }
    }

    private void removeNode(EntityReference entityReference)
    {
        EntityReference parentReference = getParentReference(entityReference);
        if (parentReference != null) {
            InstalledExtensionDocumentTreeNode parentNode = this.nodes.get(parentReference);
            if (parentNode != null) {
                parentNode.children.remove(entityReference);
                // Cleanup non-extension pages that have no children.
                if (parentNode.children.isEmpty() && parentNode.customizedLocales == null) {
                    removeNode(parentReference);
                }
            }
        }
        this.nodes.remove(entityReference);
    }

    private EntityReference getParentReference(EntityReference entityReference)
    {
        if (entityReference.getType() == EntityType.DOCUMENT) {
            DocumentReference documentReference = (DocumentReference) entityReference;
            SpaceReference spaceReference = documentReference.getLastSpaceReference();
            String defaultDocumentName =
                this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName();
            if (documentReference.getName().equals(defaultDocumentName)) {
                if (spaceReference.getParent().getType() == EntityType.SPACE) {
                    // Nested page: the parent is the home page of the parent space.
                    return new DocumentReference(defaultDocumentName, new SpaceReference(spaceReference.getParent()));
                } else {
                    // Top level page: the parent is the wiki.
                    return documentReference.getWikiReference();
                }
            } else {
                // Terminal page: the parent is the space home page.
                return new DocumentReference(defaultDocumentName, spaceReference);
            }
        }

        return null;
    }
}
