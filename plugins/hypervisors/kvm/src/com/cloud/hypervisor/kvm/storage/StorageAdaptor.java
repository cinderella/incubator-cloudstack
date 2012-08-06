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
package com.cloud.hypervisor.kvm.storage;

import java.util.List;

import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk.PhysicalDiskFormat;
import com.cloud.storage.Storage.StoragePoolType;

public interface StorageAdaptor {

    public KVMStoragePool getStoragePool(String uuid);

    public KVMPhysicalDisk getPhysicalDisk(String volumeUuid,
            KVMStoragePool pool);

    public KVMStoragePool createStoragePool(String name, String host, int port,
            String path, String userInfo, StoragePoolType type);

    public boolean deleteStoragePool(String uuid);

    public KVMPhysicalDisk createPhysicalDisk(String name, KVMStoragePool pool,
            PhysicalDiskFormat format, long size);

    public boolean deletePhysicalDisk(String uuid, KVMStoragePool pool);

    public KVMPhysicalDisk createDiskFromTemplate(KVMPhysicalDisk template,
            String name, PhysicalDiskFormat format, long size,
            KVMStoragePool destPool);

    public KVMPhysicalDisk createTemplateFromDisk(KVMPhysicalDisk disk,
            String name, PhysicalDiskFormat format, long size,
            KVMStoragePool destPool);

    public List<KVMPhysicalDisk> listPhysicalDisks(String storagePoolUuid,
            KVMStoragePool pool);

    public KVMPhysicalDisk copyPhysicalDisk(KVMPhysicalDisk disk, String name,
            KVMStoragePool destPools);

    public KVMPhysicalDisk createDiskFromSnapshot(KVMPhysicalDisk snapshot,
            String snapshotName, String name, KVMStoragePool destPool);

    public KVMStoragePool getStoragePoolByUri(String uri);

    public KVMPhysicalDisk getPhysicalDiskFromURI(String uri);

    public boolean refresh(KVMStoragePool pool);

    public boolean deleteStoragePool(KVMStoragePool pool);

    public boolean createFolder(String uuid, String path);

}
