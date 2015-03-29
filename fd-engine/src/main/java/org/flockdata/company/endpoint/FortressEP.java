/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.company.endpoint;

import org.flockdata.helper.CompanyResolver;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.SecurityHelper;
import org.flockdata.registration.bean.FortressInputBean;
import org.flockdata.registration.bean.FortressResultBean;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.service.CompanyService;
import org.flockdata.track.bean.DocumentResultBean;
import org.flockdata.track.service.FortressService;
import org.flockdata.track.service.SchemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.TimeZone;

/**
 * User: Mike Holdsworth
 * Date: 4/05/13
 * Time: 8:23 PM
 */
@RestController
@RequestMapping("/fortress")
public class FortressEP {

    @Autowired
    CompanyService companyService;

    @Autowired
    SchemaService schemaService;

    @Autowired
    FortressService fortressService;

    @Autowired
    SecurityHelper securityHelper;

    @RequestMapping(value = "/", produces = "application/json", method = RequestMethod.GET)
    public Collection<FortressResultBean> findFortresses(HttpServletRequest request) throws FlockException {
        // curl -u mike:123 -X GET  http://localhost:8080/ab/company/Monowai/fortresses
        Company company = CompanyResolver.resolveCompany(request);
        return fortressService.findFortresses(company);
    }

    @RequestMapping(value = "/", produces = "application/json", consumes = "application/json", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public FortressResultBean registerFortress( @RequestBody FortressInputBean fortressInputBean, HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        Fortress fortress = fortressService.registerFortress(company, fortressInputBean, true);
        return new FortressResultBean(fortress);

    }

    @RequestMapping(value = "/{fortressName}/{docTypeName}", produces = "application/json", consumes = "application/json", method = RequestMethod.PUT)
    public DocumentResultBean registerDocumentType(HttpServletRequest request, @PathVariable("fortressName") String fortressName, @PathVariable("docTypeName") String docTypeName) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        Fortress fortress = fortressService.getFortress(company, fortressName);
        return new DocumentResultBean(schemaService.resolveByDocCode(fortress, docTypeName, Boolean.TRUE), fortress);

    }

    @RequestMapping(value = "/{code}", method = RequestMethod.GET)
    public Fortress getFortress(@PathVariable("code") String fortressCode, HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        Fortress fortress = fortressService.findByCode(company, fortressCode);
        if ( fortress == null)
            fortress = fortressService.findByName(company, fortressCode);

        if (fortress == null)
           throw new FlockException("Unable to locate the fortress "+ fortressCode);

        return fortress;
    }

    @RequestMapping(value = "/{code}", method = RequestMethod.DELETE)
    public String delete(@PathVariable("code") String fortressCode, HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return fortressService.delete(company, fortressCode);
    }


    @RequestMapping(value = "/{code}/docs", method = RequestMethod.GET)
    public Collection<DocumentResultBean> getDocumentTypes(@PathVariable("code") String code, HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return  fortressService.getFortressDocumentsInUse(company, code);
    }
    @RequestMapping(value = "/timezones", method = RequestMethod.GET)
    public String[] getTimezones(HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return TimeZone.getAvailableIDs();
    }

}