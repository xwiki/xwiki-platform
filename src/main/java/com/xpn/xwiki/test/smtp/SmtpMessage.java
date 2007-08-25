/*
 * Dumbster: a dummy SMTP server.
 * Copyright (C) 2003, Jason Paul Kitchen
 * lilnottsman@yahoo.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.xpn.xwiki.test.smtp;

import java.util.*;

/**
 * Container for a complete SMTP message - headers and message body.
 */
public class SmtpMessage {
  /** Headers: Map of List of String hashed on header name. */
  private Map headers;
  /** Message body. */
  private StringBuffer body;

  /**
   * Constructor. Initializes headers Map and body buffer.
   */
  public SmtpMessage() {
    headers = new HashMap();
    body = new StringBuffer();
  }

  /**
   * Update the headers or body depending on the SmtpResponse object and line of input.
   * @param response SmtpResponse object
   * @param params remainder of input line after SMTP command has been removed
   */
  public void store(SmtpResponse response, String params) {
    if (params != null) {
      if (SmtpState.DATA_HDR == response.getNextState()) {
        int headerNameEnd = params.indexOf(':');
        if (headerNameEnd >= 0) {
          String name = params.substring(0, headerNameEnd).trim();
          String value = params.substring(headerNameEnd+1).trim();
          addHeader(name, value);
        }
      } else if (SmtpState.DATA_BODY == response.getNextState()) {
        body.append(params);
        body.append("\n");
      }
    }
  }

  /**
   * Get an Iterator over the header names.
   * @return an Iterator over the set of header names (String)
   */
  public Iterator getHeaderNames() {
    return headers.keySet().iterator();
  }

  /**
   * Get the value(s) associated with the given header name.
   * @param name header name
   * @return value(s) associated with the header name
   */
  public String[] getHeaderValues(String name) {
    List values = (List)headers.get(name);
    if (values == null) {
      return new String[0];
    } else {
      return (String[])values.toArray(new String[0]);
    }
  }

  /**
   * Get the first values associated with a given header name.
   * @param name header name
   * @return first value associated with the header name
   */
  public String getHeaderValue(String name) {
    List values = (List)headers.get(name);
    if (values == null) {
      return null;
    } else {
      return (String)values.iterator().next();
    }
  }

  /**
   * Get the message body.
   * @return message body
   */
  public String getBody() {
    return body.toString();
  }

  /**
   * Adds a header to the Map.
   * @param name header name
   * @param value header value
   */
  private void addHeader(String name, String value) {
    List valueList = (List)headers.get(name);
    if (valueList == null) {
      valueList = new ArrayList();
      headers.put(name, valueList);
    }
    valueList.add(value);
  }

  /**
   * String representation of the SmtpMessage.
   * @return a String
   */
  public String toString() {
    StringBuffer msg = new StringBuffer();
    for(Iterator i = headers.keySet().iterator(); i.hasNext();) {
      String name = (String)i.next();
      List values = (List)headers.get(name);
      for(Iterator j = values.iterator(); j.hasNext();) {
        String value = (String)j.next();
        msg.append(name);
        msg.append(": ");
        msg.append(value);
        msg.append('\n');
      }
    }
    msg.append('\n');
    msg.append(body);
    msg.append('\n');
    return msg.toString();
  }
}
