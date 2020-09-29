package es.bsc_imim.annotation.evaluator.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FilenameUtils;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.creole.ResourceInstantiationException;
import gate.util.GateException;

/**
 * 
 * Conversion from WebAnno XMI Uima format to Gate in order to provide the format 
 * for calculate agreement with GATE.
 * It takes all the webanno documents annotated by each expert and generates one document
 * with n numbers of sets, each set is for an annotator.
 * 
 * @author jcorvi
 *
 */
public class App2 {
	
	public static void main( String[] args ) {
    	
    	Options options = new Options();

        Option input = new Option("i", "input", true, "input directory path");
        input.setRequired(true);
        options.addOption(input);
        
        Option output = new Option("o", "output", true, "Output directory path");
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

        File outputDirectory = new File(outputPath);
	    if(!outputDirectory.exists())
	    	outputDirectory.mkdirs();
        
	    try {
			Gate.init();
		} catch (GateException e) {
			System.out.println("Wrapper::generatePlainText :: Gate Exception  ");
			System.out.println(e);
			System.exit(1);
		}
	    
	    generateFormatForGateAgreement(inputPath, outputPath);
		
	}

	/**
	 * Generate format for GATE agreement from xmi webanno export.
	 */
	private static void generateFormatForGateAgreement(String inputDirectoryPath, String outputPath) {
		System.out.println("App::generateFormatForGateAgreement :: INIT ");
		if (java.nio.file.Files.isDirectory(Paths.get(inputDirectoryPath))) {
			File inputDirectory = new File(inputDirectoryPath);
			File[] files =  inputDirectory.listFiles();
			for (File file : files) {
				System.out.println("App::generateFormatForGateAgreement :: document " + file);
				if(file.isDirectory()){
					String document_name = file.getName();
					try {
						File[] files_to_unzip =  file.listFiles();
						for (File file2 : files_to_unzip) {
							if(file2.getAbsolutePath().endsWith(".zip")) {
								
								unZipIt(file2.getAbsolutePath(),  file2.getAbsolutePath().replace(".zip", ""));
							}
						}
						File[] documents =  file.listFiles();
						for (File folder_annotator : documents) {
							if(folder_annotator.isDirectory()) {
								File[] annotations =  folder_annotator.listFiles();
								for (File anno : annotations) {
									if(anno.getAbsolutePath().endsWith(".xmi")) {
										System.out.println("App::generateFormatForGateAgreement :: processing document : " + anno.getAbsolutePath());
										String set_name = FilenameUtils.removeExtension(anno.getName());
										gate.Document gateDocument = Factory.newDocument(anno.toURI().toURL(), "UTF-8");
										gate.Document newDocument = gateDocument;
										File outputGATEFile = new File (outputPath +  File.separator + document_name.replace(".txt", ".xml"));
										if(outputGATEFile.exists()) {
											newDocument = Factory.newDocument(outputGATEFile.toURI().toURL(), "UTF-8");
										}
										AnnotationSet as = gateDocument.getAnnotations("CasView0");
										AnnotationSet findings = as.get("custom:FINDING");
										AnnotationSet cdog = as.get("custom:CDoG");
										
										newDocument.getAnnotations(set_name).addAll(findings);
										newDocument.getAnnotations(set_name).addAll(cdog);
										
										AnnotationSet new_findings = newDocument.getAnnotations(set_name).get("custom:FINDING");
										AnnotationSet new_cdog = newDocument.getAnnotations(set_name).get("custom:CDoG");
										
										FeatureMap document_features = gateDocument.getFeatures();
										
										//CDoG_Finging Relation in GATE
										for (Annotation ann : new_cdog) {
											if(ann.getFeatures().get("CDoG_Finding")!=null && !ann.getFeatures().get("CDoG_Finding").toString().trim().equals("")) {
												String[] relations_links =  ann.getFeatures().get("CDoG_Finding").toString().split(" ");
												for (String relation_id : relations_links) {
													String entity_source_id = document_features.get("custom:CDoGCDoG_FindingLink_"+relation_id+".target").toString();
													for (Annotation finding : new_findings) {
														if(finding.getFeatures().get("xmi:id")!=null && finding.getFeatures().get("xmi:id").equals(entity_source_id)) {
															newDocument.getAnnotations(set_name).getRelations().addRelation("CDoG_Finding", ann.getId(), finding.getId());
															System.out.println(ann.getId() + " - CDoG_Finding with - " + finding.getId() );	
															System.out.println(gate.Utils.stringFor(newDocument, ann) + " - CDoG_Finding with - " + gate.Utils.stringFor(newDocument, finding));	
														}
													}
												}
											}
										}

										//CDoG_disc_expression
										for (Annotation ann : new_cdog) {
											if(ann.getFeatures().get("disc_expression")!=null && !ann.getFeatures().get("disc_expression").toString().trim().equals("")) {
												String[] relations_links =  ann.getFeatures().get("disc_expression").toString().split(" ");
												for (String relation_id : relations_links) {
													String entity_source_id = document_features.get("custom:CDoGDisc_expressionLink_"+relation_id+".target").toString();
													for (Annotation finding : new_cdog) {
														if(finding.getFeatures().get("xmi:id")!=null && finding.getFeatures().get("xmi:id").equals(entity_source_id)) {
															newDocument.getAnnotations(set_name).getRelations().addRelation("CDoG_disc_expression", ann.getId(), finding.getId());
															System.out.println(ann.getId() + " - CDoG_disc_expression with - " + finding.getId() );	
															System.out.println(gate.Utils.stringFor(newDocument, ann) + " - CDoG_disc_expression with - " + gate.Utils.stringFor(newDocument, finding));
														}
													}
												}
											}
										}
										
										//Finding_disc_expression
										for (Annotation ann : new_findings) {
											if(ann.getFeatures().get("disc_expression")!=null && !ann.getFeatures().get("disc_expression").toString().trim().equals("")) {
												String[] relations_links =  ann.getFeatures().get("disc_expression").toString().split(" ");
												for (String relation_id : relations_links) {
													String entity_source_id = document_features.get("custom:FINDINGDisc_expressionLink_"+relation_id+".target").toString();
													for (Annotation finding : new_findings) {
														if(finding.getFeatures().get("xmi:id")!=null && finding.getFeatures().get("xmi:id").equals(entity_source_id)) {
															newDocument.getAnnotations(set_name).getRelations().addRelation("Finding_disc_expression", ann.getId(), finding.getId());
															System.out.println(ann.getId() + " - Finding_disc_expression with - " + finding.getId() );	
															System.out.println(gate.Utils.stringFor(newDocument, ann) + " - Finding_disc_expression with - " + gate.Utils.stringFor(newDocument, finding));
														}
													}
												}
											}
										}

										newDocument.removeAnnotationSet("CasView0");
										try {
											java.io.Writer out = new java.io.BufferedWriter(new java.io.OutputStreamWriter(new FileOutputStream(outputGATEFile, false)));
										    out.write(newDocument.toXml());
										    out.flush();
										    out.close();
										    gateDocument.cleanup();
										    gateDocument=null;
										    newDocument.cleanup();
										    newDocument=null;
										    out=null;
										} catch (MalformedURLException e) {
											System.out.println("App::process :: error with document " + file.getAbsolutePath());
											e.printStackTrace();
										}  catch (FileNotFoundException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										} catch (IOException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										} 
									}
								}
							}
						}
                    } catch (ResourceInstantiationException e) {
						System.out.println("App::processEvaluation :: error with document " + file.getAbsolutePath());
						System.out.println(e);
					} catch (MalformedURLException e) {
						System.out.println("App::processEvaluation :: error with document " + file.getAbsolutePath());
						System.out.println(e);
					}
				}
			}
			
			
		}else {
			System.out.println("App::processEvaluation :: No directory :  " + inputDirectoryPath);
		}
		System.out.println("App::processEvaluation :: END ");
		
	}
   
