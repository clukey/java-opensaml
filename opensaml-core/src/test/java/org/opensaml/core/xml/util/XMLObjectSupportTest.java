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

package org.opensaml.core.xml.util;

import javax.xml.namespace.QName;

import org.testng.annotations.Test;
import org.testng.Assert;
import org.opensaml.core.xml.XMLObjectBaseTestCase;
import org.opensaml.core.xml.XMLRuntimeException;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.core.xml.mock.SimpleXMLObject;
import org.opensaml.core.xml.mock.SimpleXMLObjectBuilder;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.core.xml.util.XMLObjectSupport;

/**
 * Tests of XMLObjectHelper utility methods.
 */
public class XMLObjectSupportTest extends XMLObjectBaseTestCase {

    /** Tests cloning an XMLObject. */
    @Test
    public void testXMLObjectClone() {
        SimpleXMLObjectBuilder sxoBuilder = (SimpleXMLObjectBuilder) XMLObjectProviderRegistrySupport.getBuilderFactory()
            .getBuilder(SimpleXMLObject.ELEMENT_NAME);
        
        SimpleXMLObject origChildObj = sxoBuilder.buildObject();
        origChildObj.setValue("FooBarBaz");
        
        SimpleXMLObject origParentObj = sxoBuilder.buildObject();
        origParentObj.getSimpleXMLObjects().add(origChildObj);
        
        SimpleXMLObject clonedParentObj = null;
        try {
            clonedParentObj = XMLObjectSupport.cloneXMLObject(origParentObj);
        } catch (MarshallingException e) {
            Assert.fail("Object cloning failed on marshalling: " + e.getMessage());
        } catch (UnmarshallingException e) {
            Assert.fail("Object cloning failed on unmarshalling: " + e.getMessage());
        }
        
        Assert.assertFalse(origParentObj == clonedParentObj, "Parent XMLObjects were the same reference");
        Assert.assertFalse(origParentObj.getDOM().isSameNode(clonedParentObj.getDOM()),
                "Parent DOM node was not cloned properly");
        
        Assert.assertFalse(clonedParentObj.getSimpleXMLObjects().isEmpty(), "Cloned parent had no children");
        SimpleXMLObject clonedChildObj = (SimpleXMLObject) clonedParentObj.getSimpleXMLObjects().get(0);
        
        Assert.assertFalse(origChildObj == clonedChildObj, "Child XMLObjects were the same reference");
        Assert.assertFalse(origChildObj.getDOM().isSameNode(clonedChildObj.getDOM()),
                "Child DOM node was not cloned properly");
        
        Assert.assertEquals(clonedChildObj.getValue(), "FooBarBaz", "Text content of child was not the expected value");
    }
    
    /** Tests cloning an XMLObject. */
    @Test
    public void testXMLObjectCloneWithRootInNewDocument() {
        SimpleXMLObjectBuilder sxoBuilder = (SimpleXMLObjectBuilder) XMLObjectProviderRegistrySupport.getBuilderFactory()
            .getBuilder(SimpleXMLObject.ELEMENT_NAME);
        
        SimpleXMLObject origChildObj = sxoBuilder.buildObject();
        origChildObj.setValue("FooBarBaz");
        
        SimpleXMLObject origParentObj = sxoBuilder.buildObject();
        origParentObj.getSimpleXMLObjects().add(origChildObj);
        
        SimpleXMLObject clonedParentObj = null;
        try {
            clonedParentObj = XMLObjectSupport.cloneXMLObject(origParentObj, true);
        } catch (MarshallingException e) {
            Assert.fail("Object cloning failed on marshalling: " + e.getMessage());
        } catch (UnmarshallingException e) {
            Assert.fail("Object cloning failed on unmarshalling: " + e.getMessage());
        }
        
        Assert.assertFalse(origParentObj == clonedParentObj, "Parent XMLObjects were the same reference");
        Assert.assertFalse(origParentObj.getDOM().isSameNode(clonedParentObj.getDOM()),
                "Parent DOM node was not cloned properly");
        
        Assert.assertFalse(clonedParentObj.getSimpleXMLObjects().isEmpty(), "Cloned parent had no children");
        SimpleXMLObject clonedChildObj = (SimpleXMLObject) clonedParentObj.getSimpleXMLObjects().get(0);
        
        Assert.assertFalse(origChildObj == clonedChildObj, "Child XMLObjects were the same reference");
        Assert.assertFalse(origChildObj.getDOM().isSameNode(clonedChildObj.getDOM()),
                "Child DOM node was not cloned properly");
        
        Assert.assertEquals(clonedChildObj.getValue(), "FooBarBaz", "Text content of child was not the expected value");
        
        // Test rootInNewDocument requirements
        Assert.assertFalse(origParentObj.getDOM().getOwnerDocument().isSameNode(clonedParentObj.getDOM().getOwnerDocument()), 
                "Cloned objects DOM's were owned by the same Document");
        Assert.assertTrue(clonedParentObj.getDOM().getOwnerDocument().getDocumentElement().isSameNode(clonedParentObj.getDOM()), 
                "Cloned object was not the new Document root");
    }
    
    @Test
    public void testBuildXMLObject() {
        try {
            XMLObjectSupport.buildXMLObject(simpleXMLObjectQName);
        } catch (Exception e) {
            Assert.fail("Expected XMLObject could not be built");
        }
        
        try {
            XMLObjectSupport.buildXMLObject(simpleXMLObjectQName, XSString.TYPE_NAME);
        } catch (Exception e) {
            Assert.fail("Expected XMLObject could not be built");
        }
        
        try {
            XMLObjectSupport.buildXMLObject(new QName("urn:test:bogus", "foo"));
            Assert.fail("buildXMLObject did not throw as expected");
        } catch (XMLRuntimeException e) {
            // expected
        }
        
        try {
            XMLObjectSupport.buildXMLObject(new QName("urn:test:bogus", "foo"), XSString.TYPE_NAME);
            Assert.fail("buildXMLObject did not throw as expected");
        } catch (XMLRuntimeException e) {
            // expected
        }
    }

}