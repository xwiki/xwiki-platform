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
package org.xwiki.query.xwql.internal.hql;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.query.internal.jpql.node.Start;
import org.xwiki.query.jpql.internal.JPQLParser;
import org.xwiki.query.xwql.internal.QueryAnalyzer;
import org.xwiki.query.xwql.internal.QueryContext;
import org.xwiki.query.xwql.internal.QueryTranslator;
import org.xwiki.query.Query;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;

@Component
@Named("hql")
@Singleton
public class XWQLtoHQLTranslator implements QueryTranslator
{
    @Inject
    protected DocumentAccessBridge documentAccessBridge;

    @Override
    public String translate(String input) throws Exception
    {
        input = input.trim();
        String lcInput = input.toLowerCase();
        String addition = "select doc.fullName from Document as doc ";
        if (lcInput.startsWith("where") || lcInput.startsWith("order") || lcInput.length() == 0) {
            input = addition + input;
        } else if (lcInput.startsWith("from")) {
            input = addition + "," + input.substring(4);
        }
        JPQLParser parser = new JPQLParser();
        Start tree = parser.parse(input);
        QueryContext context = new QueryContext(tree, getDocumentAccessBridge());
        // analize query and store info in context
        tree.apply(new QueryAnalyzer(context));

        Printer printer = getPrinter(context);
        return printer.print();
    }

    @Override
    public String getOutputLanguage()
    {
        return Query.HQL;
    }

    protected Printer getPrinter(QueryContext context)
    {
        return new Printer(context, this);
    }

    public DocumentAccessBridge getDocumentAccessBridge()
    {
        return documentAccessBridge;
    }
}
