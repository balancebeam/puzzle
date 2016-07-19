package io.anyway.puzzle.core.spring.namespace;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

import io.anyway.puzzle.core.spring.PluginServiceReferenceFactoryBean;
import io.anyway.puzzle.core.spring.parser.PluginServiceBeanDefinitionParser;

public class PluginServiceNamespaceHandlerSupport extends NamespaceHandlerSupport{

	@Override
	public void init() {
		registerBeanDefinitionParser("reference", new PluginServiceBeanDefinitionParser(PluginServiceReferenceFactoryBean.class));  
	}

}
