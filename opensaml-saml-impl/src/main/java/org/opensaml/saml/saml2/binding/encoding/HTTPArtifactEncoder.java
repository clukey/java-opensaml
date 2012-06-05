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

package org.opensaml.saml.saml2.binding.encoding;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.net.UriSupport;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.encoder.MessageEncodingException;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.binding.SAMLBindingSupport;
import org.opensaml.saml.common.binding.artifact.AbstractSAMLArtifact;
import org.opensaml.saml.common.binding.artifact.SAMLArtifactMap;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.config.SAMLConfigurationSupport;
import org.opensaml.saml.saml2.binding.artifact.AbstractSAML2Artifact;
import org.opensaml.saml.saml2.binding.artifact.SAML2ArtifactBuilder;
import org.opensaml.saml.saml2.binding.artifact.SAML2ArtifactType0004;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SAML 2 Artifact Binding encoder, support both HTTP GET and POST.
 */
public class HTTPArtifactEncoder extends BaseSAML2MessageEncoder {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(HTTPArtifactEncoder.class);

    /** Whether the POST encoding should be used, instead of GET. */
    private boolean postEncoding;

    /** Velocity engine used to evaluate the template when performing POST encoding. */
    private VelocityEngine velocityEngine;

    /** ID of the velocity template used when performing POST encoding. */
    private String velocityTemplateId;

    /** SAML artifact map used to store created artifacts for later retrieval. */
    private SAMLArtifactMap artifactMap;

    /** Default artifact type to use when encoding messages. */
    private byte[] defaultArtifactType;

    /**
     * Constructor.
     * 
     * @param engine velocity engine used to construct the POST form
     * @param template ID of velocity template used to construct the POST form
     * @param map artifact map used to store artifact/message bindings
     */
    public HTTPArtifactEncoder(VelocityEngine engine, String template, SAMLArtifactMap map) {
        super();
        postEncoding = false;
        defaultArtifactType = SAML2ArtifactType0004.TYPE_CODE;
    }

    /** {@inheritDoc} */
    public String getBindingURI() {
        return SAMLConstants.SAML2_ARTIFACT_BINDING_URI;
    }
    
    /**
     * Gets whether the encoder will encode the artifact via POST encoding.
     * 
     * @return true if POST encoding will be used, false if GET encoding will be used
     */
    public boolean isPostEncoding() {
        return postEncoding;
    }

