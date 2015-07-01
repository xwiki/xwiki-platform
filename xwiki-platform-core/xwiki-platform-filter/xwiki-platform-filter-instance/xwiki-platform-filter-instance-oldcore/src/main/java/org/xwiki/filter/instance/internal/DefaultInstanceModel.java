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
package org.xwiki.filter.instance.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.filter.FilterException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.EntityReferenceTree;
import org.xwiki.model.reference.EntityReferenceTreeNode;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.SpaceReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * @version $Id$
 * @since 6.2M1
 */
@Component
@Singleton
public class DefaultInstanceModel implements InstanceModel
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private QueryManager queryManager;

    @Inject
    private SpaceReferenceResolver<String> spaceResolver;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localSerializer;

    @Override
    public List<WikiReference> getWikiReferences() throws FilterException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        try {
            List<String> wikis = xcontext.getWiki().getVirtualWikisDatabaseNames(xcontext);

            List<WikiReference> wikiReferences = new ArrayList<>(wikis.size());
            for (String wikiName : wikis) {
                wikiReferences.add(new WikiReference(new WikiReference(wikiName)));
            }
            Collections.sort(wikis);

            return wikiReferences;
        } catch (XWikiException e) {
            throw new FilterException("Failed to get the list of wikis", e);
        }
    }

    @Override
    public EntityReferenceTreeNode getSpaceReferences(WikiReference wikiReference) throws FilterException
    {
        // Get the spaces
        List<String> spaceReferenceStrings;
        try {
            spaceReferenceStrings =
                this.queryManager.getNamedQuery("getSpaces").setWiki(wikiReference.getName()).execute();
        } catch (QueryException e) {
            throw new FilterException(String.format("Failed to get the list of spaces in wiki [%s]", wikiReference), e);
        }

        // Get references
        List<SpaceReference> spaceReferences = new ArrayList<>(spaceReferenceStrings.size());
        for (String spaceReferenceString : spaceReferenceStrings) {
            spaceReferences.add(this.spaceResolver.resolve(spaceReferenceString, wikiReference));
        }

        // Create the tree
        EntityReferenceTree tree = new EntityReferenceTree(spaceReferences);

        return tree.getChildren().iterator().next();
    }

    @Override
    public List<DocumentReference> getDocumentReferences(SpaceReference spaceReference) throws FilterException
    {
        List<String> documentNames;
        try {
            Query query =
                this.queryManager.createQuery(
                    "select distinct doc.name from Document doc where doc.space = :space order by doc.name asc",
                    Query.XWQL);
            query.bindValue("space", localSerializer.serialize(spaceReference));
            query.setWiki(spaceReference.getWikiReference().getName());

            documentNames = query.execute();
        } catch (QueryException e) {
            throw new FilterException(
                String.format("Failed to get the list of documents in space [%s]", spaceReference), e);
        }

        List<DocumentReference> documentReferences = new ArrayList<>(documentNames.size());
        for (String documentName : documentNames) {
            documentReferences.add(new DocumentReference(documentName, spaceReference));
        }

        return documentReferences;
    }
}
