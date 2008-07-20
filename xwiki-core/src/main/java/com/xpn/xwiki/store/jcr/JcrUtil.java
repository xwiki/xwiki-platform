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

import java.io.IOException;

import org.apache.jackrabbit.util.ISO9075;

import com.xpn.xwiki.util.Util;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

public class JcrUtil {
	public static Node getOrCreateSubNode(Node root, String subnodename, String subnodetype) throws RepositoryException {
		subnodename = ISO9075.encode(subnodename);
		try {
			return root.getNode(subnodename);
		} catch (PathNotFoundException e) {
			return root.addNode(subnodename, subnodetype);
		}
	}
	
	public static Object fromValue(Value v) throws IllegalStateException, IOException, RepositoryException {
	    switch (v.getType()) {
	        case PropertyType.BINARY: return Util.getFileContentAsBytes(v.getStream());
	        case PropertyType.BOOLEAN: return v.getBoolean();
	        case PropertyType.DATE: return v.getDate();
	        case PropertyType.DOUBLE: return v.getDouble();
	        case PropertyType.LONG: return v.getLong();
	        default: return v.getString();
	    }
	}
}
