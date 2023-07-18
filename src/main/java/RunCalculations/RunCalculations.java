package RunCalculations;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.openscience.cdk.AtomContainerSet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import ToxPredictor.Application.TESTConstants;
import ToxPredictor.Application.Calculations.RunFromSmiles;
import ToxPredictor.Application.Calculations.RunFromSmiles.PredictionDashboard;
import ToxPredictor.Application.model.PredictionResults;
import ToxPredictor.Database.DSSToxRecord;

/**
* @author TMARTI02
*/
public class RunCalculations {
	
	
	public void runSDF() {
		
		String folderSrc="sdf/";

		int num=7;
		
		String fileNameSDF="snapshot_compounds"+num+".sdf";
		String filepathSDF=folderSrc+fileNameSDF;
		
		System.out.println(fileNameSDF);

		int maxCount=-1;//set to -1 to run all in sdf
		boolean skipMissingSID=true;
		String destJsonPath="reports/TEST_results_all_endpoints_"+fileNameSDF.replace(".sdf", ".json");
		RunFromSmiles.runSDF_all_endpoints_write_continuously(filepathSDF,destJsonPath,skipMissingSID,maxCount);
	}
	
	/**
	 * Version that doesnt use files to do I/O- master process handles the file writing
	 * 
	 */
	public void runSDF_using_objects(int num, int maxCount, int batchSize) {
		//int num=1;//which SDF file number to use		
		//int maxCount=25;//set to -1 to run all in sdf
		//int batchSize=10;//number of chemicals to pass to a processor in a single call (set to 50 or 500)
		boolean skipMissingSID=true;//if true skips chemicals that dont have a DTXSID
		String [] endpoints= RunFromSmiles.allEndpoints;
//		String [] endpoints= RunFromSmiles.twoEndpoints;
		String method = TESTConstants.ChoiceConsensus;// what QSAR method being used (default- runs all methods and
		boolean createReports = true;// whether to store report
		boolean createDetailedReports = false;// detailed reports have lots more info and creates more html files
		
		String folderSrc="sdf/";
		String fileNameSDF="snapshot_compounds"+num+".sdf";
		String filePathSDF=folderSrc+fileNameSDF;
		
		AtomContainerSet acs=RunFromSmiles.readSDFV3000(filePathSDF);
		acs = RunFromSmiles.filterAtomContainerSet(acs, skipMissingSID,maxCount);

		System.out.println(fileNameSDF);
		System.out.println("atom container count="+acs.getAtomContainerCount());
		
		File resultsFolder=new File("reports");
		resultsFolder.mkdirs();
		String destJsonPath="reports/Objects_TEST_results_all_endpoints_"+fileNameSDF.replace(".sdf", ".json");
		
		
		Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().disableHtmlEscaping().create();

		
		try {

			FileWriter fw=new FileWriter(destJsonPath);
			
			while(true) {

				//Extract batchSize number of chemicals: 
				AtomContainerSet chemicals=new AtomContainerSet();
				for (int i=1;i<=batchSize;i++) {
					chemicals.addAtomContainer(acs.getAtomContainer(0));
					acs.removeAtomContainer(0);
					if(acs.getAtomContainerCount()==0) break;
				}

				//Run chemicals on node:
				List<PredictionResults>resultsArray=RunFromSmiles.runEndpointsAsList(chemicals, endpoints, method,createReports,createDetailedReports,DSSToxRecord.strSID);
				
				//Write results to file
				for (PredictionResults pr:resultsArray) {
					fw.write(gson.toJson(pr)+"\r\n");
					fw.flush();
				}

				if(acs.getAtomContainerCount()==0) break;
			}

			fw.close();
			

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return;
	}
	
	public void runSDF_concurrently(int num, int maxCount, int batchSize) {
		//int num=1;//which SDF file number to use		
		//int maxCount=25;//set to -1 to run all in sdf
		//int batchSize=10;//number of chemicals to pass to a processor in a single call (set to 50 or 500)
		boolean skipMissingSID=true;//if true skips chemicals that dont have a DTXSID
		String [] endpoints= RunFromSmiles.allEndpoints;
//		String [] endpoints= RunFromSmiles.twoEndpoints;
		String method = TESTConstants.ChoiceConsensus;// what QSAR method being used (default- runs all methods and
		boolean createReports = true;// whether to store report
		boolean createDetailedReports = false;// detailed reports have lots more info and creates more html files
		
		String folderSrc="sdf/";
		String fileNameSDF="snapshot_compounds"+num+".sdf";
		String filePathSDF=folderSrc+fileNameSDF;
		
		AtomContainerSet acs=RunFromSmiles.readSDFV3000(filePathSDF);
		acs = RunFromSmiles.filterAtomContainerSet(acs, skipMissingSID,maxCount);

		System.out.println(fileNameSDF);
		System.out.println("atom container count="+acs.getAtomContainerCount());
		
		File resultsFolder=new File("reports");
		resultsFolder.mkdirs();
		String destJsonPath="reports/Objects_TEST_results_all_endpoints_"+fileNameSDF.replace(".sdf", ".json");
		
		Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().disableHtmlEscaping().create();
		
		try {

			FileWriter fw=new FileWriter(destJsonPath);
			
			EndPointsResults endpointsResults = new EndPointsResults(endpoints, method, createReports, createDetailedReports, DSSToxRecord.strSID);
			
			List<PredictionResults>resultsArray = new ArrayList<PredictionResults>();
			
			while(true) {

				//Extract batchSize number of chemicals: 
				AtomContainerSet batchSet=new AtomContainerSet();
				for (int i=1;i<=batchSize;i++) {
					batchSet.addAtomContainer(acs.getAtomContainer(0));
					acs.removeAtomContainer(0);
					if(acs.getAtomContainerCount()==0) break;
				}

				//Run chemicals on starting node:
				//List<PredictionResults>resultsArrayPartial=RunFromSmiles.runEndpointsAsList(batchSet, endpoints, method,createReports,createDetailedReports,DSSToxRecord.strSID);
				List<PredictionResults>resultsArrayPartial=endpointsResults.calculateResults(batchSet);
				
				resultsArray.addAll(resultsArrayPartial);

				if(acs.getAtomContainerCount()==0) break;
			}
			
			//Write results to file
			for (PredictionResults pr:resultsArray) {
				fw.write(gson.toJson(pr)+"\r\n");
				fw.flush();
			}

			fw.close();
			

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return;
	}
		
	public void runSDF_using_objects() {
		int num=1;//which SDF file number to use		
		int maxCount=25;//set to -1 to run all in sdf
		int batchSize=10;//number of chemicals to pass to a processor in a single call (set to 50 or 500)
		boolean skipMissingSID=true;//if true skips chemicals that dont have a DTXSID
		String [] endpoints= RunFromSmiles.allEndpoints;
//		String [] endpoints= RunFromSmiles.twoEndpoints;
		String method = TESTConstants.ChoiceConsensus;// what QSAR method being used (default- runs all methods and
		boolean createReports = true;// whether to store report
		boolean createDetailedReports = false;// detailed reports have lots more info and creates more html files
		
		String folderSrc="sdf/";
		String fileNameSDF="snapshot_compounds"+num+".sdf";
		String filePathSDF=folderSrc+fileNameSDF;
		
		AtomContainerSet acs=RunFromSmiles.readSDFV3000(filePathSDF);
		acs = RunFromSmiles.filterAtomContainerSet(acs, skipMissingSID,maxCount);

		System.out.println(fileNameSDF);
		System.out.println("atom container count="+acs.getAtomContainerCount());
		
		File resultsFolder=new File("reports");
		resultsFolder.mkdirs();
		String destJsonPath="reports/Objects_TEST_results_all_endpoints_"+fileNameSDF.replace(".sdf", ".json");
		
		
		Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().disableHtmlEscaping().create();

		
		try {

			FileWriter fw=new FileWriter(destJsonPath);
			
			while(true) {

				//Extract batchSize number of chemicals: 
				AtomContainerSet chemicals=new AtomContainerSet();
				for (int i=1;i<=batchSize;i++) {
					chemicals.addAtomContainer(acs.getAtomContainer(0));
					acs.removeAtomContainer(0);
					if(acs.getAtomContainerCount()==0) break;
				}

				//Run chemicals on node:
				List<PredictionResults>resultsArray=RunFromSmiles.runEndpointsAsList(chemicals, endpoints, method,createReports,createDetailedReports,DSSToxRecord.strSID);
				
				//Write results to file
				for (PredictionResults pr:resultsArray) {
					fw.write(gson.toJson(pr)+"\r\n");
					fw.flush();
				}

				if(acs.getAtomContainerCount()==0) break;
			}

			fw.close();
			

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return;
	}
	
	/**
	 * Runs each file using a separate processor. Ideally each file would be run on a separate machine
	 */
	void runSDFsMultiThreaded() {
		
		String folderSrc="C:\\Users\\TMARTI02\\OneDrive - Environmental Protection Agency (EPA)\\0 java\\hibernate_qsar_model_building\\data\\dsstox\\sdf\\";
		String destFolder="reports\\";
		
		
		File Folder=new File(folderSrc);
		
		boolean skipMissingSID=true;
//		int maxCount=10;//set to -1 to run all chemicals
		int maxCount=-1;//set to -1 to run all chemicals
		
		//Run using multiple threads
		ExecutorService es = Executors.newFixedThreadPool(5);
		
		for(int i=1;i<=35;i++) {
//		
			File file=new File(folderSrc+"snapshot_compounds"+i+".sdf");
			
			System.out.println(file.getName());
						
		    es.submit(() -> {
		    	String filepathSDF=file.getAbsolutePath();
		    	String destJsonPath=destFolder+"TEST_results_all_endpoints_"+file.getName().replace(".sdf", ".json");
		    	RunFromSmiles.runSDF_all_endpoints_write_continuously(filepathSDF,destJsonPath,skipMissingSID,maxCount);
		    });
		}
		
		es.shutdown();
		
	}
	
	
	
	void lookAtResultsInJson(String filepathJson,boolean convertLogMolarUnits) {
		
		try {
			

			System.out.println("#\t"+PredictionDashboard.getHeaderString("\t", PredictionDashboard.fields));

			Gson gson=new Gson();
			
			BufferedReader br=new BufferedReader(new FileReader(filepathJson));
			
			int count=0;

//			for (String line:lines) {
			while (true) {
				String line=br.readLine();
				if(line==null) break;
				count++;
				PredictionResults pr=gson.fromJson(line,PredictionResults.class);
				
//				if(pr.getDTXSID().equals("DTXSID90857464")) {
//					System.out.println(gson.toJson(pr));
//				}
								
				PredictionDashboard pd=RunFromSmiles.convertResultsToPredictionFormat(pr,convertLogMolarUnits);
				System.out.println(count+"\t"+pd.toString("\t", PredictionDashboard.fields));
			}
			
			br.close();
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	
	}
	
	

	void lookAtResultsInJsonFiles(String folderPath,boolean convertLogMolarUnits,boolean printPredictionDashboardRecords) {
		
		try {
			
			File folder=new File(folderPath);
			
			int counter=0;

			if (printPredictionDashboardRecords)
				System.out.println("#\t"+PredictionDashboard.getHeaderString("\t", PredictionDashboard.fields));

			Gson gson=new Gson();
			
			for (File file:folder.listFiles()) {
			
				if (!file.getName().contains(".json")) continue;
				
//				Path path = Paths.get(file.getAbsolutePath());
//				List<String> lines = Files.readAllLines(path,StandardCharsets.ISO_8859_1);//runs out of memory for 50K reports!

				BufferedReader br=new BufferedReader(new FileReader(file.getAbsolutePath()));
				
				int count=0;

//				for (String line:lines) {
				while (true) {
					
					String line=br.readLine();
					
					if(line==null) break;
					
					count++;
					counter++;
					
					
					if(printPredictionDashboardRecords) {
						PredictionResults pr=gson.fromJson(line,PredictionResults.class);
						PredictionDashboard pd=RunFromSmiles.convertResultsToPredictionFormat(pr,convertLogMolarUnits);
						System.out.println(counter+"\t"+pd.toString("\t", PredictionDashboard.fields));
					}
				}
				
				br.close();
				
				count/=18;
				
				if(!printPredictionDashboardRecords)
					System.out.println(file.getName()+"\t"+count);

			}
			
			System.out.println("Chemicals ran = "+counter/18);
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	
	}

	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		RunCalculations r=new RunCalculations();
		
		long startTime = System.currentTimeMillis();

//		r.runSDF();
//		r.runSDFsMultiThreaded();
//		r.runSDF_using_objects();
		//r.runSDF_using_objects(1, 50, 16);
		r.runSDF_concurrently(1, 100, 50);
		//r.runSDF_concurrently(2, 100, 50);
		
//		String fileNameJson="TEST_results_all_endpoints_snapshot_compounds4.json";
//		String fileNameJson="sample.json";
//		String destJsonPath="reports/"+fileNameJson;
//		r.lookAtResultsInJson(destJsonPath, false);
		
//		r.lookAtResultsInJsonFiles("reports", false, false);
		
		long endTime = System.currentTimeMillis();
		long runTime = (endTime-startTime)/1000;
		System.out.println("This took : "+runTime+" secs");
		
	}

}
