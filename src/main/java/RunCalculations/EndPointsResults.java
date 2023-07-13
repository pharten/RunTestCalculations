package RunCalculations;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
	private static int nprocs = Runtime.getRuntime().availableProcessors()-14;
	private static int nTasks = nprocs;
	private ExecutorService executor = null;
	
	EndPointsResults(String [] endpoints_in, String method_in, boolean createReports_in, boolean createDetailedReports_in, String key_in) {
		endpoints = endpoints_in;
		method = method_in;
		createReports = createReports_in;
		createDetailedReports = createDetailedReports_in;
		key = key_in;
	}
	
	public List<PredictionResults> calculateResults(AtomContainerSet batchSet, int batchSize) throws InterruptedException, ExecutionException {
		
		executor = Executors.newFixedThreadPool(nprocs);
		List<Future<List<PredictionResults>>> predictionResults = new ArrayList<Future<List<PredictionResults>>>(nTasks);
		int chemicalsCount = batchSet.getAtomContainerCount();
		int taskSize = (batchSize+nTasks-1)/nTasks;

		List<AtomContainerSet> chemicalSet = new ArrayList<AtomContainerSet>(nTasks);
		for (int j=0; j<nTasks; j++) {
			AtomContainerSet chemicals = new AtomContainerSet();
			for (int i=0;i<taskSize;i++) {
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
		
		List<PredictionResults> batchPredictionResults = new ArrayList<PredictionResults>();
		for(Future<List<PredictionResults>> fut : futureList) {
            try {
                // Future.get() waits for task to get completed
    			batchPredictionResults.addAll(fut.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
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

