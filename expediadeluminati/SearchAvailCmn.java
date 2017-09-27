/*
 * SearchAvailCmn.java
 *
 * Copyright (c) 2003 Hotel Reservation Service GmbH
 * All rights reserved.
 */
package de.hrs.marketwatch.webtranslator.processors.expediadeluminati;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.TableRow;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import si.hermes.bc.webtranslator.actionmanager.ActionManager;
import si.hermes.bc.webtranslator.utils.Page;
import de.hrs.marketwatch.common.Breakfast;
import de.hrs.marketwatch.common.ErrorCodes;
import de.hrs.marketwatch.common.Price;
import de.hrs.marketwatch.common.Prices;
import de.hrs.marketwatch.common.util.HtmlSpecialChars;
import de.hrs.marketwatch.webtranslator.processors.common.BaseCommon;
import de.hrs.marketwatch.webtranslator.processors.common.HotelInfo;
import de.hrs.marketwatch.webtranslator.processors.common.RateInfo;
import de.hrs.marketwatch.webtranslator.processors.common.TranslatorException;
import de.hrs.marketwatch.webtranslator.processors.common.Utils;
import de.hrs.marketwatch.webtranslator.processors.common.html.HtmlParserUtils;

public class SearchAvailCmn extends BaseCommon {

    private static final Logger log = Logger.getLogger(SearchAvailCmn.class
	    .getName());
    private static final String ROOM_NOT_FOUND_MESSAGE = "No rooms available on the selected dates";
    private static final String ROOM_NOT_FOUND_MESSAGE1 = "Keine Verfügbarkeit";
    private static final String ERROR_404_PAGE_NOT_FOUND_MSG = "404 Page Not Found";
    private static final int HOTELS_PER_PAGE = 50;

    private static final String SPLIT_N_CUT = "\":\"MULTICITYVICINITY\"";
    private static final String SPLIT_N_CUT_CASE2 = "\"datelessSearch\"";
    private static final String SPLIT_N_CUT_HID = "expedia.de/";
    private static final String SPLIT_N_CUT_HID_ENC = "expedia.de%2F";
    private static final String SPLIT_N_CUT_HNAME = "hotelName\":\"";
    private static final String SPLIT_N_CUT_HCOUNT = "<!--tlTotalNumHotels:";
    private String SUGGEST_LINK = "http://suggest.expedia.com/hint/es/v1/ac/de_DE/";
    private String homePage = "http://www.expedia.de";

    private static final String ROOM_NOT_FOUND_MESSAGE2 = "This room type is not available for your "
	    + "selected dates";

    private static final String ROOM_NOT_FOUND_MESSAGE3 = "Dieser Zimmertyp ist für die "
	    + "ausgewählten Daten nicht verfügbar";

    private static final String ROOM_NOT_FOUND_MESSAGE4 = "Für die ausgewählten Unterkünfte sind "
	    + "zurzeit keine Details verfügbar";
    private static final String ROOM_NOT_FOUND_MESSAGE5 = "Nicht erstattbar";
    private static final String ROOM_NOT_FOUND_MESSAGE6 = "Zu diesen Daten sind keine Zimmer verfügbar";
    private Map<HotelInfo, String> hotelsCollection = new HashMap<HotelInfo, String>();
    private Collection<RateInfo> ratesCollection = new ArrayList<RateInfo>();
    public String country = "de";
    private static String token;
    private static String guid;

    public StringBuffer appendUnchangedParameters(StringBuffer buffer,
	    String cityName, int pageCounter, String cityId) {

	buffer.append("/Hotel-Search?inpAjax=true&responsive=true");
	buffer.append("&destination=").append(cityName);
	buffer.append("&adults=2");
	buffer.append("&regionId=").append(cityId).append("&page=1");
	buffer.append("&startDate=").append(req_dateFrom).append("&endDate=")
		.append(req_dateTo);
	buffer.append("&hashParam=");

	return buffer;
    }

    public HashMap<String, String> setUnchangedParameters(String cityName,
	    String cityId, String hashParam) {

	HashMap<String, String> parameters = new HashMap<String, String>();
	parameters.put("inpAjax", "true");
	parameters.put("responsive", "true");
	parameters.put("destination", cityName);
	parameters.put("adults", "2");
	parameters.put("regionId", cityId);
	parameters.put("page", "1");
	parameters.put("startDate", req_dateFrom);
	parameters.put("endDate", req_dateTo);
	parameters.put("hashParam", hashParam);

	return parameters;
    }

