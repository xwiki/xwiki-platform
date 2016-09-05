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
 */
package org.xwiki.wysiwyg.server;

import java.lang.reflect.Type;

import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.RPC;
import com.google.gwt.user.server.rpc.RPCRequest;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.xpn.xwiki.web.Utils;

/**
 * Generic {@link RemoteServiceServlet} that dispatches the RPC calls to components registered through the component
 * manager.
 * <p>
 * See http://stuffthathappens.com/blog/2009/09/14/guice-with-gwt/.
 * 
 * @version $Id$
 */
public class XWikiRemoteServiceServlet extends RemoteServiceServlet
{
    /**
     * Field required by all {@link java.io.Serializable} classes.
     */
    private static final long serialVersionUID = 1911611911891893986L;

    @Override
    public String processCall(String payload) throws SerializationException
    {
        try {
            RPCRequest req = RPC.decodeRequest(payload, null, this);
            RemoteService service = (RemoteService) Utils.getComponent((Type) req.getMethod().getDeclaringClass());
            return RPC.invokeAndEncodeResponse(service, req.getMethod(), req.getParameters(),
                req.getSerializationPolicy());
        } catch (IncompatibleRemoteServiceException ex) {
            log("IncompatibleRemoteServiceException in the processCall(String) method.", ex);
            return RPC.encodeResponseForFailure(null, ex);
        }
    }
}
