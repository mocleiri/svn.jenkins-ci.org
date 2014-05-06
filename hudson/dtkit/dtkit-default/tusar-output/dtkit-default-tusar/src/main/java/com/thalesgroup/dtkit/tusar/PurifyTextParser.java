package com.thalesgroup.dtkit.tusar;

import com.thalesgroup.dtkit.util.converter.ConversionException;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PurifyTextParser {

    private final String transform_purify_output_ver = "0.1";

    public static void parse(String sourceFile, String targetFile) {
        String reg1 = "^\\[(W|E)\\]\\s([A-Z]+: [A-Za-z ]+) in (.*) \\{([0-9]+) occurrences?\\}.*";
        String reg2 = "^\\[I\\] Summary of all memory leaks\\.\\.\\. \\{([0-9]+) bytes?, ([0-9]+) blocks?\\}.*";
        String reg3 = "^\\[I\\] Exiting with code.*";
        String reg4 = "^\\[I\\] Program terminated at.*";


        boolean isPurify = false;
        FileInputStream in = null;
        FileWriter writer = null;
        BufferedReader br = null;
        String line = null;
        try {
            in = new FileInputStream(sourceFile);
            br = new BufferedReader(new InputStreamReader(in));

            writer = new FileWriter(targetFile);


            while ((line = br.readLine()) == null | line.length() == 0) {
                continue;
            }
            line.replaceAll("\\n", "").trim();
            String reg = "^\\[I\\] Starting Purify'd (.+) at ([0-9][0-9]\\/[0-9][0-9]\\/[0-9][0-9][0-9][0-9]\\s[0-9][0-9]:[0-9][0-9]:[0-9][0-9]).*";
            Pattern pattern = null;
            Matcher matcher = null;
            String executable_name = "";
            String date_run = "";
            String version_of_parser = "";
            String purify_version = "";
            String os_version = "";
            String instrumented_executable_name = "";
            String working_directory = "";
            String executable_arguments = "";
            String process_id = "";
            String thread_id = "";

            if (line.matches(reg)) {

                pattern = Pattern.compile(reg);
                matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    executable_name = matcher.group(1);
                    date_run = matcher.group(2);
                }
            } else {

                // throw exception this is not a purify file or the good version cannot parse the first line
                // sortir
            }
            line = br.readLine();

            line.replaceAll("\\n", "").trim();
            reg = "^\\s+(.*).*";
            if (line.matches(reg)) {
                pattern = Pattern.compile(reg);
                matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    purify_version = matcher.group(1);
                }
            }

            line = br.readLine();
            line.replaceAll("\\n", "").trim();
            reg = "^\\s+(.*).*";
            if (line.matches(reg)) {
                pattern = Pattern.compile(reg);
                matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    os_version = matcher.group(1);
                }
            }

            line = br.readLine();
            line.replaceAll("\\n", "").trim();
            reg = "^\\s+Instrumented executable:\\s+(.*).*";
            if (line.matches(reg)) {
                pattern = Pattern.compile(reg);
                matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    instrumented_executable_name = matcher.group(1);
                }
            }

            line = br.readLine();
            line.replaceAll("\\n", "").trim();
            reg = "^\\s+Working directory:\\s+(.*).*";
            if (line.matches(reg)) {
                pattern = Pattern.compile(reg);
                matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    working_directory = matcher.group(1);
                }
            }

            line = br.readLine();
            line.replaceAll("\\n", "").trim();
            reg = "^\\s+Command line arguments:\\s+(.*).*";
            if (line.matches(reg)) {
                pattern = Pattern.compile(reg);
                matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    executable_arguments = matcher.group(1);
                }
            }

            line = br.readLine();
            line.replaceAll("\\n", "").trim();
            reg = "^\\s+Process ID:\\s+(.*).*";
            if (line.matches(reg)) {
                pattern = Pattern.compile(reg);
                matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    process_id = matcher.group(1);
                }
            }
            line = br.readLine();
            line.replaceAll("\\n", "").trim();
            reg = "^\\s+Thread ID:\\s+(.*).*";
            if (line.matches(reg)) {
                pattern = Pattern.compile(reg);
                matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    thread_id = matcher.group(1);
                }
            }

				/* 
                 * date_run="05/03/2012 16:54:33" purify_version="Version 7.0.1.0-001 build 11785 "
				 * os_version="Windows 6.1 7601  Multiprocessor Free" 
				 * instrumented_executable_name="C:\ORCHESTRA\IBM\RationalPurifyPlus\PurifyPlus\cache\hello$Purify_C_koundoussi_jenkins_jobs_helloPurify_workspace_PurifyPlusProject_Purify.exe" 
				 * working_directory="C:\koundoussi\jenkins\jobs\helloPurify\workspace\PurifyPlusProject\Purify" 
				 * executable_arguments="&lt;none&gt;" process_id="0xfd8" thread_id="0x120">
				 */

            StringBuffer out = new StringBuffer();
            out.append("<purify version_of_parser=\"0.1\" executable_name=\"");
            isPurify = true;
            out.append(executable_name);
            out.append("\" date_run=\"");
            out.append(date_run);
            out.append("\" purify_version=\"");
            out.append(purify_version);
            out.append("\" os_version=\"");
            out.append(os_version);
            out.append("\" instrumented_executable_name=\"");
            out.append(instrumented_executable_name);
            out.append("\" working_directory=\"");
            out.append(working_directory);
            out.append("\" executable_arguments=\"");
            out.append(executable_arguments);
            out.append("\" >");
            writer.write(out.toString());

            String classification = "";
            String type = "";
            String found_in = "";
            String number_of_occurences = "";
            String parentReg = null;
            boolean parentOpened = false;
            boolean terminated = false;
            boolean isReg1 = false;
            boolean isReg2 = false;
            boolean isReg3 = false;
            boolean isReg4 = false;
            String location = "Error";
            while ((line = br.readLine()) != null) {
                line.replaceAll("\\n", "").trim();

                if ((isReg1 = (line.matches(reg1))) | (isReg2 = (line.matches(reg2))) | (isReg4 = (line.matches(reg4)))) {
                    line = line.trim();
                    if (parentReg == reg1 && parentOpened == true) {
                        writer.write("\n </stack_trace>\n</problem>");
                        parentReg = null;
                        parentOpened = false;
                    } else if (parentReg == reg2 && parentOpened == true) {
                        writer.write("\n</memory_leaks>");
                        parentReg = null;
                        parentOpened = false;
                    }
                    if (isReg1) {
                        parentReg = reg1;
                        pattern = Pattern.compile(reg1);
                        matcher = pattern.matcher(line);
                        if (matcher.matches()) {
                            classification = matcher.group(1);
                            type = matcher.group(2);
                            found_in = matcher.group(3);
                            number_of_occurences = matcher.group(4);
                            out = new StringBuffer();
                            out.append("\n<problem classification=\"");
                            out.append(classification);
                            out.append("\" type=\"");
                            out.append(type);
                            out.append("\" found_in=\"");
                            out.append(found_in);
                            out.append("\" number_of_occurrences=\"");
                            out.append(number_of_occurences);
                            out.append("\" >");
                            out.append("\n <stack_trace>");
                            writer.write(out.toString());
                            parentOpened = true;
                        }
                    } else if (isReg2) {

                        parentReg = reg2;
                        pattern = Pattern.compile(reg2);
                        matcher = pattern.matcher(line);
                        if (matcher.matches()) {
                            String total_bytes = matcher.group(1);
                            String total_blocks = matcher.group(2);
                            out = new StringBuffer();
                            out.append("\n<memory_leaks total_bytes=\"");
                            out.append(total_bytes);
                            out.append("\" total_blocks=\"");
                            out.append(total_blocks);
                            out.append("\" >");
                            writer.write(out.toString());
                            parentOpened = true;
                        }


                    } else if (isReg4) {
                        parentReg = reg4;
                        if (isPurify) {
                            writer.write("\n</purify>");
                            isPurify = false;
                        }
                    }


                } else {
                    if (parentReg == reg1) {

                        String name = "";
                        String file = "";
                        String line_number = "";

                        if (line.matches("^\\s+(Error|Allocation) location.*")) {
                            pattern = Pattern.compile("^\\s+(Error|Allocation) location.*");
                            matcher = pattern.matcher(line);
                            if (matcher.matches()) {
                                location = matcher.group(1);

                            }

                        } else if (line.matches("^\\s+(.*)\\s+\\[(.*):([0-9]+)\\]/.*")) {// this is a stacktrace
							/*<function name="strlen        
							 * " file="f:\dd\vctools\crt_bld\SELF_X86\crt\src\build\INTEL\mt_obj\printf.obj" 
							 * line_number="54" location="Error" />
							 */

                            pattern = Pattern.compile("^\\s+(.*)\\s+\\[(.*):([0-9]+)\\]/.*");
                            matcher = pattern.matcher(line);
                            if (matcher.matches()) {
                                name = matcher.group(1);
                                file = matcher.group(2);
                                line_number = matcher.group(3);

                                out = new StringBuffer();
                                out.append("\n   <function name=\"");
                                out.append(name);
                                out.append("\" file=\"");
                                out.append(file);
                                out.append("\" line_number=\"");
                                out.append(line_number);
                                out.append("\" location=\"");
                                out.append(location);
                                out.append("\" />");
                                writer.write(out.toString());
                            }
                        } else if (line.matches("^\\s+(.*)\\s+\\[(.*)\\].*")) {
							/* we watch a stacktrace but one that doesn't include a line number 
							 * (possibly the binary)
							 */

                            pattern = Pattern.compile("^\\s+(.*)\\s+\\[(.*)\\].*");
                            matcher = pattern.matcher(line);
                            if (matcher.matches()) {
                                name = matcher.group(1);
                                file = matcher.group(2);
                                out = new StringBuffer();
                                out.append("\n   <function name=\"");
                                out.append(name);
                                out.append("\" file=\"");
                                out.append(file);
                                out.append("\" location=\"");
                                out.append(location);
                                out.append("\" />");
                                writer.write(out.toString());
                            }

                        }


                    } else if (parentReg == reg2) {
                        line = line.trim();
                        String xreg = "^\\[W\\] MLK: Memory leak of ([0-9]+) bytes? from ([0-9]+) blocks? allocated in (.*) \\[(.*)\\].*";

                        if (line.matches(xreg)) {

                            pattern = Pattern.compile(xreg);
                            matcher = pattern.matcher(line);
                            if (matcher.matches()) {
                                String bytes_lost = matcher.group(1);
                                String from_number_of_blocks = matcher.group(2);
                                String allocated_by = matcher.group(3);
                                String executable = matcher.group(4);
                                out = new StringBuffer();
                                out.append("\n <memory_leak bytes_lost=\"");
                                out.append(bytes_lost);
                                out.append("\" from_number_of_blocks=\"");
                                out.append(from_number_of_blocks);
                                out.append("\" allocated_by=\"");
                                out.append(allocated_by);
                                out.append("\" executable=\"");
                                out.append(executable);
                                out.append("\" />");
                                writer.write(out.toString());
                            }
                        }
                    }
                }
            }//end of while

            if (isPurify) {
                writer.write("\n</purify>");

            }
            writer.flush();
            writer.close();
        } catch (Exception e) {
            throw new ConversionException(e.getMessage());
        }


    }

    public static void main(String[] args) {

        String input = "C:\\Users\\GENIATIS\\.jenkins\\jobs\\helloPurify\\workspace\\PurifyPlusProject\\Purify\\my_junk.txt";
        String output = "C:\\Users\\GENIATIS\\.jenkins\\jobs\\helloPurify\\workspace\\PurifyPlusProject\\Purify\\junk_output.xml";
        PurifyTextParser.parse(input, output);

    }
}
