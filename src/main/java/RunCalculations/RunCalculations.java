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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import ToxPredictor.Application.Calculations.RunFromSmiles;
import ToxPredictor.Application.Calculations.RunFromSmiles.PredictionDashboard;
import ToxPredictor.Application.model.PredictionResults;

/**
* @author TMARTI02
*/
public class RunCalculations {
	
	
	void runSDF() {
		
		String folderSrc="C:\\Users\\TMARTI02\\OneDrive - Environmental Protection Agency (EPA)\\0 java\\hibernate_qsar_model_building\\data\\dsstox\\sdf\\";

		int num=7;
		
		String fileNameSDF="snapshot_compounds"+num+".sdf";
		String filepathSDF=folderSrc+fileNameSDF;
		
		System.out.println(fileNameSDF);

		int maxCount=-1;//set to -1 to run all in sdf
		boolean skipMissingSID=true;
		String destJsonPath="reports/TEST_results_all_endpoints_"+fileNameSDF.replace(".sdf", ".json");
		RunFromSmiles.runSDF_all_endpoints_write_continuously(filepathSDF,destJsonPath,skipMissingSID,maxCount);
	}
	
	
//	/**
//	 * Runs all files in a loop- but really each should be run on a separate machine to parallelize
//	 */
//	void runSDFs() {
//		
//		String folderSrc="C:\\Users\\TMARTI02\\OneDrive - Environmental Protection Agency (EPA)\\0 java\\hibernate_qsar_model_building\\data\\dsstox\\sdf\\";
//		String destFolder="reports\\";
//		
//		
//		File Folder=new File(folderSrc);
//		
//		boolean skipMissingSID=true;
//		int maxCount=10;//set to -1 to run all chemicals
//		
//		for (File file:Folder.listFiles()) {
//			if(!file.getName().contains(".sdf"))continue;
//			String filepathSDF=file.getAbsolutePath();
//			String destJsonPath=destFolder+"TEST_results_all_endpoints_"+file.getName().replace(".sdf", ".json");
//			RunFromSmiles.runSDF_all_endpoints_write_continuously(filepathSDF,destJsonPath,skipMissingSID,maxCount);
////			lookAtResultsInJson(destJsonPath, false);//converts to tsv for main results to be able to view it better
//		}
//		
//	}
	
	
//	/**
//	 * Runs all files in a loop- but really each should be run on a separate machine to parallelize
//	 */
//	void runSDFsMultiThreaded() {
//		
//		String folderSrc="C:\\Users\\TMARTI02\\OneDrive - Environmental Protection Agency (EPA)\\0 java\\hibernate_qsar_model_building\\data\\dsstox\\sdf\\";
//		String destFolder="reports\\";
//		
//		
//		File Folder=new File(folderSrc);
//		
//		boolean skipMissingSID=true;
////		int maxCount=10;//set to -1 to run all chemicals
//		int maxCount=-1;//set to -1 to run all chemicals
//		
//		//Run using multiple threads
//		ExecutorService es = Executors.newFixedThreadPool(10);
//		
//		for(File file: Folder.listFiles()) {
//			
//			if(!file.getName().contains(".sdf"))continue; 
//						
//		    es.submit(() -> {
//		    	String filepathSDF=file.getAbsolutePath();
//		    	String destJsonPath=destFolder+"TEST_results_all_endpoints_"+file.getName().replace(".sdf", ".json");
//		    	RunFromSmiles.runSDF_all_endpoints_write_continuously(filepathSDF,destJsonPath,skipMissingSID,maxCount);
//		    });
//		    
//			if(true) break;
//
//		}
//		
//		es.shutdown();
//		
//	}
	
	
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
		
		r.runSDF();
//		r.runSDFsMultiThreaded();
		
//		String fileNameJson="TEST_results_all_endpoints_snapshot_compounds4.json";
//		String destJsonPath="reports/"+fileNameJson;
//		r.lookAtResultsInJson(destJsonPath, false);
		
//		r.lookAtResultsInJsonFiles("reports", false, false);
		
	}

}
