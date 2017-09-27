/*
 * Search.java
 *
 * Copyright (c) 2003 Hotel Reservation Service GmbH
 * All rights reserved.
 */
package de.hrs.marketwatch.webtranslator.processors.expediadeluminati;


import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;

import si.hermes.bc.webtranslator.actionmanager.WorkflowSession;
import si.hermes.bc.webtranslator.processors.Processor;
import si.hermes.bc.webtranslator.providermanager.Provider;
import si.hermes.bc.webtranslator.utils.Page;
import si.hermes.bc.webtranslator.utils.SearchRSWrapper;
import ba.hermes.translator.Request;
import ba.hermes.translator.Response;
import ba.hermes.translator.SearchRequest;
import ba.hermes.translator.SearchResponse;
import ba.hermes.translator.WebHotelInfo;
import de.hrs.marketwatch.common.ErrorCodes;
import si.hermes.bc.webtranslator.utils.ErrorHandler;
import de.hrs.marketwatch.webtranslator.processors.common.HotelInfo;
import de.hrs.marketwatch.webtranslator.processors.common.SearchRQ;
import de.hrs.marketwatch.webtranslator.processors.common.TranslatorException;


public class Search extends SearchAvailCmn implements Processor{
        
    private static final Logger log = Logger.getLogger(Search.class.getName());

        public Response parsePage(Page page, Request otarequest, Provider provider, 
                        WorkflowSession workflowSession) {
                                
        log.debug("parsePage(): entering...");
        Response response = new SearchResponse();
        
        try {
            SearchRQ request = new SearchRQ((SearchRequest) otarequest);
            if (!request.validate()) {
                log.error("parsePage(): invalid request!");
                return ErrorHandler.addErrorWarning(response,
                        ErrorCodes.REQUEST_ERROR);
            }

            setAllFields(request, provider, workflowSession);
            setProperties();
            setDateFields("d.M.yyyy");

            if (page == null) {
                log.error("parsePage(): Web page inaccesible error!");
                return ErrorHandler.addErrorWarning(response,
                        ErrorCodes.WEBPAGE_INACCESSIBLE_ERROR);
            }
            Collection<HotelInfo> hotelsCollect = this.getAllHotels(page);
            
            if (hotelsCollect.size() > 0) {
                //fill the property list
                Iterator iter = hotelsCollect.iterator();
                while (iter.hasNext()) {
                    HotelInfo thehotel = (HotelInfo) iter.next();
                    WebHotelInfo p = SearchRSWrapper.makeProperty(
                            thehotel,
                            req_cityVariations[0],
                            null,
                            req_ISOCountryCode); //context
                    ((SearchResponse)response).addWebHotelInfo(p);
                }
            } else {
                log.warn("parsePage(): no hotels found.");
                response = ErrorHandler.addErrorWarning(response, 
                        ErrorCodes.NO_HOTELS_FOUND_WARNING);
            }
        } catch (TranslatorException e) {
            response = ErrorHandler.addErrorWarning(response, e.getOtaErrorCode());
        } catch (Exception ex) {
            log.error("parsePage(): Internal server error", ex);
            return ErrorHandler.addErrorWarning(response,
                    ErrorCodes.INTERNAL_SERVER_ERROR);
        }
        return response;
    }
}