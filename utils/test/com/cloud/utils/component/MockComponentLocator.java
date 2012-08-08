// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.utils.component;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.NoOp;

import com.cloud.utils.Pair;
import com.cloud.utils.db.DatabaseCallback;
import com.cloud.utils.db.DatabaseCallbackFilter;
import com.cloud.utils.db.GenericDao;

/**
 * defining mock components.
 */
public class MockComponentLocator extends ComponentLocator {
    MockComponentLibrary _library = new MockComponentLibrary();

    public MockComponentLocator(String server) {
        super(server);
    }

    public ComponentInfo<? extends GenericDao<?, ? extends Serializable>> addDao(String name, Class<? extends GenericDao<?, ? extends Serializable>> dao) {
        return _library.addDao(name, dao);
    }

    public ComponentInfo<Manager> addManager(String name, Class<? extends Manager> manager) {
        return _library.addManager(name, manager);
    }

    public <T> ComponentInfo<Adapter> addOneAdapter(Class<T> interphace, String name, Class<? extends T> adapterClass) {
        return _library.addOneAdapter(interphace, name, adapterClass);
    }

    public <T> List<ComponentInfo<Adapter>> addAdapterChain(Class<T> interphace, List<Pair<String, Class<? extends T>>> adapters) {
        return _library.addAdapterChain(interphace, adapters);
    }

    public <T> ComponentInfo<PluggableService> addService(String name, Class<T> serviceInterphace, Class<? extends PluggableService> service) {
        return _library.addService(name, serviceInterphace, service);
    }

    @Override
    protected Pair<XmlHandler, HashMap<String, List<ComponentInfo<Adapter>>>> parse2(String filename) {
        Pair<XmlHandler, HashMap<String, List<ComponentInfo<Adapter>>>> result = new Pair<XmlHandler, HashMap<String, List<ComponentInfo<Adapter>>>>(new XmlHandler("fake"), new HashMap<String, List<ComponentInfo<Adapter>>>());
        _daoMap = new LinkedHashMap<String, ComponentInfo<GenericDao<?, ? extends Serializable>>>();
        _managerMap = new LinkedHashMap<String, ComponentInfo<Manager>>();
        _checkerMap = new LinkedHashMap<String, ComponentInfo<SystemIntegrityChecker>>();
        _adapterMap = new HashMap<String, Adapters<? extends Adapter>>();
        _pluginsMap = new HashMap<String, ComponentInfo<PluggableService>>();
        _factories = new HashMap<Class<?>, Class<?>>();
        _daoMap.putAll(_library.getDaos());
        _managerMap.putAll(_library.getManagers());
        result.second().putAll(_library.getAdapters());
        _factories.putAll(_library.getFactories());
        _pluginsMap.putAll(_library.getPluggableServices());
        return result;
    } 

    public void makeActive(InterceptorLibrary interceptors) {
        s_singletons.clear();
        s_locators.clear();
        s_factories.clear();
        s_callbacks = new Callback[] { NoOp.INSTANCE, new DatabaseCallback()};
        s_callbackFilter = new DatabaseCallbackFilter();
        s_interceptors.clear();
        if (interceptors != null) {
            resetInterceptors(interceptors);
        }
        s_tl.set(this);
        parse("fake file");
    }

    protected class MockComponentLibrary extends ComponentLibraryBase implements ComponentLibrary { 

        @Override
        public Map<String, List<ComponentInfo<Adapter>>> getAdapters() {
            return _adapters;
        }

        @Override
        public Map<Class<?>, Class<?>> getFactories() {
            return new HashMap<Class<?>, Class<?>>();
        }

        @Override
        public Map<String, ComponentInfo<GenericDao<?, ?>>> getDaos() {
            return _daos;
        }

        @Override
        public Map<String, ComponentInfo<Manager>> getManagers() {
            return _managers;
        }

        @Override
        public Map<String, ComponentInfo<PluggableService>> getPluggableServices() {
            return _pluggableServices;
        }
    }
}
