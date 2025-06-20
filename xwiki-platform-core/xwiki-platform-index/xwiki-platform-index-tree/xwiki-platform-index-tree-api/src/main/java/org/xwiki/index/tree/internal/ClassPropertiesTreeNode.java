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
package org.xwiki.index.tree.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.model.reference.ClassPropertyReference;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * The class properties tree node.
 * 
 * @version $Id$
 * @since 8.3M2
 * @since 7.4.5
 */
@Component
@Named("classProperties")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class ClassPropertiesTreeNode extends AbstractDocumentRelatedTreeNode
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Default constructor.
     */
    public ClassPropertiesTreeNode()
    {
        super("classProperties");
    }

    @Override
    protected List<String> getChildren(DocumentReference documentReference, int offset, int limit) throws Exception
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiDocument document = xcontext.getWiki().getDocument(documentReference, xcontext);
        List<String> properties = new ArrayList<>(document.getXClass().getPropertyList());
        Collections.sort(properties);
        List<String> children = new ArrayList<>();
        for (String property : subList(properties, offset, limit)) {
            children.add(serialize(new ClassPropertyReference(property, documentReference)));
        }
        return children;
    }

    @Override
    protected int getChildCount(DocumentReference documentReference) throws Exception
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiDocument document = xcontext.getWiki().getDocument(documentReference, xcontext);
        return document.getXClass().getPropertyList().size();
    }
}
