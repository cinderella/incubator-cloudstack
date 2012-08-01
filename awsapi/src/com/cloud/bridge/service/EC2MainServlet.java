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
package com.cloud.bridge.service;

import com.cloud.bridge.util.ConfigurationHelper;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.UUID;

public class EC2MainServlet extends HttpServlet{

   private static final long serialVersionUID = 2201599478145974479L;

   public static final String EC2_REST_SERVLET_PATH="/rest/AmazonEC2/";
   public static final String EC2_SOAP_SERVLET_PATH="/services/AmazonEC2/";

   /**
    * We build the path to where the keystore holding the WS-Security X509 certificates
    * are stored.
    */
   public void init( ServletConfig config ) throws ServletException {
      ConfigurationHelper.preConfigureConfigPathFromServletContext(config.getServletContext());
   }

   protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
      doGetOrPost(req, resp);
   }

   protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
      doGetOrPost(req, resp);
   }

   protected void doGetOrPost(HttpServletRequest request, HttpServletResponse response) {
      String action = request.getParameter( "Action" );

      if(action != null){
         //We presume it's a Query/Rest call
         try {
            RequestDispatcher dispatcher = request.getRequestDispatcher(EC2_REST_SERVLET_PATH);
            dispatcher.forward(request, response);
         } catch (ServletException e) {
            throw new RuntimeException(e);
         } catch (IOException e) {
            throw new RuntimeException(e);
         }
      }
      else {
         try {
            request.getRequestDispatcher(EC2_SOAP_SERVLET_PATH).forward(request, response);
         } catch (ServletException e) {
            throw new RuntimeException(e);
         } catch (IOException e) {
            throw new RuntimeException(e);
         }
      }

   }

   private void faultResponse(HttpServletResponse response, String errorCode, String errorMessage) {
      try {
         OutputStreamWriter out = new OutputStreamWriter(response.getOutputStream());
         response.setContentType("text/xml; charset=UTF-8");
         out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
         out.write("<Response><Errors><Error><Code>");
         out.write(errorCode);
         out.write("</Code><Message>");
         out.write(errorMessage);
         out.write("</Message></Error></Errors><RequestID>");
         out.write(UUID.randomUUID().toString());
         out.write("</RequestID></Response>");
         out.flush();
         out.close();
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }
}