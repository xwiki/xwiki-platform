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
package org.xwiki.index.tree.internal.nestedpages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.tree.TreeNode;

/**
 * The document tree node.
 * 
 * @version $Id$
 * @since 8.3M2, 7.4.5
 */
@Component
@Named("document")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DocumentTreeNode extends AbstractDocumentTreeNode implements Initializable
{
    private static final String FIELD_TITLE = "title";

    private static final String PARAMETER_LOCALE = "locale";

    @Inject
    @Named("count")
    protected QueryFilter countQueryFilter;

    @Inject
    @Named("hidden/document")
    protected QueryFilter hiddenDocumentQueryFilter;

    @Inject
    private LocalizationContext localizationContext;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    @Named("context")
    private Provider<ComponentManager> contextComponentManagerProvider;

    @Inject
    @Named("childPage/nestedPages")
    private QueryFilter childPageFilter;

    @Inject
    @Named("hiddenPage/nestedPages")
    private QueryFilter hiddenPageFilter;

    @Inject
    @Named("documentReferenceResolver/nestedPages")
    private QueryFilter documentReferenceResolverFilter;

    /**
     * We use a {@link LinkedHashMap} because the order of the key is important.
     */
    private Map<String, TreeNode> nonLeafChildNodes = new LinkedHashMap<String, TreeNode>();

    /**
     * Default constructor.
     */
    public DocumentTreeNode()
    {
        super("document");
    }

    @Override
    public void initialize() throws InitializationException
    {
        String[] nonLeafChildNodeTypes = new String[] {"translations", "attachments", "classProperties", "objects"};
        ComponentManager contextComponentManager = this.contextComponentManagerProvider.get();
        try {
            for (String nonLeafChildNodeType : nonLeafChildNodeTypes) {
                TreeNode treeNode = contextComponentManager.getInstance(TreeNode.class, nonLeafChildNodeType);
                this.nonLeafChildNodes.put(nonLeafChildNodeType, treeNode);
            }
        } catch (ComponentLookupException e) {
            throw new InitializationException("Failed to lookup the child components.", e);
        }
    }

    @Override
    protected List<String> getChildren(DocumentReference documentReference, int offset, int limit) throws Exception
    {
        List<String> children = new ArrayList<String>();
        String serializedDocRef = this.defaultEntityReferenceSerializer.serialize(documentReference);

        if (offset == 0) {
            for (Map.Entry<String, TreeNode> entry : this.nonLeafChildNodes.entrySet()) {
                if (hasChild(entry.getKey(), entry.getValue(), documentReference)) {
                    children.add(entry.getKey() + ':' + serializedDocRef);
                }
            }

            if (showAddDocument(documentReference)) {
                children.add("addDocument:" + serializedDocRef);
            }
        }

        children.addAll(serialize(getChildDocuments(documentReference, offset, limit)));

        return children;
    }

    protected List<DocumentReference> getChildDocuments(DocumentReference documentReference, int offset, int limit)
        throws QueryException
    {
        if (!getDefaultDocumentName().equals(documentReference.getName())) {
            return Collections.emptyList();
        }

        String orderBy = getOrderBy();
        Query query;
        if (areTerminalDocumentsShown()) {
            if (FIELD_TITLE.equals(orderBy)) {
                query = this.queryManager.getNamedQuery("nestedPagesOrderedByTitle");
                query.bindValue(PARAMETER_LOCALE, this.localizationContext.getCurrentLocale().toString());
            } else {
                query = this.queryManager.getNamedQuery("nestedPagesOrderedByName");
            }
        } else {
            if (FIELD_TITLE.equals(orderBy)) {
                query = this.queryManager.getNamedQuery("nonTerminalPagesOrderedByTitle");
                query.bindValue(PARAMETER_LOCALE, this.localizationContext.getCurrentLocale().toString());
            } else {
                // Query only the spaces table.
                query = this.queryManager.createQuery(
                    "select reference, 0 as terminal from XWikiSpace page order by lower(name), name", Query.HQL);
            }
        }

        query.setWiki(documentReference.getWikiReference().getName());
        query.setOffset(offset);
        query.setLimit(limit);

        query.addFilter(this.childPageFilter);
        query.bindValue("parent", this.localEntityReferenceSerializer.serialize(documentReference.getParent()));

        if (!areHiddenEntitiesShown()) {
            query.addFilter(this.hiddenPageFilter);
        }

        return query.addFilter(this.documentReferenceResolverFilter).execute();
    }

    @Override
    protected int getChildCount(DocumentReference documentReference) throws Exception
    {
        int count = 0;
        for (Map.Entry<String, TreeNode> entry : this.nonLeafChildNodes.entrySet()) {
            if (hasChild(entry.getKey(), entry.getValue(), documentReference)) {
                count++;
            }
        }

        if (showAddDocument(documentReference)) {
            count++;
        }

        return count + getChildDocumentsCount(documentReference);
    }

    protected int getChildDocumentsCount(DocumentReference documentReference) throws QueryException
    {
        if (!getDefaultDocumentName().equals(documentReference.getName())) {
            return 0;
        }

        int count = getChildSpacesCount(documentReference);
        if (areTerminalDocumentsShown()) {
            count += getChildTerminalPagesCount(documentReference);
        }
        return count;
    }

    private int getChildTerminalPagesCount(DocumentReference documentReference) throws QueryException
    {
        Query query = this.queryManager
            .createQuery("where doc.translation = 0 and doc.space = :space and doc.name <> :defaultDocName", Query.HQL);
        query.addFilter(this.countQueryFilter);
        if (Boolean.TRUE.equals(getProperties().get("filterHiddenDocuments"))) {
            query.addFilter(this.hiddenDocumentQueryFilter);
        }
        query.setWiki(documentReference.getWikiReference().getName());
        query.bindValue("space", this.localEntityReferenceSerializer.serialize(documentReference.getParent()));
        query.bindValue("defaultDocName", getDefaultDocumentName());
        return ((Long) query.execute().get(0)).intValue();
    }

    @Override
    protected EntityReference getParent(DocumentReference documentReference) throws Exception
    {
        if (getDefaultDocumentName().equals(documentReference.getName())) {
            EntityReference parentReference = documentReference.getParent().getParent();
            if (parentReference.getType() == EntityType.SPACE) {
                return new DocumentReference(getDefaultDocumentName(), new SpaceReference(parentReference));
            } else {
                return parentReference;
            }
        } else {
            return new DocumentReference(getDefaultDocumentName(), documentReference.getLastSpaceReference());
        }
    }

    private boolean showAddDocument(DocumentReference documentReference)
    {
        return Boolean.TRUE.equals(getProperties().get("showAddDocument"))
            && "reference".equals(getProperties().get("hierarchyMode"))
            && getDefaultDocumentName().equals(documentReference.getName())
            && this.authorization.hasAccess(Right.EDIT, documentReference.getParent());
    }

    private boolean hasChild(String nodeType, TreeNode childNode, DocumentReference documentReference)
    {
        return hasChild(nodeType, childNode, this.defaultEntityReferenceSerializer.serialize(documentReference));
    }

    private boolean hasChild(String nodeType, TreeNode childNode, String serializedDocumentReference)
    {
        String showChild = "show" + StringUtils.capitalize(nodeType);
        if (Boolean.TRUE.equals(getProperties().get(showChild))) {
            String nodeId = nodeType + ':' + serializedDocumentReference;
            childNode.getProperties().putAll(getProperties());
            return childNode.getChildCount(nodeId) > 0;
        }
        return false;
    }
}