    /**
     * Sets whether the encoder will encode the artifact via POST encoding.
     * 
     * @param post true if POST encoding will be used, false if GET encoding will be used
     */
    public void setPostEncoding(boolean post) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        postEncoding = post;
    }
    
    /**
     * Get the VelocityEngine instance.
     * 
     * @return return the VelocityEngine instance
     */
    public VelocityEngine getVelocityEngine() {
        return velocityEngine;
    }

    /**
     * Set the VelocityEngine instance.
     * 
     * @param newVelocityEngine the new VelocityEngine instane
     */
    public void setVelocityEngine(VelocityEngine newVelocityEngine) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        velocityEngine = newVelocityEngine;
    }
    
    /**
     * Get the Velocity template id.
     * 
     * @return return the Velocity template id
     */
    public String getVelocityTemplateId() {
        return velocityTemplateId;
    }

    /**
     * Set the Velocity template id.
     * 
     * @param newVelocityTemplateId the new Velocity template id
     */
    public void setVelocityTemplateId(String newVelocityTemplateId) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        velocityTemplateId = newVelocityTemplateId;
    }

    /**
     * Get the SAML artifact map to use.
     * 
     * @return the artifactMap.
     */
    public SAMLArtifactMap getArtifactMap() {
        return artifactMap;
    }

    /**
     * Set the SAML artifact map to use.
     * 
     * @param newArtifactMap the new artifactMap 
     */
    public void setArtifactMap(SAMLArtifactMap newArtifactMap) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        artifactMap = newArtifactMap;
    }

    /** {@inheritDoc} */
    public boolean providesMessageConfidentiality(MessageContext messageContext) throws MessageEncodingException {
        return false;
    }

    /** {@inheritDoc} */
    public boolean providesMessageIntegrity(MessageContext messageContext) throws MessageEncodingException {
        return false;
    }
    
    /** {@inheritDoc} */
    protected void doDestroy() {
        velocityEngine = null;
        velocityTemplateId = null;
        artifactMap = null;
        super.doDestroy();
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (artifactMap == null) {
            throw new ComponentInitializationException("SAML artifact map must be supplied");
        }
        if (isPostEncoding()) {
            if (velocityEngine == null) {
                throw new ComponentInitializationException("VelocityEngine must be supplied");
            }
            if (velocityTemplateId == null) {
                throw new ComponentInitializationException("Velocity template id must be supplied");
            }
        }
    }

    /** {@inheritDoc} */
    protected void doEncode() throws MessageEncodingException {
        HttpServletResponse response = getHttpServletResponse();
        response.setCharacterEncoding("UTF-8");

        if (postEncoding) {
            postEncode();
        } else {
            getEncode();
        }
    }

    /**
     * Performs HTTP POST based encoding.
     * 
     * 
     * @throws MessageEncodingException thrown if there is a problem POST encoding the artifact
     */
    protected void postEncode() throws MessageEncodingException {
        log.debug("Performing HTTP POST SAML 2 artifact encoding");
        
        MessageContext<SAMLObject> messageContext = getMessageContext();

        log.debug("Creating velocity context");
        VelocityContext context = new VelocityContext();
        Encoder esapiEncoder = ESAPI.encoder();
        String endpointURL = getEndpointURL(messageContext).toString();
        String encodedEndpointURL = esapiEncoder.encodeForHTMLAttribute(endpointURL);
        log.debug("Setting action parameter to: '{}', encoded as '{}'", endpointURL, encodedEndpointURL);
        context.put("action", encodedEndpointURL);
        context.put("SAMLArt", buildArtifact(messageContext).base64Encode());

        String relayState = SAMLBindingSupport.getRelayState(messageContext);
        if (SAMLBindingSupport.checkRelayState(relayState)) {
            String encodedRelayState = esapiEncoder.encodeForHTMLAttribute(relayState);
            log.debug("Setting RelayState parameter to: '{}', encoded as '{}'", relayState, encodedRelayState);
            context.put("RelayState", encodedRelayState);
        }

        try {
            log.debug("Invoking velocity template");
            HttpServletResponse response = getHttpServletResponse();
            OutputStreamWriter outWriter = new OutputStreamWriter(response.getOutputStream());
            velocityEngine.mergeTemplate(velocityTemplateId, "UTF-8", context, outWriter);
        } catch (Exception e) {
            log.error("Error invoking velocity template to create POST form", e);
            throw new MessageEncodingException("Error creating output document", e);
        }
    }

    /**
     * Performs HTTP GET based encoding.
     * 
     * @throws MessageEncodingException thrown if there is a problem GET encoding the artifact
     */
    protected void getEncode() throws MessageEncodingException {
        log.debug("Performing HTTP GET SAML 2 artifact encoding");

        MessageContext<SAMLObject> messageContext = getMessageContext();
        
        URI endpointUri = getEndpointURL(messageContext);

        List<Pair<String, String>> params = UriSupport.parseQueryString(endpointUri.getQuery());

        AbstractSAMLArtifact artifact = buildArtifact(messageContext);
        if (artifact == null) {
            log.error("Unable to build artifact for message to relying party");
            throw new MessageEncodingException("Unable to builder artifact for message to relying party");
        }
        params.add(new Pair<String, String>("SAMLart", artifact.base64Encode()));

        String relayState = SAMLBindingSupport.getRelayState(messageContext);
        if (SAMLBindingSupport.checkRelayState(relayState)) {
            params.add(new Pair<String, String>("RelayState", relayState));
        }

        HttpServletResponse response = getHttpServletResponse();
        try {
            response.sendRedirect(endpointUri.toASCIIString());
        } catch (IOException e) {
            throw new MessageEncodingException("Problem sending HTTP redirect", e);
        }
    }

    /**
     * Builds the SAML 2 artifact for the outgoing message.
     * 
     * @param messageContext current request context
     * 
     * @return SAML 2 artifact for outgoing message
     * 
     * @throws MessageEncodingException thrown if the artifact can not be created
     */
    protected AbstractSAML2Artifact buildArtifact(MessageContext<SAMLObject> messageContext) 
            throws MessageEncodingException {

        SAML2ArtifactBuilder artifactBuilder;
        
        byte[] artifactType = getSamlArtifactType(messageContext);
        if (artifactType != null) {
            artifactBuilder = SAMLConfigurationSupport.getSAML2ArtifactBuilderFactory()
                    .getArtifactBuilder(artifactType);
        } else {
            artifactBuilder = SAMLConfigurationSupport.getSAML2ArtifactBuilderFactory()
                    .getArtifactBuilder(defaultArtifactType);
            storeSamlArtifactType(defaultArtifactType);
        }

        AbstractSAML2Artifact artifact = artifactBuilder.buildArtifact(messageContext);
        if (artifact == null) {
            log.error("Unable to build artifact for message to relying party");
            throw new MessageEncodingException("Unable to builder artifact for message to relying party");
        }
        String encodedArtifact = artifact.base64Encode();
        try {
            artifactMap.put(encodedArtifact, getInboundMessageIssuer(messageContext),
                    getOutboundMessageIssuer(messageContext), messageContext.getMessage());
        } catch (MarshallingException e) {
            log.error("Unable to marshall assertion to be represented as an artifact", e);
            throw new MessageEncodingException("Unable to marshall assertion to be represented as an artifact", e);
        }

        return artifact;
    }
    
    /**
     * @param messageContext
     * @return
     */
    private String getOutboundMessageIssuer(MessageContext<SAMLObject> messageContext) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @param messageContext
     * @return
     */
    private String getInboundMessageIssuer(MessageContext<SAMLObject> messageContext) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @param defaultArtifactType2
     */
    private void storeSamlArtifactType(byte[] artifactType) {
        // TODO Auto-generated method stub
        
    }

    private byte[] getSamlArtifactType(MessageContext<SAMLObject> messageContext) {
        // TODO Auto-generated method stub
        return null;
    }
}