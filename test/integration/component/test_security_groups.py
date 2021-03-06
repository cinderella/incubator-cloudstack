# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
# 
#   http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

""" P1 for Security groups
"""
#Import Local Modules
import marvin
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin import remoteSSHClient
from integration.lib.utils import *
from integration.lib.base import *
from integration.lib.common import *

#Import System modules
import time
import subprocess


class Services:
    """Test Security groups Services
    """

    def __init__(self):
        self.services = {
                "disk_offering":{
                    "displaytext": "Small",
                    "name": "Small",
                    "disksize": 1
                },
                "account": {
                    "email": "test@test.com",
                    "firstname": "Test",
                    "lastname": "User",
                    "username": "test",
                    # Random characters are appended in create account to 
                    # ensure unique username generated each time
                    "password": "fr3sca",
                },
                "virtual_machine": {
                # Create a small virtual machine instance with disk offering 
                    "displayname": "Test VM",
                    "username": "root",     # VM creds for SSH
                    "password": "password",
                    "ssh_port": 22,
                    "hypervisor": 'XenServer',
                    "privateport": 22,
                    "publicport": 22,
                    "protocol": 'TCP',
                    "userdata": 'This is sample data',
                },
                "host": {
                         "publicport": 22,
                         "username": "root",     # Host creds for SSH
                         "password": "fr3sca",
                },
                "service_offering": {
                    "name": "Tiny Instance",
                    "displaytext": "Tiny Instance",
                    "cpunumber": 1,
                    "cpuspeed": 100, # in MHz
                    "memory": 64, # In MBs
                },
                "security_group": {
                    "name": 'SSH',
                    "protocol": 'TCP',
                    "startport": 22,
                    "endport": 22,
                    "cidrlist": '0.0.0.0/0',
                },
                "security_group_2": {
                    "name": 'ICMP',
                    "protocol": 'ICMP',
                    "startport": -1,
                    "endport": -1,
                    "cidrlist": '0.0.0.0/0',                                     
                },
            "ostypeid": '0c2c5d19-525b-41be-a8c3-c6607412f82b',
            # CentOS 5.3 (64-bit)
            "sleep": 60,
            "timeout": 10,
            "mode":'basic',
            # Networking mode: Basic or Advanced
        }


