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
        Stack<Request> requests = this.request.get();
        if (requests != null) {
            requests.push(request);
        }
    }
    
    public void popRequest()
    {
        Stack<Request> requests = this.request.get();
        if (requests != null) {
            requests.pop();
        }
    }
    
    public Request getRequest()
    {
        Request result = null;
        Stack<Request> requests = this.request.get();
        if (requests != null) {
            result = requests.peek();
        }
        return result;
    }

    public Response getResponse()
    {
        Response result = null;
        Stack<Response> responses = this.response.get();
        if (responses != null) {
            result = responses.peek();
        }
        return result;
    }

    public void pushResponse(Response response)
    {
        Stack<Response> responses = this.response.get();
        if (responses != null) {
            responses.push(response);
        }
    }
    
    public void popResponse()
    {
        Stack<Response> responses = this.response.get();
        if (responses != null) {
            responses.pop();
        }
    }

    public Session getSession()
    {
        Session result = null;
        Stack<Session> sessions = this.session.get();
        if (sessions != null) {
            result = sessions.peek();
        }
        return result;
    }

    public void pushSession(Session session)
    {
        Stack<Session> sessions = this.session.get();
        if (sessions != null) {
            sessions.push(session);
        }
    }
    
    public void popSession()
    {
        Stack<Session> sessions = this.session.get();
        if (sessions != null) {
            sessions.pop();
        }
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
