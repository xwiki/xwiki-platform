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
package org.xwiki.container.internal;

import java.util.Stack;

import org.xwiki.component.annotation.Component;
import org.xwiki.container.ApplicationContext;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.container.Response;
import org.xwiki.container.Session;

/**
 * We're using ThreadLocals to store the request, response and session so that each thread 
 * (i.e. each user request) has its own value for these objects. In addition we sometime need
 * to create a new request, response or session even while in the same thread. For this use case
 * we've added the possibility to push/pop different implementations for these Objects.
 */
@Component
public class DefaultContainer implements Container
{
    private ApplicationContext applicationContext;
    private ThreadLocal<Stack<Request>> request = new ThreadLocal<Stack<Request>>();
    private ThreadLocal<Stack<Response>> response = new ThreadLocal<Stack<Response>>();
    private ThreadLocal<Stack<Session>> session = new ThreadLocal<Stack<Session>>();

    public ApplicationContext getApplicationContext()
    {
        return this.applicationContext;
    }

    public void pushRequest(Request request)
    {
        this.request.get().push(request);
    }
    
    public void popRequest()
    {
        this.request.get().pop();
    }
    
    public Request getRequest()
    {
        return this.request.get().peek();
    }

    public Response getResponse()
    {
        return this.response.get().peek();
    }

    public void pushResponse(Response response)
    {
        this.response.get().push(response);
    }
    
    public void popResponse()
    {
        this.response.get().pop();
    }

    public Session getSession()
    {
        return this.session.get().peek();
    }

    public void pushSession(Session session)
    {
        this.session.get().push(session);
    }
    
    public void popSession()
    {
        this.session.get().pop();
    }

    public void setApplicationContext(ApplicationContext context)
    {
        this.applicationContext = context;
    }

    public void setRequest(Request request)
    {
        Stack<Request> stack = new Stack<Request>();
        stack.push(request);
        this.request.set(stack);
    }
    
    public void removeRequest()
    {
        this.request.remove();
    }

    public void setResponse(Response response)
    {
        Stack<Response> stack = new Stack<Response>();
        stack.push(response);
        this.response.set(stack);
    }

    public void removeResponse()
    {
        this.response.remove();
    }

    public void setSession(Session session)
    {
        Stack<Session> stack = new Stack<Session>();
        stack.push(session);
        this.session.set(stack);
    }

    public void removeSession()
    {
        this.session.remove();
    }
}