class TestDefaultSecurityGroup(cloudstackTestCase):
    
    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            self.dbclient.close()
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        cls.services = Services().services
        cls.api_client = super(TestDefaultSecurityGroup, cls).getClsTestClient().getApiClient()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        
        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostypeid"]
                            )
        cls.services["domainid"] = cls.domain.id
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = template.id
        
        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            admin=True,
                            domainid=cls.domain.id
                            )
        cls.services["account"] = cls.account.account.name

        cls._cleanup = [
                        cls.account,
                        cls.service_offering
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = super(TestDefaultSecurityGroup, cls).getClsTestClient().getApiClient()
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    def test_01_deployVM_InDefaultSecurityGroup(self):
        """Test deploy VM in default security group
        """

        # Validate the following:
        # 1. deploy Virtual machine using admin user
        # 2. listVM should show a VM in Running state 
        # 3. listRouters should show one router running

        self.virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    accountid=self.account.account.name,
                                    domainid=self.account.account.domainid,
                                    serviceofferingid=self.service_offering.id
                                )
        self.debug("Deployed VM with ID: %s" % self.virtual_machine.id)
        self.cleanup.append(self.virtual_machine)
        
        list_vm_response = list_virtual_machines(
                                                 self.apiclient,
                                                 id=self.virtual_machine.id
                                                 )

        self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % self.virtual_machine.id
            )
        self.assertEqual(
                         isinstance(list_vm_response, list),
                         True,
                         "Check for list VM response"
                         )
        vm_response = list_vm_response[0]
        self.assertNotEqual(
                            len(list_vm_response),
                            0,
                            "Check VM available in List Virtual Machines"
                        )

        self.assertEqual(

                            vm_response.id,
                            self.virtual_machine.id,
                            "Check virtual machine id in listVirtualMachines"
                        )

        self.assertEqual(
                    vm_response.displayname,
                    self.virtual_machine.displayname,
                    "Check virtual machine displayname in listVirtualMachines"
                    )
        
        # Verify List Routers response for account 
        self.debug(
                   "Verify list routers response for account: %s" \
                   % self.account.account.name
                   )
        routers = list_routers(
                               self.apiclient,
                               zoneid=self.zone.id,
                               listall=True
                               )
        self.assertEqual(
                         isinstance(routers, list),
                         True,
                         "Check for list Routers response"
                         )
        
        self.debug("Router Response: %s" % routers)
        self.assertEqual(
                        len(routers), 
                        1, 
                        "Check virtual router is created for account or not"
                        )
        return

    def test_02_listSecurityGroups(self):
        """Test list security groups for admin account
        """

        # Validate the following:
        # 1. listSecurityGroups in admin account
        # 2. There should be one security group (default) listed for the admin account
        # 3. No Ingress Rules should be part of the default security group
        
        sercurity_groups = SecurityGroup.list(
                                              self.apiclient,
                                              account=self.account.account.name,
                                              domainid=self.account.account.domainid
                                              )
        self.assertEqual(
                         isinstance(sercurity_groups, list),
                         True,
                         "Check for list security groups response"
                         )
        self.assertNotEqual(
                            len(sercurity_groups), 
                            0, 
                            "Check List Security groups response"
                            )
        self.debug("List Security groups response: %s" % 
                                            str(sercurity_groups))
        self.assertEqual(
                         hasattr(sercurity_groups, 'ingressrule'),
                         False,
                         "Check ingress rule attribute for default security group"
                         )
        return
    
    def test_03_accessInDefaultSecurityGroup(self):
        """Test access in default security group
        """

        # Validate the following:
        # 1. deploy Virtual machine using admin user
        # 2. listVM should show a VM in Running state 
        # 3. listRouters should show one router running

        self.virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    accountid=self.account.account.name,
                                    domainid=self.account.account.domainid,
                                    serviceofferingid=self.service_offering.id
                                )
        self.debug("Deployed VM with ID: %s" % self.virtual_machine.id)
        self.cleanup.append(self.virtual_machine)
        
        list_vm_response = list_virtual_machines(
                                                 self.apiclient,
                                                 id=self.virtual_machine.id
                                                 )
        self.assertEqual(
                         isinstance(list_vm_response, list),
                         True,
                         "Check for list VM response"
                         )
        
        self.debug(
                "Verify listVirtualMachines response for virtual machine: %s" \
                % self.virtual_machine.id
            )

        vm_response = list_vm_response[0]
        self.assertNotEqual(
                            len(list_vm_response),
                            0,
                            "Check VM available in List Virtual Machines"
                        )

        self.assertEqual(

                            vm_response.id,
                            self.virtual_machine.id,
                            "Check virtual machine id in listVirtualMachines"
                        )

        self.assertEqual(
                    vm_response.displayname,
                    self.virtual_machine.displayname,
                    "Check virtual machine displayname in listVirtualMachines"
                    )
        # Default Security group should not have any ingress rule
        sercurity_groups = SecurityGroup.list(
                                              self.apiclient,
                                              account=self.account.account.name,
                                              domainid=self.account.account.domainid
                                              )
        self.assertEqual(
                         isinstance(sercurity_groups, list),
                         True,
                         "Check for list security groups response"
                         )
        
        self.debug("List Security groups response: %s" % 
                                            str(sercurity_groups))
        self.assertNotEqual(
                            len(sercurity_groups), 
                            0, 
                            "Check List Security groups response"
                            )
        self.assertEqual(
                         hasattr(sercurity_groups, 'ingressrule'),
                         False,
                         "Check ingress rule attribute for default security group"
                         )
        
        # SSH Attempt to VM should fail
        with self.assertRaises(Exception):
            self.debug("SSH into VM: %s" % self.virtual_machine.ssh_ip)
            ssh = remoteSSHClient.remoteSSHClient(
                                    self.virtual_machine.ssh_ip,
                                    self.virtual_machine.ssh_port,
                                    self.virtual_machine.username,
                                    self.virtual_machine.password
                                    )
        return
    

class TestAuthorizeIngressRule(cloudstackTestCase):
    
    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            self.dbclient.close()
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        cls.services = Services().services
        cls.api_client = super(TestAuthorizeIngressRule, cls).getClsTestClient().getApiClient()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        
        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostypeid"]
                            )
        cls.services["domainid"] = cls.domain.id
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = template.id
        
        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )
        cls.services["account"] = cls.account.account.name
        cls._cleanup = [
                        cls.account,
                        cls.service_offering
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = super(TestAuthorizeIngressRule, cls).getClsTestClient().getApiClient()
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    def test_01_authorizeIngressRule(self):
        """Test authorize ingress rule
        """

        # Validate the following:
        #1. Create Security group for the account.
        #2. Createsecuritygroup (ssh-incoming) for this account
        #3. authorizeSecurityGroupIngress to allow ssh access to the VM
        #4. deployVirtualMachine into this security group (ssh-incoming)
        
        security_group = SecurityGroup.create(
                                              self.apiclient, 
                                              self.services["security_group"], 
                                              account=self.account.account.name, 
                                              domainid=self.account.account.domainid
                                              )
        self.debug("Created security group with ID: %s" % security_group.id)
        # Default Security group should not have any ingress rule
        sercurity_groups = SecurityGroup.list(
                                              self.apiclient,
                                              account=self.account.account.name,
                                              domainid=self.account.account.domainid
                                              )
        self.assertEqual(
                         isinstance(sercurity_groups, list),
                         True,
                         "Check for list security groups response"
                         )
        
        self.assertEqual(
                            len(sercurity_groups), 
                            2, 
                            "Check List Security groups response"
                            )
        # Authorize Security group to SSH to VM
        ingress_rule = security_group.authorize(
                                self.apiclient,
                                self.services["security_group"], 
                                account=self.account.account.name, 
                                domainid=self.account.account.domainid
                                )
        self.assertEqual(
                          isinstance(ingress_rule, dict),
                          True,
                          "Check ingress rule created properly"
                    )
        
        self.debug("Authorizing ingress rule for sec group ID: %s for ssh access" 
                                                            % security_group.id)
        self.virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    accountid=self.account.account.name,
                                    domainid=self.account.account.domainid,
                                    serviceofferingid=self.service_offering.id,
                                    securitygroupids=[security_group.id]
                                )
        self.debug("Deploying VM in account: %s" % self.account.account.name)
        # Should be able to SSH VM
        try:
            self.debug("SSH into VM: %s" % self.virtual_machine.id)
            self.virtual_machine.get_ssh_client()
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.virtual_machine.ipaddress, e)
                      )
        return


