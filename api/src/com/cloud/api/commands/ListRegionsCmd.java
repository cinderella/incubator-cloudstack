// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.DomainResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.RegionResponse;
import com.cloud.region.Region;

@Implementation(description="Lists Regions", responseObject=DomainResponse.class)
public class ListRegionsCmd extends BaseListCmd {
	public static final Logger s_logger = Logger.getLogger(ListDomainsCmd.class.getName());
	
    private static final String s_name = "listregionsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="List domain by domain ID.")
    private Long id;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="List domain by domain name.")
    private String domainName;
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getRegionName() {
        return domainName;
    }
    
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute(){
        List<? extends Region> result = _regionService.listRegions(this);
        ListResponse<RegionResponse> response = new ListResponse<RegionResponse>();
        List<RegionResponse> regionResponses = new ArrayList<RegionResponse>();
        for (Region region : result) {
        	RegionResponse regionResponse = _responseGenerator.createRegionResponse(region);
        	regionResponse.setObjectName("region");
        	regionResponses.add(regionResponse);
        }

        response.setResponses(regionResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
