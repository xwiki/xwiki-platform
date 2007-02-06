package com.xpn.xwiki.xmlrpc;

import com.xpn.xwiki.XWikiException;

import java.util.Map;

public interface ConfluenceRpcInterface {

	public String login(String username, String password) throws XWikiException;

	public boolean logout(String token) throws XWikiException;

	public Object[] getSpaces(String token) throws XWikiException;

	public Map getSpace(String token, String spaceKey) throws XWikiException;

	public Object[] getPages(String token, String spaceKey) throws XWikiException;

	public Map getPage(String token, String pageId) throws XWikiException;

	public Object[] getPageHistory(String token, String pageId) throws XWikiException;

	public Object[] search(String token, String query, int maxResults) throws XWikiException;

	public String renderContent(String token, String spaceKey, String pageId, String content);

	public Object[] getAttachments(String token, String pageId) throws XWikiException;

	public Object[] getComments(String token, String pageId) throws XWikiException;

	public Map storePage(String token, Map pageht) throws XWikiException;

	public void deletePage(String token, String pageId) throws XWikiException;

	public Map getUser(String token, String username);

	public void addUser(String token, Map user, String password);

	public void addGroup(String token, String group);

	public Object[] getUserGroups(String token, String username);

	public void addUserToGroup(String token, String username, String groupname);
	
	public Map addSpace(String token, Map spaceProperties) throws XWikiException;
}
