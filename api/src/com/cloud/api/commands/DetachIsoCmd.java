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
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.UserVmResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.uservm.UserVm;

@Implementation(description="Detaches any ISO file (if any) currently attached to a virtual machine.", responseObject=UserVmResponse.class)
public class DetachIsoCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DetachIsoCmd.class.getName());

    private static final String s_name = "detachisoresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="vm_instance")
    @Parameter(name=ApiConstants.VIRTUAL_MACHINE_ID, type=CommandType.LONG, required=true, description="The ID of the virtual machine")
    private Long virtualMachineId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getVirtualMachineId() {
        return virtualMachineId;
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
        UserVm vm = _entityMgr.findById(UserVm.class, getVirtualMachineId());
        if (vm != null) {
            return vm.getAccountId();
        } else {
            throw new InvalidParameterValueException("Unable to find vm by id " + getVirtualMachineId());
        }  
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_ISO_DETACH;
    }

    @Override
    public String getEventDescription() {
        return  "detaching ISO from vm: " + getVirtualMachineId();
    }
	
    @Override
    public void execute(){
        boolean result = _templateService.detachIso(virtualMachineId);
        if (result) {
            UserVm userVm = _entityMgr.findById(UserVm.class, virtualMachineId);         
            UserVmResponse response = _responseGenerator.createUserVmResponse("virtualmachine", userVm).get(0);            
            response.setResponseName(DeployVMCmd.getResultObjectName());           
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to detach iso");
        }
    }
}
