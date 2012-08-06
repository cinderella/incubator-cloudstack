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

from cloudstackAPI import *
import unittest
import cloudstackTestClient

#class UserName(object):
#    def __init__(self, account, domain, type=0):
#        self.account = account
#        self.domain = domain
#        self.accounttype = type
#    def __call__(self, cls):
#        class Wrapped(cls):
#            cls.UserName = self.account
#            cls.DomainName = self.domain
#            cls.AcctType = self.accounttype
#        return Wrapped

def UserName(Name, DomainName, AcctType):
    def wrapper(cls):
        orig_init = cls.__init__
        def __init__(self, *args, **kws):
            cls.UserName = Name
            cls.DomainName = DomainName
            cls.AcctType = AcctType
            orig_init(self, *args, **kws)
        cls.__init__ = __init__
        return cls
    return wrapper

class cloudstackTestCase(unittest.case.TestCase):
    clstestclient = None
    
    def __init__(self, args):
        unittest.case.TestCase.__init__(self, args)
#        self.testClient = cloudstackTestClient.cloudstackTestClient()
        
    @classmethod
    def getClsTestClient(cls):
        return cls.clstestclient
