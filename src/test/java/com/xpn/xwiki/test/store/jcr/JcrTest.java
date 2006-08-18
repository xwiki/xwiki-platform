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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.compact.ParseException;
import org.apache.portals.graffito.jcr.exception.PersistenceException;

import com.xpn.xwiki.store.jcr.XWikiJcrSession;

public class JcrTest extends AbstractBaseJCRTest {
    public JcrTest(String arg0) {
		super(arg0);
	}
	public void testConnect() throws LoginException, InvalidNodeTypeDefException, IOException, RepositoryException, ParseException, SecurityException, IllegalArgumentException, NoSuchMethodException, ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
    	XWikiJcrSession ses = jcr.getSession(context.getDatabase());
    	try {
    		Node n = ses.getStoreNode();
    		System.out.println(n.getPath());
    	} finally {
    		ses.logout();
    	}
    }
    public void testRefDel() throws LoginException, RepositoryException {
    	XWikiJcrSession ses = jcr.getSession(context.getDatabase());
    	try {
    		Node rn = ses.getRootNode().addNode("test");
    		Node tn1 = rn.addNode("tn1"); tn1.addMixin("mix:referenceable");
    		Node tn2 = rn.addNode("tn2"); tn2.addMixin("mix:referenceable");
    		tn1.setProperty("ref", tn2);
    		tn2.setProperty("ref", tn1);
    		ses.save();
    		System.out.println(tn1.getProperty("ref"));
    		tn1.remove();
    		System.out.println(tn2.getProperty("ref"));
    		try {
    			ses.save();
    		} catch (PersistenceException e) {
    			assertEquals(e.getCause().getClass(), ReferentialIntegrityException.class);
    		}
    	} finally {
    		try {
    			ses.logout();
    		} catch (PersistenceException e) {
    			assertEquals(e.getCause().getClass(), ReferentialIntegrityException.class);
    		}
    	}
    }
    public void testQuery() throws LoginException, RepositoryException {
    	XWikiJcrSession ses = jcr.getSession(context.getDatabase());
    	try {
    		Node rn = ses.getRootNode().addNode("test");
    		Node tn1 = rn.addNode("tn1"); tn1.addMixin("mix:referenceable");
    		Node tn2 = rn.addNode("tn2"); tn2.addMixin("mix:referenceable");
    		tn1.setProperty("ref", tn2);
    		tn2.setProperty("ref", tn1);
    		ses.save();
    		
    		QueryResult qr = ses.getQueryManager().createQuery("test/tn1/jcr:deref(@ref, '*')", Query.XPATH).execute();
    		NodeIterator ni = qr.getNodes();
    		/*while (ni.hasNext()) {
    			System.out.println( ni.nextNode().getPath() );
    		}*/
    		assertEquals( 1, ni.getSize());
    		// nested axis is planned in jackrabbit 1.1
    		//QueryResult qr = ses.getQueryManager().createQuery("test[@node2/testprop='4']/testprop", Query.XPATH).execute();
    		//System.out.println( qr.getRows().nextRow() );
    	} finally {
    		ses.logout();
    	}
    }
}
