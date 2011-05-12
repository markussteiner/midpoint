/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.web.model;

import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import java.io.Serializable;

/**
 *
 * @author semancik
 */
public abstract class ObjectDto implements Serializable {

    private ObjectStage stage;
    ObjectType xmlObject;

    ObjectDto() {
        stage = null;
        xmlObject = null;
    };

    /**
     * Initialize DTO using XML (JAX-B) object.
     * DTO initialized like this may only be part of other DTOs.
     * It cannot be submited directly.
     * @param object
     */
    public ObjectDto(ObjectType object) {
        xmlObject = object;
    }

    /**
     * Initialize DTO using object stage.
     * DTO initalized like this cannot be part of other DTOs.
     * It can be submited directly.
     * @param stage
     */
    ObjectDto(ObjectStage stage) {
        this.stage = stage;
    }

    public ObjectType getXmlObject() {
        if (stage != null ) {
            return stage.getObject();
        }
        if (xmlObject!=null) {
            return xmlObject;
        }
        throw new IllegalStateException();
    }

    /**
     * This method is NOT public. It must be used ONLY by this interface
     * implementation. It MUST NOT be used by the clients of this interface.
     * 
     * @return
     */
    public ObjectStage getStage() {
        return stage;
    }

    /**
     * This method is NOT public. It must be used ONLY by this interface
     * implementation. It MUST NOT be used by the clients of this interface.
     *
     * @param stage
     */
    public void setStage(ObjectStage stage) {
        this.stage = stage;
    }

    /**
     * This method is NOT public. It must be used ONLY by this interface
     * implementation. It MUST NOT be used by the clients of this interface.
     *
     * @param xmlObject
     */
    public void setXmlObject(ObjectType xmlObject) {
        this.xmlObject = xmlObject;
    }

    public String getName() {
        return getXmlObject().getName();
    }

    public void setName(String value) {
        getXmlObject().setName(value);
    }

    public String getOid() {
        return getXmlObject().getOid();
    }

    public void setOid(String value) {
        getXmlObject().setOid(value);
    }

    public String getVersion() {
        return getXmlObject().getVersion();
    }

    public void setVersion(String value) {
        getXmlObject().setVersion(value);
    }

}
