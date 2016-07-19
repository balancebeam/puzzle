package io.anyway.puzzle.core.spring.parser;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

import io.anyway.puzzle.core.exception.PuzzleException;

public class PluginServiceBeanDefinitionParser  extends AbstractSingleBeanDefinitionParser{
	
	private Class<?> type;
	
	public PluginServiceBeanDefinitionParser(Class<?> type){
		this.type= type;
	}
	
	@Override
	protected Class<?> getBeanClass(Element element) {  
        return type;  
    }  
	
	@Override
    protected void doParse(Element element, BeanDefinitionBuilder bean) {  
        String id = element.getAttribute("id");
        String name = element.getAttribute("name");
        String interfaceName= element.getAttribute("interface");
        
        if (StringUtils.hasText(id)) {  
            bean.addPropertyValue("id", id);  
        }  
        if (StringUtils.hasText(name)) {  
            bean.addPropertyValue("name", name);  
        }  
        if (!StringUtils.hasText(interfaceName)) { 
        	throw new PuzzleException("<"+element.getNodeName()+"/> miss interface attribute");
        }  
        bean.addPropertyValue("interface", interfaceName); 
        
    } 

}
