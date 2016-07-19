package io.anyway.puzzle.bridge.plugin.internal;

import java.net.URL;
import java.net.URLClassLoader;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Enumeration;
import java.util.NoSuchElementException;

public class PluginFrameworkClassLoader extends URLClassLoader {
	
	public PluginFrameworkClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
	}

	public URL getResource(String name) {
		URL resource = findResource(name);
		if (resource == null) {
			ClassLoader parent = getParent();
			if (parent != null)
				resource = parent.getResource(name);
		}
		return resource;
	}

	protected synchronized Class<?> loadClass(String name, boolean resolve)
			throws ClassNotFoundException {
		Class<?> clazz = findLoadedClass(name);
		if (clazz == null) {
			try {
				clazz = findClass(name); // find local class firstly
			} catch (ClassNotFoundException e) {
				ClassLoader parent = getParent();
				if (parent != null) {
					clazz = parent.loadClass(name);
				}
				if (clazz == null) {
					clazz = getSystemClassLoader().loadClass(name);
				}
			}
		}
		if (resolve)
			resolveClass(clazz);
		return clazz;
	}

	// we want to ensure that the framework has AllPermissions
	protected PermissionCollection getPermissions(CodeSource codesource) {
		return allPermissions;
	}

	static final PermissionCollection allPermissions = new PermissionCollection() {
		private static final long serialVersionUID = 482874725021998286L;
		// The AllPermission permission
		Permission allPermission = new AllPermission();

		// A simple PermissionCollection that only has AllPermission
		public void add(Permission permission) {
			// do nothing
		}

		public boolean implies(Permission permission) {
			return true;
		}

		public Enumeration<Permission> elements() {
			return new Enumeration<Permission>() {
				int cur = 0;

				public boolean hasMoreElements() {
					return cur < 1;
				}

				public Permission nextElement() {
					if (cur == 0) {
						cur = 1;
						return allPermission;
					}
					throw new NoSuchElementException();
				}
			};
		}
	};

	static {
		if (allPermissions.elements() == null)
			throw new IllegalStateException();
	}
}
