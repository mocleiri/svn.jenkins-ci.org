package org.jenkins.plugins.silktestsuite.workbench;

import hudson.FilePath;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import com.google.common.base.Strings;

final class STWTestResultConverter {
  private Map<String, Element> suites;

  public STWTestResultConverter() {
    suites = new HashMap<String, Element>();
  }
  
  public void convert(FilePath source, FilePath target) throws IOException, InterruptedException {
    BufferedReader reader = null;
    OutputStream out = null;
    try {
    reader = new BufferedReader(new InputStreamReader(source.read()));
    out = target.write();
    
    Document targetDocument = new Document();
    Element suite = addTestSuite(null, "SilkTestWorkbench");
    targetDocument.setRootElement(suite);
    String line = null;
    do {
      line = reader.readLine();
      if (line != null)
        convertLine(suite, line.split("\t"));
    } while(line  != null);
    
    XMLOutputter xmlOut = new XMLOutputter(Format.getPrettyFormat());
    xmlOut.output(targetDocument, out);
    } finally {
      if (reader != null) reader.close();
      if (out != null) out.close();
    }
  }


  private Element addTestSuite(Element parent, String name) {
    Element newElement = new Element("testsuite");
    newElement.setAttribute("name", name);
    newElement.setAttribute("tests", "0");
    newElement.setAttribute("failures", "0");
    newElement.setAttribute("errors", "0");
    newElement.setAttribute("skipped", "0");
    
    if (parent != null)
      parent.addContent(newElement);
    
    this.suites.put(name, newElement);
    
    return newElement;
  }

  
  private static void addTestCase(Element suite, String[] line) {
    Element element = new Element("testcase");
    element.setAttribute("name", line[0]);
    element.setAttribute("classname", createClassName(suite, line[0]));
    
    int scriptStatus = Strings.isNullOrEmpty(line[6]) ? 0 : Integer.parseInt(line[6]);
    
    String playbackError;
    if (line.length == 8)
      playbackError = line[7];
    else
      playbackError = "";
    
    switch (scriptStatus) {
    case 0:
      break;
    case 9:
      addErrorElement(element, "The script did not complete execution. It may have stopped before it reached the end but not a result of a playback error.", null);
      increaseCount(suite, "error");
      break;
    case 10:
      addErrorElement(element, "The script failed with playback errors.", playbackError);
      increaseCount(suite, "error");
      break;
    case 11:
      addErrorElement(element, "The script failed to execute or the script contained verifications that failed.", null);
      increaseCount(suite, "failures");
      break;
    default:
      assert(false);
      break;
    }
    
    suite.addContent(element);
    increaseCount(suite, "tests");
  }
  
  private static void addErrorElement(Element testcase, String message, String playbackError) {
    StringBuilder msg = new StringBuilder(message);
    if (Strings.isNullOrEmpty(playbackError)) {
      msg.append("\n\n");
      msg.append(playbackError);
    }
    
    Element error = new Element("error");
    error.setAttribute("message", msg.toString());
    error.setAttribute("type", "Error");
    
    testcase.addContent(error);
  }


  private void convertLine(Element suite, String[] line) {
    String projectName = line[1];
    Element projectSuite = this.suites.get(projectName);
    if (projectSuite == null)
      projectSuite = addTestSuite(suite, projectName);
    addTestCase(projectSuite, line);
  }

  private static void increaseCount(Element suite, String attribute) {
    if (suite != null) {
      int value = Integer.parseInt(suite.getAttributeValue(attribute));
      suite.setAttribute(attribute, String.valueOf(value+1));
      increaseCount(suite.getParentElement(), attribute);
    }
  }

  private static String createClassName(Element suite, String testCaseName) {
    StringBuilder sb = new StringBuilder(testCaseName);
    sb.insert(0, '.');
    
    while (suite != null) {
      sb.insert(0, suite.getAttributeValue("name"));
      suite = suite.getParentElement();
      if (suite != null)
        sb.insert(0, '.');
    }
    
    return sb.toString();
  }
}