class TestRevokeIngressRule(cloudstackTestCase):
    
    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            self.dbclient.close()
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        cls.services = Services().services
        cls.api_client = super(TestRevokeIngressRule, cls).getClsTestClient().getApiClient()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        
        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostypeid"]
                            )
        cls.services["domainid"] = cls.domain.id
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = template.id
        
        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )
        cls.services["account"] = cls.account.account.name
        cls._cleanup = [
                        cls.account,
                        cls.service_offering
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = super(TestRevokeIngressRule, cls).getClsTestClient().getApiClient()
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    def test_01_revokeIngressRule(self):
        """Test revoke ingress rule
        """

        # Validate the following:
        #1. Create Security group for the account.
        #2. Createsecuritygroup (ssh-incoming) for this account
        #3. authorizeSecurityGroupIngress to allow ssh access to the VM
        #4. deployVirtualMachine into this security group (ssh-incoming)
        #5. Revoke the ingress rule, SSH access should fail
        
        security_group = SecurityGroup.create(
                                              self.apiclient, 
                                              self.services["security_group"], 
                                              account=self.account.account.name, 
                                              domainid=self.account.account.domainid
                                              )
        self.debug("Created security group with ID: %s" % security_group.id)
        
        # Default Security group should not have any ingress rule
        sercurity_groups = SecurityGroup.list(
                                              self.apiclient,
                                              account=self.account.account.name,
                                              domainid=self.account.account.domainid
                                              )
        self.assertEqual(
                         isinstance(sercurity_groups, list),
                         True,
                         "Check for list security groups response"
                         )
        
        self.assertEqual(
                            len(sercurity_groups), 
                            2, 
                            "Check List Security groups response"
                            )
        # Authorize Security group to SSH to VM
        self.debug("Authorizing ingress rule for sec group ID: %s for ssh access" 
                                                            % security_group.id)
        ingress_rule = security_group.authorize(
                                        self.apiclient,
                                        self.services["security_group"], 
                                        account=self.account.account.name, 
                                        domainid=self.account.account.domainid
                                        )
        
        self.assertEqual(
                          isinstance(ingress_rule, dict),
                          True,
                          "Check ingress rule created properly"
                    )
        
        ssh_rule = (ingress_rule["ingressrule"][0]).__dict__
        self.virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    accountid=self.account.account.name,
                                    domainid=self.account.account.domainid,
                                    serviceofferingid=self.service_offering.id,
                                    securitygroupids=[security_group.id]
                                )
        self.debug("Deploying VM in account: %s" % self.account.account.name)
        
        # Should be able to SSH VM
        try:
            self.debug("SSH into VM: %s" % self.virtual_machine.id)
            self.virtual_machine.get_ssh_client()
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.virtual_machine.ipaddress, e)
                      )
        
        self.debug("Revoking ingress rule for sec group ID: %s for ssh access" 
                                                            % security_group.id)
        # Revoke Security group to SSH to VM
        result = security_group.revoke(
                                self.apiclient, 
                                id=ssh_rule["ruleid"]
                                )

        # SSH Attempt to VM should fail
        with self.assertRaises(Exception):
            self.debug("SSH into VM: %s" % self.virtual_machine.id)
            remoteSSHClient.remoteSSHClient(
                                        self.virtual_machine.ssh_ip,
                                        self.virtual_machine.ssh_port,
                                        self.virtual_machine.username,
                                        self.virtual_machine.password
                                        )
        return


