package io.pdf.core.spring.namespace;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

import io.pdf.core.spring.PluginServiceReferenceFactoryBean;
import io.pdf.core.spring.parser.PluginServiceBeanDefinitionParser;

public class PluginServiceNamespaceHandlerSupport extends NamespaceHandlerSupport{

	@Override
	public void init() {
		registerBeanDefinitionParser("reference", new PluginServiceBeanDefinitionParser(PluginServiceReferenceFactoryBean.class));  
	}

}
