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

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.RegionResponse;
import com.cloud.region.Region;
import com.cloud.user.Account;

@Implementation(description="Adds a Region", responseObject=RegionResponse.class)
public class AddRegionCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(AddRegionCmd.class.getName());

    private static final String s_name = "addregionresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="Id of the Region")
    private Long id;
    
    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="adds Region with this name")
    private String regionName;

    @Parameter(name=ApiConstants.END_POINT, type=CommandType.STRING, required=true, description="end_point of the Region")
    private String endPoint;
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }
    
    public String getRegionName() {
        return regionName;
    }

    public String getEndPoint() {
        return endPoint;
    }  

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }
    
    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
    
    @Override
    public void execute(){
        Region region = _regionService.addRegion(getId(), getRegionName(), getEndPoint());
        if (region != null) {
        	RegionResponse response = _responseGenerator.createRegionResponse(region);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to add Region");
        }
    }
}
