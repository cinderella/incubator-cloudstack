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
import com.cloud.api.BaseListTaggedResourcesCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.FirewallResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.network.rules.FirewallRule;

@Implementation(description="Lists all firewall rules for an IP address.", responseObject=FirewallResponse.class)
public class ListFirewallRulesCmd extends BaseListTaggedResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListFirewallRulesCmd.class.getName());

    private static final String s_name = "listfirewallrulesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @IdentityMapper(entityTableName="firewall_rules")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="Lists rule with the specified ID.")
    private Long id;
    
    @IdentityMapper(entityTableName="user_ip_address")
    @Parameter(name=ApiConstants.IP_ADDRESS_ID, type=CommandType.LONG, description="the id of IP address of the firwall services")
    private Long ipAddressId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    
    public Long getIpAddressId() {
        return ipAddressId;
    }
    
    public Long getId() {
        return id;
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
        List<? extends FirewallRule> result = _firewallService.listFirewallRules(this);
        ListResponse<FirewallResponse> response = new ListResponse<FirewallResponse>();
        List<FirewallResponse> fwResponses = new ArrayList<FirewallResponse>();
        
        for (FirewallRule fwRule : result) {
            FirewallResponse ruleData = _responseGenerator.createFirewallResponse(fwRule);
            ruleData.setObjectName("firewallrule");
            fwResponses.add(ruleData);
        }
        response.setResponses(fwResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response); 
    }
}
