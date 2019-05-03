package es.bsc_imim.annotation.evaluator.main;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Paths;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import gate.Factory;
import gate.Gate;
import gate.creole.ResourceInstantiationException;
import gate.util.GateException;

/**
 * 
 * Application for annotation validation.
 * Gate format.
 * 
 * @author jcorvi
 *
 */
public class App {
	
	public static void main( String[] args ) {
    	
    	Options options = new Options();

        Option input = new Option("i", "input", true, "input directory path");
        input.setRequired(true);
        options.addOption(input);
        
        Option output = new Option("o", "output", true, "Output file with the results");
        output.setRequired(true);
        options.addOption(output);
        
        Option workdir = new Option("workdir", "workdir", true, "workDir directory path");
        workdir.setRequired(false);
        options.addOption(workdir);
        
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;
    	try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
        }
    	
       
        String inputPath = cmd.getOptionValue("input");
        String outputPath = cmd.getOptionValue("output");
        
        if (!java.nio.file.Files.isDirectory(Paths.get(inputPath))) {
        	System.out.println("Please set the inputDirectoryPath ");
			System.exit(1);
    	}

	    try {
			Gate.init();
		} catch (GateException e) {
			System.out.println("Wrapper::generatePlainText :: Gate Exception  ");
			System.out.println(e);
			System.exit(1);
		}
	    
	    processEvaluation(inputPath, outputPath);
		
	}

	/**
	 * Process evaluation of Documents
	 */
	private static void processEvaluation(String inputDirectoryPath, String outputPath) {
		AnnotationEvaluator service = new AnnotationEvaluator("EVALUATION", "BSC", AnnotationEvaluator.measuresClassification[0].toString(), 0);
		System.out.println("App::processEvaluation :: INIT ");
		if (java.nio.file.Files.isDirectory(Paths.get(inputDirectoryPath))) {
			File inputDirectory = new File(inputDirectoryPath);
			File[] files =  inputDirectory.listFiles();
			for (File file : files) {
				if(file.getName().endsWith(".xml")){
					try {
						System.out.println("Wrapper::processTagger :: processing file : " + file.getAbsolutePath());
						gate.Document gateDocument = Factory.newDocument(file.toURI().toURL(), "UTF-8");
						service.processDocument(gateDocument);
					} catch (ResourceInstantiationException e) {
						System.out.println("App::processEvaluation :: error with document " + file.getAbsolutePath());
						System.out.println(e);
					} catch (MalformedURLException e) {
						System.out.println("App::processEvaluation :: error with document " + file.getAbsolutePath());
						System.out.println(e);
					}
				}
			}
			
			String results = service.getFscoreMeasures(false);
			String results2 = service.getClassificationMeasures(false);
			System.out.println(results);
			System.out.println(results2);
		}else {
			System.out.println("App::processEvaluation :: No directory :  " + inputDirectoryPath);
		}
		System.out.println("App::processEvaluation :: END ");
		
	}
   
}
