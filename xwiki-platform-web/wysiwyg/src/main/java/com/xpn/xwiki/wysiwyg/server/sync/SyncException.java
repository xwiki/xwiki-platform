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
package com.xpn.xwiki.wysiwyg.server.sync;

import java.io.PrintStream;
import java.io.PrintWriter;

import javax.servlet.ServletException;

import org.apache.velocity.exception.MethodInvocationException;
import org.hibernate.JDBCException;

import com.google.gwt.user.client.rpc.IsSerializable;

public class SyncException extends Exception implements IsSerializable
{
    private String message;

    private Exception exception;

    public SyncException()
    {
        super();
    }

    public SyncException(String message, Exception e)
    {
        super();
        this.setMessage(message);
        this.setException(e);
    }

    public String getMessage()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append(message);
        if (exception != null) {
            buffer.append("\nWrapped Exception: ");
            buffer.append(exception.getMessage());
        }
        return buffer.toString();
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public Exception getException()
    {
        return exception;
    }

    public void setException(Exception e)
    {
        this.exception = e;
    }

    public void printStackTrace(PrintWriter s)
    {
        super.printStackTrace(s);
        if (exception != null) {
            s.write("\n\nWrapped Exception:\n\n");
            if (exception.getCause() != null)
                exception.getCause().printStackTrace(s);
            else if (exception instanceof org.hibernate.JDBCException) {
                (((JDBCException) exception).getSQLException()).printStackTrace(s);
            } else if (exception instanceof MethodInvocationException) {
                (((MethodInvocationException) exception).getWrappedThrowable()).printStackTrace(s);
            } else if (exception instanceof ServletException) {
                (((ServletException) exception).getRootCause()).printStackTrace(s);
            } else {
                exception.printStackTrace(s);
            }
        }
    }

    public void printStackTrace(PrintStream s)
    {
        super.printStackTrace(s);
        if (exception != null) {
            s.print("\n\nWrapped Exception:\n\n");
            if (exception.getCause() != null)
                exception.getCause().printStackTrace(s);
            else if (exception instanceof org.hibernate.JDBCException) {
                (((JDBCException) exception).getSQLException()).printStackTrace(s);
            } else if (exception instanceof MethodInvocationException) {
                (((MethodInvocationException) exception).getWrappedThrowable()).printStackTrace(s);
            } else if (exception instanceof ServletException) {
                (((ServletException) exception).getRootCause()).printStackTrace(s);
            } else {
                exception.printStackTrace(s);
            }
        }
    }
}
