package hudson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mutable representation of string with HTML mark up.
 *
 * <p>
 * This class is used to put mark up on plain text.
 * See <a href="https://hudson.dev.java.net/source/browse/hudson/hudson/main/core/src/test/java/hudson/MarkupTextTest.java?view=markup">
 * the test code</a> for a typical usage and its result.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.70
 */
public class MarkupText extends AbstractMarkupText {
    private final String text;

    /**
     * Added mark up tags.
     */
    private final List<Tag> tags = new ArrayList<Tag>();

    /**
     * Represents one mark up inserted into text.
     */
    private static final class Tag implements Comparable<Tag> {
        private final int pos;
        private final String markup;

        public Tag(int pos, String markup) {
            this.pos = pos;
            this.markup = markup;
        }

        public int compareTo(Tag that) {
            return this.pos-that.pos;
        }
    }

    /**
     * Represents a substring of a {@link MarkupText}.
     */
    public final class SubText extends AbstractMarkupText {
        private final int start,end;
        private final int[] groups;

        public SubText(Matcher m, int textOffset) {
            start = m.start() + textOffset;
            end   = m.end() + textOffset;

            int cnt = m.groupCount();
            groups = new int[cnt*2];
            for( int i=0; i<cnt; i++ ) {
                groups[i*2  ] = m.start(i+1) + textOffset;
                groups[i*2+1] = m.end(i+1) + textOffset;
            }
        }

        @Override
        public String getText() {
            return text.substring(start,end);
        }

        @Override
        public void addMarkup(int startPos, int endPos, String startTag, String endTag) {
            MarkupText.this.addMarkup(startPos+start,  endPos+start, startTag, endTag);
        }

        /**
         * Surrounds this subtext with the specified start tag and the end tag.
         *
         * <p>
         * Start/end tag text can contain special tokens "$0", "$1", ...
         * and they will be replaced by their {@link #group(int) group match}.
         * "\$" can be used to escape characters.
         */
        public void surroundWith(String startTag, String endTag) {
            addMarkup(0,length(),replace(startTag),replace(endTag));
        }

        /**
         * Works like {@link #surroundWith(String, String)} except
         * that the token replacement is not performed on parameters.
         */
        public void surroundWithLiteral(String startTag, String endTag) {
            addMarkup(0,length(),startTag,endTag);
        }

        /**
         * Gets the start index of the captured group within {@link MarkupText#getText()}.
         *
         * @param groupIndex
         *      0 means the start of the whole subtext. 1, 2, ... are
         *      groups captured by '(...)' in the regexp.
         */
        public int start(int groupIndex) {
            if(groupIndex==0)    return start;
            return groups[groupIndex*2-2];
        }

        /**
         * Gets the start index of this subtext within {@link MarkupText#getText()}.
         */
        public int start() {
            return start;
        }

        /**
         * Gets the end index of the captured group within {@link MarkupText#getText()}.
         */
        public int end(int groupIndex) {
            if(groupIndex==0)    return end;
            return groups[groupIndex*2-1];
        }

        /**
         * Gets the end index of this subtext within {@link MarkupText#getText()}.
         */
        public int end() {
            return end;
        }

        /**
         * Gets the text that represents the captured group.
         */
        public String group(int groupIndex) {
            if(start(groupIndex)==-1)
                return null;
            return text.substring(start(groupIndex),end(groupIndex));
        }

        /**
         * Replaces the group tokens like "$0", "$1", and etc with their actual matches.
         */
        public String replace(String s) {
            StringBuffer buf = new StringBuffer();

            for( int i=0; i<s.length(); i++) {
                char ch = s.charAt(i);
                if (ch == '\\') {// escape char
                    i++;
                    buf.append(s.charAt(i));
                } else if (ch == '$') {// replace by group
                    i++;

                    // get the group number
                    int groupId = s.charAt(i) - '0';

                    // add the group text
                    String group = group(groupId);
                    if (group != null)
                        buf.append(group);
                } else {
                    // other chars
                    buf.append(ch);
                }
            }

            return buf.toString();
        }

        @Override
        protected SubText createSubText(Matcher m) {
            return new SubText(m,start);
        }
    }

    public MarkupText(String text) {
        this.text = text;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public void addMarkup( int startPos, int endPos, String startTag, String endTag ) {
        rangeCheck(startPos);
        rangeCheck(endPos);
        if(startPos>endPos) throw new IndexOutOfBoundsException();

        // when multiple tags are added to the same range, we want them to show up like
        // <b><i>abc</i></b>, not <b><i>abc</b></i>. Do this by inserting them to different
        // places.
        tags.add(0,new Tag(startPos, startTag));
        tags.add(new Tag(endPos,endTag));
    }

    private void rangeCheck(int pos) {
        if(pos<0 || pos>text.length())
            throw new IndexOutOfBoundsException();
    }

    /**
     * Returns the fully marked-up text.
     */
    public String toString() {
        if(tags.isEmpty())
            return text;    // the most common case

        // somewhat inefficient implementation, if there are a lot of mark up and text is large.
        Collections.sort(tags);
        StringBuilder buf = new StringBuilder();
        buf.append(text);
        int offset = 0;     // remember the # of chars inserted.
        for (Tag tag : tags) {
            buf.insert(tag.pos+offset,tag.markup);
            offset += tag.markup.length();
        }

        return buf.toString();
    }

    // perhaps this method doesn't need to be here to remain binary compatible with past versions,
    // but having this seems to be safer.
    @Override
    public List<SubText> findTokens(Pattern pattern) {
        return super.findTokens(pattern);
    }

    @Override
    protected SubText createSubText(Matcher m) {
        return new SubText(m,0);
    }
}
