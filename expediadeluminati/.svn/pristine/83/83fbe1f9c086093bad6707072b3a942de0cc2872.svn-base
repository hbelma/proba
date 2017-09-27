/*
 * ISOCountryInfo.java
 *
 * Copyright (c) 2003 Hotel Reservation Service GmbH
 * All rights reserved.
 */
package de.hrs.marketwatch.webtranslator.processors.expediadeluminati;


public class ISOCountryInfo {

    /** Country code in two character ISO 3166 format */
    private String countryCodeISO2;
    /** Country code in three character ISO 3166 format */
    private String countryCodeISO3;
    /** Country code in ISO 3166 number format */
    private String countryCodeISONum;
    /** Country name in English language */
    private String countryNameUs;
    /** Country name in German language */
    private String countryNameDe;


    /**
     * ISO 3166 country info constructor.
     *
     * @param countryCodeISO2   ISO 3166 two char country code
     * @param countryCodeISO3   ISO 3166 three char country code
     * @param countryCodeISONum ISO 3166 three char country number
     * @param countryNameUs country name in English language
     * @param countryNameDe country name in German language
     */
    public ISOCountryInfo(String countryCodeISO2,
              String countryCodeISO3,
              String countryCodeISONum,
              String countryNameUs,
              String countryNameDe) {

    this.countryCodeISO2 = countryCodeISO2.trim();
    this.countryCodeISO3 = countryCodeISO3.trim();
    this.countryCodeISONum = countryCodeISONum.trim();
    this.countryNameUs = countryNameUs.trim();
    this.countryNameDe = countryNameDe.trim();
    }

    /**
     * Prints object contents. Used for debuging purposes only.
     *
     * @return object contents in <code>String</code> format.
     */
    public String toString() {

    StringBuffer sb = new StringBuffer("");

    sb.append("countryCodeISO2="+countryCodeISO2+"; "
         +"countryCodeISO3="+countryCodeISO3+"; "
         +"countryCodeISONum="+countryCodeISONum+"; "
         +"countryNameUs="+countryNameUs+"; "
         +"countryNameDe="+countryNameDe);

    return sb.toString();
    }

    /**
     * Returns ISO 3166 two char country code.
     *
     * @return ISO 3166 two char country code.
     */
    public String getCountryCodeISO2() {
        return countryCodeISO2;
    }

    /**
     * Returns ISO 3166 three char country code.
     *
     * @return ISO 3166 three char country code.
     */
    public String getCountryCodeISO3() {
        return countryCodeISO3;
    }

    /**
     * Returns ISO 3166 three char country number.
     *
     * @return ISO 3166 three char country number.
     */
    public String getCountryCodeISONum() {
        return countryCodeISONum;
    }

    /**
     * Returns country name in German language.
     *
     * @return country name in German language.
     */
    public String getCountryNameDe() {
        return countryNameDe;
    }

    /**
     * Returns country name in English language.
     *
     * @return country name in English language.
     */
    public String getCountryNameUs() {
        return countryNameUs;
    }

    /**
     * Sets ISO 3166 two char country code.
     *
     */
    public void setCountryCodeISO2(String string) {
        countryCodeISO2 = string.trim();
    }

    /**
     * Sets ISO 3166 three char country code.
     *
     */
    public void setCountryCodeISO3(String string) {
        countryCodeISO3 = string.trim();
    }

    /**
     * Sets ISO 3166 three char country number.
     *
     */
    public void setCountryCodeISONum(String string) {
        countryCodeISONum = string.trim();
    }

    /**
     * Sets country name in German language.
     *
     */
    public void setCountryNameDe(String string) {
        countryNameDe = string.trim();
    }

    /**
     * Sets country name in English language.
     *
     */
    public void setCountryNameUs(String string) {
        countryNameUs = string.trim();
    }

}
