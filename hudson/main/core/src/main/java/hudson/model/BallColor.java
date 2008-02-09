package hudson.model;

import org.jvnet.localizer.Localizable;

import java.util.Locale;

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
    RED("red",Messages._BallColor_Failed()),
    RED_ANIME("red_anime",Messages._BallColor_InProgress()),
    YELLOW("yellow",Messages._BallColor_Unstable()),
    YELLOW_ANIME("yellow_anime",Messages._BallColor_InProgress()),
    BLUE("blue",Messages._BallColor_Success()),
    BLUE_ANIME("blue_anime",Messages._BallColor_InProgress()),
    // for historical reasons they are called grey.
    GREY("grey",Messages._BallColor_Pending()),
    GREY_ANIME("grey_anime",Messages._BallColor_InProgress()),

    DISABLED("grey",Messages._BallColor_Disabled()),
    DISABLED_ANIME("grey_anime",Messages._BallColor_InProgress()),
    ABORTED("grey",Messages._BallColor_Aborted()),
    ABORTED_ANIME("grey_anime",Messages._BallColor_InProgress()),
    ;

    private final Localizable description;
    private final String image;

    BallColor(String image, Localizable description) {
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
        return description.toString();
    }

    /**
     * Also used as a final name.
     */
    public String toString() {
        return name().toLowerCase(Locale.ENGLISH);
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
