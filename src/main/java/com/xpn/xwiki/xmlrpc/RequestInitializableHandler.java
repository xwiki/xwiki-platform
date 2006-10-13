package com.xpn.xwiki.xmlrpc;

import com.xpn.xwiki.XWikiException;
import org.apache.xmlrpc.common.XmlRpcHttpRequestConfigImpl;

import javax.servlet.Servlet;
import javax.servlet.ServletRequest;

public interface RequestInitializableHandler {
	public void init(Servlet servlet, ServletRequest request)
	        throws XWikiException;

	static class Config extends XmlRpcHttpRequestConfigImpl {
		private ServletRequest request;

		public ServletRequest getRequest() {
			return request;
		}

		public void setRequest(ServletRequest request) {
			this.request = request;
		}
	}
}
