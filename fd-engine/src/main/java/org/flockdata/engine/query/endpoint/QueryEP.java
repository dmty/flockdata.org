/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.engine.query.endpoint;

import org.flockdata.engine.matrix.MatrixResults;
import org.flockdata.engine.query.service.MatrixService;
import org.flockdata.engine.query.service.QueryService;
import org.flockdata.engine.track.service.ConceptService;
import org.flockdata.helper.CompanyResolver;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.JsonUtils;
import org.flockdata.helper.NotFoundException;
import org.flockdata.model.Company;
import org.flockdata.search.model.*;
import org.flockdata.track.bean.DocumentResultBean;
import org.flockdata.track.bean.MatrixInputBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Query track services
 * User: mike
 * Date: 5/04/14
 * Time: 9:31 AM
 */
@RestController
@RequestMapping("${org.fd.engine.system.api:api}/v1/query")
public class QueryEP {

    private final MatrixService matrixService;

    private final QueryService queryService;

    private final ConceptService conceptService;

    @Autowired
    public QueryEP(MatrixService matrixService, ConceptService conceptService, QueryService queryService) {
        this.matrixService = matrixService;
        this.conceptService = conceptService;
        this.queryService = queryService;

    }

    @RequestMapping(value = "/matrix", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public MatrixResults getMatrixResult(@RequestBody MatrixInputBean matrixInput, HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return matrixService.getMatrix(company, matrixInput);
    }


    @RequestMapping(value = "/", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public EsSearchResult searchQueryParam(@RequestBody QueryParams queryParams, HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return queryService.search(company, queryParams);
    }

    @RequestMapping(value = "/es", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public Map<String, Object> searchEsParam(@RequestBody QueryParams queryParams, HttpServletRequest request) throws FlockException, IOException {
        Company company = CompanyResolver.resolveCompany(request);
        queryParams.setEntityOnly(false);
        queryParams.setCompany(company.getName());

        EsSearchResult result = queryService.search(company, queryParams);
        if (result.getJson() == null)
            throw new NotFoundException("No search results were found");
        return JsonUtils.toMap(result.getJson());
    }

    @RequestMapping(value = "/tagcloud", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public TagCloud getTagCloudEsParam(@RequestBody TagCloudParams tagCloudParams, HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return queryService.getTagCloud(company, tagCloudParams);
    }

    @RequestMapping(value = "/key", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public EntityKeyResults getKeys(@RequestBody QueryParams queryParams, HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return queryService.getKeys(company, queryParams);
    }


    @RequestMapping(value = "/documents", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    @Deprecated // fd-view is using this. it should point to the /doc/ EP
    public Collection<DocumentResultBean> getDocumentsInUse(@RequestBody(required = false) Collection<String> fortresses, HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return conceptService.getDocumentsInUse(company, fortresses);
    }


    @RequestMapping(value = "/concepts", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    @Deprecated // fd-view is using this. it should point to the /concept/ EP
    public Set<DocumentResultBean> getConcepts(@RequestBody(required = false) Collection<String> documents, HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return conceptService.findConcepts(company, documents, false);
    }


    @RequestMapping(value = "/relationships", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    @Deprecated // fd-view is using this. it should point to the /concept/ EP
    public Set<DocumentResultBean> getRelationships(@RequestBody(required = false) Collection<String> documents, HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        // Todo: DAT-100 Sherry's comment. Should be Concepts, not Doc Types
        return conceptService.findConcepts(company, documents, true);
    }


}
