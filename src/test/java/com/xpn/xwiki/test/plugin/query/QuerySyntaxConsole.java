package com.xpn.xwiki.test.plugin.query;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import javax.jcr.query.Query;

import org.apache.jackrabbit.core.query.QueryParser;
import org.apache.jackrabbit.core.query.QueryRootNode;

import com.xpn.xwiki.plugin.query.XWikiNamespaceResolver;

/** Show query tree and JCRSQL query for entered xpath query */
public class QuerySyntaxConsole {
	public static void main(String[] args) {
		try {			
			System.out.print(">");
			BufferedReader bif = new BufferedReader( new InputStreamReader( System.in ) );			
			for (;;) {
				String s = bif.readLine();
				QueryRootNode qrn = QueryParser.parse(s, Query.XPATH, XWikiNamespaceResolver.getInstance());
				System.out.println("dump query:\n" + qrn.dump());
				System.out.println("jcrsql query:\n" + QueryParser.toString(qrn, Query.SQL, XWikiNamespaceResolver.getInstance()));
				
				System.out.print("-------------------\n>");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