class TestDhcpOnlyRouter(cloudstackTestCase):
    
    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            self.dbclient.close()
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        cls.services = Services().services
        cls.api_client = super(TestDhcpOnlyRouter, cls).getClsTestClient().getApiClient()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        
        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostypeid"]
                            )
        
        cls.services["domainid"] = cls.domain.id
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = template.id
        
        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )
        cls.services["account"] = cls.account.account.name
        cls.virtual_machine = VirtualMachine.create(
                                    cls.api_client,
                                    cls.services["virtual_machine"],
                                    accountid=cls.account.account.name,
                                    domainid=cls.account.account.domainid,
                                    serviceofferingid=cls.service_offering.id
                                )
        cls._cleanup = [
                        cls.account,
                        cls.service_offering
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = super(TestDhcpOnlyRouter, cls).getClsTestClient().getApiClient()
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    def test_01_dhcpOnlyRouter(self):
        """Test router services for user account
        """
        # Validate the following
        #1. List routers for any user account
        #2. The only service supported by this router should be dhcp
        
        # Find router associated with user account
        list_router_response = list_routers(
                                    self.apiclient,
                                    zoneid=self.zone.id,
                                    listall=True
                                    )
        self.assertEqual(
                            isinstance(list_router_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        router = list_router_response[0]

        hosts = list_hosts(
                           self.apiclient,
                           zoneid=router.zoneid,
                           type='Routing',
                           state='Up',
                           virtualmachineid=self.virtual_machine.id
                           )
        self.assertEqual(
                            isinstance(hosts, list),
                            True,
                            "Check list host returns a valid list"
                        )
        host = hosts[0]
        
        self.debug("Router ID: %s, state: %s" % (router.id, router.state))

        self.assertEqual(
                            router.state,
                            'Running',
                            "Check list router response for router state"
                        )

        result = get_process_status(
                                host.ipaddress,
                                self.services['host']["publicport"],
                                self.services['host']["username"],
                                self.services['host']["password"],
                                router.linklocalip,
                                "service dnsmasq status"
                                )
        res = str(result)
        self.debug("Dnsmasq process status: %s" % res)

        self.assertEqual(
                            res.count("running"),
                            1,
                            "Check dnsmasq service is running or not"
                        )
        return
    

class TestdeployVMWithUserData(cloudstackTestCase):
    
    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            self.dbclient.close()
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        cls.services = Services().services
        cls.api_client = super(TestdeployVMWithUserData, cls).getClsTestClient().getApiClient()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        
        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostypeid"]
                            )
        
        cls.services["domainid"] = cls.domain.id
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = template.id
        
        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )
        cls.services["account"] = cls.account.account.name
        cls._cleanup = [
                        cls.account,
                        cls.service_offering
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = super(TestdeployVMWithUserData, cls).getClsTestClient().getApiClient()
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return
    
    def test_01_deployVMWithUserData(self):
        """Test Deploy VM with User data"""
        
        # Validate the following
        # 1. CreateAccount of type user
        # 2. CreateSecurityGroup ssh-incoming
        # 3. authorizeIngressRule to allow ssh-access
        # 4. deployVirtualMachine into this group with some base64 encoded user-data
        # 5. wget http://10.1.1.1/latest/user-data to get the latest userdata from the
        #    router for this VM
        
        # Find router associated with user account
        list_router_response = list_routers(
                                    self.apiclient,
                                    zoneid=self.zone.id,
                                    listall=True
                                    )
        self.assertEqual(
                            isinstance(list_router_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        router = list_router_response[0]
        
        security_group = SecurityGroup.create(
                                              self.apiclient, 
                                              self.services["security_group"], 
                                              account=self.account.account.name, 
                                              domainid=self.account.account.domainid
                                              )
        self.debug("Created security group with ID: %s" % security_group.id)
        # Default Security group should not have any ingress rule
        sercurity_groups = SecurityGroup.list(
                                              self.apiclient,
                                              account=self.account.account.name,
                                              domainid=self.account.account.domainid
                                              )
        self.assertEqual(
                         isinstance(sercurity_groups, list),
                         True,
                         "Check for list security groups response"
                         )
        self.assertEqual(
                            len(sercurity_groups), 
                            2, 
                            "Check List Security groups response"
                            )
        
        self.debug(
            "Authorize Ingress Rule for Security Group %s for account: %s" \
                % (
                    security_group.id,
                    self.account.account.name
                ))
        
        # Authorize Security group to SSH to VM
        ingress_rule = security_group.authorize(
                                        self.apiclient,
                                        self.services["security_group"], 
                                        account=self.account.account.name, 
                                        domainid=self.account.account.domainid
                                        )
        self.assertEqual(
                          isinstance(ingress_rule, dict),
                          True,
                          "Check ingress rule created properly"
                    )
        self.virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    accountid=self.account.account.name,
                                    domainid=self.account.account.domainid,
                                    serviceofferingid=self.service_offering.id,
                                    securitygroupids=[security_group.id]
                                )
        self.debug("Deploying VM in account: %s" % self.account.account.name)
        # Should be able to SSH VM
        try:
            self.debug(
                       "SSH to VM with IP Address: %s"\
                       % self.virtual_machine.ssh_ip
                       )
            
            ssh = self.virtual_machine.get_ssh_client()
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.virtual_machine.ipaddress, e)
                      )
        
        cmds = [
               "wget http://%s/latest/user-data" % router.guestipaddress,
               "cat user-data",
               ]
        for c in cmds:
            result = ssh.execute(c)
            self.debug("%s: %s" % (c, result))
            
        res = str(result)
        self.assertEqual(
                            res.count(self.services["virtual_machine"]["userdata"]),
                            1, 
                            "Verify user data"
                        )
        return


class TestDeleteSecurityGroup(cloudstackTestCase):
    
    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        
        self.services = Services().services

        # Get Zone, Domain and templates
        self.domain = get_domain(self.apiclient, self.services)
        self.zone = get_zone(self.apiclient, self.services)
        
        template = get_template(
                            self.apiclient,
                            self.zone.id,
                            self.services["ostypeid"]
                            )
        
        self.services["domainid"] = self.domain.id
        self.services["virtual_machine"]["zoneid"] = self.zone.id
        self.services["virtual_machine"]["template"] = template.id
        
        self.service_offering = ServiceOffering.create(
                                            self.apiclient,
                                            self.services["service_offering"]
                                            )
        self.account = Account.create(
                            self.apiclient,
                            self.services["account"],
                            domainid=self.domain.id
                            )
        self.services["account"] = self.account.account.name
        self.cleanup = [
                        self.account,
                        self.service_offering
                        ]
        return

    def tearDown(self):
        try:
            self.dbclient.close()
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        cls.services = Services().services
        cls.api_client = super(TestDeleteSecurityGroup, cls).getClsTestClient().getApiClient()
        cls._cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = super(TestDeleteSecurityGroup, cls).getClsTestClient().getApiClient()
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return
    
    def test_01_delete_security_grp_running_vm(self):
        """Test delete security group with running VM"""
        
        # Validate the following
        # 1. createsecuritygroup (ssh-incoming) for this account
        # 2. authorizeSecurityGroupIngress to allow ssh access to the VM
        # 3. deployVirtualMachine into this security group (ssh-incoming)
        # 4. deleteSecurityGroup created in step 1. Deletion should fail
        #    complaining there are running VMs in this group
        
        security_group = SecurityGroup.create(
                                              self.apiclient, 
                                              self.services["security_group"], 
                                              account=self.account.account.name, 
                                              domainid=self.account.account.domainid
                                              )
        self.debug("Created security group with ID: %s" % security_group.id)
        # Default Security group should not have any ingress rule
        sercurity_groups = SecurityGroup.list(
                                              self.apiclient,
                                              account=self.account.account.name,
                                              domainid=self.account.account.domainid
                                              )
        self.assertEqual(
                         isinstance(sercurity_groups, list),
                         True,
                         "Check for list security groups response"
                         )
        
        self.assertEqual(
                            len(sercurity_groups), 
                            2, 
                            "Check List Security groups response"
                            )
        self.debug(
            "Authorize Ingress Rule for Security Group %s for account: %s" \
                % (
                    security_group.id,
                    self.account.account.name
                ))
        
        # Authorize Security group to SSH to VM
        ingress_rule = security_group.authorize(
                                        self.apiclient,
                                        self.services["security_group"], 
                                        account=self.account.account.name, 
                                        domainid=self.account.account.domainid
                                        )
        self.assertEqual(
                          isinstance(ingress_rule, dict),
                          True,
                          "Check ingress rule created properly"
                    )
        
        self.virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    accountid=self.account.account.name,
                                    domainid=self.account.account.domainid,
                                    serviceofferingid=self.service_offering.id,
                                    securitygroupids=[security_group.id]
                                )
        self.debug("Deploying VM in account: %s" % self.account.account.name)
        
        # Deleting Security group should raise exception
        security_group.delete(self.apiclient)
        
        #sleep to ensure that Security group is deleted properly
        time.sleep(self.services["sleep"])
        
        # Default Security group should not have any ingress rule
        sercurity_groups = SecurityGroup.list(
                                              self.apiclient,
                                              id=security_group.id
                                              )
        self.assertNotEqual(
                            sercurity_groups, 
                            None, 
                            "Check List Security groups response"
                            )
        return

    def test_02_delete_security_grp_withoout_running_vm(self):
        """Test delete security group without running VM"""
        
        # Validate the following
        # 1. createsecuritygroup (ssh-incoming) for this account
        # 2. authorizeSecurityGroupIngress to allow ssh access to the VM
        # 3. deployVirtualMachine into this security group (ssh-incoming)
        # 4. deleteSecurityGroup created in step 1. Deletion should fail
        #    complaining there are running VMs in this group
        
        security_group = SecurityGroup.create(
                                              self.apiclient, 
                                              self.services["security_group"], 
                                              account=self.account.account.name, 
                                              domainid=self.account.account.domainid
                                              )
        self.debug("Created security group with ID: %s" % security_group.id)
        # Default Security group should not have any ingress rule
        sercurity_groups = SecurityGroup.list(
                                              self.apiclient,
                                              account=self.account.account.name,
                                              domainid=self.account.account.domainid
                                              )
        self.assertEqual(
                         isinstance(sercurity_groups, list),
                         True,
                         "Check for list security groups response"
                         )
        self.assertEqual(
                            len(sercurity_groups), 
                            2, 
                            "Check List Security groups response"
                            )
        
        self.debug(
            "Authorize Ingress Rule for Security Group %s for account: %s" \
                % (
                    security_group.id,
                    self.account.account.name
                ))
        # Authorize Security group to SSH to VM
        ingress_rule = security_group.authorize(
                                        self.apiclient,
                                        self.services["security_group"], 
                                        account=self.account.account.name, 
                                        domainid=self.account.account.domainid
                                        )
        self.assertEqual(
                          isinstance(ingress_rule, dict),
                          True,
                          "Check ingress rule created properly"
                    )
        
        self.virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    accountid=self.account.account.name,
                                    domainid=self.account.account.domainid,
                                    serviceofferingid=self.service_offering.id,
                                    securitygroupids=[security_group.id]
                                )
        self.debug("Deploying VM in account: %s" % self.account.account.name)
        
        # Destroy the VM
        self.virtual_machine.delete(self.apiclient)
        
        config = list_configurations(
                                     self.apiclient,
                                     name='expunge.delay'
                                     )
        self.assertEqual(
                          isinstance(config, list),
                          True,
                          "Check list configurations response"
                    )
        response = config[0]
        self.debug("expunge.delay: %s" % response.value)
        # Wait for some time more than expunge.delay
        time.sleep(int(response.value) * 2)
        
        # Deleting Security group should raise exception
        try:
            self.debug("Deleting Security Group: %s" % security_group.id)
            security_group.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to delete security group - ID: %s" \
                      % security_group.id
                      )
        return
    

