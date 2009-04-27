package hudson.plugins.checkstyle.util;

import hudson.util.ChartUtil.NumberOnlyBuildLabel;

import org.jfree.chart.labels.CategoryToolTipGenerator;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.urls.CategoryURLGenerator;
import org.jfree.data.category.CategoryDataset;

/**
 * {@link StackedBarRenderer} that provides direct access to the individual
 * results of a build via links. This renderer does not render tooltips, these
 * need to be defined in sub-classes.
 *
 * @author Ulli Hafner
 */
public abstract class AbstractBoxRenderer extends StackedBarRenderer implements CategoryToolTipGenerator, CategoryURLGenerator {
    /** Unique identifier of this class. */
    private static final long serialVersionUID = -2619266954533495982L;
    /** Base URL of the graph links. */
    private final String url;
    /** Tooltip provider for the clickable map. */
    private final ToolTipBuilder toolTipBuilder;

    /**
     * Creates a new instance of <code>AbstractAreaRenderer</code>.
     *
     * @param url
     *            base URL of the graph links
     * @param toolTipProvider
     *            tooltip provider for the clickable map
     */
    public AbstractBoxRenderer(final String url, final ToolTipProvider toolTipProvider) {
        super();
        setItemURLGenerator(this);
        setToolTipGenerator(this);
        toolTipBuilder = new ToolTipBuilder(toolTipProvider);
        this.url = "/" + url + "/";
    }

    /** {@inheritDoc} */
    public final String generateURL(final CategoryDataset dataset, final int row, final int column) {
        return getLabel(dataset, column).build.getNumber() + url;
    }

    /**
     * Gets the tool tip builder.
     *
     * @return the tool tip builder
     */
    public final ToolTipBuilder getToolTipBuilder() {
        return toolTipBuilder;
    }

    /**
     * Returns the Hudson build label at the specified column.
     *
     * @param dataset
     *            data set of values
     * @param column
     *            the column
     * @return the label of the column
     */
    private NumberOnlyBuildLabel getLabel(final CategoryDataset dataset, final int column) {
        return (NumberOnlyBuildLabel)dataset.getColumnKey(column);
    }
}