    public Collection<HotelInfo> getAllHotels(Page page)
	    throws TranslatorException, IOException, ParserException {

	String firstHotelPage = null;

	String cityName = null;
	int pageCounter = 1;
	StringBuffer destBuffer = new StringBuffer(homePage + "luminati");

	String roomTypeListPageCnt = null;
	String startingLink = null;
	int start = 0;
	int end = 0;
	String hotels = null;
	String[] hotelsAjax = null;

	String searchParam = "\"retailHotelInfoModel\":";
	int iCurrentPageNum = 1;
	for (int i = 0; i < req_cityVariations.length; i++) {
	    cityName = req_cityVariations[i];
	    cityName = cityName.replaceAll(" ", "%20");

	    String searchParamsPage = this.workflowSession.makeRequestLuminati(
		    "GET", SUGGEST_LINK + cityName, country);
	    String searchParams = getSuggestParams(searchParamsPage);

	    String[] paramSplit = searchParams.split("\\|");
	    String hashParam = null;
	    if (paramSplit.length > 0) {
		cityName = paramSplit[0];
		String cityId = paramSplit[1];
		// using makeRequest but with luminati proxy
		String hashParameters = this.workflowSession
			.makeRequestLuminati("GET",
				"http://www.expedia.de/Hotel-Search?", country);
		hashParam = getHashParams(hashParameters);
		destBuffer = appendUnchangedParameters(destBuffer, cityName,
			pageCounter, cityId);

		startingLink = destBuffer.toString();
		startingLink = startingLink.replaceAll(" ", "+");
		startingLink = startingLink.replaceAll(", ", "%2C+");
		/*
		 * HashMap<String, String> parameters =
		 * setUnchangedParameters(cityName, cityId, hashParam);
		 */
		// using makeRequest but with luminati proxy
		firstHotelPage = this.workflowSession.makeRequestLuminati(
			"POST", startingLink.toString() + hashParam, country);
	    }
	    roomTypeListPageCnt = firstHotelPage;
	    start = roomTypeListPageCnt.indexOf(searchParam);
	    start = start + searchParam.length();
	    end = roomTypeListPageCnt.indexOf("\"pagination\" : ", start);
	    if (start < end) {
		hotels = roomTypeListPageCnt.substring(start, end);
	    } else {
		log.debug("error substringing");
	    }
	    hotelsAjax = hotels.split(SPLIT_N_CUT);
	    if (hotelsAjax.length < 4) {
		hotelsAjax = hotels.split(SPLIT_N_CUT_CASE2);
	    }
	    getHotelsFromOnePage(hotelsAjax);

	    int startId = roomTypeListPageCnt.indexOf(SPLIT_N_CUT_HCOUNT);
	    startId = startId + SPLIT_N_CUT_HCOUNT.length();
	    int endId = roomTypeListPageCnt.indexOf("-->", startId);
	    int iTotalHotels = 0;
	    int iTotalPages = 0;
	    if (startId < endId) {
		String hotelCount = roomTypeListPageCnt.substring(startId,
			endId);
		if (hotelCount.indexOf(".") > -1) {
		    hotelCount = hotelCount.replaceAll("\\.", "");
		}
		iTotalHotels = Integer.parseInt(hotelCount);
		iTotalPages = Math.round(iTotalHotels / HOTELS_PER_PAGE);
		if (iTotalHotels % HOTELS_PER_PAGE != 0) {
		    iTotalPages = iTotalPages + 1;
		}
	    } else {
		log.debug("error substringing");
		continue;
	    }

	    while (iCurrentPageNum < iTotalPages) {

		pageCounter = pageCounter + 1;
		String nextLink = startingLink;
		nextLink = nextLink.replaceFirst("page=1", "page="
			+ pageCounter);
		String nextHotelPage = null;
		String hashParamPage = this.workflowSession
			.makeRequestLuminati("GET",
				"http://www.expedia.de/Hotel-Search?", country);
		if (hashParamPage != null) {
		    hashParam = getHashParams(hashParamPage);
		    nextHotelPage = this.workflowSession.makeRequestLuminati(
			    "POST", nextLink + hashParam, country);
		} else {
		    hashParamPage = this.workflowSession.makeRequestLuminati(
			    "GET", "http://www.expedia.de/Hotel-Search?",
			    country);
		    hashParam = getHashParams(hashParamPage);
		    nextHotelPage = this.workflowSession.makeRequestLuminati(
			    "POST", nextLink + hashParam, country);
		}

		if (nextHotelPage == null) {
		    hashParamPage = this.workflowSession.makeRequestLuminati(
			    "GET", "http://www.expedia.de/Hotel-Search?",
			    country);
		    hashParam = getHashParams(hashParamPage);
		    nextHotelPage = this.workflowSession.makeRequestLuminati(
			    "POST", nextLink + hashParam, country);
		}

		roomTypeListPageCnt = nextHotelPage;

		start = roomTypeListPageCnt.indexOf(searchParam);
		start = start + searchParam.length();
		end = roomTypeListPageCnt.indexOf("pagination\" :", start);

		if (start < end) {
		    hotels = roomTypeListPageCnt.substring(start, end);
		} else {
		    log.debug("error substringing");
		}
		hotelsAjax = hotels.split(SPLIT_N_CUT);
		getHotelsFromOnePage(hotelsAjax);
		iCurrentPageNum++;
	    } // while
	    break;
	}
	return hotelsCollection.keySet();

    }

