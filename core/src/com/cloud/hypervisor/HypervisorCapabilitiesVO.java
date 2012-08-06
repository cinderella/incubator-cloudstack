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
package com.cloud.hypervisor;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.api.Identity;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.NumbersUtil;

@Entity
@Table(name="hypervisor_capabilities")
public class HypervisorCapabilitiesVO implements HypervisorCapabilities, Identity {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;

    @Column(name="hypervisor_type")
    @Enumerated(value=EnumType.STRING)
    private HypervisorType hypervisorType;

    @Column(name="hypervisor_version")
    private String hypervisorVersion;

    @Column(name="max_guests_limit")
    private Long maxGuestsLimit;

    @Column(name="security_group_enabled")
    private boolean securityGroupEnabled;

    @Column(name="uuid")
    private String uuid;

    @Column(name="max_data_volumes_limit")
    private Integer maxDataVolumesLimit;

    protected HypervisorCapabilitiesVO() {
    	this.uuid = UUID.randomUUID().toString();
    }

    public HypervisorCapabilitiesVO(HypervisorType hypervisorType, String hypervisorVersion, Long maxGuestsLimit, boolean securityGroupEnabled) {
        this.hypervisorType = hypervisorType;
        this.hypervisorVersion = hypervisorVersion;
        this.maxGuestsLimit = maxGuestsLimit;
        this.securityGroupEnabled = securityGroupEnabled;
    	this.uuid = UUID.randomUUID().toString();
    }

    /**
     * @param hypervisorType the hypervisorType to set
     */
    public void setHypervisorType(HypervisorType hypervisorType) {
        this.hypervisorType = hypervisorType;
    }


    /**
     * @return the hypervisorType
     */
    @Override
    public HypervisorType getHypervisorType() {
        return hypervisorType;
    }

    /**
     * @param hypervisorVersion the hypervisorVersion to set
     */
    public void setHypervisorVersion(String hypervisorVersion) {
        this.hypervisorVersion = hypervisorVersion;
    }

    /**
     * @return the hypervisorVersion
     */
    @Override
    public String getHypervisorVersion() {
        return hypervisorVersion;
    }

    public void setSecurityGroupEnabled(Boolean securityGroupEnabled) {
        this.securityGroupEnabled = securityGroupEnabled;
    }

    /**
     * @return the securityGroupSupport
     */
    @Override
    public boolean isSecurityGroupEnabled() {
        return securityGroupEnabled;
    }

    /**
     * @param maxGuests the maxGuests to set
     */
    public void setMaxGuestsLimit(Long maxGuestsLimit) {
        this.maxGuestsLimit = maxGuestsLimit;
    }

    /**
     * @return the maxGuests
     */
    @Override
    public Long getMaxGuestsLimit() {
        return maxGuestsLimit;
    }


    public long getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return NumbersUtil.hash(id);
    }
    
    @Override
    public String getUuid() {
    	return this.uuid;
    }
    
    public void setUuid(String uuid) {
    	this.uuid = uuid;
    }

    @Override
    public Integer getMaxDataVolumesLimit() {
        return maxDataVolumesLimit;
    }

    public void setMaxDataVolumesLimit(Integer maxDataVolumesLimit) {
        this.maxDataVolumesLimit = maxDataVolumesLimit;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof HypervisorCapabilitiesVO) {
            return ((HypervisorCapabilitiesVO)obj).getId() == this.getId();
        } else {
            return false;
        }
    }

}