	/**
     * Basic unzipping folder method
     * @param input
     * @param output
     * @throws IOException
     */
    private static void unZipIt(String zipFile, String outputFolder){
    	byte[] buffer = new byte[1024];
        try{
	       	//create output directory is not exists
	       	File folder = new File(outputFolder);
	       	if(!folder.exists()){
	       		folder.mkdir();
	       	}
	       	//get the zip file content
	       	ZipInputStream zis =
	       		new ZipInputStream(new FileInputStream(zipFile));
	       	//get the zipped file list entry
	       	ZipEntry ze = zis.getNextEntry();
	       	if(ze==null) {
	       		System.out.println("Error unziping file, please review if you zip file provided is not corrupt file remember that it must contain a list.def file inside.");
               	System.exit(1);
	       	}
	       	while(ze!=null){
	       	   String fileName = ze.getName();
	           File newFile = new File(outputFolder + File.separator + fileName);
	           System.out.println("file unzip : "+ newFile.getAbsoluteFile());
	           //create all non exists folders
	           //else you will hit FileNotFoundException for compressed folder
	           new File(newFile.getParent()).mkdirs();
	           FileOutputStream fos = new FileOutputStream(newFile);
	           int len;
	           while ((len = zis.read(buffer)) > 0) {
	        	   fos.write(buffer, 0, len);
	           }
	           fos.close();
	           ze = zis.getNextEntry();
	       	}
	       	zis.closeEntry();
	       	zis.close();
	       	System.out.println("Done");
       }catch(IOException ex){
          ex.printStackTrace();
       }
    }
	
}
