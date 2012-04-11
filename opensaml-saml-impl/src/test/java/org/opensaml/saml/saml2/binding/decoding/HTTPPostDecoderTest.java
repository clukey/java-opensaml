/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensaml.saml.saml2.binding.decoding;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.Assert;
import org.testng.Assert;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyPair;

import net.shibboleth.utilities.java.support.codec.Base64Support;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;

import org.opensaml.core.xml.XMLObjectBaseTestCase;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.binding.BasicSAMLMessageContext;
import org.opensaml.saml.common.binding.decoding.SAMLMessageDecoder;
import org.opensaml.saml.saml2.binding.decoding.HTTPPostDecoder;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.RequestAbstractType;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.security.SecurityException;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.CredentialSupport;
import org.opensaml.security.crypto.KeySupport;
import org.opensaml.ws.message.decoder.MessageDecodingException;
import org.opensaml.ws.transport.http.HttpServletRequestAdapter;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureSupport;
import org.opensaml.xmlsec.signature.support.Signer;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Test case for HTTP POST decoders.
 */
public class HTTPPostDecoderTest extends XMLObjectBaseTestCase {
    
    private String authnRequestDestination = "https://idp.example.com/idp/sso";
    
    private String expectedRelayValue = "relay";
    
    private SAMLMessageDecoder decoder;
    
    private BasicSAMLMessageContext messageContext;
    
    private MockHttpServletRequest httpRequest;

    /** {@inheritDoc} */
    @BeforeMethod
    protected void setUp() throws Exception {
        httpRequest = new MockHttpServletRequest();
        httpRequest.setMethod("POST");
        httpRequest.setParameter("RelayState", expectedRelayValue);
        
        messageContext = new BasicSAMLMessageContext();
        messageContext.setInboundMessageTransport(new HttpServletRequestAdapter(httpRequest));
        
        decoder = new HTTPPostDecoder();
    }

    /**
     * Test decoding a SAML httpRequest.
     */
    @Test
    public void testRequestDecoding() throws Exception {
        httpRequest.setParameter("SAMLRequest", "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPHNhbWxwOkF1dGhuUm"
                + "VxdWVzdCBJRD0iZm9vIiBJc3N1ZUluc3RhbnQ9IjE5NzAtMDEtMDFUMDA6MDA6MDAuMDAwWiIgVmVyc2lvbj0iMi4wIiB4bW"
                + "xuczpzYW1scD0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6Mi4wOnByb3RvY29sIi8+");
        
        decoder.decode(messageContext);

        Assert.assertTrue(messageContext.getInboundMessage() instanceof RequestAbstractType);
        Assert.assertTrue(messageContext.getInboundSAMLMessage() instanceof RequestAbstractType);
        Assert.assertEquals(messageContext.getRelayState(), expectedRelayValue);
    }

    /**
     * Test decoding a SAML response.
     */
    @Test
    public void testResponseDecoding() throws Exception {
        httpRequest.setParameter("SAMLResponse", "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPHNhbWxwOlJlc3Bvbn"
                + "NlIElEPSJmb28iIElzc3VlSW5zdGFudD0iMTk3MC0wMS0wMVQwMDowMDowMC4wMDBaIiBWZXJzaW9uPSIyLjAiIHhtbG5zOnN"
                + "hbWxwPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6cHJvdG9jb2wiPjxzYW1scDpTdGF0dXM+PHNhbWxwOlN0YXR1c0Nv"
                + "ZGUgVmFsdWU9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDpzdGF0dXM6U3VjY2VzcyIvPjwvc2FtbHA6U3RhdHVzPjwvc"
                + "2FtbHA6UmVzcG9uc2U+");

        decoder.decode(messageContext);

        Assert.assertTrue(messageContext.getInboundMessage() instanceof Response);
        Assert.assertTrue(messageContext.getInboundSAMLMessage() instanceof Response);
        Assert.assertEquals(messageContext.getRelayState(), expectedRelayValue);
    }
    
    @Test
    public void testMessageEndpointGood() throws Exception {
        AuthnRequest samlRequest = (AuthnRequest) unmarshallElement("/data/org/opensaml/saml/saml2/binding/AuthnRequest.xml");
        
        String deliveredEndpointURL = samlRequest.getDestination();
        
        httpRequest.setParameter("SAMLRequest", encodeMessage(samlRequest));
        
        populateRequestURL(httpRequest, deliveredEndpointURL);
        
        try {
            decoder.decode(messageContext);
        } catch (SecurityException e) {
            Assert.fail("Caught SecurityException: " + e.getMessage());
        } catch (MessageDecodingException e) {
            Assert.fail("Caught MessageDecodingException: " + e.getMessage());
        }
    }
    
    @Test
    public void testMessageEndpointGoodWithQueryParams() throws Exception {
        AuthnRequest samlRequest = (AuthnRequest) unmarshallElement("/data/org/opensaml/saml/saml2/binding/AuthnRequest.xml");
        
        String deliveredEndpointURL = samlRequest.getDestination() + "?paramFoo=bar&paramBar=baz";
        
        httpRequest.setParameter("SAMLRequest", encodeMessage(samlRequest));
        
        populateRequestURL(httpRequest, deliveredEndpointURL);

        try {
            decoder.decode(messageContext);
        } catch (SecurityException e) {
            Assert.fail("Caught SecurityException: " + e.getMessage());
        } catch (MessageDecodingException e) {
            Assert.fail("Caught MessageDecodingException: " + e.getMessage());
        }
    }
    
