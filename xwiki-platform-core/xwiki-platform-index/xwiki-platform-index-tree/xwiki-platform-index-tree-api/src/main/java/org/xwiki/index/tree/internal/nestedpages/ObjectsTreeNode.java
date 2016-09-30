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
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * The objects tree node.
 * 
 * @version $Id$
 * @since 8.3M2
 * @since 7.4.5
 */
@Component
@Named("objects")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class ObjectsTreeNode extends AbstractDocumentTreeNode
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Default constructor.
     */
    public ObjectsTreeNode()
    {
        super("objects");
    }

    @Override
    protected List<String> getChildren(DocumentReference documentReference, int offset, int limit) throws Exception
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiDocument document = xcontext.getWiki().getDocument(documentReference, xcontext);
        String serializedDocRef = this.defaultEntityReferenceSerializer.serialize(documentReference);
        List<String> classNames = new ArrayList<String>();
        for (DocumentReference classReference : document.getXObjects().keySet()) {
            classNames.add(this.defaultEntityReferenceSerializer.serialize(classReference));
        }
        Collections.sort(classNames);
        List<String> children = new ArrayList<String>();
        for (String className : subList(classNames, offset, limit)) {
            children.add("objectsOfType:" + serializedDocRef + "/" + className);
        }
        return children;
    }

    @Override
    protected int getChildCount(DocumentReference documentReference) throws Exception
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiDocument document = xcontext.getWiki().getDocument(documentReference, xcontext);
        return document.getXObjects().size();
    }
}
