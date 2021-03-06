package se.l4.dust.core.internal;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;

import se.l4.dust.Dust;
import se.l4.dust.api.Namespace;
import se.l4.dust.api.NamespacePlugin;
import se.l4.dust.api.Namespaces;
import se.l4.dust.api.resource.Resource;
import se.l4.dust.api.resource.Resources;

@Singleton
public class NamespacesImpl
	implements Namespaces
{
	private final static SecureRandom random = new SecureRandom();

	private final Injector injector;
	private final Resources resources;

	private final Map<String, Namespace> packages;
	private final Map<String, Namespace> prefixes;
	private final Map<String, Namespace> uris;

	private final List<Namespace> namespaces;

	@Inject
	public NamespacesImpl(Injector injector, Resources resources)
	{
		this.injector = injector;
		this.resources = resources;
		packages = new ConcurrentHashMap<>();
		prefixes = new ConcurrentHashMap<>();
		uris = new ConcurrentHashMap<>();

		namespaces = Lists.newArrayList();

		bind(Dust.NAMESPACE_COMMON).add();
		bind(Dust.NAMESPACE_PARAMETERS).add();
	}

	@Override
	public NamespaceBinder bind(String nsUri)
	{
		return new NamespaceBinderImpl(nsUri);
	}

	protected static final String generateVersion(String ns)
	{
		return Long.toHexString(random.nextLong());
	}

	private void addNamespace(String uri,
			String prefix,
			String pkg,
			String version,
			String resourceReference,
			ClassLoader loader,
			List<NamespacePlugin> plugins)
	{
		Namespace ns = new NamespaceImpl(uri, prefix, pkg, version, resourceReference, loader, resources);
		namespaces.add(ns);
		uris.put(uri, ns);

		if(pkg != null)
		{
			packages.put(pkg, ns);
		}

		if(prefix != null)
		{
			prefixes.put(prefix, ns);
		}

		for(NamespacePlugin plugin : plugins)
		{
			plugin.register(injector, ns);
		}
	}

	@Override
	public boolean isBound(String ns)
	{
		return uris.containsKey(ns);
	}

	@Override
	public Namespace getBinding(String pkg)
	{
		return packages.get(pkg);
	}

	@Override
	public Namespace getNamespaceByPrefix(String prefix)
	{
		return prefixes.get(prefix);
	}

	@Override
	public Namespace getNamespaceByURI(String uri)
	{
		return uris.get(uri);
	}

	@Override
	public Namespace findNamespaceFor(Class<?> c)
	{
		String pkg = c.getName();
		while(true)
		{
			Namespace ns = packages.get(pkg);
			if(ns != null) return ns;

			int idx = pkg.lastIndexOf('.');
			if(idx == -1) return null;

			pkg = pkg.substring(0, idx);
		}
	}

	@Override
	public Iterable<Namespace> list()
	{
		return namespaces;
	}

	private class NamespaceBinderImpl
		implements NamespaceBinder
	{
		private final String uri;
		private String pkg;
		private String version;
		private String prefix;
		private ClassLoader loader;
		private String resourceReference;
		private List<NamespacePlugin> plugins;
		private boolean manual;

		public NamespaceBinderImpl(String uri)
		{
			this.uri = uri;
			plugins = Lists.newArrayList();
		}

		@Override
		public NamespaceBinder setPackage(String pkg)
		{
			if(loader != null)
			{
				// If no loader provides use the context loader
				loader = Thread.currentThread().getContextClassLoader();
			}

			this.pkg = pkg;
			return this;
		}

		@Override
		public NamespaceBinder setPackage(Package pkg)
		{
			return setPackage(pkg.getName());
		}

		@Override
		public NamespaceBinder setPackageFromClass(Class<?> type)
		{
			loader = type.getClassLoader();
			resourceReference = type.getSimpleName() + ".class";
			return setPackage(type.getPackage());
		}

		@Override
		public NamespaceBinder setVersion(String version)
		{
			this.version = version;

			return this;
		}

		@Override
		public NamespaceBinder setPrefix(String prefix)
		{
			this.prefix = prefix;

			return this;
		}

		@Override
		public NamespaceBinder manual()
		{
			this.manual = true;

			return this;
		}

		@Override
		public NamespaceBinder with(NamespacePlugin plugin)
		{
			plugins.add(plugin);

			return this;
		}

		@Override
		public void add()
		{
			if(version == null)
			{
				version = generateVersion(uri);
			}

			if(pkg == null && ! manual)
			{
				// No package set, try to autodetect
				StackTraceElement[] trace = new Exception().getStackTrace();

				try
				{
					ClassLoader loader = Thread.currentThread().getContextClassLoader();
					Class<?> type = loader.loadClass(trace[1].getClassName());
					if(Module.class.isAssignableFrom(type))
					{
						// Set the package if this a module
						setPackageFromClass(type);
					}
				}
				catch(ClassNotFoundException e)
				{
				}
			}

			addNamespace(uri, prefix, pkg, version, resourceReference, loader, plugins);
		}

	}

	private static class NamespaceImpl
		implements Namespace
	{
		private final String uri;
		private final String prefix;
		private final String pkg;
		private final String version;
		private final Locator locator;
		private final Resources resources;

		public NamespaceImpl(String uri, String prefix, String pkg, String version, String resourceReference, ClassLoader loader, Resources resources)
		{
			this.uri = uri;
			this.prefix = prefix;
			this.pkg = pkg;
			this.version = version;
			this.resources = resources;

			locator = loader != null
				? new ClassLoaderLocator(loader, pkg, resourceReference)
				: new FailingLocator(uri);
		}

		@Override
		public String getPrefix()
		{
			return prefix;
		}

		@Override
		public String getUri()
		{
			return uri;
		}

		@Override
		public String getVersion()
		{
			return version;
		}

		@Override
		public URL getClasspathResource(String resource)
		{
			return locator.locateResource(resource);
		}

		@Override
		public Resource getResource(String resource)
			throws IOException
		{
			return resources.locate(uri, resource);
		}

		@Override
		public String getPackage()
		{
			return pkg;
		}
	}

	private static interface Locator
	{
		URL locateResource(String path);

		URI resolveResource(String path);
	}

	private static class ClassLoaderLocator
		implements Locator
	{
		private final String base;
		private final ClassLoader loader;
		private final URI reference;

		public ClassLoaderLocator(ClassLoader loader, String base, String resourceReference)
		{
			this.loader = loader;
			this.base = base.replace('.', '/');

			if(resourceReference != null)
			{
				URL resource = loader.getResource(resolve(this.base, resourceReference));
				if(resource != null)
				{
					try
					{
						this.reference = resource.toURI().resolve(".").normalize();
					}
					catch(URISyntaxException e)
					{
						throw Throwables.propagate(e);
					}
				}
				else
				{
					this.reference = null;
				}
			}
			else
			{
				this.reference = null;
			}
		}

		private static String resolve(String base, String path)
		{
			if(path.startsWith("/"))
			{
				return base + path;
			}
			else
			{
				return base + '/' + path;
			}
		}

		@Override
		public URI resolveResource(String path)
		{
			return reference.resolve(path);
		}

		@Override
		public URL locateResource(String path)
		{
			return loader.getResource(resolve(base, path));
		}
	}

	private static class FailingLocator
		implements Locator
	{
		private final String uri;

		public FailingLocator(String uri)
		{
			this.uri = uri;
		}

		@Override
		public URI resolveResource(String path)
		{
			return null;
		}

		@Override
		public URL locateResource(String path)
		{
			return null;
//			throw new AssetException("The namespace " + uri + " does not have any assets. Did you tie it to a package or class?");
		}
	}
}
