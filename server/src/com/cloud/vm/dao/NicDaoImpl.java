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
package com.cloud.vm.dao;

import java.util.List;

import javax.ejb.Local;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.vm.NicVO;
import com.cloud.vm.VirtualMachine;

@Local(value=NicDao.class)
public class NicDaoImpl extends GenericDaoBase<NicVO, Long> implements NicDao {
    private final SearchBuilder<NicVO> AllFieldsSearch;
    private final GenericSearchBuilder<NicVO, String> IpSearch;
    
    protected NicDaoImpl() {
        super();
        
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("instance", AllFieldsSearch.entity().getInstanceId(), Op.EQ);
        AllFieldsSearch.and("network", AllFieldsSearch.entity().getNetworkId(), Op.EQ);
        AllFieldsSearch.and("vmType", AllFieldsSearch.entity().getVmType(), Op.EQ);
        AllFieldsSearch.and("address", AllFieldsSearch.entity().getIp4Address(), Op.EQ);
        AllFieldsSearch.and("isDefault", AllFieldsSearch.entity().isDefaultNic(), Op.EQ);
        AllFieldsSearch.done();
        
        IpSearch = createSearchBuilder(String.class);
        IpSearch.select(null, Func.DISTINCT, IpSearch.entity().getIp4Address());
        IpSearch.and("network", IpSearch.entity().getNetworkId(), Op.EQ);
        IpSearch.and("address", IpSearch.entity().getIp4Address(), Op.NNULL);
        IpSearch.done();
    }
    
    @Override
    public void removeNicsForInstance(long instanceId) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("instance", instanceId);
        remove(sc);
    }
    
    @Override
    public List<NicVO> listByVmId(long instanceId) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("instance", instanceId);
        return listBy(sc);
    }
    
    @Override
    public List<NicVO> listByVmIdIncludingRemoved(long instanceId) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("instance", instanceId);
        return listIncludingRemovedBy(sc);
    }
    
    
    @Override
    public List<String> listIpAddressInNetwork(long networkId) {
        SearchCriteria<String> sc = IpSearch.create();
        sc.setParameters("network", networkId);
        return customSearch(sc, null);
    }
    
    @Override
    public List<NicVO> listByNetworkId(long networkId) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("network", networkId);
        return listBy(sc);
    }
    
    @Override
    public NicVO findByInstanceIdAndNetworkId(long networkId, long instanceId) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("network", networkId);
        sc.setParameters("instance", instanceId);
        return findOneBy(sc);
    }
    
    @Override
    public NicVO findByInstanceIdAndNetworkIdIncludingRemoved(long networkId, long instanceId) {
        SearchCriteria<NicVO> sc = createSearchCriteria();
        sc.addAnd("networkId", SearchCriteria.Op.EQ, networkId);
        sc.addAnd("instanceId", SearchCriteria.Op.EQ, instanceId);
        return findOneIncludingRemovedBy(sc);
    }
    
    @Override
    public NicVO findByNetworkIdAndType(long networkId, VirtualMachine.Type vmType) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("network", networkId);
        sc.setParameters("vmType", vmType);
        return findOneBy(sc);
    }
    
    @Override
    public NicVO findByIp4AddressAndNetworkId(String ip4Address, long networkId) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("address", ip4Address);
        sc.setParameters("network", networkId);
        return findOneBy(sc);
    }
    
    @Override
    public NicVO findDefaultNicForVM(long instanceId) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("instance", instanceId);
        sc.setParameters("isDefault", 1);
        return findOneBy(sc);
    }
}
