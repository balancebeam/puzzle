package io.anyway.puzzle.jasper.service;

import io.anyway.puzzle.jasper.JspDispatcherFilter;
import org.springframework.stereotype.Service;

import io.anyway.puzzle.core.PluginContext;
import io.anyway.puzzle.core.service.PluginExtensionPointService;

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