    public void getHotelsFromOnePage(String[] hotelsAjax)
	    throws ParserException, IOException, TranslatorException {

	for (int j = 1; j < hotelsAjax.length; j++) {
	    HotelInfo hotel = new HotelInfo();
	    String currHotelStr = hotelsAjax[j];
	    int startId = currHotelStr.indexOf(SPLIT_N_CUT_HID);

	    if (startId < 0) {
		startId = currHotelStr.indexOf(SPLIT_N_CUT_HID_ENC);
		startId = startId + SPLIT_N_CUT_HID_ENC.length();
	    } else {
		startId = startId + SPLIT_N_CUT_HID.length();
	    }

	    int endId = currHotelStr.indexOf(".Hotel-Information", startId);
	    String hotelNameId = null;
	    if (startId < endId) {
		hotelNameId = currHotelStr.substring(startId, endId);
	    } else {
		log.debug("error substringing");
		continue;
	    }

	    String hotelName = null;
	    String hotelId = null;
	    String[] separateHots = hotelNameId.split("\\.h");
	    hotelId = separateHots[1];
	    hotel.setHotelId(hotelId);

	    startId = currHotelStr.indexOf(SPLIT_N_CUT_HNAME);
	    startId = startId + SPLIT_N_CUT_HNAME.length();
	    endId = currHotelStr.indexOf("\",\"", startId);
	    if (startId < endId) {
		hotelName = currHotelStr.substring(startId, endId);
	    } else {
		log.debug("error substringing");
		continue;
	    }
	    hotel.setHotelName(hotelName);
	    String currentLink = "/" + hotelNameId + ".Hotel-Information";
	    String hotelAddressPage = null;

	    for (int retries = 0; retries < 3; retries++) {
		try {
		    ActionManager am = (ActionManager) this.workflowSession;
		    am.renewConnection();
		    hotelAddressPage = this.workflowSession
			    .makeRequestLuminati("GET", Utils.setURL(
				    currentLink, this.req_homepage), country);
		    if (hotelAddressPage != null
			    && (!hotelAddressPage
				    .contains(ERROR_404_PAGE_NOT_FOUND_MSG))) {
			break;
		    }

		} catch (final java.net.SocketTimeoutException e) {
		    // connection timed out...let's try again
		}
	    }

	    if (hotelAddressPage != null) {
		String hotelAddress = getAddress(hotelAddressPage);
		if (hotelAddress == null) {
		    this.throwPageError(
			    "getNextPage() address of hotel not found", null,
			    ErrorCodes.HOTEL_NOT_FOUND, "");
		}
		hotelAddress = cleanAddress(hotelAddress);

		hotel.setHotelAddress(hotelAddress);

		hotelsCollection.put(hotel, "");
	    } else {
		this.throwPageError("getNextPage() hotel not found", null,
			ErrorCodes.HOTEL_NOT_FOUND, "");
	    }

	}

    }

