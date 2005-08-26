package com.xpn.xwiki.plugin.charts.mocks;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

public class MockServletContext implements ServletContext {
	private Map attributes = new HashMap();
	
	public ServletContext getContext(String arg0) {
		return null;
	}

	public int getMajorVersion() {
		return 0;
	}

	public int getMinorVersion() {
		return 0;
	}

	public String getMimeType(String arg0) {
		return null;
	}

	public Set getResourcePaths(String arg0) {
		return null;
	}

	public URL getResource(String arg0) throws MalformedURLException {
		return null;
	}

	public InputStream getResourceAsStream(String arg0) {
		return null;
	}

	public RequestDispatcher getRequestDispatcher(String arg0) {
		return null;
	}

	public RequestDispatcher getNamedDispatcher(String arg0) {
		return null;
	}

	public Servlet getServlet(String arg0) throws ServletException {
		return null;
	}

	public Enumeration getServlets() {
		return null;
	}

	public Enumeration getServletNames() {	
		return null;
	}

	public void log(String arg0) {
	}

	public void log(Exception arg0, String arg1) {
	}

	public void log(String arg0, Throwable arg1) {
	}

	public String getRealPath(String arg0) {
		return null;
	}

	public String getServerInfo() {
		return null;
	}

	public String getInitParameter(String arg0) {
		return null;
	}

	public Enumeration getInitParameterNames() {
		return null;
	}

	public Object getAttribute(String name) {
		return attributes.get(name);
	}

	public Enumeration getAttributeNames() {
		return new Vector(attributes.keySet()).elements();
	}

	public void setAttribute(String name, Object value) {
		attributes.put(name, value);
	}

	public void removeAttribute(String name) {
		attributes.remove(name);
	}

	public String getServletContextName() {
		return null;
	}

}
