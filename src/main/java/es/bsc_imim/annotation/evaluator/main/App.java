package es.bsc_imim.annotation.evaluator.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
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
        
        Option output = new Option("o", "output", true, "Output folder");
        output.setRequired(true);
        options.addOption(output);
        
        Option gold = new Option("s1", "goldStandardAnnotationSet", true, "Annotation Set Gold Standard, also serves as an annotation set that contains annotacion of an expert to calculate concensus agreement.");
        gold.setRequired(true);
        options.addOption(gold);
        Option eva = new Option("s2", "evaluationAnnotationSet", true, "Annotation Set Evaluation, also serves as an annotation set that contains annotacion of an expert to calculate concensus agreement.");
        eva.setRequired(true);
        options.addOption(eva);
        
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
        String goldStandardAnnotationSet = cmd.getOptionValue("goldStandardAnnotationSet");
        String evaluationAnnotationSet = cmd.getOptionValue("evaluationAnnotationSet");
        
        if (goldStandardAnnotationSet==null) {
        	System.out.println("Please set the gold annotationset ");
			System.exit(1);
    	}
        
        if (evaluationAnnotationSet==null) {
        	System.out.println("Please set the evaluation annotationset ");
			System.exit(1);
    	}
        
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
	    
	    processEvaluation(inputPath, outputPath, goldStandardAnnotationSet, evaluationAnnotationSet );
		
	}

	/**
	 * Process evaluation of Documents
	 */
	private static void processEvaluation(String inputDirectoryPath, String outputPath, String goldStandardAnnotationSet, String evaluationAnnotationSet) {
		AnnotationEvaluator service = new AnnotationEvaluator(goldStandardAnnotationSet, evaluationAnnotationSet, AnnotationEvaluator.measuresClassification[0].toString(), 0);
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
			
			String results = service.getFscoreMeasures(true);
			//String results2 = service.getClassificationMeasures(true);
			try {
				createTxtFile(outputPath+File.separator+goldStandardAnnotationSet+"_"+evaluationAnnotationSet+".txt", results);
				//createTxtFile(outputPath+".classitifation.txt", results2);
			} catch (FileNotFoundException e) {
				System.out.println("App::processEvaluation :: FileNotFoundException " + outputPath);
				System.out.println(e);
			} catch (IOException e) {
				System.out.println("App::processEvaluation :: IOException " + outputPath);
				System.out.println(e);
			}
			
			String results3 = service.getFscoreMeasuresCSV(true);
			String results4 = service.getClassificationMeasures(false);
			System.out.println(results3);
			System.out.println(results4);
			try {
				createTxtFile(outputPath+File.separator+goldStandardAnnotationSet+"_"+evaluationAnnotationSet+".csv", results3);
			} catch (FileNotFoundException e) {
				System.out.println("App::processEvaluation :: FileNotFoundException " + outputPath);
				System.out.println(e);
			} catch (IOException e) {
				System.out.println("App::processEvaluation :: IOException " + outputPath);
				System.out.println(e);
			}
		}else {
			System.out.println("App::processEvaluation :: No directory :  " + inputDirectoryPath);
		}
		System.out.println("App::processEvaluation :: END ");
		
	}
	
	/**
	 * Create a plain text file with the given string
	 * @param path
	 * @param plainText
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private static void createTxtFile(String path, String plainText) throws FileNotFoundException, IOException {
		File fout = new File(path);
		FileOutputStream fos = new FileOutputStream(fout);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos,StandardCharsets.UTF_8));
		bw.write(plainText);
		bw.flush();
		bw.close();
	}
	
   
}