    public Collection<RateInfo> getRates() throws IOException,
	    TranslatorException, ParserException {

	StringBuffer buffer = new StringBuffer("https://www.expedia.de");
	req_hotelName = cleanHotelName(req_hotelName);
	buffer.append("/" + req_cityName.replaceAll(". ", "-"));
	buffer.append("-Hotels-");
	buffer.append(req_hotelName.replaceAll("\\s+", "-"));
	buffer.append(".h" + req_context);
	buffer.append(".Hotel-Beschreibung?");
	buffer.append("chkin=" + req_dateFrom);
	buffer.append("&chkout=" + req_dateTo);
	if (req_roomType != null) {
	    if (req_roomType.equals("DOUBLE"))
		buffer.append("&rm1=a2");
	    else
		buffer.append("&rm1=a1");
	} else {
	    buffer.append("&rm1=a2");
	}

	if (req_context == null || req_context.length() == 0) {
	    this.throwPageError("getRates() - Hotel_ID is null or empty", null,
		    ErrorCodes.HOTEL_NOT_FOUND, "");
	}
	RateInfo rate = new RateInfo();
	// https://www.expedia.de/Lohmar-Hotels-Landhotel-Naafs-Hauschen.h7309148.Hotel-Beschreibung?chkin=19.3.2016&chkout=20.3.2016&rm1=a2
	String initialPageStr = this.workflowSession.makeRequestLuminati("GET",
		buffer.toString(), country);
	//because sometime this page is null, we will do retry logic
	if (initialPageStr == null){
		initialPageStr = this.workflowSession.makeRequestLuminati("GET",
				buffer.toString(), country);
	}
	if (initialPageStr == null) {
	    this.throwPageError("getRates() hotelPage is null", null,
		    ErrorCodes.HOTEL_NOT_FOUND, "");
	}
	if (initialPageStr.contains(ERROR_404_PAGE_NOT_FOUND_MSG)) {
	    this.throwPageError("getRates() no hotel found: "
		    + this.req_context, null, ErrorCodes.HOTEL_NOT_FOUND, "");
	}
	checkRoomFound(initialPageStr);

	token = parseVariableFromSite(initialPageStr, "infosite.token");
	if (token == null) {
		token = parseVariableFromSite(initialPageStr, "infosite.token");
	}
	guid = parseVariableFromSite(initialPageStr, "infosite.guid");
	if (guid == null) {
	    guid = parseVariableFromSite(initialPageStr, "infosite.guid");
	}
	HashMap<Integer, String> parameters = new HashMap<Integer, String>();

	if (guid != null) {
	    guid = guid.replaceAll("[^a-zA-Z0-9]", "");
	    parameters.put(1, guid);
	}

	if (token == null) {
	    this.throwPageError("getRates() token is null", null,
		    ErrorCodes.HOTEL_NOT_FOUND, "");
	}

		String adults = getNumberOfAdults();
		String children = "0";
		String urlJson = getJSONUrl(req_context, req_dateFrom, req_dateTo,
				adults, children);
		log.debug("Token: " + token);
		
		String hotelPage = this.workflowSession.makeRequestLuminati("GET",
				urlJson, null, parameters, country);

		if (hotelPage == null || hotelPage.isEmpty()) {
			log.debug("====getJsonUrlRetry first time====");
			hotelPage = getJsonUrlRetry(initialPageStr, parameters, adults,
					children);
		}
		if (hotelPage == null) {
			this.throwPageError("getRates() hotelPage is null", null,
					ErrorCodes.HOTEL_NOT_FOUND, "");
		}

		if (!hotelPage.startsWith("{")) {
			log.debug("====getJsonUrlRetry second time====");
			hotelPage = getJsonUrlRetry(initialPageStr, parameters, adults,
					children);
		}
		
		rate = getRates(hotelPage, req_roomType);

		ratesCollection.add(rate);
		return ratesCollection;

    }

    public RateInfo getRates(String jsonPage, String roomType)
	    throws TranslatorException {

	    findAvailabilityErrors(jsonPage);
    	RateInfo rate = new RateInfo();
	try {
	    JSONObject obj = new JSONObject(jsonPage);
	    JSONArray rateList = obj.getJSONArray("offers");
	    double minPrice;
	    int counter = findCheapestRate(roomType, rateList);
	    JSONObject cheapestRate = rateList.optJSONObject(counter);
	    Price finalPrice = null;
	    String rateDescription = "";
	    String roomDescription = "";
	    if (cheapestRate != null) {
			minPrice = getCheapestPrice(cheapestRate);
			JSONObject cheapestRatePriceObj = cheapestRate.getJSONObject(
				"price").getJSONObject("priceObject");
			String currency = getCurrency(cheapestRatePriceObj);
			roomDescription = getRoomDescription(cheapestRate);
			rateDescription = getRateDescription(cheapestRate);
			finalPrice = Prices.parsePrice(minPrice + currency);

	    }

	    rate.setRatePrice(finalPrice.getAmount());
	    rate.setCurrency(finalPrice.getCurrencyCode());
	    rate.setRateDescription(rateDescription);
	    rate.setRoomDescription(roomDescription);
	    rate.setBreakfast(Breakfast.getBreakfastCode(rate.getRoomDescription() + rate.getRateDescription()));
	} catch (JSONException e) {
	    log.debug("JSON object missing " + e);
	}

	return rate;
    }
    
	private String getJsonUrlRetry(String initialPageStr,
			HashMap<Integer, String> parameters, String adults, String children)
			throws IOException {
		String urlJson;
		String hotelPage;
		token = parseVariableFromSite(initialPageStr, "infosite.token");
		urlJson = getJSONUrl(req_context, req_dateFrom, req_dateTo, adults,
				children);
		hotelPage = this.workflowSession.makeRequestLuminati("GET",
				urlJson, null, parameters, country);
		return hotelPage;
	}

