/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.common.mapping;

import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertFalse;

import static com.evolveum.midpoint.prism.util.PrismAsserts.*;
import static com.evolveum.midpoint.common.mapping.MappingTestEvaluator.*;

import com.evolveum.midpoint.common.CommonTestConstants;
import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.common.expression.StringPolicyResolver;
import com.evolveum.midpoint.common.expression.evaluator.GenerateExpressionEvaluator;
import com.evolveum.midpoint.common.mapping.Mapping;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProtectedStringType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.StringPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ValuePolicyType;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.testng.AssertJUnit.assertEquals;

/**
 * @author Radovan Semancik
 */
public class TestMappingDynamicSysVar {
	
	private static final String NS_EXTENSION = "http://midpoint.evolveum.com/xml/ns/test/extension";
	private static final String PATTERN_NUMERIC = "^\\d+$";
	
	private MappingTestEvaluator evaluator;
	    
    @BeforeClass
    public void setupFactory() throws SAXException, IOException, SchemaException {
    	evaluator = new MappingTestEvaluator();
    	evaluator.init();
    }
    
    @Test
    public void testScriptSystemVariablesConditionAddObjectTrueGroovy() throws Exception {
    	testScriptSystemVariablesConditionAddObjectTrue("mapping-script-system-variables-condition-groovy.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionAddObjectTrueSourcecontextGroovy() throws Exception {
    	testScriptSystemVariablesConditionAddObjectTrue("mapping-script-system-variables-condition-sourcecontext-groovy.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionAddObjectTrueXPath() throws Exception {
    	testScriptSystemVariablesConditionAddObjectTrue("mapping-script-system-variables-condition-xpath.xml");
    }
    
    public void testScriptSystemVariablesConditionAddObjectTrue(String filename) throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testScriptSystemVariablesConditionAddObjectTrue";
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	PrismObject<UserType> user = evaluator.getUserOld();
    	user.asObjectable().getEmployeeType().clear();
    	user.asObjectable().getEmployeeType().add("CAPTAIN");
    	ObjectDelta<UserType> delta = ObjectDelta.createAddDelta(user);
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				filename, 
    			TEST_NAME, "title", delta);
		    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("Captain jack"));
	  	PrismAsserts.assertTripleNoMinus(outputTriple);
    }
    
    /**
     * Change property that is not a source in this mapping
     */
    @Test
    public void testScriptSystemVariablesConditionModifyObjectTrueGroovyUnrelated() throws Exception {
    	final String TEST_NAME = "testScriptSystemVariablesConditionAddObjectTrueGroovyUnrelated";
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	// GIVEN
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID, 
    			evaluator.toPath("employeeNumber"), evaluator.getPrismContext(), "666");
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				"mapping-script-system-variables-condition-groovy.xml", 
    			TEST_NAME, "title", delta);
		    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		assertNull("Unexpected value in outputTriple: "+outputTriple, outputTriple);
    }
    
    @Test
    public void testScriptSystemVariablesConditionAddObjectFalseGroovy() throws Exception {
    	testScriptSystemVariablesConditionAddObjectFalse("mapping-script-system-variables-condition-groovy.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionAddObjectFalseSourcecontextGroovy() throws Exception {
    	testScriptSystemVariablesConditionAddObjectFalse("mapping-script-system-variables-condition-sourcecontext-groovy.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionAddObjectFalseXPath() throws Exception {
    	testScriptSystemVariablesConditionAddObjectFalse("mapping-script-system-variables-condition-xpath.xml");
    }
    
    public void testScriptSystemVariablesConditionAddObjectFalse(String filename) throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testScriptSystemVariablesConditionAddObjectFalse";
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	PrismObject<UserType> user = evaluator.getUserOld();
    	user.asObjectable().getEmployeeType().clear();
    	user.asObjectable().getEmployeeType().add("SAILOR");
    	ObjectDelta<UserType> delta = ObjectDelta.createAddDelta(user);
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				filename, 
    			TEST_NAME, "title", delta);
		    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		assertNull("Unexpected output triple: "+outputTriple, outputTriple);
    }

    @Test
    public void testScriptSystemVariablesConditionAddObjectFalseNoValGroovy() throws Exception {
    	testScriptSystemVariablesConditionAddObjectFalseNoVal("mapping-script-system-variables-condition-groovy.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionAddObjectFalseNoValSourcecontextGroovy() throws Exception {
    	testScriptSystemVariablesConditionAddObjectFalseNoVal("mapping-script-system-variables-condition-sourcecontext-groovy.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionAddObjectFalseNoValXPath() throws Exception {
    	testScriptSystemVariablesConditionAddObjectFalseNoVal("mapping-script-system-variables-condition-xpath.xml");
    }
    
    public void testScriptSystemVariablesConditionAddObjectFalseNoVal(String filename) throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testScriptSystemVariablesConditionAddObjectFalseNoVal";
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	PrismObject<UserType> user = evaluator.getUserOld();
    	PrismProperty<String> employeeTypeProperty = user.findProperty(UserType.F_EMPLOYEE_TYPE);
    	employeeTypeProperty.clear();
    	ObjectDelta<UserType> delta = ObjectDelta.createAddDelta(user);
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				filename, 
    			TEST_NAME, "title", delta);
		    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		assertNull("Unexpected output triple: "+outputTriple, outputTriple);
    }
    
    @Test
    public void testScriptSystemVariablesConditionAddObjectFalseNoPropertyGroovy() throws Exception {
    	testScriptSystemVariablesConditionAddObjectFalseNoProperty("mapping-script-system-variables-condition-groovy.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionAddObjectFalseNoPropertySourcecontextGroovy() throws Exception {
    	testScriptSystemVariablesConditionAddObjectFalseNoProperty("mapping-script-system-variables-condition-sourcecontext-groovy.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionAddObjectFalseNoPropertyXPath() throws Exception {
    	testScriptSystemVariablesConditionAddObjectFalseNoProperty("mapping-script-system-variables-condition-xpath.xml");
    }
    
    public void testScriptSystemVariablesConditionAddObjectFalseNoProperty(String filename) throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testScriptSystemVariablesConditionAddObjectFalseNoProperty";
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	
    	PrismObject<UserType> user = evaluator.getUserOld();
    	user.removeProperty(UserType.F_EMPLOYEE_TYPE);
    	ObjectDelta<UserType> delta = ObjectDelta.createAddDelta(user);
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				filename, 
    			TEST_NAME, "title", delta);
		
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		assertNull("Unexpected output triple: "+outputTriple, outputTriple);
    }

    
    @Test
    public void testScriptSystemVariablesConditionTrueToTrueGroovy() throws Exception {
    	testScriptSystemVariablesConditionTrueToTrue("mapping-script-system-variables-condition-groovy.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionTrueToTrueXPath() throws Exception {
    	testScriptSystemVariablesConditionTrueToTrue("mapping-script-system-variables-condition-xpath.xml");
    }
    
    public void testScriptSystemVariablesConditionTrueToTrue(String filename) throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testScriptSystemVariablesConditionTrueToTrue";
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID, 
    			evaluator.toPath("name"), evaluator.getPrismContext(), PrismTestUtil.createPolyString("Jack"));
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				filename, 
    			TEST_NAME, "title", delta);
		
		PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
		user.asObjectable().getEmployeeType().add("CAPTAIN");
		mapping.getSourceContext().recompute();
    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("Captain Jack"));
	  	PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Captain jack"));
    }

    @Test
    public void testScriptSystemVariablesConditionFalseToFalseGroovy() throws Exception {
    	testScriptSystemVariablesConditionFalseToFalse("mapping-script-system-variables-condition-groovy.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionFalseToFalseXPath() throws Exception {
    	testScriptSystemVariablesConditionFalseToFalse("mapping-script-system-variables-condition-xpath.xml");
    }
    
    public void testScriptSystemVariablesConditionFalseToFalse(String filename) throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testScriptSystemVariablesConditionFalseToFalse";
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID, 
    			evaluator.toPath("name"), evaluator.getPrismContext(), PrismTestUtil.createPolyString("Jack"));
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				filename, 
    			TEST_NAME, "title", delta);
    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		assertNull("Unexpected value in outputTriple: "+outputTriple, outputTriple);
    }
    
    @Test
    public void testScriptSystemVariablesConditionFalseToTrueGroovy() throws Exception {
    	testScriptSystemVariablesConditionFalseToTrue("mapping-script-system-variables-condition-groovy.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionFalseToTrueXPath() throws Exception {
    	testScriptSystemVariablesConditionFalseToTrue("mapping-script-system-variables-condition-xpath.xml");
    }
    
    public void testScriptSystemVariablesConditionFalseToTrue(String filename) throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testScriptSystemVariablesConditionFalseToTrue";
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID, 
    			evaluator.toPath("name"), evaluator.getPrismContext(), PrismTestUtil.createPolyString("Jack"));
    	delta.addModificationAddProperty(evaluator.toPath("employeeType"), "CAPTAIN");
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				filename, TEST_NAME, "title", delta);
    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("Captain Jack"));
	  	PrismAsserts.assertTripleNoMinus(outputTriple);
    }
    
    @Test
    public void testScriptSystemVariablesConditionTrueToFalseGroovy() throws Exception {
    	testScriptSystemVariablesConditionTrueToFalse("mapping-script-system-variables-condition-groovy.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionTrueToFalseXPath() throws Exception {
    	testScriptSystemVariablesConditionTrueToFalse("mapping-script-system-variables-condition-xpath.xml");
    }
    
    public void testScriptSystemVariablesConditionTrueToFalse(String filename) throws Exception {
    	// GIVEN
    	final String TEST_NAME = "testScriptSystemVariablesConditionTrueToFalse";
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID, 
    			evaluator.toPath("name"), evaluator.getPrismContext(), "Jack");
    	delta.addModificationDeleteProperty(evaluator.toPath("employeeType"), "CAPTAIN");
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				filename, TEST_NAME, "title", delta);
		
		PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
		user.asObjectable().getEmployeeType().add("CAPTAIN");
		mapping.getSourceContext().recompute();
    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTripleNoPlus(outputTriple);
	  	PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Captain jack"));
    }
    
    @Test
    public void testScriptSystemVariablesConditionEmptyTrue() throws Exception {
    	final String TEST_NAME = "testScriptSystemVariablesConditionEmptyTrue";
    	testScriptSystemVariablesConditionEmptyTrue(TEST_NAME, "mapping-script-system-variables-condition-empty.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionEmptyTrueFunction() throws Exception {
    	final String TEST_NAME = "testScriptSystemVariablesConditionEmptyTrueFunction";
    	testScriptSystemVariablesConditionEmptyTrue(TEST_NAME, "mapping-script-system-variables-condition-empty-function.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionEmptySingleTrue() throws Exception {
    	final String TEST_NAME = "testScriptSystemVariablesConditionEmptySingleTrue";
    	testScriptSystemVariablesConditionEmptyTrue(TEST_NAME, "mapping-script-system-variables-condition-empty-single.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionEmptySingleTrueFunction() throws Exception {
    	final String TEST_NAME = "testScriptSystemVariablesConditionEmptySingleTrueFunction";
    	testScriptSystemVariablesConditionEmptyTrue(TEST_NAME, "mapping-script-system-variables-condition-empty-single-function.xml");
    }
    
    public void testScriptSystemVariablesConditionEmptyTrue(final String TEST_NAME, String filename) throws Exception {
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	// GIVEN
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID, 
    			evaluator.toPath("name"), evaluator.getPrismContext(), PrismTestUtil.createPolyString("Jack"));
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				filename, 
    			TEST_NAME, "title", delta);
		
		PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
		user.asObjectable().getEmployeeType().clear();
		user.asObjectable().setEmployeeNumber(null);
		mapping.getSourceContext().recompute();
    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("Landlubber Jack"));
	  	PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Landlubber jack"));
    }
    
    @Test
    public void testScriptSystemVariablesConditionEmptySingleFalseToTrue() throws Exception {
    	final String TEST_NAME = "testScriptSystemVariablesConditionEmptySingleFalseToTrue";
    	testScriptSystemVariablesConditionEmptyFalseToTrue(TEST_NAME, "mapping-script-system-variables-condition-empty-single.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionEmptySingleFalseToTrueFunction() throws Exception {
    	final String TEST_NAME = "testScriptSystemVariablesConditionEmptySingleFalseToTrueFunction";
    	testScriptSystemVariablesConditionEmptyFalseToTrue(TEST_NAME, "mapping-script-system-variables-condition-empty-single-function.xml");
    }
    
    public void testScriptSystemVariablesConditionEmptyFalseToTrue(final String TEST_NAME, String filename) throws Exception {
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	// GIVEN
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID, 
    			evaluator.toPath("employeeNumber"), evaluator.getPrismContext());
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				filename, 
    			TEST_NAME, "title", delta);
		
		PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
		user.asObjectable().setEmployeeNumber("666");
		mapping.getSourceContext().recompute();
    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("Landlubber jack"));
	  	PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testScriptSystemVariablesConditionEmptyFalse() throws Exception {
    	final String TEST_NAME = "testScriptSystemVariablesConditionEmptyFalse";
    	testScriptSystemVariablesConditionEmptyFalse(TEST_NAME, "mapping-script-system-variables-condition-empty.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionEmptyFalseFunction() throws Exception {
    	final String TEST_NAME = "testScriptSystemVariablesConditionEmptyFalse";
    	testScriptSystemVariablesConditionEmptyFalse(TEST_NAME, "mapping-script-system-variables-condition-empty-function.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionEmptySingleFalse() throws Exception {
    	final String TEST_NAME = "testScriptSystemVariablesConditionEmptySingleFalse";
    	testScriptSystemVariablesConditionEmptyFalse(TEST_NAME, "mapping-script-system-variables-condition-empty-single.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionEmptySingleFalseFunction() throws Exception {
    	final String TEST_NAME = "testScriptSystemVariablesConditionEmptySingleFalseFunction";
    	testScriptSystemVariablesConditionEmptyFalse(TEST_NAME, "mapping-script-system-variables-condition-empty-single-function.xml");
    }
    
    public void testScriptSystemVariablesConditionEmptyFalse(final String TEST_NAME, String filename) throws Exception {
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	// GIVEN
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID, 
    			evaluator.toPath("name"), evaluator.getPrismContext(), PrismTestUtil.createPolyString("Jack"));
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				filename, 
    			TEST_NAME, "title", delta);
		
		PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
		user.asObjectable().getEmployeeType().add("SAILOR");
		user.asObjectable().setEmployeeNumber("666");
		mapping.getSourceContext().recompute();
    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		assertNull("Unexpected value in outputTriple: "+outputTriple, outputTriple);
    }
    
    @Test
    public void testScriptSystemVariablesConditionEmptySingleTrueToFalse() throws Exception {
    	final String TEST_NAME = "testScriptSystemVariablesConditionEmptySingleTrueToFalse";
    	testScriptSystemVariablesConditionEmptyTrueToFalse(TEST_NAME, "mapping-script-system-variables-condition-empty-single.xml");
    }
    
    @Test
    public void testScriptSystemVariablesConditionEmptySingleTrueToFalseFunction() throws Exception {
    	final String TEST_NAME = "testScriptSystemVariablesConditionEmptySingleTrueToFalseFunction";
    	testScriptSystemVariablesConditionEmptyTrueToFalse(TEST_NAME, "mapping-script-system-variables-condition-empty-single-function.xml");
    }
    
    public void testScriptSystemVariablesConditionEmptyTrueToFalse(final String TEST_NAME, String filename) throws Exception {
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	// GIVEN
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID, 
    			evaluator.toPath("employeeNumber"), evaluator.getPrismContext(), "666");
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				filename, 
    			TEST_NAME, "title", delta);
		
		PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
		user.asObjectable().setEmployeeNumber(null);
		mapping.getSourceContext().recompute();
    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTripleNoPlus(outputTriple);
	  	PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("Landlubber jack"));
    }

    @Test
    public void testNpeFalseToTrue() throws Exception {
    	final String TEST_NAME = "testNpeFalseToTrue";
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	// GIVEN
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID, 
    			UserType.F_ADDITIONAL_NAME, evaluator.getPrismContext(), "Captain Sparrow");
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				"mapping-npe.xml", 
    			TEST_NAME, "title", delta);
		    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTriplePlus(outputTriple, PrismTestUtil.createPolyString("15"));
	  	PrismAsserts.assertTripleNoMinus(outputTriple);
    }
    
    @Test
    public void testNpeTrueToFalse() throws Exception {
    	final String TEST_NAME = "testNpeTrueToFalse";
    	System.out.println("===[ "+TEST_NAME+"]===");
    	
    	// GIVEN
    	ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class, evaluator.USER_OLD_OID, 
    			UserType.F_ADDITIONAL_NAME, evaluator.getPrismContext());
    	
		Mapping<PrismPropertyValue<PolyString>> mapping = evaluator.createMapping(
				"mapping-npe.xml", 
    			TEST_NAME, "title", delta);
		
		PrismObject<UserType> user = (PrismObject<UserType>) mapping.getSourceContext().getOldObject();
		user.asObjectable().setAdditionalName(PrismTestUtil.createPolyStringType("Sultan of the Caribbean"));
		mapping.getSourceContext().recompute();
		    	        
    	OperationResult opResult = new OperationResult(TEST_NAME);
    	    	
    	// WHEN
		mapping.evaluate(opResult);
    	
    	// THEN
		PrismValueDeltaSetTriple<PrismPropertyValue<PolyString>> outputTriple = mapping.getOutputTriple();
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTripleNoPlus(outputTriple);
	  	PrismAsserts.assertTripleMinus(outputTriple, PrismTestUtil.createPolyString("23"));
    }
}
