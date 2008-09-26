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
package com.xpn.xwiki.plugin.charts.exceptions;

import java.io.PrintStream;
import java.io.PrintWriter;

public class ChartingException extends Exception
{
    public ChartingException()
    {
        super();
    }

    public ChartingException(String message)
    {
        super(message);
    }

    public ChartingException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public ChartingException(Throwable cause)
    {
        super(cause);
    }

    @Override
    public String getMessage()
    {
        if (getCause() != null) {
            return getCause().getMessage();
        } else {
            return super.getMessage();
        }
    }

    @Override
    public void printStackTrace()
    {
        if (getCause() != null) {
            getCause().printStackTrace();
        } else {
            super.printStackTrace();
        }
    }

    @Override
    public void printStackTrace(PrintStream s)
    {
        if (getCause() != null) {
            getCause().printStackTrace(s);
        } else {
            super.printStackTrace(s);
        }
    }

    @Override
    public void printStackTrace(PrintWriter s)
    {
        if (getCause() != null) {
            getCause().printStackTrace(s);
        } else {
            super.printStackTrace(s);
        }
    }
}