    private void findAvailabilityErrors(String jsonPage)
	    throws TranslatorException {

		try {
			    if (jsonPage.toString().indexOf("availabilityErrors") > -1 ) {
			    	JSONObject obj = new JSONObject(jsonPage);
			    	JSONArray availErrorsList = obj.getJSONArray("availabilityErrors");

					if (availErrorsList.length() != 0) {
						    for (int i = 0; i < availErrorsList.length(); i++) {
								JSONObject error = availErrorsList.optJSONObject(i);
								String errMsg = error.get("messageKey").toString();
									if (errMsg.indexOf("roomsUnavailableForSelectedDates") > -1) {
									    this.throwPageError(
										    "findAvailabilityErrors () rooms are sold out for choosen date",
										    null, ErrorCodes.NO_ROOMS_FOUND_WARNING, "");
									}
					    }

							//get specific error message
							JSONObject availErrorObj = availErrorsList.getJSONObject(0);
							String message = availErrorObj.get("message").toString();
							if (message != null) {
							    log.debug(message);
							    this.throwPageError("availabilityErrors", null,
								    ErrorCodes.NO_ROOMS_FOUND_WARNING, "");

									}
				    	}
			    }
		} catch (JSONException e) {
		    log.debug("Availability Errors" + e);
		}
    }

    private static int findCheapestRate(String roomType, JSONArray rateList) {
	int counter = 0;

	try {
	    double minPrice = Double.MAX_VALUE;
	    String cheapestPrice = "";

	    for (int i = 0; i < rateList.length(); i++) {
		JSONObject rate = rateList.optJSONObject(i);
		String numOfAdults = rate.get("maxAdults").toString();
		String exceedsMaxGuests = null;
		String exceedsMaxAdults = null;
		String displayMultiSourceToolTip = null;
		
		if(numOfAdults.equals("0"))
		{
			numOfAdults = rate.getString("maxGuests").toString();
		}
	
		if (roomType.equalsIgnoreCase("DOUBLE")
				&& numOfAdults.equalsIgnoreCase("1")) {
			continue;
		}
		if (rate.get("price").toString().equalsIgnoreCase("null")) {

			continue;
		}
		
		if(rate.has("exceedsMaxGuests")) {
			exceedsMaxGuests = rate.get("exceedsMaxGuests").toString();
			if(exceedsMaxGuests.contains("true"))
				continue;
			else if(rate.has("exceedsMaxAdults")) {
					exceedsMaxAdults = rate.get("exceedsMaxAdults").toString();
					if(exceedsMaxAdults.contains("true"))
						continue;
				}		
		}	
		
		if(rate.has("displayMultiSourceToolTip")){
			displayMultiSourceToolTip = rate.getString("displayMultiSourceToolTip").toString();
			if(displayMultiSourceToolTip.contains("true"))
				continue;
			}

		if (((rate.get("roomName").toString().indexOf("Einzelzimmer") > -1)
				|| (rate.get("roomName").toString()
						.indexOf("Single Room") > -1)
				|| (rate.get("roomName").toString()
						.indexOf("1Einzelbett") > -1) 
				|| (rate.get("roomName").toString().
						indexOf("1 Single Bed") > -1)
				||  (rate.get("roomName").toString().
						indexOf("Doppelzimmer zur Einzelnutzung") > -1))
				&& roomType.equals("DOUBLE")) {
			continue;
		}
		JSONObject priceObj = rate.getJSONObject("price")
			.getJSONObject("priceObject");
		cheapestPrice = priceObj.get("amount").toString();
		double ratePrice = Double.valueOf(cheapestPrice);
		if (ratePrice < minPrice) {
		    minPrice = ratePrice;
		    counter = i;
		}
	    }
	} catch (JSONException e) {
	    log.debug("JSON object missing " + e);
	}
	return counter;
    }

    private static double getCheapestPrice(JSONObject cheapestRate) {
	double minPrice = 0;
	try {
	    minPrice = Double.valueOf(cheapestRate.getJSONObject("price")
		    .get("unformattedTotalPrice").toString()
		    .replaceAll("[^\\d.]", ""));// CHECK THIS!!!!!!!!!!
	} catch (JSONException e) {
	    log.debug("JSON object missing " + e);
	}
	return minPrice;
    }

    private static String getCurrency(JSONObject cheapestRatePriceObj) {
	String currency = "";
	try {
	    currency = cheapestRatePriceObj.get("currency").toString();
	} catch (JSONException e) {
	    log.debug("JSON object missing " + e);
	}
	return currency;

    }

