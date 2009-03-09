package com.thalesgroup.hudson.plugins.gnat.aunit;

import hudson.FilePath;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class AUnitParsing{

    private static final String PATTERN_LINE_TOTAL="(\\s)+Total Tests Run: \\d";
    private static final String PATTERN_LINE_SUCCESSFUL_TESTS="(\\s)+Successful Tests: \\d";
    private static final String PATTERN_LINE_FAILED_ASSERTIONS="(\\s)+Failed Assertions: \\d";
    private static final String PATTERN_LINE_UNEPECTED_ERRORS="(\\s)+Unexpected Errors: \\d";

     
    private static final String AUNIT_CLASS_WORLD="class";
    
	private AUnitTestSuite suite = new AUnitTestSuite();

	
	class TestCaseDTO{
		public String classname;
		public String name;
	}
	
	private com.thalesgroup.hudson.plugins.gnat.aunit.AUnitTestCase buildTestCaseInfo(String line){
		
		
		String[] parts = line.split(":");			

		//First part
		String classStr = parts[0].trim();
		String className = classStr.substring(0, classStr.indexOf(AUNIT_CLASS_WORLD));
		className=className.trim();
		
		//Second part
		String methodname = parts[1].trim();
		
		TestCaseDTO vTestCaseDTO=new TestCaseDTO();
		vTestCaseDTO.classname=className;
		vTestCaseDTO.name=methodname;
		
		AUnitTestCase testCase = new AUnitTestCase();
		testCase.setClassname(vTestCaseDTO.classname);			
		testCase.setName(vTestCaseDTO.name);		
		
		return testCase;		
	}
	

	
	private void processSuccessLines(int nbSucess, List<String> lines){
		
		for (String line:lines){			
			suite.addTestCase(buildTestCaseInfo(line));
		}
	}
	
	private void processFailedLines(int nbFailed, List<String> lines){
		
		AUnitTestCase testCase = null;
		
		suite.setNbFailure(nbFailed);		
		for (Iterator<String> it=lines.iterator(); it.hasNext();){
			
			String line = it.next();					
			testCase = buildTestCaseInfo(line);
			testCase.setFailed(true);
			
			if (it.hasNext()){
				String nextLine = it.next();
				nextLine=nextLine.trim();
				testCase.setFailedMessage(nextLine);
				testCase.setFailedBody(nextLine);
			}

			suite.addTestCase(testCase);
				
		}
	}
	

	
	private void processErrorsLines(int nbErrors, List<String> lines){
		suite.setNbErrors(nbErrors);
	}
	
    
    private int nbOfMainLine(String line){		
		Pattern p =Pattern.compile("\\d");
		Matcher m = p.matcher(line);
		while (m.find()){
			return Integer.parseInt(m.group());	
		}		
		return 0;
	}
	
	
	public void process(FilePath junitOutputPath, String vAUnitExecLog, String executableProjTest) throws IOException{

		int nbSucess = 0;
		List<String> successLines = new ArrayList<String>();
		int nbFailed = 0;
		List<String> failedLines = new ArrayList<String>();
		int nbErrors = 0;
		List<String> errorsLines = new ArrayList<String>();		
		
        
        boolean stopSuccessLines = false;
        boolean stopFailedLines  = false;
		
		InputStreamReader source =  new InputStreamReader(new ByteArrayInputStream(vAUnitExecLog.getBytes()));		
		BufferedReader reader = new BufferedReader(source);
        String line = null;
        while ((line=reader.readLine()) != null) {
        	
        	if (line.startsWith("Time"))
        		continue;
            
        	if (line.startsWith("-"))
        		continue;
        	
        	if (line.trim().length() == 0)
        		continue;
        		
        	if (line.matches(PATTERN_LINE_TOTAL)){
            	suite.setNbTests(nbOfMainLine(line));	
            }

            else if (line.matches(PATTERN_LINE_SUCCESSFUL_TESTS)){
            	//make nothing
            	continue;
            }
            
            else if (line.matches(PATTERN_LINE_FAILED_ASSERTIONS)){
            	nbFailed=nbOfMainLine(line);
            	stopSuccessLines = true;
            }
            
            else if (line.matches(PATTERN_LINE_UNEPECTED_ERRORS)){
            	nbErrors=nbOfMainLine(line);	
            	stopFailedLines=true;
            }
            
            else if (!stopSuccessLines){
            	successLines.add(line);
            }
            
            else if (!stopFailedLines){
            	failedLines.add(line);
            }
            
            else {
            	//add unxpected errors line
            	errorsLines.add(line);
            }
            
        }
        reader.close();
        source.close();

        
        processSuccessLines(nbSucess, successLines);
        processFailedLines(nbFailed, failedLines);
        processErrorsLines(nbErrors,errorsLines);
                
        
		try{
			FilePath filePath = new FilePath(junitOutputPath,"TEST-"+ executableProjTest.hashCode()+".xml");
			FileOutputStream fw=new FileOutputStream(new File(filePath.toURI()));
			fw.write(suite.generateXML().getBytes());
			fw.close();
		}
		catch (Exception ioe){
			System.out.println(ioe.toString());
		}
	}
	



	
	
	
	

}
