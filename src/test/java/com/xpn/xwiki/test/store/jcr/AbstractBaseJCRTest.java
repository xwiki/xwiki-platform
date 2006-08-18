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
package com.xpn.xwiki.test.store.jcr;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.jcr.NodeIterator;

import junit.framework.TestCase;

import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.jcr.IJcrProvider;
import com.xpn.xwiki.store.jcr.XWikiJcrSession;

public abstract class AbstractBaseJCRTest extends TestCase {	
	protected XWikiConfig	config;
	protected XWikiContext	context;
	protected IJcrProvider	jcr;
	
	public AbstractBaseJCRTest(String arg0) {
		super(arg0);
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		config = new XWikiConfig();
		getConfig();
    	context = new XWikiContext();
    	context.setDatabase("xwikitest");    	
    	jcr = getJCR();
    	jcr.initWorkspace(context.getDatabase());
    	XWikiJcrSession ses = jcr.getSession(context.getDatabase());
    	try {
    		NodeIterator ni = ses.getRootNode().getNodes("test");
    		while (ni.hasNext())
    			ni.nextNode().remove();
    		ses.save();
    	} finally {
    		ses.logout();
    	}
	}
	protected void tearDown() throws Exception {
		jcr.shutdown();
		super.tearDown();
	}
	protected void getConfig() {
		config.put("xwiki.store.jcr.provider",						"com.xpn.xwiki.store.jcr.JackRabbitJCRProvider");
    	config.put("xwiki.store.jcr.jackrabbit.repository.config",	"jackrabbit/repository.xml");
    	config.put("xwiki.store.jcr.jackrabbit.repository.path",	"jackrabbitrepo");
    	config.put("xwiki.store.jcr.jackrabbit.nodetypes.config",	"jackrabbit/nodetypes.cnd");
	}
	
	protected IJcrProvider getJCR() throws SecurityException, NoSuchMethodException, ClassNotFoundException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		String sprovider = config.getProperty("xwiki.store.jcr.provider");
		Constructor cn = Class.forName(sprovider).getConstructor(new Class[]{XWikiConfig.class, XWikiContext.class});
		IJcrProvider jcr = (IJcrProvider) cn.newInstance(new Object[]{config, null});
		return jcr;
	}
}