    private static String getRoomDescription(JSONObject cheapestRate) {
	String roomDescription = "";
	try {
	    roomDescription = cheapestRate.get("roomName").toString();
	    roomDescription = roomDescription.replaceAll(
		    "[^a-zA-Z0-9���������������\\s]", "");
	} catch (JSONException e) {
	    log.debug("JSON object missing " + e);
	}

	return roomDescription;
    }

    private static String getRateDescription(JSONObject cheapestRate) {

	String rateDescription = "";
	try {
	    JSONObject rateDescObj = cheapestRate.getJSONObject("includedFees");
	    JSONArray feesArray = rateDescObj.getJSONArray("fees");
	    if (feesArray.length() == 0) {
		rateDescObj = cheapestRate.getJSONObject("excludedFees");
		feesArray = rateDescObj.getJSONArray("fees");
	    }

	    for (int i = 0; i < feesArray.length(); i++) {
		JSONObject rate = feesArray.optJSONObject(i);
		rateDescription = rate.get("name").toString() + " "
			+ rate.get("amount").toString() + " "
			+ rate.get("currency").toString() + "\n";
	    }
	    String amenities = cheapestRate.get("amenities").toString();
	    amenities = amenities.replaceAll(":", " ");
	    amenities = amenities.replaceAll("[^a-zA-Z���������������\\s]", "");
	    rateDescription = rateDescription + amenities;
	    rateDescription = rateDescription
		    .replaceAll("^\\p{L}\\p{Nd}]+", "");
	} catch (JSONException e) {
	    log.debug("JSON object missing " + e);
	}
	return rateDescription;

    }

    private String getNumberOfAdults() {
	String adults;
	if (req_roomType != null) {
	    if (req_roomType.equals("DOUBLE"))
		adults = "2";
	    else
		adults = "1";
	} else {
	    adults = "1";
	}
	return adults;
    }

    public static String getJSONUrl(String hotelId, String checkIn,
			String checkOut, String adults, String children) {
		StringBuffer urlJsonBuff = new StringBuffer(
				"https://www.expedia.de/api/infosite/");
		urlJsonBuff.append(hotelId);
		urlJsonBuff.append("/getOffers?token=");
		urlJsonBuff.append(token);
		urlJsonBuff.append("&chkin=");
		urlJsonBuff.append(checkIn);
		urlJsonBuff.append("&chkout=");
		urlJsonBuff.append(checkOut);
		urlJsonBuff.append("&adults=");
		urlJsonBuff.append(adults);
		urlJsonBuff.append("&children=");
		urlJsonBuff.append(children);

		return urlJsonBuff.toString();
		// String urlJson = "https://www.expedia.de/api/infosite/" + req_context
		// + "/getOffers?token=" + token
		// +
		// "&chkin=04.11.2016&chkout=05.11.2016&adults=2&children=0";
	}

    public static String parseVariableFromSite(String page, String property) {

	String value = null;
	Scanner scanner = new Scanner(page);
	while (scanner.hasNextLine()) {
	    String line = scanner.nextLine();
	    if (line.contains(property)) {
		value = line.substring(line.indexOf('\'') + 1,
			line.lastIndexOf('\''));
	    }
	}

	System.out.println(property + " : " + value);

	return value;
    }

	private String cleanHotelName(String req_hotelName) {
		if (req_hotelName.indexOf(".") > -1)
			req_hotelName = req_hotelName.replace(".", "");
		if (req_hotelName.indexOf("å") > -1)
			req_hotelName = req_hotelName.replaceAll("å", "a");
		if (req_hotelName.indexOf("ä") > -1)
			req_hotelName = req_hotelName.replaceAll("ä", "a");
		if (req_hotelName.indexOf("u0026") > -1)
			req_hotelName = req_hotelName.replaceAll("\u0026", "");
		if (req_hotelName.indexOf("u0026") > -1)
			req_hotelName = req_hotelName.replaceAll("u0026", "");
		if (req_hotelName.indexOf("u0027") > -1)
			req_hotelName = req_hotelName.replaceAll("\u0027", "'");
		if (req_hotelName.indexOf("u0027") > -1)
			req_hotelName = req_hotelName.replaceAll("u0027", "'");
		if (req_hotelName.indexOf("�") > -1)
			req_hotelName = req_hotelName.replaceAll("�", "o");
		if (req_hotelName.indexOf("ö") > -1)
			req_hotelName = req_hotelName.replaceAll("ö", "o");

		req_hotelName = HtmlSpecialChars.transform(req_hotelName);
		return req_hotelName;
	}

