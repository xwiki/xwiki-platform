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
package com.xpn.xwiki.atom;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WSSEHttpHeader {
  
  private static String userNameExpression = "Username=\"([a-zA-Z]+)\"";
  
  private static String nonceExpression = "Nonce=\"([A-Za-z0-9+/=]+)\"";
  
  private static String passwordDigestExpression = "PasswordDigest=\"([A-Za-z0-9+/=]+)\"";
  
  private static String createdExpression = "Created=\"([0-9:\\-+TZ]+)\"";
  
  private static String parseCreatedExpression = "(\\d{4})(?:-?(\\d{2})(?:-?(\\d\\d?)(?:T(\\d{2}):(\\d{2}):(\\d{2})(?:\\.\\d+)?(?:(Z)|([+-]\\d{2}:\\d{2}))?)?)?)?";
  
  private static Pattern headerPattern;
  
  private static Pattern createdPattern = Pattern.compile(parseCreatedExpression);
  
  private static SimpleDateFormat df = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ssZ" );

  private String userName;
  
  private String nonce;
  
  private String created;
  
  private String passwordDigest;
  
  /**
   * @param created The created to set.
   */
  void setCreated(String created) {
    this.created = created;
  }

  /**
   * @param nonce The nonce to set.
   */
  private void setNonce(String nonce) {
    this.nonce = nonce;
  }

  /**
   * @param passwordDigest The passwordDigest to set.
   */
  private void setPasswordDigest(String passwordDigest) {
    this.passwordDigest = passwordDigest;
  }

  /**
   * @param userName The userName to set.
   */
  private void setUserName(String userName) {
    this.userName = userName;
  }

  /**
   * @return Returns the created.
   */
  public String getCreated() {
    return created;
  }

  /**
   * @return Returns the nonce.
   */
  public String getNonce() {
    return nonce;
  }

  /**
   * @return Returns the passwordDigest.
   */
  public String getPasswordDigest() {
    return passwordDigest;
  }

  /**
   * @return Returns the userName.
   */
  public String getUserName() {
    return userName;
  }

  public static WSSEHttpHeader parseHttpHeader(String httpHeader) throws IOException {
    WSSEHttpHeader wsseHeader = null;
    Matcher matcher = getHeaderPattern().matcher(httpHeader);
    if (matcher.matches()) {
        wsseHeader = new WSSEHttpHeader();
        wsseHeader.setUserName(matcher.group(1));
        wsseHeader.setPasswordDigest(matcher.group(2));
        wsseHeader.setNonce(matcher.group(3));
        wsseHeader.setCreated(matcher.group(4));
        
        if (!Base64.isArrayByteBase64(wsseHeader.getPasswordDigest().getBytes())) {
          throw new IOException("Invalid Password Digest : " + wsseHeader.getPasswordDigest());
        }
        if (!Base64.isArrayByteBase64(wsseHeader.getNonce().getBytes())) {
          throw new IOException("Invalid Nonce : " + wsseHeader.getPasswordDigest());
        }
    }
    return wsseHeader;
  }
  
  public boolean isAuthenticated(String password) {
    return new String(Base64.encodeBase64(DigestUtils.sha(getNonce()+getCreated()+password))).equals(getPasswordDigest());    
  }

  /**
   * 
   */
  WSSEHttpHeader() {
    super();
    // TODO Auto-generated constructor stub
  }

  /**
   * @return Returns the passwordDigestExpression.
   */
  public static String getPasswordDigestExpression() {
    return passwordDigestExpression;
  }

  /**
   * @return Returns the userNameExpression.
   */
  public static String getUserNameExpression() {
    return userNameExpression;
  }

  /**
   * @return Returns the createdExpression.
   */
  public static String getCreatedExpression() {
    return createdExpression;
  }

  /**
   * @return Returns the nonceExpression.
   */
  public static String getNonceExpression() {
    return nonceExpression;
  }

  /**
   * @return Returns the parseCreatedExpression.
   */
  public static String getParseCreatedExpression() {
    return parseCreatedExpression;
  }

  /**
   * @return Returns the headerPattern.
   */
  public static Pattern getHeaderPattern() {
    if (headerPattern == null) {
      StringBuffer sb = new StringBuffer("UsernameToken ");
      sb.append(userNameExpression);
      sb.append(", ");
      sb.append(passwordDigestExpression);
      sb.append(", ");
      sb.append(nonceExpression);
      sb.append(", ");
      sb.append(createdExpression);
      headerPattern = Pattern.compile(sb.toString());      
    }
    return headerPattern;
  }
  
  public Calendar parseCreated() {
    return parseCreated(created);
  }

  public static Calendar parseCreated(String w3CDTFValue) {
    Calendar cal = null;
    Matcher matcher = createdPattern.matcher(w3CDTFValue);
    if (matcher.matches()) {
      String value;
      int year;
      int day;
      int month;
      year = Integer.parseInt(matcher.group(1));
      if ((value = matcher.group(2)) == null) {
        month = Calendar.JANUARY;
      } else {
        month = Integer.parseInt(value) - (1 - Calendar.JANUARY);
      }
      if ((value = matcher.group(3)) == null) {
        day = 1;
      } else {
        day = Integer.parseInt(value);
      }
      cal = new GregorianCalendar(year, month, day);
      String timeZone = matcher.group(7);
      if (timeZone != null && !timeZone.equals("Z")) {
        cal.setTimeZone(TimeZone.getTimeZone(timeZone));
      }      
      if ((value = matcher.group(4)) != null) {
        cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(value));
      }
      if ((value = matcher.group(5)) != null) {
        cal.set(Calendar.MINUTE, Integer.parseInt(value));
      }
      if ((value = matcher.group(6)) != null) {
        cal.set(Calendar.SECOND, Integer.parseInt(value));
      }
    }
    return cal;
  }
  
  public static String CalendarToW3CDSTFormat(Calendar cal) {
    StringBuffer sb = new StringBuffer(df.format(cal.getTime()));
    sb.insert(sb.length()-2, ":");
    return sb.toString();
  }
}
