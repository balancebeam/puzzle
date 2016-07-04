package io.pdf.core.spring;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.osgi.internal.loader.ModuleClassLoader;
import org.springframework.core.io.ClassPathResource;

public class PluginClassPathResource extends ClassPathResource{

	public PluginClassPathResource(String path, ClassLoader classLoader) {
		super(path, classLoader);
	}
	
	protected URL resolveURL() {
		if(getClassLoader() instanceof ModuleClassLoader){
			return ((ModuleClassLoader)getClassLoader()).findLocalResource(getPath());
		}
		return super.resolveURL();
	}
	
	public InputStream getInputStream() throws IOException {
		if(getClassLoader() instanceof ModuleClassLoader){
			URL url= ((ModuleClassLoader)getClassLoader()).findLocalResource(getPath());
			if(url!=null){
				return url.openStream();
			}
			throw new FileNotFoundException(getDescription() + " cannot be opened because it does not exist");
		}
		return super.getInputStream();
	}
	
}
