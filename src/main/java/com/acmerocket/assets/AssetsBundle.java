package com.acmerocket.assets;

import com.google.common.cache.CacheBuilderSpec;
import com.yammer.dropwizard.ConfiguredBundle;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * An assets bundle (like {@link com.yammer.dropwizard.assets.AssetsBundle}) that utilizes configuration to provide the
 * ability to override how assets are loaded and cached.  Specifying an override is useful during the development phase
 * to allow assets to be loaded directly out of source directories instead of the classpath and to force them to not be
 * cached by the browser or the server.  This allows developers to edit an asset, save and then immediately refresh the
 * web browser and see the updated assets.  No compilation or copy steps are necessary.
 */
public class AssetsBundle implements ConfiguredBundle<AssetsBundleConfiguration> {
    private static final Logger LOG = LoggerFactory.getLogger(AssetsBundle.class);

    private static final String DEFAULT_PATH = "/assets";
    protected static final CacheBuilderSpec DEFAULT_CACHE_SPEC = CacheBuilderSpec.parse("maximumSize=100");
    private static final String DEFAULT_INDEX_FILE = "index.htm";
    
    private final String resourcePath;
    private final CacheBuilderSpec cacheBuilderSpec;
    private final String uriPath;
    private final String indexFile;

    /**
     * Creates a new {@link AssetsBundle} which serves up static assets from
     * {@code src/main/resources/assets/*} as {@code /assets/*}.
     *
     * @see AssetsBundle#ConfiguredAssetsBundle(String, CacheBuilderSpec)
     */
    public AssetsBundle() {
        this(DEFAULT_PATH, DEFAULT_CACHE_SPEC);
    }

    /**
     * Creates a new {@link AssetsBundle} which will configure the service to serve the static files
     * located in {@code src/main/resources/${path}} as {@code /${path}}. For example, given a
     * {@code path} of {@code "/assets"}, {@code src/main/resources/assets/example.js} would be
     * served up from {@code /assets/example.js}.
     *
     * @param path the classpath and URI root of the static asset files
     * @see AssetsBundle#ConfiguredAssetsBundle(String, CacheBuilderSpec)
     */
    public AssetsBundle(String path) {
        this(path, DEFAULT_CACHE_SPEC, path, DEFAULT_INDEX_FILE);
    }

    /**
     * Creates a new {@link AssetsBundle} which will configure the service to serve the static files
     * located in {@code src/main/resources/${resourcePath}} as {@code /${uriPath}}. For example, given a
     * {@code resourcePath} of {@code "/assets"} and a uriPath of {@code "/js"},
     * {@code src/main/resources/assets/example.js} would be served up from {@code /js/example.js}.
     *
     * @param resourcePath the resource path (in the classpath) of the static asset files
     * @param uriPath      the uri path for the static asset files
     * @see AssetsBundle#ConfiguredAssetsBundle(String, CacheBuilderSpec)
     */
    public AssetsBundle(String resourcePath, String uriPath) {
        this(resourcePath, DEFAULT_CACHE_SPEC, uriPath, DEFAULT_INDEX_FILE);
    }

    /**
     * Creates a new AssetsBundle which will configure the service to serve the static files
     * located in {@code src/main/resources/${resourcePath}} as {@code /${uriPath}}. If no file name is
     * in ${uriPath}, ${indexFile} is appended before serving. For example, given a
     * {@code resourcePath} of {@code "/assets"} and a uriPath of {@code "/js"},
     * {@code src/main/resources/assets/example.js} would be served up from {@code /js/example.js}.
     *
     * @param resourcePath        the resource path (in the classpath) of the static asset files
     * @param uriPath             the uri path for the static asset files
     * @param indexFile           the name of the index file to use
     */
    public AssetsBundle(String resourcePath, String uriPath, String indexFile) {
        this(resourcePath, DEFAULT_CACHE_SPEC, uriPath, indexFile);
    }
    
    /**
     * Creates a new {@link AssetsBundle} which will configure the service to serve the static files
     * located in {@code src/main/resources/${path}} as {@code /${path}}. For example, given a
     * {@code path} of {@code "/assets"}, {@code src/main/resources/assets/example.js} would be
     * served up from {@code /assets/example.js}.
     *
     * @param resourcePath     the resource path (in the classpath) of the static asset files
     * @param cacheBuilderSpec the spec for the cache builder
     */
    public AssetsBundle(String resourcePath, CacheBuilderSpec cacheBuilderSpec) {
        this(resourcePath, cacheBuilderSpec, resourcePath, DEFAULT_INDEX_FILE);
    }

    /**
     * Creates a new {@link AssetsBundle} which will configure the service to serve the static files
     * located in {@code src/main/resources/${resourcePath}} as {@code /${uriPath}}. For example, given a
     * {@code resourcePath} of {@code "/assets"} and a uriPath of {@code "/js"},
     * {@code src/main/resources/assets/example.js} would be served up from {@code /js/example.js}.
     *
     * @param resourcePath     the resource path (in the classpath) of the static asset files
     * @param cacheBuilderSpec the spec for the cache builder
     * @param uriPath          the uri path for the static asset files
     */
    public AssetsBundle(String resourcePath, CacheBuilderSpec cacheBuilderSpec, String uriPath, String indexFile) {
        checkArgument(resourcePath.startsWith("/"), "%s is not an absolute path", resourcePath);
        checkArgument(!"/".equals(resourcePath), "%s is the classpath root", resourcePath);
        this.resourcePath = resourcePath.endsWith("/") ? resourcePath : (resourcePath + '/');
        this.cacheBuilderSpec = cacheBuilderSpec;
        this.uriPath = uriPath.endsWith("/") ? uriPath : (uriPath + '/');
        this.indexFile = indexFile;
        
        LOG.debug("Constructed {}", this);
    }

    @Override
    public void run(AssetsBundleConfiguration bundleConfig, Environment env) throws Exception {
        AssetsConfiguration config = bundleConfig.getAssetsConfiguration();

        // Let the cache spec from the configuration override the one specified in the code
        CacheBuilderSpec spec = (config.getCacheSpec() != null)
                ? CacheBuilderSpec.parse(config.getCacheSpec())
                : cacheBuilderSpec;

        Iterable<Map.Entry<String, String>> overrides = config.getOverrides();
        Iterable<Map.Entry<String, String>> mimeTypes = config.getMimeTypes();

        env.addServlet(new AssetServlet(resourcePath, spec, uriPath, indexFile, overrides, mimeTypes), uriPath + "*");
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) { /* no-op */ }
    
    public String toString() {
        return "AssetBundle[resource=" + this.resourcePath + ", uri=" + this.uriPath 
                + ", index=" + this.indexFile + "]";
    }
}
