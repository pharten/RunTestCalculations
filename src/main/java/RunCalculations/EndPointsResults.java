package RunCalculations;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.openscience.cdk.AtomContainerSet;

import ToxPredictor.Application.Calculations.RunFromSmiles;
import ToxPredictor.Application.model.PredictionResults;

public class EndPointsResults {

	/**
	 * @param args
	 */
	private static String[] endpoints;
	private static String method;
	private static boolean createReports;
	private static boolean createDetailedReports;
	private static String key;
	private static int nprocs = Runtime.getRuntime().availableProcessors();
	//private static int nTasks = nprocs;
	private ExecutorService executor = null;
	
	EndPointsResults(String [] endpoints_in, String method_in, boolean createReports_in, boolean createDetailedReports_in, String key_in) {
		endpoints = endpoints_in;
		method = method_in;
		createReports = createReports_in;
		createDetailedReports = createDetailedReports_in;
		key = key_in;
	}
	
	public List<PredictionResults> calculateResults(AtomContainerSet batchSet) throws InterruptedException, ExecutionException {
		
		int batchSize = batchSet.getAtomContainerCount();
		int nprocsUsed = (batchSize <= nprocs ? batchSize:nprocs);
		//nprocsUsed = 50;
		executor = Executors.newFixedThreadPool(nprocsUsed);
		
		List<Future<List<PredictionResults>>> predictionResults = new ArrayList<Future<List<PredictionResults>>>(nprocsUsed);
		int batchSizeEachProc = (batchSize+nprocsUsed-1)/nprocsUsed;

		List<AtomContainerSet> chemicalSet = new ArrayList<AtomContainerSet>(nprocsUsed);
		for (int j=0; j<nprocsUsed; j++) {
			AtomContainerSet chemicals = new AtomContainerSet();
			for (int i=0;i<batchSizeEachProc;i++) {
				chemicals.addAtomContainer(batchSet.getAtomContainer(0));
				batchSet.removeAtomContainer(0);
				if(batchSet.getAtomContainerCount()==0) break;
			}
			chemicalSet.add(chemicals);
			if(batchSet.getAtomContainerCount()==0) break;
		}
		
		int setCount = chemicalSet.size();
		List<Future<List<PredictionResults>>> futureList = new ArrayList<Future<List<PredictionResults>>>();
		for (int j=0; j<setCount; j++) {
			ConcurrentRun task = new ConcurrentRun(chemicalSet.get(j));
			Future<List<PredictionResults>> future = executor.submit(task);
			futureList.add(future);
			//predictionResults.add(futureList);
		}
		
		int timeout = 200;
		List<PredictionResults> batchPredictionResults = new ArrayList<PredictionResults>();
		for(Future<List<PredictionResults>> fut : futureList) {
            try {
                // Future.get() waits for task to get completed
            	List<PredictionResults> predictions = fut.get(timeout, TimeUnit.SECONDS);
    			batchPredictionResults.addAll(predictions);
    			System.out.println("Prediction list "+(futureList.indexOf(fut)+1)+" out of "+futureList.size()+" with "+predictions.size()+" did finish");
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
				// TODO Auto-generated catch block
				System.out.println("Prediction list "+(futureList.indexOf(fut)+1)+" out of "+futureList.size()+" did not finish in "+timeout+" seconds");
				continue;
			}
		}
		executor.shutdown();
		
		return batchPredictionResults;
	}
	

	/**
	 * This illustrate the use of the Callable task framework.
	 */
	public static class ConcurrentRun implements Callable<List<PredictionResults>> {

		//private static final long serialVersionUID = -5069206897899763709L;

		private final AtomContainerSet mChemicals;

		public ConcurrentRun(AtomContainerSet chemicals) {
			mChemicals = chemicals;
		}

		@Override
		public List<PredictionResults> call() throws Exception {
			
			List<PredictionResults> resultsPartial = RunFromSmiles.runEndpointsAsList(mChemicals, endpoints, method, createReports, createDetailedReports, key);
//			for (int i=mFrom; i<mTo; i++) {
//				try {
//					mMixtures.get(i).calculateMixtureScore(mState);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
			return resultsPartial;
		}
	}
	
}

