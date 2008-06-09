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
 * Represents an SMTP action or command.
 */
public class SmtpActionType {
  /** Internal value for the action type. */
  private byte value;

  /** Internal representation of the CONNECT action. */
  private static final byte CONNECT_BYTE =  1;
  /** Internal representation of the EHLO action. */
  private static final byte EHLO_BYTE =     2;
  /** Internal representation of the MAIL FROM action. */
  private static final byte MAIL_BYTE =     3;
  /** Internal representation of the RCPT action. */
  private static final byte RCPT_BYTE =     4;
  /** Internal representation of the DATA action. */
  private static final byte DATA_BYTE =     5;
  /** Internal representation of the DATA END (.) action. */
  private static final byte DATA_END_BYTE = 6;
  /** Internal representation of the QUIT action. */
  private static final byte QUIT_BYTE =     7;
  /** Internal representation of an unrecognized action: body text gets this action type. */
  private static final byte UNREC_BYTE =    8;
  /** Internal representation of the blank line action: separates headers and body text. */
  private static final byte BLANK_LINE_BYTE =    9;

  /** Internal representation of the stateless RSET action. */
  private static final byte RSET_BYTE = -1;
  /** Internal representation of the stateless VRFY action. */
  private static final byte VRFY_BYTE = -2;
  /** Internal representation of the stateless EXPN action. */
  private static final byte EXPN_BYTE = -3;
  /** Internal representation of the stateless HELP action. */
  private static final byte HELP_BYTE = -4;
  /** Internal representation of the stateless NOOP action. */
  private static final byte NOOP_BYTE = -5;

  /** CONNECT action. */
  public static final SmtpActionType CONNECT = new SmtpActionType(CONNECT_BYTE);
  /** EHLO action. */
  public static final SmtpActionType EHLO = new SmtpActionType(EHLO_BYTE);
  /** MAIL action. */
  public static final SmtpActionType MAIL = new SmtpActionType(MAIL_BYTE);
  /** RCPT action. */
  public static final SmtpActionType RCPT = new SmtpActionType(RCPT_BYTE);
  /** DATA action. */
  public static final SmtpActionType DATA = new SmtpActionType(DATA_BYTE);
  /** "." action. */
  public static final SmtpActionType DATA_END = new SmtpActionType(DATA_END_BYTE);
  /** Body text action. */
  public static final SmtpActionType UNRECOG = new SmtpActionType(UNREC_BYTE);
  /** QUIT action. */
  public static final SmtpActionType QUIT = new SmtpActionType(QUIT_BYTE);
  /** Header/body separator action. */
  public static final SmtpActionType BLANK_LINE = new SmtpActionType(BLANK_LINE_BYTE);

  /** Stateless RSET action. */
  public static final SmtpActionType RSET = new SmtpActionType(RSET_BYTE);
  /** Stateless VRFY action. */
  public static final SmtpActionType VRFY = new SmtpActionType(VRFY_BYTE);
  /** Stateless EXPN action. */
  public static final SmtpActionType EXPN = new SmtpActionType(EXPN_BYTE);
  /** Stateless HELP action. */
  public static final SmtpActionType HELP = new SmtpActionType(HELP_BYTE);
  /** Stateless NOOP action. */
  public static final SmtpActionType NOOP = new SmtpActionType(NOOP_BYTE);

  /**
   * Create a new SMTP action type. Private to ensure no invalid values.
   * @param value one of the _BYTE values
   */
  private SmtpActionType(byte value) {
    this.value = value;
  }

  /**
   * Indicates whether the action is stateless or not.
   * @return true iff the action is stateless
   */
  public boolean isStateless() {
    return value < 0;
  }

  /**
   * String representation of this SMTP action type.
   * @return a String
   */
  public String toString() {
    switch(value) {
      case CONNECT_BYTE:
        return "Connect";
      case EHLO_BYTE:
        return "EHLO";
      case MAIL_BYTE:
        return "MAIL";
      case RCPT_BYTE:
        return "RCPT";
      case DATA_BYTE:
        return "DATA";
      case DATA_END_BYTE:
        return ".";
      case QUIT_BYTE:
        return "QUIT";
      case RSET_BYTE:
        return "RSET";
      case VRFY_BYTE:
        return "VRFY";
      case EXPN_BYTE:
        return "EXPN";
      case HELP_BYTE:
        return "HELP";
      case NOOP_BYTE:
        return "NOOP";
      case UNREC_BYTE:
        return "Unrecognized command / data";
      case BLANK_LINE_BYTE:
        return "Blank line";
      default:
        return "Unknown";
    }
  }
}
