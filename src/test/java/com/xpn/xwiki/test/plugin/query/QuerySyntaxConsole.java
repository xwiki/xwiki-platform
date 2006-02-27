/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 *
 * @author amelentev
 */
package com.xpn.xwiki.test.plugin.query;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import javax.jcr.query.Query;

import org.apache.jackrabbit.core.query.LocationStepQueryNode;
import org.apache.jackrabbit.core.query.NodeTypeQueryNode;
import org.apache.jackrabbit.core.query.QueryParser;
import org.apache.jackrabbit.core.query.QueryRootNode;
import org.apache.jackrabbit.name.QName;

import com.xpn.xwiki.plugin.query.XWikiNamespaceResolver;

/** Show query tree and JCRSQL query for entered xpath query */
public class QuerySyntaxConsole {
	public static void main(String[] args) {
		String ql = Query.XPATH;
		try {			
			//System.out.print(ql+">");
			BufferedReader bif = new BufferedReader( new InputStreamReader( System.in ) );			
			for (;;) {
				System.out.print("-------------------\n"+ql+">");
				String s = bif.readLine();
				if (s.equals(""))
					continue;
				if (s.equals(Query.XPATH)) {
					ql = Query.XPATH;
					continue;
				} else if (s.equals(Query.SQL)) {
					ql = Query.SQL;
					continue;
				}
				QueryRootNode qrn = QueryParser.parse(s, ql, XWikiNamespaceResolver.getInstance());
				//((LocationStepQueryNode)qrn.getLocationNode().getOperands()[0]).addOperand(new NodeTypeQueryNode(qrn.getLocationNode().getOperands()[0], QName.fromJCRName("xwiki:type", XWikiNamespaceResolver.getInstance()) ));
				System.out.println("dump query:\n" + qrn.dump());
				System.out.println("jcrsql query:\n" + QueryParser.toString(qrn, Query.SQL, XWikiNamespaceResolver.getInstance()));
				System.out.println("xpath query:\n" + QueryParser.toString(qrn, Query.XPATH, XWikiNamespaceResolver.getInstance()));			
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
