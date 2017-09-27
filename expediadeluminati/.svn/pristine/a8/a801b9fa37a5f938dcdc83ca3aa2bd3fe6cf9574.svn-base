/*
 * Available.java
 *
 * Copyright (c) 2003 Hotel Reservation Service GmbH
 * All rights reserved.
 */
package de.hrs.marketwatch.webtranslator.processors.expediadeluminati;

import java.util.Collection;

import org.apache.log4j.Logger;

import si.hermes.bc.webtranslator.actionmanager.WorkflowSession;
import si.hermes.bc.webtranslator.processors.Processor;
import si.hermes.bc.webtranslator.providermanager.Provider;
import si.hermes.bc.webtranslator.utils.AvailRSWrapper;
import si.hermes.bc.webtranslator.utils.Page;
import ba.hermes.translator.AvailRequest;
import ba.hermes.translator.AvailResponse;
import ba.hermes.translator.Request;
import ba.hermes.translator.Response;
import ba.hermes.translator.WebRateInfo;
import de.hrs.marketwatch.common.ErrorCodes;
import si.hermes.bc.webtranslator.utils.ErrorHandler;
import de.hrs.marketwatch.webtranslator.processors.common.AvailRQ;
import de.hrs.marketwatch.webtranslator.processors.common.RateInfo;
import de.hrs.marketwatch.webtranslator.processors.common.TranslatorException;

public class Available extends SearchAvailCmn implements Processor{

        private static final Logger log = Logger.getLogger(Available.class.getName());

        public Response parsePage(Page page, Request otarequest, Provider provider,
                        WorkflowSession workflowSession) {
                 log.debug("parsePage(): entering...");
                 Response response = new AvailResponse();
                try {

                    AvailRQ request = new AvailRQ((AvailRequest) otarequest, false);
                    if (!request.validate()) {
                        log.error("parsePage(): request error!");
                        return ErrorHandler.addErrorWarning(response,
                                ErrorCodes.REQUEST_ERROR);
                    }

                    setAllFields(request, provider, workflowSession);
                    setProperties();
                    setDateFields("d.M.yyyy");

                    if (page == null) {
                        log.error("parsePage(): Web page parsing error!");
                        return ErrorHandler.addErrorWarning(response,
                                ErrorCodes.WEBPAGE_PARSING_ERROR);
                    }
                        Collection<RateInfo> rates = getRates();

                        if (rates == null) {
                        log.error("parsePage(): rates collection is null!");
                        response = ErrorHandler.addErrorWarning(response,
                                ErrorCodes.REQUIRED_INFO_NOT_FOUND_ERROR);
                        return response;
                    }
                    WebRateInfo stay = null;
                    //
                    if (rates.size() > 0) {
                        RateInfo rateInfo = (RateInfo) (rates.iterator().next());
                        stay = new WebRateInfo();
                        AvailRSWrapper.setRateInfo(stay, rateInfo, request.getRoomType());
                        ((AvailResponse)response).addRoomStay(stay);
                        // Check for pegasus
                        if (rateInfo.isPegasus()) {
                            response = ErrorHandler.addErrorWarning(response,
                                    ErrorCodes.PEGASUS_WARNING);
                        }
                    }
                }catch (TranslatorException ex) {
                    response = ErrorHandler.addErrorWarning(response, ex.getOtaErrorCode());
                } catch (Exception ex) {
                    log.error("parsePage(): Internal server error", ex);
                    return ErrorHandler.addErrorWarning(response,
                            ErrorCodes.INTERNAL_SERVER_ERROR);
                }
                return response;
            }
}
