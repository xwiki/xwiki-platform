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
package org.xwiki.wikistream.instance.internal.input;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.instance.input.AbstractInstanceInputEventGenerator;
import org.xwiki.wikistream.instance.input.EntityEventGenerator;
import org.xwiki.wikistream.instance.internal.XWikiDocumentFilter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

@Component
@Named("documents")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DocumentInstanceInputEventGenerator extends AbstractInstanceInputEventGenerator<XWikiDocumentFilter>
{
    @Inject
    private EntityEventGenerator<XWikiDocument> documentParser;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private QueryManager queryManager;

    @Override
    public void beginWikiSpace(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        super.beginWikiSpace(name, parameters);

        List<String> documents;
        try {
            documents =
                this.queryManager
                    .createQuery("select distinct doc.name from Document doc where doc.space=:space", Query.XWQL)
                    .bindValue("space", this.currentSpaces.peek()).setWiki(this.currentWiki).execute();
        } catch (QueryException e) {
            throw new WikiStreamException("Failed to get documents for space [" + this.currentSpaces.peek()
                + "] and wiki [" + this.currentWiki + "]", e);
        }

        XWikiContext xcontext = this.xcontextProvider.get();

        for (String documentName : documents) {
            DocumentReference reference = new DocumentReference(this.currentWiki, this.currentSpaces, documentName);

            XWikiDocument document;
            try {
                document = xcontext.getWiki().getDocument(reference, xcontext);
            } catch (XWikiException e) {
                throw new WikiStreamException("Failed to get document [" + reference + "]", e);
            }

            this.documentParser.write(document, document, this.properties);
        }
    }
}
