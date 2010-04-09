package hudson.plugins.clearcase;

/**
 * A Clearcase View. This class holds view properties.
 * 
 * @author vlatombe
 * 
 */
public class View {
    private final String name;

    private final String path;
    private final String configSpec;
    public View(String name, String path, String configSpec) {
        super();
        this.name = name;
        this.configSpec = configSpec;
        this.path = path;
    }

    public String getConfigSpec() {
        return configSpec;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "View [configSpec=" + configSpec + ", name=" + name + ", path="
                + path + "]";
    }
}
