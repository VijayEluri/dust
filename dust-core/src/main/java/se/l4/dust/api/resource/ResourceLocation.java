package se.l4.dust.api.resource;

/**
 * The location of a resource.
 * 
 * @author Andreas Holstenson
 *
 */
public interface ResourceLocation
{
	/**
	 * Create a new location that represents this resource but with a
	 * different extension.
	 * 
	 * @param newExtension
	 * @return
	 */
	ResourceLocation withExtension(String newExtension);

	/**
	 * Get the name of this resource.
	 * 
	 * @return
	 */
	String getName();
}