    public Div parseHotelContent(Page preLastPage) throws ParserException {
	return (Div) HtmlParserUtils.getTagByAttribute(preLastPage.getContent()
		.toString(), preLastPage.getContentEncoding(), "id",
		"HotelContent");
    }

    public void checkRoomFound(String preLastPageCnt)
	    throws TranslatorException {
	if (preLastPageCnt.indexOf(ROOM_NOT_FOUND_MESSAGE) > -1
		|| preLastPageCnt.indexOf(ROOM_NOT_FOUND_MESSAGE1) > -1
		|| preLastPageCnt.indexOf(ROOM_NOT_FOUND_MESSAGE2) > -1
		|| preLastPageCnt.indexOf(ROOM_NOT_FOUND_MESSAGE3) > -1
		|| preLastPageCnt.indexOf(ROOM_NOT_FOUND_MESSAGE4) > -1
		|| preLastPageCnt.indexOf(ROOM_NOT_FOUND_MESSAGE5) > -1
		|| preLastPageCnt.indexOf(ROOM_NOT_FOUND_MESSAGE6) > -1) {
	    this.throwPageError("getRates() no rooms found", null,
		    ErrorCodes.NO_SUITABLE_ROOMS_FOUND_WARNING, "");
	}
    }

    public String getNextPage(Page firstHotelPage, int pageCounter,
	    hotelSearchInfo hsiObj) throws IOException, ParserException,
	    TranslatorException {

	String pageForm = firstHotelPage.getContent().toString();
	if (pageForm.contains(ERROR_404_PAGE_NOT_FOUND_MSG)) {
	    this.throwPageError("getNextPage() hotel not found", null,
		    ErrorCodes.HOTEL_NOT_FOUND, "");
	}

	// AJAX Search engine (Execute with only the necessary parameters)...
	StringBuffer strUrl = new StringBuffer(req_homepage);
	strUrl.append("/Hotel-Search-AJAX?action=filterSearch");
	strUrl.append("&adultCountInput=").append(hsiObj.numOfAdults);
	strUrl.append("&childCountInput=").append(hsiObj.numOfKids);
	strUrl.append("&hidChildAgeArray=0");
	strUrl.append("&hidChildArray=0");
	strUrl.append("&hidCurrentRoom=0");
	strUrl.append("&hidRegionType=CITY");
	strUrl.append("&hidSM=0");
	strUrl.append("&hidSeniorArray=0");
	strUrl.append("&hideSortType=0");
	strUrl.append("&inpAirport=").append(hsiObj.City);
	strUrl.append("&inpAttraction=").append(hsiObj.City);
	strUrl.append("&inpCity=").append(hsiObj.City);
	strUrl.append("&inpCityForHotelGroup=").append(hsiObj.City);
	strUrl.append("&inpEndDate=").append(hsiObj.endDate);
	strUrl.append("&inpPageIndex=").append(pageCounter);
	strUrl.append("&inpPaginationRequest=1");
	strUrl.append("&inpRegionType=CITY");
	strUrl.append("&inpShoppingMode=0");
	strUrl.append("&inpStartDate=").append(hsiObj.startDate);
	strUrl.append("&inpVersion=3");
	strUrl.append("&roomCountInput=").append(hsiObj.numOfRooms);
	strUrl.append("&selSortType=0");

	String nextPage = this.workflowSession.makeRequestLuminati("POST",
		strUrl.toString(), country);

	String strPageContent = nextPage;
	String strToFindStart = "<hrd><![CDATA[";

	int startInd = strPageContent.indexOf(strToFindStart);
	int endInd = strPageContent.indexOf("</hrd>");

	if (startInd > (int) 0 && endInd > (int) 0) {
	    strPageContent = strPageContent.substring(
		    startInd + strToFindStart.length(), endInd);
	    nextPage = (new StringBuffer(strPageContent)).toString();
	} else {
	    this.throwPageError(
		    "getNextPage() Parsing error: Cannot find <hrd> in AJAX XML",
		    null, ErrorCodes.WEBPAGE_PARSING_ERROR, "");
	}

	return nextPage;
    } // getNextPage

    public String getAddress(String hotelAddressPage) throws ParserException,
	    TranslatorException {

	String addressContent = null;
	String address = null;
	if (hotelAddressPage != null) {
	    addressContent = hotelAddressPage;
	    if (addressContent.contains(ERROR_404_PAGE_NOT_FOUND_MSG)) {
		this.throwPageError("getAllHotels() hotel not found", null,
			ErrorCodes.HOTEL_NOT_FOUND, "");
	    }
	    String hotelParam = "street-address\\\">";
	    int startHot = addressContent.indexOf(hotelParam);
	    startHot = startHot + hotelParam.length();
	    int endHot = addressContent.indexOf("</span>", startHot);
	    if ((startHot >= 0) && (endHot >= 0)) {
		address = addressContent.substring(startHot, endHot);
		address = address.trim();
	    }
	} else {
	    this.throwPageError(" getAddress() address of hotel not found",
		    null, ErrorCodes.HOTEL_NOT_FOUND, "");
	}
	return address;

    }

