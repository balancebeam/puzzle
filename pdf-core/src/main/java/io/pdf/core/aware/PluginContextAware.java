package io.pdf.core.aware;

import org.springframework.beans.factory.Aware;

import io.pdf.core.PluginContext;
/**
 * Auto aware PluginContext
 * @author yangzz
 *
 */
public interface PluginContextAware extends Aware{
	
	void setPluginContext(PluginContext ctx);
	
}
