package hudson.model;

/**
 * Ball color used for the build status indication.
 *
 * <p>
 * There are four basic colors, plus their animated "bouncy" versions.
 * {@link #ordinal()} is the sort order. 
 *
 * <p>
 * Note that mutiple {@link BallColor} instances may map to the same
 * RGB color, to avoid the rainbow effect.
 *
 * <h2>Historical Note</h2>
 * <p>
 * Hudson started to overload colors &mdash; for example grey could mean
 * either disabled, aborted, or not yet built. As a result, {@link BallColor}
 * becomes more like a "logical" color, in the sense that different {@link BallColor}
 * values can map to the same RGB color. See issue #956.
 *
 * @author Kohsuke Kawaguchi
 */
public enum BallColor {
    RED("red","Failed"),
    RED_ANIME("red_anime","In progress"),
    YELLOW("yellow","Unstable"),
    YELLOW_ANIME("yellow_anime","In progress"),
    BLUE("blue","Success"),
    BLUE_ANIME("blue_anime","In progress"),
    // for historical reasons they are called grey.
    GREY("grey","Pending"),
    GREY_ANIME("grey_anime","In progress"),

    DISABLED("grey","Disabled"),
    DISABLED_ANIME("grey_anime","In progress"),
    ABORTED("grey","Aborted"),
    ABORTED_ANIME("grey_anime","In progress"),
    ;

    private final String description;
    private final String image;

    BallColor(String image, String description) {
        // name() is not usable in the constructor, so I have to repeat the name twice
        // in the constants definition.
        this.image = image+".gif";
        this.description = description;
    }

    /**
     * String like "red.gif" that represents the file name of the image.
     */
    public String getImage() {
        return image;
    }

    /**
     * Gets the human-readable description used as img/@alt.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Also used as a final name.
     */
    public String toString() {
        return name().toLowerCase();
    }

    /**
     * Gets the animated version.
     */
    public BallColor anime() {
        if(name().endsWith("_ANIME"))   return this;
        else                            return valueOf(name()+"_ANIME");
    }

    /**
     * Gets the unanimated version.
     */
    public BallColor noAnime() {
        if(name().endsWith("_ANIME"))   return valueOf(name().substring(0,name().length()-6));
        else                            return this;
    }
}
