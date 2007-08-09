package hudson.plugins.violations.render;

import java.util.Map;
import java.util.Set;
import hudson.model.Build;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.File;

import hudson.plugins.violations.parse.ParseXML;
import hudson.plugins.violations.parse.FileModelParser;
import hudson.plugins.violations.model.FileModel;
import hudson.plugins.violations.model.Violation;
import hudson.plugins.violations.generate.XMLUtil;


/**
 * A proxy class for FileModel used to allow
 * lazy loading of FileModel. This class
 * is also used to render the FileModel.
 */
public class FileModelProxy  {
   private static final Logger LOG
        = Logger.getLogger(FileModelProxy.class.getName());

    private final File      xmlFile;
    private FileModel fileModel;
    private String    contextPath;
    private Build     build;

    /**
     * Construct this proxy.
     * @param xmlFile the xmlfile to create the FileModel from.
     */
    public FileModelProxy(File xmlFile) {
        this.xmlFile = xmlFile;
    }


    /**
     * Fluid setting of the build attribute.
     * @param build the owner build.
     * @return this object.
     */
    public FileModelProxy build(Build build) {
        this.build = build;
        return this;
    }

    /**
     * Fluid setting of context path.
     * This is used for getting icons.
     * @param contextPath the current WEB context path.
     * @return this object.
     */
    public FileModelProxy contextPath(String contextPath) {
        this.contextPath = contextPath;
        return this;
    }

    /**
     * get the current build.
     * @return the current build.
     */
    public Build getBuild() {
        return build;
    }

    /**
     * Get the type line.
     * @param type the violation type.
     * @return a rendered line containing the number of violations
     *         and the number of suppressed violations.
     */
    public String typeLine(String type) {
        FileModel.LimitType l = fileModel.getLimitTypeMap().get(type);
        StringBuilder b = new StringBuilder();
        if (l == null) {
            return type + " ?number?";
        }
        b.append(type);
        b.append("&nbsp;&nbsp;&nbsp;");
        b.append(l.getNumber());
        b.append(" violation");
        if (l.getNumber() > 1) {
            b.append("s");
        }
        if (l.getSuppressed() != 0) {
            b.append(" (");
            b.append(l.getSuppressed());
            b.append(" not shown)");
        }
        return b.toString();
    }

    /**
     * Wheter to show lines.
     * @return true if the file model contains lines.
     */
    public boolean getShowLines() {
        return getFileModel().getLines().size() != 0;
    }

    private void addBlock(
        StringBuilder ret, StringBuilder b, int startLine, int previousLine) {
        if (b.length() == 0) {
            return;
        }
        ret.append("<tr><td colspan='3' class='source heading'>");
        ret.append("File: " + new File(fileModel.getDisplayName()).getName());
        ret.append(" Lines ");
        ret.append(startLine + " to " + previousLine);
        ret.append("</td></tr>\n");
        ret.append(b.toString());
        ret.append("<tr><td class='source empty' colspan='3'>&nbsp;");
        ret.append("</td></tr>\n");
    }

    /**
     * This gets called from the index.jelly script to
     * render the marked up contents of the file.
     * @return a table of lines and associated violations in html.
     */
    public String getFileContent() {
        StringBuilder ret = new StringBuilder();
        StringBuilder b = new StringBuilder();
        ret.append("<table class='source'>\n");
        int previousLine = -1;
        int startLine = 0;
        int currentLine = -1;
        for (Map.Entry<Integer, String> e
                 : fileModel.getLines().entrySet()) {
            currentLine = e.getKey();
            String line = e.getValue();
            // Check if at start of block
            if (currentLine != (previousLine + 1)) {
                // Start of block
                // Check if need to write previous block
                addBlock(ret, b, startLine, previousLine);
                b = new StringBuilder();
                startLine = currentLine;
            }
            previousLine = currentLine;

            Set<Violation> v = fileModel.getLineViolationMap().get(currentLine);

            // TR
            b.append("<tr " + (v != null ? "class='violation'" : "") + ">");

            // Icon
            if (v != null) {
                showIcon(b, v);
            } else {
                b.append("<td class='source icon'/>\n");
            }

            // Line number
            b.append("<td class='source line' id='line" + currentLine + "'>");
            if (v != null) {
                addVDiv(b);
            }
            b.append(currentLine);
            if (v != null) {
                showDiv(b, v);
                b.append("</div>");
            }
            b.append("</td>");
            b.append("<td class='source message'>");
            b.append(XMLUtil.escapeHTMLContent(
                         contextPath + "/plugin/violations/images/tab.png",
                         line));
            b.append("</td>\n");
            b.append("</tr>\n");
        }
        addBlock(ret, b, startLine, previousLine);
        ret.append("</table>");
        return ret.toString();
    }

    /**
     * Get the file model.
     * If the file model is present, return it, otherwise
     * parse the xml file.
     * @return the file model or null if unable to parse.
     */
    public FileModel getFileModel() {
        if (fileModel != null) {
            return fileModel;
        }
        if (!xmlFile.exists()) {
            LOG.log(Level.WARNING, "The file " + xmlFile + " does not exist");
            return null;
        }
        try {
            FileModel t = new FileModel();
            ParseXML.parse(xmlFile, new FileModelParser().fileModel(t));
            fileModel = t;
            return fileModel;
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Unable to parse " + xmlFile, ex);
            return null;
        }
    }

    // Following should be in generate!
    private void addVDiv(StringBuilder b) {
        b.append("<div class='healthReport'");
        b.append(
            "onmouseover=\"this.className='healthReport hover';return true;\"");
        b.append("onmouseout=\"this.className='healthReport';return true;\">");
    }

    private void showIcon(
        StringBuilder b, Set<Violation> violations) {
        b.append("<td class='source icon'>");
        addVDiv(b);
        b.append("<a class='healthReport'>");
        b.append(
            "<img src='"
            + contextPath
            + "/plugin/violations/images/12x12/dialog-warning.png'/>");
        b.append("</a>");
        showDiv(b, violations);
        b.append("</div>");
        b.append("</td>");
    }
    private void showDiv(
        StringBuilder b,  Set<Violation> violations) {
        b.append("<div class='healthReportDetails'>\n");
        b.append(" <table border='1' >\n");
        b.append("  <thead>\n");
        b.append("   <tr>\n");
        b.append("     <th align='left'> Type</th>\n");
        b.append("     <th align='left'> Description</th>\n");
        b.append("   </tr>\n");
        b.append("  </thead>\n");
        b.append("  <tbody>\n");
        for (Violation v: violations) {
            b.append("   <tr>\n");
            b.append("     <td align='left'>");
            b.append(v.getType());
            b.append("</td>\n");
            b.append("     <td  align='left'>");
            if (v.getPopupMessage() != null) {
                b.append(XMLUtil.escapeContent(v.getPopupMessage()));
            } else {
                b.append(XMLUtil.escapeContent(v.getMessage()));
            }
            b.append("</td>\n");
            b.append("   </tr>\n");
        }
        b.append("  </tbody>\n");
        b.append(" </table>\n");
        b.append("</div>\n");
    }

}
