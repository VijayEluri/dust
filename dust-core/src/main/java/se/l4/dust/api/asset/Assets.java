package se.l4.dust.api.asset;

import se.l4.dust.api.Context;
import se.l4.dust.api.Namespaces;
import se.l4.dust.api.resource.Resource;
import se.l4.dust.api.resource.Resources;

/**
 * Asset management, helps with locating and registering assets. Assets are
 * files that are usually located within the classpath of a project. See
 * {@link Resources} for details.
 *
 * <h2>Usage in templates</h2>
 * If assets are used in a template the template must define their namespace
 * as a xmlns-attribute. This is usually done on the top-level element of the
 * template, but can be done anywhere where the asset usage is within scope.
 * To resolve an asset to a string a property expansion is used as follows
 * ${asset:namespace:path/to/file}. This property will be replaced with a URL
 * that points to the given asset.
 *
 * <h3>Example usage</h3>
 * <pre>
 * 	&lt;html xmlns:e="http://example.org"&gt;
 * 		&lt;head&gt;
 * 			&lt;link href="${asset:e:default.css}" rel="stylesheet" type="text/css"/&gt;
 * 		&lt;/head&gt;
 * 	&lt;/html&gt;
 * </pre>
 *
 * <h2>Namespace prefixes and URLs</h2>
 * Usually one should bind namespaces containing a prefix as it will be used
 * when serving an asset over HTTP. The URL will normally be
 * {@code /asset/prefix/path/to/file} where prefix was defined when binding
 * in {@link Namespaces}. If no prefix has been set for namespace no
 * assets from that namespace can be served.
 *
 * @author Andreas Holstenson
 *
 */
public interface Assets
{
	/**
	 * Attempt to locate the given asset by traversing all of the registered
	 * sources and returning the first match.
	 *
	 * @param uri
	 * @param file
	 * @return
	 */
	Asset locate(Context context, String namespace, String file);

	/**
	 * Get if the extension is protected and requires a checksum in the URL.
	 * Certain filetypes should be protected when served from e.g. the
	 * classpath. The protected filetypes require that a checksum is calculated
	 * and included in the URL, example: Index.xml becomes Index.checksum.xml
	 *
	 * <p>
	 * By default .xml and .class files are protected (templates and classes)
	 *
	 * @param extension
	 * @return
	 */
	boolean isProtectedExtension(String extension);

	/**
	 * Add a protected extension, see {@link #isProtectedExtension(String)}
	 * for details.
	 *
	 * @param extension
	 */
	void addProtectedExtension(String extension);

	/**
	 * Add access to a temporary asset in the given namespace. This is useful
	 * when assets are dynamically generated at runtime.
	 *
	 * @param ns
	 * @param path
	 * @param resource
	 */
	void addTemporaryAsset(String ns, String path, Resource resource);

	/**
	 * Start building a custom combined asset. Combined assets are special in
	 * that they combine several files into a single one. This can be used
	 * to combined CSS or JavaScript files at runtime to reduce the number
	 * of HTTP-requests required.
	 *
	 * @param namespace
	 * @param pathToFile
	 * @return
	 */
	AssetBuilder define(String namespace, String pathToFile);

	/**
	 * Start creating a new {@link AssetPipeline asset pipeline}.
	 *
	 * @return
	 */
	AssetPipelineBuilder newPipeline();

	/**
	 * Create a new pipeline with the given processors.
	 *
	 * @param processors
	 * @return
	 */
	AssetPipeline pipeline(AssetProcessor... processors);
}