    @Test
    public void testMessageEndpointInvalidURI() throws Exception {
        AuthnRequest samlRequest = (AuthnRequest) unmarshallElement("/data/org/opensaml/saml/saml2/binding/AuthnRequest.xml");
        
        String deliveredEndpointURL = samlRequest.getDestination() + "/some/other/endpointURI";
        
        httpRequest.setParameter("SAMLRequest", encodeMessage(samlRequest));
        
        populateRequestURL(httpRequest, deliveredEndpointURL);

        try {
            decoder.decode(messageContext);
            Assert.fail("Passed delivered endpoint check, should have failed");
        } catch (SecurityException e) {
            // do nothing, failure expected
        } catch (MessageDecodingException e) {
            Assert.fail("Caught MessageDecodingException: " + e.getMessage());
        }
    }
    
    @Test
    public void testMessageEndpointInvalidHost() throws Exception {
        AuthnRequest samlRequest = (AuthnRequest) unmarshallElement("/data/org/opensaml/saml/saml2/binding/AuthnRequest.xml");
        
        String deliveredEndpointURL = "https://bogusidp.example.com/idp/sso";
        
        httpRequest.setParameter("SAMLRequest", encodeMessage(samlRequest));
        
        populateRequestURL(httpRequest, deliveredEndpointURL);

        try {
            decoder.decode(messageContext);
            Assert.fail("Passed delivered endpoint check, should have failed");
        } catch (SecurityException e) {
            // do nothing, failure expected
        } catch (MessageDecodingException e) {
            Assert.fail("Caught MessageDecodingException: " + e.getMessage());
        }
    }
    
    @Test
    public void testMessageEndpointMissingDestinationNotSigned() throws Exception {
        AuthnRequest samlRequest = (AuthnRequest) unmarshallElement("/data/org/opensaml/saml/saml2/binding/AuthnRequest.xml");
        samlRequest.setDestination(null);
        
        String deliveredEndpointURL = authnRequestDestination;
        
        httpRequest.setParameter("SAMLRequest", encodeMessage(samlRequest));
        
        populateRequestURL(httpRequest, deliveredEndpointURL);

        try {
            decoder.decode(messageContext);
        } catch (SecurityException e) {
            Assert.fail("Caught SecurityException: " + e.getMessage());
        } catch (MessageDecodingException e) {
            Assert.fail("Caught MessageDecodingException: " + e.getMessage());
        }
    }
    
    @Test
    public void testMessageEndpointMissingDestinationSigned() throws Exception {
        AuthnRequest samlRequest = (AuthnRequest) unmarshallElement("/data/org/opensaml/saml/saml2/binding/AuthnRequest.xml");
        samlRequest.setDestination(null);
        
        Signature signature = (Signature) buildXMLObject(Signature.DEFAULT_ELEMENT_NAME);
        KeyPair kp = KeySupport.generateKeyPair("RSA", 1024, null);
        Credential signingCred = CredentialSupport.getSimpleCredential(kp.getPublic(), kp.getPrivate());
        signature.setSigningCredential(signingCred);
        samlRequest.setSignature(signature);
        SignatureSupport.prepareSignatureParams(signature, signingCred, null, null);
        marshallerFactory.getMarshaller(samlRequest).marshall(samlRequest);
        Signer.signObject(signature);
        
        String deliveredEndpointURL = authnRequestDestination;
        
        httpRequest.setParameter("SAMLRequest", encodeMessage(samlRequest));
        
        populateRequestURL(httpRequest, deliveredEndpointURL);

        try {
            decoder.decode(messageContext);
            Assert.fail("Passed delivered endpoint check, should have failed, binding requires endpoint on signed message");
        } catch (SecurityException e) {
            // do nothing, failure expected
        } catch (MessageDecodingException e) {
            Assert.fail("Caught MessageDecodingException: " + e.getMessage());
        }
    }
    
    private void populateRequestURL(MockHttpServletRequest request, String requestURL) {
        URL url = null;
        try {
            url = new URL(requestURL);
        } catch (MalformedURLException e) {
            Assert.fail("Malformed URL: " + e.getMessage());
        }
        request.setScheme(url.getProtocol());
        request.setServerName(url.getHost());
        if (url.getPort() != -1) {
            request.setServerPort(url.getPort());
        } else {
            if ("https".equalsIgnoreCase(url.getProtocol())) {
                request.setServerPort(443);
            } else if ("http".equalsIgnoreCase(url.getProtocol())) {
                request.setServerPort(80);
            }
        }
        request.setRequestURI(url.getPath());
        request.setQueryString(url.getQuery());
    }
    
    protected String encodeMessage(SAMLObject message) throws Exception {
        marshallerFactory.getMarshaller(message).marshall(message);
        String messageStr = SerializeSupport.nodeToString(message.getDOM());
        
        return Base64Support.encode(messageStr.getBytes("UTF-8"), Base64Support.UNCHUNKED);
    }
    
}