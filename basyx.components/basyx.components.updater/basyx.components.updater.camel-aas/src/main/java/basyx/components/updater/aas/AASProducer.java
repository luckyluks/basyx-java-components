/*******************************************************************************
* Copyright (C) 2021 the Eclipse BaSyx Authors
* 
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/

* 
* SPDX-License-Identifier: EPL-2.0
******************************************************************************/

package basyx.components.updater.aas;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import org.eclipse.basyx.submodel.metamodel.connected.submodelelement.dataelement.ConnectedProperty;
import org.eclipse.basyx.submodel.metamodel.map.submodelelement.dataelement.property.valuetype.ValueType;
import org.eclipse.basyx.vab.modelprovider.VABElementProxy;
import org.eclipse.basyx.vab.modelprovider.api.IModelProvider;
import org.eclipse.basyx.vab.protocol.http.connector.HTTPConnectorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AASProducer extends DefaultProducer {
	private static final Logger LOG = LoggerFactory.getLogger(AASProducer.class);
    private AASEndpoint endpoint;
	private ConnectedProperty connectedProperty;

    public AASProducer(AASEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;
		LOG.info("Creating ASS Producer for endpoint " + endpoint.getEndpointUri());
		connectToElement();
    }

	@Override
	public void process(Exchange exchange) throws Exception {
		ValueType propertyValueType = connectedProperty.getValueType();
		Object messageBody = exchange.getMessage().getBody(interpretValueType(propertyValueType));
		LOG.info("Interpreted message={} as class={}", messageBody.toString(), interpretValueType(propertyValueType));
		if (propertyValueType.toString().equals("string")){
			connectedProperty.setValue(fixMessage(messageBody.toString()));
		}else{
			connectedProperty.setValue(messageBody);
		}
    }

	Class<?> interpretValueType(ValueType valueType){
		switch (valueType.toString()) {
			case "int":
				return Integer.class;
			case "float":
				return Float.class;
			case "byte":
				return String.class; // if set to Byte.class, Camel throws an TypeConversionException
			default:
				return String.class;
		}
	};

    String fixMessage(String messageBody) {
		String fixedMessageBody = "";
		if (messageBody != null) {
			if (messageBody.startsWith("\"") && messageBody.endsWith("\"")) {
				fixedMessageBody = messageBody.substring(1,messageBody.length() - 1);
			} else {
				fixedMessageBody = messageBody;
			}
		}
		return fixedMessageBody;
	}

	/**
	 * Connect the the Submodel Element for data dumping
	 */
    private void connectToElement() {
    	HTTPConnectorFactory factory = new HTTPConnectorFactory();
    	String proxyUrl = this.endpoint.getFullProxyUrl();
    	IModelProvider provider = factory.getConnector(proxyUrl);
    	VABElementProxy proxy = new VABElementProxy("", provider);
    	this.connectedProperty = new ConnectedProperty(proxy);
	}
}