    public String cleanAddress(String uncleanAddress) {

	String address = null;
	String coma = ",";

	if (uncleanAddress.indexOf(coma) > -1) {
	    int comaIndex = uncleanAddress.indexOf(coma);
	    uncleanAddress = uncleanAddress.substring(0, comaIndex);

	}
	uncleanAddress = uncleanAddress.replaceAll("<span>", "");
	uncleanAddress = Utils.trimEx(uncleanAddress);
	uncleanAddress = HtmlSpecialChars.transform(uncleanAddress);
	address = uncleanAddress;
	return address;

    }

    public String searchForCurrency(TableRow[] rowsInfo) {

	NodeList currencyMark = null;
	String currencyMarkCnt = null;
	currencyMark = rowsInfo[3].getChild(0).getChildren();
	currencyMarkCnt = currencyMark.elementAt(1).toPlainTextString();
	if (rowsInfo[0].toHtml().indexOf(
		", sind die <B>Preise in Euro angegeben</B>") > -1
		|| rowsInfo[0].toHtml().indexOf("Rates quoted in Euro") > -1
		|| rowsInfo[0].toHtml().indexOf(
			"sind die Preise in Euro angegeben") > -1
		|| rowsInfo[0]
			.toHtml()
			.indexOf(
				"Die Preise in Euro entsprechen dem momentanen Wechselkurs,") > -1) {
	    return currencyMarkCnt = "ï¿½";
	}
	if (currencyMarkCnt.indexOf("Preise in Euro angegeben") > -1) {
	    return currencyMarkCnt = "ï¿½";
	} else {
	    return null;
	}
    }

    public String getSuggestParams(String cnt) {

	String CITY_CUT = "\"f\":\"";
	// ","
	String ID_CUT = "\"id\":\"";
	// ","

	StringBuffer paramsBuffer = new StringBuffer("");

	int startCityName = cnt.indexOf(CITY_CUT);
	startCityName = startCityName + CITY_CUT.length();
	int endCityName = cnt.indexOf("\",\"", startCityName);
	if (startCityName < endCityName) {
	    paramsBuffer.append(cnt.substring(startCityName, endCityName));
	} else {
	    log.debug("error substringing");
	}

	int startCityId = cnt.indexOf(ID_CUT);
	startCityId = startCityId + ID_CUT.length();
	int endCityId = cnt.indexOf("\",\"", startCityId);
	if (startCityId < endCityId) {
	    paramsBuffer.append("|").append(
		    cnt.substring(startCityId, endCityId));
	} else {
	    log.debug("error substringing");
	}

	return paramsBuffer.toString();
    }

    public String getHashParams(String cnt) {

	String HASH_CUT = "hashParam: '";
	// ',
	String hashParam = "";
	int startHash = cnt.indexOf(HASH_CUT);
	startHash = startHash + HASH_CUT.length();
	int endHash = cnt.indexOf("',", startHash);
	if (startHash < endHash) {
	    hashParam = cnt.substring(startHash, endHash);
	} else {
	    log.debug("error substringing");
	}
	return hashParam;
    }
}

class hotelSearchInfo {

    String City = null;
    String startDate = null;
    String endDate = null;
    String numOfRooms = null;
    String numOfAdults = null;
    String numOfKids = null;

    public hotelSearchInfo(String c, String sd, String ed, String nor,
	    String noa, String nok) {
	this.City = c;
	this.startDate = sd;
	this.endDate = ed;
	this.numOfRooms = nor;
	this.numOfAdults = noa;
	this.numOfKids = nok;
    }

    public void clear() {
	this.City = "";
	this.startDate = "";
	this.endDate = "";
	this.numOfRooms = "";
	this.numOfAdults = "";
	this.numOfKids = "";
    }

    public void dispose() {
	this.City = null;
	this.startDate = null;
	this.endDate = null;
	this.numOfRooms = null;
	this.numOfAdults = null;
	this.numOfKids = null;
    }

    public String toString() {
	return new String("City=" + this.City + " \n" + "StartDate="
		+ this.startDate + " \n" + "EndDate=" + this.endDate + " \n"
		+ "NumberOfRooms=" + this.numOfRooms + " \n"
		+ "NumberOdAdults=" + this.numOfAdults + " \n"
		+ "NumberOfKids=" + this.numOfKids + " \n");
    }
}
