/*
 * Dumbster: a dummy SMTP server.
 * Copyright (C) 2003, Jason Paul Kitchen
 * lilnottsman@yahoo.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.xpn.xwiki.test.smtp;

/**
 * SMTP response container.
 */
public class SmtpResponse {
  /** Response code - see RFC-2821. */
  private int code;
  /** Response message. */
  private String message;
  /** New state of the SMTP server once the request has been executed. */
  private SmtpState nextState;

  /**
   * Constructor.
   * @param code response code
   * @param message response message
   * @param next next state of the SMTP server
   */
  public SmtpResponse(int code, String message, SmtpState next) {
    this.code = code;
    this.message = message;
    this.nextState = next;
  }

  /**
   * Get the response code.
   * @return response code
   */
  public int getCode() {
    return code;
  }

  /**
   * Get the response message.
   * @return response message
   */
  public String getMessage() {
    return message;
  }

  /**
   * Get the next SMTP server state.
   * @return state
   */
  public SmtpState getNextState() {
    return nextState;
  }
}
