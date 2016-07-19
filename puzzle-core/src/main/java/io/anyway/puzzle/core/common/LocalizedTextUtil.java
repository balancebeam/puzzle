package io.anyway.puzzle.core.common;

import java.util.LinkedHashSet;
import java.util.Locale;

import io.anyway.puzzle.core.PluginContext;
import io.anyway.puzzle.core.i18n.PluginMessageConfigurer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.AbstractMessageSource;

public final class LocalizedTextUtil {
	
	private static Log logger= LogFactory.getLog(LocalizedTextUtil.class);
	
	private LocalizedTextUtil(){}
	
	public static String getMessage(Class<?> aClass, String code, Object[] args,String defaultMessage, Locale locale) {
		try{
			return getMessage(aClass,code,args,locale);
		}catch(NoSuchMessageException e){	
			logger.warn("not find the property: {key="+code+",locale="+locale+"}");
			//TODO format defaultMessage
			return defaultMessage;
		}
	}

	public static String getMessage(Class<?> aClass, String code, Object[] args,Locale locale) throws NoSuchMessageException {
		PluginContext pluginContext= PluginContextUtil.getPluginContext(aClass);
		if(null!= pluginContext){
			String text= getInternalMessage(pluginContext,code,args,locale);
			if(null!= text){
				return text;
			}
		}
		
		LinkedHashSet<PluginContext> hset= new LinkedHashSet <PluginContext>();
		for (Class<?> clazz = aClass.getSuperclass(); (clazz != null) && !clazz.equals(Object.class);clazz = clazz.getSuperclass()) {
			PluginContext ctx= PluginContextUtil.getPluginContext(clazz);
			if(null!= ctx){
				hset.add(ctx);
			}
		}
		Class<?>[] interfaces = aClass.getInterfaces();
		if(interfaces!= null){
			for (Class<?> anInterface: interfaces) {
				PluginContext ctx= PluginContextUtil.getPluginContext(anInterface);
				if(null!= ctx){
					hset.add(ctx);
				}
			}
		}
		if(null!= pluginContext){
			hset.remove(pluginContext);
		}
		
		for(PluginContext each: hset){
			String text= getInternalMessage(each,code,args,locale);
			if(null!= text){
				return text;
			}
		}
		
		//WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(ComponentFramework.getFrameworkServletContext());
//		try{
//			AbstractMessageSource messageSource= wac.getBean(AbstractMessageSource.class);
//			return messageSource.getMessage(code, args, locale);
//		}catch(BeansException e){
//			logger.warn("Framework does not config MessageSource");
//		}
		throw new NoSuchMessageException(code,locale); 
	}

	private static String getInternalMessage(PluginContext pluginContext, String code, Object[] args,Locale locale){
		ApplicationContext applicationContext= pluginContext.getApplicationContext();
		if(null!= applicationContext){
			try{
				AbstractMessageSource messageSource= applicationContext.getBean(AbstractMessageSource.class);
				if(messageSource instanceof PluginMessageConfigurer){
					return ((PluginMessageConfigurer)messageSource).getLocalMessage(code, args, locale);
				}
				return messageSource.getMessage(code, args, locale);
				
			}catch(BeansException e){
				logger.warn("Plugin ["+pluginContext.getName()+"] does not config MessageSource");
			}
			catch(NoSuchMessageException e){
				logger.warn("Plugin ["+pluginContext.getName()+"] can not find available proptery: {key="+code+",locale="+locale+"}");
			}
		}
		return null;
	}
	
}
