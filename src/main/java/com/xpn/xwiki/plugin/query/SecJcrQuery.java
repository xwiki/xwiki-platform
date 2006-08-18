package com.xpn.xwiki.plugin.query;

public class SecJcrQuery extends JcrQuery {
	public SecJcrQuery(String query, String language, IQueryFactory qf) {
		super(query, language, qf);
	}
	// TODO: security!
}
