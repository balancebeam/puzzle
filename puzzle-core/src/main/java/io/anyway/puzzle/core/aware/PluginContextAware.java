package io.anyway.puzzle.core.aware;

import io.anyway.puzzle.core.PluginContext;
import org.springframework.beans.factory.Aware;

/**
 * Auto aware PluginContext
 * @author yangzz
 *
 */
public interface PluginContextAware extends Aware{
	
	void setPluginContext(PluginContext ctx);
	
}
