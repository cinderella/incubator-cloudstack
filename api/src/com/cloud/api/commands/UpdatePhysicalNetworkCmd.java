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

import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.PhysicalNetworkResponse;
import com.cloud.async.AsyncJob;
import com.cloud.event.EventTypes;
import com.cloud.network.PhysicalNetwork;
import com.cloud.user.Account;

@Implementation(description="Updates a physical network", responseObject=PhysicalNetworkResponse.class, since="3.0.0")
public class UpdatePhysicalNetworkCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(UpdatePhysicalNetworkCmd.class.getName());

    private static final String s_name = "updatephysicalnetworkresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @IdentityMapper(entityTableName="physical_network")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="physical network id")
    private Long id;

    @Parameter(name=ApiConstants.NETWORK_SPEED, type=CommandType.STRING, description="the speed for the physical network[1G/10G]")
    private String speed;

    @Parameter(name=ApiConstants.TAGS, type=CommandType.LIST, collectionType=CommandType.STRING, description="Tag the physical network")
    private List<String> tags;
    
    @Parameter(name=ApiConstants.STATE, type=CommandType.STRING, description="Enabled/Disabled")
    private String state;

    @Parameter(name=ApiConstants.VLAN, type=CommandType.STRING, description="the VLAN for the physical network")
    private String vlan;
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    
    public List<String> getTags() {
        return tags;
    }

    public String getNetworkSpeed() {
        return speed;
    }
    
    public String getState() {
        return state;
    }

    public Long getId() {
        return id;
    }

    public String getVlan() {
        return vlan;
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
        PhysicalNetwork result = _networkService.updatePhysicalNetwork(getId(),getNetworkSpeed(), getTags(), getVlan(), getState());
        if (result != null) {
            PhysicalNetworkResponse response = _responseGenerator.createPhysicalNetworkResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        }else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update physical network");
        }
    }

    @Override
    public String getEventDescription() {
        return  "Updating Physical network: " + getId();
    }
    
    @Override
    public String getEventType() {
        return EventTypes.EVENT_PHYSICAL_NETWORK_UPDATE;
    }
    
    @Override
    public AsyncJob.Type getInstanceType() {
        return AsyncJob.Type.PhysicalNetwork;
    }
}
