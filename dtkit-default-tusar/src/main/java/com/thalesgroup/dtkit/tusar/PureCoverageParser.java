package com.thalesgroup.dtkit.tusar;

import au.com.bytecode.opencsv.CSVReader;
import com.thalesgroup.dtkit.util.converter.ConversionException;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;

public class PureCoverageParser {


    public static void parse(String sourceFile, String targetFile) {

        CSVReader reader = null;
        String[] nextLine;
        FileWriter writer = null;
        StringBuffer comments = new StringBuffer();
        HashMap<String, String> commentAtt = new HashMap<String, String>();
        ArrayList<String> coverageAtt = new ArrayList<String>();
        ArrayList<String> coverageData = new ArrayList<String>();

        try {
            reader = new CSVReader(new FileReader(sourceFile), '\t');
            writer = new FileWriter(targetFile);

            writer.append("<coverage>\n");

            //	System.out.println("starting");
            int coverageDataLine = 0;
            while ((nextLine = reader.readNext()) != null) {
                //System.out.print(".");
                if (nextLine != null && nextLine.length == 2 && nextLine[0].trim().equalsIgnoreCase("comment")) {
                    comments.append(PureCoverageParser.escapeText(nextLine[1]));
                    comments.append(" ");
                } else if (nextLine != null && nextLine.length == 3 && nextLine[0].trim().equalsIgnoreCase("comment")) {
                    if (comments != null) {
                        PureCoverageParser.flushComments(writer, comments.toString());
                        comments = null;
                    }

                    commentAtt.put(PureCoverageParser.formatAttribute(PureCoverageParser.escapeText(nextLine[1])), '\"' + PureCoverageParser.escapeText(nextLine[2]) + '\"');
                } else if (nextLine != null && nextLine[0].trim().equalsIgnoreCase("coveragedata")) {
                    if (commentAtt != null) {
                        PureCoverageParser.flushCommentAttributes(writer, commentAtt);
                        commentAtt = null;
                    }
                    if (coverageDataLine == 0) {
                        for (String col : nextLine) {
                            coverageAtt.add(formatAttribute(col));
                        }
                    } else {
                        StringBuffer data = new StringBuffer();
                        String att = null;
                        String val = null;
                        for (int i = 1; i < nextLine.length; i++) {
                            data.append(coverageAtt.get(i));
                            data.append("=");
                            data.append('\"' + PureCoverageParser.escapeText(nextLine[i]).trim() + '\"');
                            data.append(" ");
                        }
                        coverageData.add(data.toString().trim());
                        data = null;

                    }
                    coverageDataLine++;

                } else if (nextLine != null && nextLine[0].trim().equalsIgnoreCase("sourcelines")) {
                    if (coverageData != null) {
                        PureCoverageParser.flushCoverageData(writer, coverageData);
                        coverageData = null;
                        coverageDataLine = 0;
                    }
                    writer.append("\n");
                    writer.append("<source file=");
                    writer.append('\"' + PureCoverageParser.escapeText(nextLine[1]) + '\"');
                    writer.append(" object=");
                    writer.append('\"' + PureCoverageParser.escapeText(nextLine[2]) + '\"');
                    writer.append(" > \n");
                    int sourceLine = 0;
                    ArrayList<String> lineHeaders = new ArrayList<String>();
                    while (((nextLine = reader.readNext()) != null) && !(PureCoverageParser.escapeText(nextLine[0]).trim().equalsIgnoreCase("sourcelines"))) {
                        if (sourceLine == 0) {
                            for (String col : nextLine) {
                                lineHeaders.add(formatAttribute(col));
                            }
                        } else {
                            writer.append("<line ");
                            for (int i = 0; i < nextLine.length; i++) {

                                if (lineHeaders.get(i) != null && lineHeaders.get(i).trim().length() > 0) {
                                    writer.append(lineHeaders.get(i));
                                    writer.append("=");
                                    writer.append('\"' + PureCoverageParser.escapeText(nextLine[i]) + '\"');
                                    writer.append(" ");
                                }
                            }
                            writer.append("/>\n");
                        }


                        sourceLine++;
                    }
                    sourceLine = 0;
                    lineHeaders = null;
                    writer.append("</source>");
                }


            }

            writer.append("\n</coverage>");
            writer.flush();


        } catch (Exception e) {
            throw new ConversionException(e.getMessage());
        }

    }

    public static String escapeText(String s) {

        if (s.indexOf('&') != -1 || s.indexOf('<') != -1
                || s.indexOf('>') != -1 || s.indexOf('#') != -1) {
            StringBuffer result = new StringBuffer(s.length() + 4);
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '&') {
                    result.append("&amp;");
                } else if (c == '#') {
                    result.append("_");
                    System.out.println(result);
                } else if (c == '<') result.append("&lt;");
                else if (c == '>') result.append("&gt;");
                else result.append(c);
            }
            return result.toString().trim();
        } else {
            return s.trim();
        }
    }

    public static String formatAttribute(String t) {
        String s = PureCoverageParser.escapeText(t);
        if (s.indexOf('/') != -1 || s.indexOf('(') != -1
                || s.indexOf(')') != -1 || s.indexOf(' ') != -1) {
            StringBuffer result = new StringBuffer(s.length() + 4);
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '/') result.append("_");
                else if (c == '(') result.append("");
                else if (c == ')') result.append("");
                else if (c == ' ') result.append("_");
                else result.append(c);
            }
            return result.toString().trim();
        } else {
            return s.trim();
        }

    }


    private static void flushComments(FileWriter writer, String comments) {
        try {
            writer.append("\n");
            writer.append("<comment>");
            writer.append("\n");
            writer.append(comments);
            writer.append("\n");
            writer.append("</comment>");
        } catch (Exception e) {
            throw new ConversionException(e.getMessage());
        }
    }

    private static void flushCommentAttributes(FileWriter writer, HashMap<String, String> commentAtt) {
        try {
            writer.append("\n");
            writer.append("<config ");
            for (String att : commentAtt.keySet()) {
                writer.append(att);
                writer.append("=");
                writer.append(commentAtt.get(att));
                writer.append(" ");

            }
            writer.append("/>");
        } catch (Exception e) {
            throw new ConversionException(e.getMessage());
        }
    }

    private static void flushCoverageData(FileWriter writer, ArrayList<String> coverageData) {
        try {
            writer.append("\n");
            writer.append("<coverageData>");
            for (String data : coverageData) {
                writer.append("\n");
                writer.append("<data ");
                writer.append(data);
                writer.append(" />");
            }
            writer.append("\n </coverageData>");
        } catch (Exception e) {
            throw new ConversionException(" flushCoverageData execption :" + e.getMessage(), e);
        }
    }

}
