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
package com.cloud.api.response;

import com.cloud.api.ApiConstants;
import com.cloud.utils.IdentityProxy;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class NicResponse extends BaseResponse {
    
    @SerializedName("id") @Param(description="the ID of the nic")
    private final IdentityProxy id = new IdentityProxy("nics");

    @SerializedName("networkid") @Param(description="the ID of the corresponding network")
    private final IdentityProxy networkId = new IdentityProxy("networks");
 
    @SerializedName("networkname") @Param(description="the name of the corresponding network")
    private String  networkName ;
    
    @SerializedName(ApiConstants.NETMASK) @Param(description="the netmask of the nic")
    private String netmask;
    
    @SerializedName(ApiConstants.GATEWAY) @Param(description="the gateway of the nic")
    private String gateway;
    
    @SerializedName(ApiConstants.IP_ADDRESS) @Param(description="the ip address of the nic")
    private String ipaddress;
    
    @SerializedName("isolationuri") @Param(description="the isolation uri of the nic")
    private String isolationUri;
    
    @SerializedName("broadcasturi") @Param(description="the broadcast uri of the nic")
    private String broadcastUri;
    
    @SerializedName(ApiConstants.TRAFFIC_TYPE) @Param(description="the traffic type of the nic")
    private String trafficType;
    
    @SerializedName(ApiConstants.TYPE) @Param(description="the type of the nic")
    private String type;
    
    @SerializedName(ApiConstants.IS_DEFAULT) @Param(description="true if nic is default, false otherwise")
    private Boolean isDefault;

    @SerializedName("macaddress") @Param(description="true if nic is default, false otherwise")
    private String macAddress;
    
    public Long getId() {
        return id.getValue();
    }

    public void setId(Long id) {
        this.id.setValue(id);
    }

    public void setNetworkid(Long networkid) {
        this.networkId.setValue(networkid);
    }

    public void setNetworkName(String networkname) {
       this.networkName = networkname;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public void setIpaddress(String ipaddress) {
        this.ipaddress = ipaddress;
    }

    public void setIsolationUri(String isolationUri) {
        this.isolationUri = isolationUri;
    }

    public void setBroadcastUri(String broadcastUri) {
        this.broadcastUri = broadcastUri;
    }

    public void setTrafficType(String trafficType) {
        this.trafficType = trafficType;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

	public void setMacAddress(String macAddress) {
		this.macAddress = macAddress;
	}

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        NicResponse other = (NicResponse) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
    
}
