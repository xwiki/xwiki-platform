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
package org.xwiki.index.tree.internal.nestedspaces.parentchild;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.index.tree.internal.AbstractDocumentTreeNode;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.tree.TreeNode;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * The document node in the parent-child over nested spaces hierarchy.
 * 
 * @version $Id$
 * @since 8.3M2
 * @since 7.4.5
 */
@Component
@Named("document/parentChildOnNestedSpaces")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DocumentTreeNode extends AbstractDocumentTreeNode
{
    @Inject
    @Named("childDocuments/parentChildOnNestedSpaces")
    private TreeNode childDocuments;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public void initialize() throws InitializationException
    {
        super.initialize();
        this.childNodes.addTreeNode(this.childDocuments, nodeId -> true);
    }

    @Override
    protected EntityReference getParent(DocumentReference documentReference) throws Exception
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiDocument document = xcontext.getWiki().getDocument(documentReference, xcontext);
        DocumentReference parentReference = document.getParentReference();
        // The parent document must be on the same space.
        if (parentReference != null && parentReference.getParent().equals(documentReference.getParent())) {
            return parentReference;
        }
        return documentReference.getParent();
    }
}
