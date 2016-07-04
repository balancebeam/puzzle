package io.pdf.jasper.service;

import org.springframework.stereotype.Service;

import io.pdf.core.PluginContext;
import io.pdf.core.service.PluginExtensionPointService;
import io.pdf.jasper.JspDispatcherFilter;

@Service
public class JasperExtensionService extends PluginExtensionPointService{

	@Override
	public void register(PluginContext PluginContext) {
		
	}

	@Override
	public void unregister(PluginContext pluginContext) {
		//卸载编译过的JSP
		JspDispatcherFilter.clearComponentJSP(pluginContext.getName());
	}

}
