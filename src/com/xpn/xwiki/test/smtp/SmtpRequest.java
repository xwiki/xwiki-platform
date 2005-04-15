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
 * Contains an SMTP client request.
 */
public class SmtpRequest {
  /** SMTP action received from client. */
  private SmtpActionType action;
  /** Current state of the SMTP state table. */
  private SmtpState state;
  /** Additional information passed from the client with the SMTP action. */
  private String params;

  /**
   * Create a new SMTP client request.
   * @param actionType type of action/command
   * @param params remainder of command line once command is removed
   * @param state current SMTP server state
   */
  public SmtpRequest(SmtpActionType actionType, String params, SmtpState state) {
    this.action = actionType;
    this.state = state;
    this.params = params;
  }

  /**
   * Execute the SMTP request returning a response. This method models the state transition table for the SMTP server.
   * @return reponse to the request
   */
  public SmtpResponse execute() {
    SmtpResponse response = null;
    if (action.isStateless()) {
      if (SmtpActionType.EXPN == action || SmtpActionType.VRFY == action) {
        response = new SmtpResponse(252, "Not supported", this.state);
      } else if (SmtpActionType.HELP == action) {
        response = new SmtpResponse(211, "No help available", this.state);
      } else if (SmtpActionType.NOOP == action) {
        response = new SmtpResponse(250, "OK", this.state);
      } else if (SmtpActionType.VRFY == action) {
        response = new SmtpResponse(252, "Not supported", this.state);
      } else if (SmtpActionType.RSET == action) {
        response = new SmtpResponse(250, "OK", SmtpState.GREET);
      } else {
        response = new SmtpResponse(500, "Command not recognized", this.state);
      }
    } else { // Stateful commands
      if (SmtpActionType.CONNECT == action) {
        if (SmtpState.CONNECT == state) {
          response = new SmtpResponse(220, "localhost Dumbster SMTP service ready", SmtpState.GREET);
        } else {
          response = new SmtpResponse(503, "Bad sequence of commands: "+action, this.state);
        }
      } else if (SmtpActionType.EHLO == action) {
        if (SmtpState.GREET == state) {
          response = new SmtpResponse(250, "OK", SmtpState.MAIL);
        } else {
          response = new SmtpResponse(503, "Bad sequence of commands: "+action, this.state);
        }
      } else if (SmtpActionType.MAIL == action) {
        if (SmtpState.MAIL == state) {
          response = new SmtpResponse(250, "OK", SmtpState.RCPT);
        } else {
          response = new SmtpResponse(503, "Bad sequence of commands: "+action, this.state);
        }
      } else if (SmtpActionType.RCPT == action) {
        if (SmtpState.RCPT == state) {
          response = new SmtpResponse(250, "OK", this.state);
        } else {
          response = new SmtpResponse(503, "Bad sequence of commands: "+action, this.state);
        }
      } else if (SmtpActionType.DATA == action) {
        if (SmtpState.RCPT == state) {
          response = new SmtpResponse(354, "Start mail input; end with <CRLF>.<CRLF>", SmtpState.DATA_HDR);
        } else {
          response = new SmtpResponse(503, "Bad sequence of commands: "+action, this.state);
        }
      } else if (SmtpActionType.UNRECOG == action) {
        if (SmtpState.DATA_HDR == state || SmtpState.DATA_BODY == state) {
          response = new SmtpResponse(-1, "", this.state);
        } else {
          response = new SmtpResponse(503, "Bad sequence of commands: "+action, this.state);
        }
      } else if (SmtpActionType.DATA_END == action) {
        if (SmtpState.DATA_HDR == state || SmtpState.DATA_BODY == state) {
          response = new SmtpResponse(250, "OK", SmtpState.QUIT);
        } else {
          response = new SmtpResponse(503, "Bad sequence of commands: "+action, this.state);
        }
      } else if (SmtpActionType.BLANK_LINE == action) {
        if (SmtpState.DATA_HDR == state) {
          response = new SmtpResponse(-1, "", SmtpState.DATA_BODY);
        } else if (SmtpState.DATA_BODY == state) {
          response = new SmtpResponse(-1, "", this.state);
        } else {
          response = new SmtpResponse(503, "Bad sequence of commands: "+action, this.state);
        }
      } else if (SmtpActionType.QUIT == action) {
        if (SmtpState.QUIT == state) {
          response = new SmtpResponse(250, "OK", SmtpState.CONNECT);
        } else {
          response = new SmtpResponse(503, "Bad sequence of commands: "+action, this.state);
        }
    } else {
        response = new SmtpResponse(500, "Command not recognized", this.state);
      }
    }
    return response;
  }

  /**
   * Create an SMTP request object given a line of the input stream from the client and the current internal state.
   * @param s line of input
   * @param state current state
   * @return a populated SmtpRequest object
   */
  public static SmtpRequest createRequest(String s, SmtpState state) {
    SmtpActionType action = null;
    String params = null;

    if (state == SmtpState.DATA_HDR) {
      if (s.equals(".")) {
        action = SmtpActionType.DATA_END;
      } else if (s.length() < 1) {
        action = SmtpActionType.BLANK_LINE;
      } else {
        action = SmtpActionType.UNRECOG;
        params = s;
      }
    } else if (state == SmtpState.DATA_BODY) {
      if (s.equals(".")) {
        action = SmtpActionType.DATA_END;
      } else {
        action = SmtpActionType.UNRECOG;
        params = s;
      }
    }else {
      String su = s.toUpperCase();
      if (su.startsWith("EHLO ") || su.startsWith("HELO")) {
        action = SmtpActionType.EHLO;
        params = s.substring(5);
      } else if (su.startsWith("MAIL FROM:")) {
        action = SmtpActionType.MAIL;
        params = s.substring(10);
      } else if (su.startsWith("RCPT TO:")) {
        action = SmtpActionType.RCPT;
        params = s.substring(8);
      } else if (su.startsWith("DATA")) {
        action = SmtpActionType.DATA;
      } else if (su.startsWith("QUIT")) {
        action = SmtpActionType.QUIT;
      } else if (su.startsWith("RSET")) {
        action = SmtpActionType.RSET;
      } else if (su.startsWith("NOOP")) {
        action = SmtpActionType.NOOP;
      } else if (su.startsWith("EXPN")) {
        action = SmtpActionType.EXPN;
      } else if (su.startsWith("VRFY")) {
        action = SmtpActionType.VRFY;
      } else if (su.startsWith("HELP")) {
        action = SmtpActionType.HELP;
      }
    }

    SmtpRequest req = new SmtpRequest(action, params, state);
    return req;
  }

  /**
   * Get the parameters of this request (remainder of command line once the command is removed.
   * @return parameters
   */
  public String getParams() {
    return params;
  }
}
