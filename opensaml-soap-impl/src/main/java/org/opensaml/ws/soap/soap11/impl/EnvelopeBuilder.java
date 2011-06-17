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

package org.opensaml.ws.soap.soap11.impl;

import org.opensaml.ws.soap.common.SOAPObjectBuilder;
import org.opensaml.ws.soap.soap11.Envelope;
import org.opensaml.ws.soap.util.SOAPConstants;
import org.opensaml.xml.AbstractXMLObjectBuilder;

/**
 * Builder of {@link org.opensaml.ws.soap.soap11.impl.EnvelopeImpl} objects.
 */
public class EnvelopeBuilder extends AbstractXMLObjectBuilder<Envelope> implements SOAPObjectBuilder<Envelope> {

    /**
     * Creates an envelope object with the default SOAP 1.1 namespace, prefix and "Envelope" as the element local name.
     * 
     * @return the build Envelope object
     */
    public Envelope buildObject(){
        return buildObject(SOAPConstants.SOAP11_NS, Envelope.DEFAULT_ELEMENT_LOCAL_NAME, SOAPConstants.SOAP11_PREFIX);
    }
    
    /** {@inheritDoc} */
    public Envelope buildObject(String namespaceURI, String localName, String namespacePrefix) {
        return new EnvelopeImpl(namespaceURI, localName, namespacePrefix);
    }
}