class TestIngressRule(cloudstackTestCase):
    
    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        
        self.services = Services().services

        # Get Zone, Domain and templates
        self.domain = get_domain(self.apiclient, self.services)
        self.zone = get_zone(self.apiclient, self.services)
        
        template = get_template(
                            self.apiclient,
                            self.zone.id,
                            self.services["ostypeid"]
                            )
        
        self.services["domainid"] = self.domain.id
        self.services["virtual_machine"]["zoneid"] = self.zone.id
        self.services["virtual_machine"]["template"] = template.id
        
        self.service_offering = ServiceOffering.create(
                                            self.apiclient,
                                            self.services["service_offering"]
                                            )
        self.account = Account.create(
                            self.apiclient,
                            self.services["account"],
                            domainid=self.domain.id
                            )
        self.services["account"] = self.account.account.name
        self.cleanup = [
                        self.account,
                        self.service_offering
                        ]
        return

    def tearDown(self):
        try:
            self.dbclient.close()
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        cls.services = Services().services
        cls.api_client = super(TestIngressRule, cls).getClsTestClient().getApiClient()
        cls._cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = super(TestIngressRule, cls).getClsTestClient().getApiClient()
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    def test_01_authorizeIngressRule_AfterDeployVM(self):
        """Test delete security group with running VM"""
        
        # Validate the following
        # 1. createsecuritygroup (ssh-incoming, 22via22) for this account
        # 2. authorizeSecurityGroupIngress to allow ssh access to the VM
        # 3. deployVirtualMachine into this security group (ssh-incoming)
        # 4. authorizeSecurityGroupIngress to allow ssh access (startport:222 to
        #    endport:22) to the VM
        
        security_group = SecurityGroup.create(
                                              self.apiclient, 
                                              self.services["security_group"], 
                                              account=self.account.account.name, 
                                              domainid=self.account.account.domainid
                                              )
        self.debug("Created security group with ID: %s" % security_group.id)
        # Default Security group should not have any ingress rule
        sercurity_groups = SecurityGroup.list(
                                              self.apiclient,
                                              account=self.account.account.name,
                                              domainid=self.account.account.domainid
                                              )
        self.assertEqual(
                         isinstance(sercurity_groups, list),
                         True,
                         "Check for list security groups response"
                         )
        self.assertEqual(
                            len(sercurity_groups), 
                            2, 
                            "Check List Security groups response"
                            )
        self.debug(
            "Authorize Ingress Rule for Security Group %s for account: %s" \
                % (
                    security_group.id,
                    self.account.account.name
                ))
        
        # Authorize Security group to SSH to VM
        ingress_rule_1 = security_group.authorize(
                                        self.apiclient,
                                        self.services["security_group"], 
                                        account=self.account.account.name, 
                                        domainid=self.account.account.domainid
                                        )
        self.assertEqual(
                          isinstance(ingress_rule_1, dict),
                          True,
                          "Check ingress rule created properly"
                    )
        self.virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    accountid=self.account.account.name,
                                    domainid=self.account.account.domainid,
                                    serviceofferingid=self.service_offering.id,
                                    securitygroupids=[security_group.id]
                                )
        self.debug("Deploying VM in account: %s" % self.account.account.name)
        
        self.debug(
            "Authorize Ingress Rule for Security Group %s for account: %s" \
                % (
                    security_group.id,
                    self.account.account.name
                ))
        # Authorize Security group to SSH to VM
        ingress_rule_2 = security_group.authorize(
                                        self.apiclient,
                                        self.services["security_group_2"], 
                                        account=self.account.account.name, 
                                        domainid=self.account.account.domainid
                                        )
        self.assertEqual(
                          isinstance(ingress_rule_2, dict),
                          True,
                          "Check ingress rule created properly"
                    )
        # SSH should be allowed on 22 & 2222 ports
        try:
            self.debug("Trying to SSH into VM %s on port %s" % (
                                        self.virtual_machine.ssh_ip,
                                        self.services["security_group"]["endport"]
                                        ))
            self.virtual_machine.get_ssh_client()
            
        except Exception as e:
            self.fail("SSH access failed for ingress rule ID: %s, %s" \
                      % (ingress_rule_1["id"], e))
            
        # User should be able to ping VM
        try:
            self.debug("Trying to ping VM %s" % self.virtual_machine.ssh_ip)
            result = subprocess.call(['ping', '-c 1', self.virtual_machine.ssh_ip])
            
            self.debug("Ping result: %s" % result)
            # if ping successful, then result should be 0
            self.assertEqual(
                          result,
                          0,
                          "Check if ping is successful or not"
                    )
            
        except Exception as e:
            self.fail("Ping failed for ingress rule ID: %s, %s" \
                      % (ingress_rule_2["id"], e))
        return

    def test_02_revokeIngressRule_AfterDeployVM(self):
        """Test Revoke ingress rule after deploy VM"""
        
        # Validate the following
        # 1. createsecuritygroup (ssh-incoming, 22via22) for this account
        # 2. authorizeSecurityGroupIngress to allow ssh access to the VM
        # 3. deployVirtualMachine into this security group (ssh-incoming)
        # 4. authorizeSecurityGroupIngress to allow ssh access (startport:222
        #    to endport:22) to the VM
        # 5. check ssh access via port 222
        # 6. revokeSecurityGroupIngress to revoke rule added in step 5. verify
        #    that ssh-access into the VM is now NOT allowed through ports 222
        #    but allowed through port 22

        security_group = SecurityGroup.create(
                                              self.apiclient, 
                                              self.services["security_group"], 
                                              account=self.account.account.name, 
                                              domainid=self.account.account.domainid
                                              )
        self.debug("Created security group with ID: %s" % security_group.id)
        
        # Default Security group should not have any ingress rule
        sercurity_groups = SecurityGroup.list(
                                              self.apiclient,
                                              account=self.account.account.name,
                                              domainid=self.account.account.domainid
                                              )
        self.assertEqual(
                         isinstance(sercurity_groups, list),
                         True,
                         "Check for list security groups response"
                         )
        self.assertEqual(
                            len(sercurity_groups), 
                            2, 
                            "Check List Security groups response"
                            )
        
        self.debug(
            "Authorize Ingress Rule for Security Group %s for account: %s" \
                % (
                    security_group.id,
                    self.account.account.name
                ))
        
        # Authorize Security group to SSH to VM
        ingress_rule = security_group.authorize(
                                        self.apiclient,
                                        self.services["security_group"], 
                                        account=self.account.account.name, 
                                        domainid=self.account.account.domainid
                                        )
        self.assertEqual(
                          isinstance(ingress_rule, dict),
                          True,
                          "Check ingress rule created properly"
                    )
        self.virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    accountid=self.account.account.name,
                                    domainid=self.account.account.domainid,
                                    serviceofferingid=self.service_offering.id,
                                    securitygroupids=[security_group.id]
                                )
        self.debug("Deploying VM in account: %s" % self.account.account.name)
        
        self.debug(
            "Authorize Ingress Rule for Security Group %s for account: %s" \
                % (
                    security_group.id,
                    self.account.account.name
                ))
        
        # Authorize Security group to SSH to VM
        ingress_rule_2 = security_group.authorize(
                                        self.apiclient,
                                        self.services["security_group_2"], 
                                        account=self.account.account.name, 
                                        domainid=self.account.account.domainid
                                        )
        self.assertEqual(
                          isinstance(ingress_rule_2, dict),
                          True,
                          "Check ingress rule created properly"
                    )
        
        ssh_rule = (ingress_rule["ingressrule"][0]).__dict__
        icmp_rule = (ingress_rule_2["ingressrule"][0]).__dict__
        
        # SSH should be allowed on 22
        try:
            self.debug("Trying to SSH into VM %s on port %s" % (
                                        self.virtual_machine.ssh_ip,
                                        self.services["security_group"]["endport"]
                                        ))
            self.virtual_machine.get_ssh_client()
            
        except Exception as e:
            self.fail("SSH access failed for ingress rule ID: %s, %s" \
                      % (ssh_rule["ruleid"], e))
        
         # User should be able to ping VM
        try:
            self.debug("Trying to ping VM %s" % self.virtual_machine.ssh_ip)
            result = subprocess.call(['ping', '-c 1', self.virtual_machine.ssh_ip])
            
            self.debug("Ping result: %s" % result)
            # if ping successful, then result should be 0
            self.assertEqual(
                          result,
                          0,
                          "Check if ping is successful or not"
                    )
            
        except Exception as e:
            self.fail("Ping failed for ingress rule ID: %s, %s" \
                      % (icmp_rule["ruleid"], e))
        
        self.debug(
            "Revoke Ingress Rule for Security Group %s for account: %s" \
                % (
                    security_group.id,
                    self.account.account.name
                ))
        
        result = security_group.revoke(
                                self.apiclient, 
                                id = icmp_rule["ruleid"]
                                )
        self.debug("Revoke ingress rule result: %s" % result)

        time.sleep(self.services["sleep"])
        # User should not be able to ping VM
        try:
            self.debug("Trying to ping VM %s" % self.virtual_machine.ssh_ip)
            result = subprocess.call(['ping', '-c 1', self.virtual_machine.ssh_ip])
            
            self.debug("Ping result: %s" % result)
            # if ping successful, then result should be 0
            self.assertNotEqual(
                          result,
                          0,
                          "Check if ping is successful or not"
                    )
            
        except Exception as e:
            self.fail("Ping failed for ingress rule ID: %s, %s" \
                      % (icmp_rule["ruleid"], e))
        return

    def test_03_stopStartVM_verifyIngressAccess(self):
        """Test Start/Stop VM and Verify ingress rule"""
        
        # Validate the following
        # 1. createsecuritygroup (ssh-incoming, 22via22) for this account
        # 2. authorizeSecurityGroupIngress to allow ssh access to the VM
        # 3. deployVirtualMachine into this security group (ssh-incoming)
        # 4. once the VM is running and ssh-access is available,
        #    stopVirtualMachine
        # 5. startVirtualMachine. After stop start of the VM is complete
        #    verify that ssh-access to the VM is allowed

        security_group = SecurityGroup.create(
                                              self.apiclient, 
                                              self.services["security_group"], 
                                              account=self.account.account.name, 
                                              domainid=self.account.account.domainid
                                              )
        self.debug("Created security group with ID: %s" % security_group.id)
        # Default Security group should not have any ingress rule
        sercurity_groups = SecurityGroup.list(
                                              self.apiclient,
                                              account=self.account.account.name,
                                              domainid=self.account.account.domainid
                                              )
        self.assertEqual(
                         isinstance(sercurity_groups, list),
                         True,
                         "Check for list security groups response"
                         )
        
        self.assertEqual(
                            len(sercurity_groups), 
                            2, 
                            "Check List Security groups response"
                            )
        
        self.debug(
            "Authorize Ingress Rule for Security Group %s for account: %s" \
                % (
                    security_group.id,
                    self.account.account.name
                ))
        
        # Authorize Security group to SSH to VM
        ingress_rule = security_group.authorize(
                                        self.apiclient,
                                        self.services["security_group"], 
                                        account=self.account.account.name, 
                                        domainid=self.account.account.domainid
                                        )
        self.assertEqual(
                          isinstance(ingress_rule, dict),
                          True,
                          "Check ingress rule created properly"
                    )
        
        self.virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    accountid=self.account.account.name,
                                    domainid=self.account.account.domainid,
                                    serviceofferingid=self.service_offering.id,
                                    securitygroupids=[security_group.id]
                                )
        self.debug("Deploying VM in account: %s" % self.account.account.name)
        
        # SSH should be allowed on 22 port
        try:
            self.debug("Trying to SSH into VM %s" % self.virtual_machine.ssh_ip)
            self.virtual_machine.get_ssh_client()
        except Exception as e:
            self.fail("SSH access failed for ingress rule ID: %s" \
                      % ingress_rule["id"]
                      )
        
        self.virtual_machine.stop(self.apiclient)
        
        # Sleep to ensure that VM is in stopped state
        time.sleep(self.services["sleep"])
        
        self.virtual_machine.start(self.apiclient)
        
        # Sleep to ensure that VM is in running state
        time.sleep(self.services["sleep"])
        
        # SSH should be allowed on 22 port after restart
        try:
            self.debug("Trying to SSH into VM %s" % self.virtual_machine.ssh_ip)
            self.virtual_machine.get_ssh_client()
        except Exception as e:
            self.fail("SSH access failed for ingress rule ID: %s" \
                      % ingress_rule["id"]
                      )
        return
