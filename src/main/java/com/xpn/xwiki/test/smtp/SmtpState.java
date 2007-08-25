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

/**
 * SMTP server state.
 */
public class SmtpState {
  /** Internal representation of the state. */
  private byte value;

  /** Internal representation of the CONNECT state. */
  private static final byte CONNECT_BYTE      = 1;
  /** Internal representation of the GREET state. */
  private static final byte GREET_BYTE        = 2;
  /** Internal representation of the MAIL state. */
  private static final byte MAIL_BYTE         = 3;
  /** Internal representation of the RCPT state. */
  private static final byte RCPT_BYTE         = 4;
  /** Internal representation of the DATA_HEADER state. */
  private static final byte DATA_HEADER_BYTE  = 5;
  /** Internal representation of the DATA_BODY state. */
  private static final byte DATA_BODY_BYTE    = 6;
  /** Internal representation of the QUIT state. */
  private static final byte QUIT_BYTE         = 7;

  /** CONNECT state: waiting for a client connection. */
  public static final SmtpState CONNECT   = new SmtpState(CONNECT_BYTE);
  /** GREET state: wating for a ELHO message. */
  public static final SmtpState GREET     = new SmtpState(GREET_BYTE);
  /** MAIL state: waiting for the MAIL FROM: command. */
  public static final SmtpState MAIL      = new SmtpState(MAIL_BYTE);
  /** RCPT state: waiting for a RCPT &lt;email address&gt; command. */
  public static final SmtpState RCPT      = new SmtpState(RCPT_BYTE);
  /** Waiting for headers. */
  public static final SmtpState DATA_HDR  = new SmtpState(DATA_HEADER_BYTE);
  /** Processing body text. */
  public static final SmtpState DATA_BODY = new SmtpState(DATA_BODY_BYTE);
  /** End of client transmission. */
  public static final SmtpState QUIT      = new SmtpState(QUIT_BYTE);

  /**
   * Create a new SmtpState object. Private to ensure that only valid states can be created.
   * @param value one of the _BYTE values.
   */
  private SmtpState(byte value) {
    this.value = value;
  }

  /**
   * String representation of this SmtpState.
   * @return a String
   */
  public String toString() {
    switch(value) {
      case CONNECT_BYTE:
        return "CONNECT";
      case GREET_BYTE:
        return "GREET";
      case MAIL_BYTE:
        return "MAIL";
      case RCPT_BYTE:
        return "RCPT";
      case DATA_HEADER_BYTE:
        return "DATA_HDR";
      case DATA_BODY_BYTE:
        return "DATA_BODY";
      case QUIT_BYTE:
        return "QUIT";
      default:
        return "Unknown";
    }
  }
}
