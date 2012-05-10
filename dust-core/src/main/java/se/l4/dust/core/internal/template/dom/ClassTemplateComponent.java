package se.l4.dust.core.internal.template.dom;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import se.l4.dust.api.TemplateException;
import se.l4.dust.api.annotation.PrepareRender;
import se.l4.dust.api.annotation.Template;
import se.l4.dust.api.annotation.TemplateParam;
import se.l4.dust.api.conversion.TypeConverter;
import se.l4.dust.api.template.RenderingContext;
import se.l4.dust.api.template.TemplateCache;
import se.l4.dust.api.template.dom.Content;
import se.l4.dust.api.template.dom.DocType;
import se.l4.dust.api.template.dom.Element;
import se.l4.dust.api.template.dom.ParsedTemplate;
import se.l4.dust.api.template.spi.TemplateOutputStream;
import se.l4.dust.core.internal.template.components.EmittableComponent;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;

/**
 * Component based on a class. 
 * 
 * @author Andreas Holstenson
 *
 */
public class ClassTemplateComponent
	extends EmittableComponent
{
	private final TypeConverter converter;
	private final Class<?> type;
	private final Injector injector;
	private final TemplateCache cache;
	private final boolean development;
	private final MethodInvocation[] prepareRender;
	private final MethodInvocation[] setMethods;
	private final Provider<Object> instance;
	
	public ClassTemplateComponent(
			String name,
			Injector injector, 
			TemplateCache cache,
			TypeConverter converter,
			Class<?> type)
	{
		this(name, injector.getInstance(Stage.class) == Stage.DEVELOPMENT, injector, cache, converter, type, null, null);
	}
	
	private ClassTemplateComponent(
			String name,
			boolean development,
			Injector injector, 
			TemplateCache cache,
			TypeConverter converter,
			Class<?> type,
			MethodInvocation[] prepareRender,
			MethodInvocation[] setMethods)
	{
		super(name, type);
		
		this.development = development;
		
		instance = createInstance(development, injector, type);
		
		this.converter = converter;
		
		this.cache = cache;
		this.type = type;
		this.injector = injector;
		
		this.prepareRender = prepareRender == null && ! development 
			? createMethodInvocations(type) 
			: prepareRender;
			
		this.setMethods = setMethods == null && ! development
			? createSetMethodInvocations(type)
			: setMethods;
	}
	
	/**
	 * Create a provider for use either in production or development. The
	 * development injector will always call {@link Injector#getInstance(Class)}
	 * to make sure that any class changes are applied.
	 * 
	 * @param development
	 * @param injector
	 * @param type
	 * @return
	 */
	private static Provider<Object> createInstance(boolean development, final Injector injector, final Class<?> type)
	{
		if(development)
		{
			return new Provider<Object>()
			{
				@Override
				public Object get()
				{
					return injector.getInstance(type);
				}
			};
		}
		else
		{
			return (Provider) injector.getProvider(type);
		}
	}

	@Override
	public Content copy()
	{
		return new ClassTemplateComponent(getName(), development, injector, cache, converter, type, prepareRender, setMethods)
			.copyAttributes(this);
	}
	
	private MethodInvocation[] createMethodInvocations(Class<?> type)
	{
		List<MethodInvocation> invocations = new ArrayList<MethodInvocation>();
		while(type != Object.class && type != null)
		{
			for(Method m : type.getDeclaredMethods())
			{
				if(m.isAnnotationPresent(PrepareRender.class))
				{
					invocations.add(new MethodInvocation(m));
				}
			}
			
			type = type.getSuperclass();
		}
		
		return invocations.toArray(new MethodInvocation[invocations.size()]);
	}
	
	private MethodInvocation[] createSetMethodInvocations(Class<?> type)
	{
		List<MethodInvocation> invocations = new ArrayList<MethodInvocation>();
		while(type != Object.class && type != null)
		{
			for(Method m : type.getDeclaredMethods())
			{
				if(m.isAnnotationPresent(PrepareRender.class))
				{
					// Skip rendering methods
					continue;
				}
				
				Annotation[][] annotations = m.getParameterAnnotations();
				_annotations:
				for(int i=0, n=annotations.length; i<n; i++)
				{
					for(Annotation a : annotations[i])
					{
						if(a instanceof TemplateParam)
						{
							invocations.add(new MethodInvocation(m));
							break _annotations;
						}
					}
				}
			}
			
			type = type.getSuperclass();
		}
		
		return invocations.toArray(new MethodInvocation[invocations.size()]);
	}
	
	@Override
	public void emit(Emitter emitter, 
			RenderingContext ctx,
			TemplateOutputStream out)
		throws IOException
	{
		Object o = instance.get();
		
		Object data = emitter.getObject();
		Object root;
		
		// Run all set methods
		MethodInvocation[] setMethods = development ? createSetMethodInvocations(type) : this.setMethods;
		for(MethodInvocation m : setMethods)
		{
			if(m.valid())
			{
				m.invoke(ctx, data, o);
			}
		}
		
		// Run all methods for prepare render
		MethodInvocation[] methods = development ? createMethodInvocations(type) : this.prepareRender;
		if(methods.length == 0)
		{
			// No processing needed
			root = o;
		}
		else
		{
			// Find the best method to invoke
			MethodInvocation bestMethod = null;
			if(methods.length == 1)
			{
				bestMethod = methods[0];
			}
			else
			{
				int bestScore = 0;
				boolean hasExact = false;
				for(MethodInvocation i : methods)
				{
					int score = i.score();
					if(score > bestScore || (! hasExact && score == i.maxScore()))
					{
						bestMethod = i;
						bestScore = score;
						hasExact = score == i.maxScore();
					}
				}
			}
			
			// Actually invoke the method
			root = bestMethod.invoke(ctx, data, o);
			if(root == null)
			{
				root = o;
			}
		}

		if(root instanceof Element)
		{
			emitter.emit(out, (Element) root);
		}
		else
		{
			// Process the template of the component 
			ParsedTemplate template = cache.getTemplate(ctx, root.getClass(), (Template) null);
			
			// Switch to new context
			Integer old = emitter.switchData(template.getRawId(), root);
			Integer oldComponent = emitter.switchComponent(template.getRawId(), (Element) getRawContents()[0]);
			
			DocType docType = template.getDocType();
			if(docType != null)
			{
				out.docType(docType.getName(), docType.getPublicId(), docType.getSystemId());
			}
			
			Element templateRoot = template.getRoot();
			
			emitter.emit(out, templateRoot);
			
			// Switch context back
			emitter.switchData(old);
			emitter.switchComponent(oldComponent);
		}
	}
	
	private class MethodInvocation
	{
		private final Method method;
		private final Argument[] arguments;
		
		public MethodInvocation(Method m)
		{
			this.method = m;
			
			Argument[] result = new Argument[m.getParameterTypes().length];
			for(int i=0, n=result.length; i<n; i++)
			{
				result[i] = new Argument(m, i);
			}
			
			arguments = result;
		}
		
		/**
		 * Score the invocation.
		 * 
		 * @return
		 */
		public int score()
		{
			int score = 0;
			for(Argument arg : arguments)
			{
				if(arg.canBeInjected())
				{
					score++;
				}
			}
			
			return score;
		}
		
		public int maxScore()
		{
			return arguments.length;
		}
		
		public boolean valid()
		{
			if(arguments.length == 0) return true;
			
			for(Argument arg : arguments)
			{
				if(! arg.canBeInjected())
				{
					return false;
				}
			}
			
			return true;
		}
		
		public Object invoke(RenderingContext ctx, Object root, Object self)
		{
			Object[] data = new Object[arguments.length];
			for(int i=0, n=arguments.length; i<n; i++)
			{
				Object value = arguments[i].getValue(ctx, root);
				value = converter.convert(value, arguments[i].typeClass);
				data[i] = value;
			}
			
			try
			{
				return method.invoke(self, data);
			}
			catch(InvocationTargetException e)
			{
				Throwable e2 = e.getCause();
				if(e2 instanceof RuntimeException)
				{
					throw (RuntimeException) e2;
				}
				
				throw new TemplateException("Unable to invoke method " + method 
					+ "; " + e2.getMessage() + "\nArguments were: " + Arrays.toString(data), e2);
			}
			catch(Exception e)
			{
				throw new TemplateException("Unable to invoke method " + method 
					+ "; " + e.getMessage() + "\nArguments were: " + Arrays.toString(data), e);
			}
		}
	}
	
	private class Argument
	{
		private final Method method;
		private final Type type;
		private final Annotation[] annotations;
		private final String attribute;
		private final Class<?> typeClass;
		private final Binding binding;
		
		public Argument(Method m, int index)
		{
			method = m;
			annotations = m.getParameterAnnotations()[index];
			type = m.getGenericParameterTypes()[index];
			typeClass = m.getParameterTypes()[index];
			
			attribute = findAttribute(m, annotations);
			binding = attribute == null ? findBinding() : null;
		}
		
		private Binding findBinding()
		{
			TypeLiteral literal = TypeLiteral.get(type);
			
			for(Binding b : (List<Binding>) injector.findBindingsByType(literal))
			{
				Key key = b.getKey();
				
				if(key.getAnnotation() != null)
				{
					for(Annotation a : annotations)
					{
						if(key.getAnnotation().equals(a))
						{
							return b;
						}
					}
				}
				
				if(key.getAnnotation() == null)
				{
					return b;
				}
			}
			
			return null;
		}
		
		private String findAttribute(Method m, Annotation[] annotations)
		{
			for(Annotation a : annotations)
			{
				if(a instanceof TemplateParam)
				{
					return ((TemplateParam) a).value();
				}
			}
			
			return null;
		}
		
		public boolean canBeInjected()
		{
			if(binding != null)
			{
				// Bindings can always be handled
				return true;
			}
			else if(attribute != null)
			{
				// If the attribute exists we can be injected
				return getAttributeValue(attribute) != null;
			}
			else
			{
				// Assume that we can be injected
				// FIXME: Actually check with the context
				return true;
			}
		}
		
		public Object getValue(RenderingContext ctx, Object root)
		{
			if(attribute != null)
			{
				Attribute attr = getAttribute(attribute);
				
				if(attr == null)
				{
					throw new TemplateException("Attribute '" +attribute+ "' is missing in template file for method " + method);
				}
				
				return attr.getValue(ctx, root);
			}
			else if(binding != null)
			{
				return binding.getProvider().get();
			}
			else
			{
				return ctx.resolveObject(method, type, annotations, root);
			}
		}
	}
	
}
