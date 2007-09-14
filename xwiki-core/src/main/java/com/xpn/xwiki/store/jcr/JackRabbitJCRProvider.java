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
 *
 */
package com.xpn.xwiki.store.jcr;

import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiEngineContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.WorkspaceImpl;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistryListener;
import org.apache.jackrabbit.core.nodetype.compact.ParseException;
import org.apache.jackrabbit.name.QName;
import org.apache.portals.graffito.jcr.mapper.Mapper;
import org.apache.portals.graffito.jcr.mapper.impl.DigesterMapperImpl;
import org.apache.portals.graffito.jcr.persistence.PersistenceManager;
import org.apache.portals.graffito.jcr.persistence.atomictypeconverter.AtomicTypeConverterProvider;
import org.apache.portals.graffito.jcr.persistence.atomictypeconverter.impl.DefaultAtomicTypeConverterProvider;
import org.apache.portals.graffito.jcr.persistence.impl.PersistenceManagerImpl;
import org.apache.portals.graffito.jcr.persistence.objectconverter.impl.ObjectConverterImpl;
import org.apache.portals.graffito.jcr.query.QueryManager;
import org.apache.portals.graffito.jcr.query.impl.QueryManagerImpl;

import javax.jcr.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

public class JackRabbitJCRProvider implements IJcrProvider {
	static final Log log = LogFactory.getLog(JackRabbitJCRProvider.class);
	Repository repository;
	XWikiConfig config;
	
	Mapper mapper;
	AtomicTypeConverterProvider converterProvider = new DefaultAtomicTypeConverterProvider();
	Map atomicTypeConverters = converterProvider.getAtomicTypeConverters();
	QueryManager queryManager;
	ObjectConverterImpl objectConverter;
	
	XWikiEngineContext econtext;
	
	public JackRabbitJCRProvider(XWikiConfig config, XWikiContext context) throws IOException, LoginException, RepositoryException, InvalidNodeTypeDefException, ParseException {
		this.config = config;
		if (context!=null)
			this.econtext = context.getEngineContext();
		mapper = new DigesterMapperImpl(getRealConfigPath("xwiki.store.jcr.mapping", "jcrmapping.xml"));
		queryManager = new QueryManagerImpl(mapper, atomicTypeConverters);
		objectConverter = new ObjectConverterImpl(mapper, converterProvider);
		String	jrconfig	= getRealConfigPath("xwiki.store.jcr.jackrabbit.repository.config",	"jackrabbit/repository.xml"),
				jcrepo		= getRealConfigPath("xwiki.store.jcr.jackrabbit.repository.path",	"jackrabbitrepo");
		log.info("Starting jackrabbit with config:" + jrconfig + ". On " +  jcrepo);
		repository = new TransientRepository(jrconfig, jcrepo);
	}
	
	private XWikiConfig getConfig() {
		return config;
	}
	
	public XWikiJcrSession getSession(String workspace) throws LoginException, RepositoryException {
		return getSession(workspace, "xwiki", "xwiki");
	}
	
	public XWikiJcrSession getSession(String workspace, String username, String password) throws LoginException, RepositoryException {
		Session s = repository.login( new SimpleCredentials(username, password.toCharArray()), workspace);
		PersistenceManager pm = new PersistenceManagerImpl(mapper, objectConverter, queryManager, s);
		return new XWikiJcrSession(s, pm, objectConverter);
	}
	
	String getRealConfigPath(String param, String defval) {
		param = getConfig().getProperty(param, defval);
		if (econtext==null)
			return new File(".").getAbsolutePath() + "/" + param;
		return econtext.getRealPath(param);
	}
	
	public boolean initWorkspace(String workspace) throws RepositoryException, IOException {		
		Session s0 = repository.login(new SimpleCredentials("xwiki", "xwiki".toCharArray()));
		try {
			WorkspaceImpl defworkspace = (WorkspaceImpl) s0.getWorkspace();
			try {
				defworkspace.createWorkspace(workspace);
				log.info("Workspace "+workspace+" created!");
			} catch (RepositoryException e) {
				log.info("Workspace "+workspace+" already exists");
			}
		} finally {
			s0.logout();
		}
		//TODO: namespace and nodetypes in one compact node def.
		XWikiJcrSession s = getSession(workspace);
		try {
			registerNamespace(s.getWorkspace(), "xwiki", "http://www.xwiki.org");
			registerNamespace(s.getWorkspace(), "xp", "http://www.xwiki.org/property");
			registerNamespace(s.getWorkspace(), "graffito", "http://incubator.apache.org/graffito");
			registerNodeTypes(s.getWorkspace(), getRealConfigPath("xwiki.store.jcr.jackrabbit.nodetypes.config", "jackrabbit/nodetypes.cnd"));
			s.save();
		} catch (RepositoryException e) {
			log.info("Node types not registered", e);
			return false;
		} finally {
			s.logout();
		}
		return true;
	}
	private void registerNodeTypes(Workspace ws, String cndFileName) throws RepositoryException, IOException {
		NodeTypeManagerImpl ntm = (NodeTypeManagerImpl) ws.getNodeTypeManager();
		FileInputStream fis = new FileInputStream(cndFileName);
		ntm.getNodeTypeRegistry().addListener(new NodeTypeRegistryListener() {
			public void nodeTypeRegistered(QName ntName) {
				log.info("node "+ntName+" registred");
			}
			public void nodeTypeReRegistered(QName ntName) {} // not implemented in jackrabbit
			public void nodeTypeUnregistered(QName ntName) {}			
		});
		ntm.registerNodeTypes(fis, NodeTypeManagerImpl.TEXT_X_JCR_CND);
	}
	private boolean registerNamespace(Workspace w, String preffix, String uri) throws UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
		try {
			w.getNamespaceRegistry().registerNamespace(preffix, uri);
			log.info("namespace '"+preffix+"' registered!");
		} catch (NamespaceException e) {
			return false;
		};
		return true;
	}

	public void shutdown() {
		// not needed becouse TransientRepository 
	}
}
