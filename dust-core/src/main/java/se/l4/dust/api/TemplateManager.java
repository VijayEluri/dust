package se.l4.dust.api;

import se.l4.dust.api.annotation.Component;
import se.l4.dust.api.template.mixin.TemplateMixin;
import se.l4.dust.api.template.spi.PropertySource;

/**
 * Manager of template related information such as registered filters and
 * components.
 * 
 * @author Andreas Holstenson
 *
 */
public interface TemplateManager
{
	/**
	 * Template information scoped to work on a single namespace.
	 * 
	 * @author Andreas Holstenson
	 *
	 */
	interface TemplateNamespace
	{
		/**
		 * Add a component to the manager, the component will be made available
		 * for usage within templates. The name of the component is resolved via
		 * a {@link Component} annotation placed on the actual class.
		 * 
		 * @param component
		 * 		the class of the component
		 */
		TemplateNamespace addComponent(Class<?> component);
		
		/**
		 * Add a component to the manager using one or more custom names.
		 * 
		 * @param component
		 * 		the class of the component
		 * @param names
		 * 		names of the component
		 */
		TemplateNamespace addComponent(Class<?> component, String... names);
		
		/**
		 * Retrieve a component in the given namespace with the specified name.
		 * If the component is not found this will throw {@link ComponentException}.
		 * 
		 * @param ns
		 * 		namespace of component
		 * @param name
		 * 		name of component
		 * @return
		 */
		Class<?> getComponent(String name);
		
		/**
		 * Check if a certain component exists.
		 * 
		 * @param ns
		 * 		namespace of component
		 * @param name
		 * 		name of component
		 * @return
		 */
		boolean hasComponent(String name);
		
		/**
		 * Get the primary name of the given component.
		 * 
		 * @param component
		 * @return
		 */
		String getComponentName(Class<?> component);
		
		/**
		 * Add a mixin to this namespace. This mixin will be triggered when
		 * the given attribute is found on an element.
		 * 
		 * @param mixin
		 * @return
		 */
		TemplateNamespace addMixin(String attribute, TemplateMixin mixin);
		
		/**
		 * Get if a mixin exists for the given attribute.
		 * 
		 * @param attribute
		 * @return
		 */
		boolean hasMixin(String attribute);
		
		/**
		 * Get a mixin (if registered) for the given attribute.
		 * 
		 * @param attribute
		 * @return
		 */
		TemplateMixin getMixin(String attribute);
	}
	
	/**
	 * Get a class suitable for accessing components and templates within
	 * a specific namespace.
	 * 
	 * @param nsUri
	 * @return
	 */
	TemplateNamespace getNamespace(String nsUri);
	
	/**
	 * Add a property source that can be used within {@literal ${}} expansions
	 * in templates.
	 * 
	 * @param prefix
	 * 		prefix of the source (used in expansions, examples: var, asset)
	 * @param source
	 * 		the source to register
	 */
	void addPropertySource(String prefix, PropertySource source);
	
	/**
	 * Get the property source associated with given prefix.
	 * 
	 * @param prefix
	 * 		the prefix to lookup
	 * @return
	 */
	PropertySource getPropertySource(String prefix);

	/**
	 * Add a property source that can be used within {@literal ${}} expansions
	 * in templates.
	 * 
	 * @param prefix
	 * 		prefix of the soruce (used in expansions, examples: var, asset)
	 * @param type
	 * 		the class of the source
	 */
	void addPropertySource(String prefix, Class<? extends PropertySource> type);
	
	/**
	 * Get a template identifier for identifying a template uniquely.
	 * 
	 * @return
	 */
	int fetchTemplateId();
}
