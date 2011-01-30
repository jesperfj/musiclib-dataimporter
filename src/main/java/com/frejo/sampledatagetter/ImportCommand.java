package com.frejo.sampledatagetter;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.force.cliforce.Command;
import com.force.cliforce.CommandContext;
import com.sforce.async.AsyncApiException;
import com.sforce.async.BatchInfo;
import com.sforce.async.ContentType;
import com.sforce.async.JobInfo;
import com.sforce.async.JobStateEnum;
import com.sforce.async.OperationEnum;
import com.sforce.async.RestConnection;
import com.sforce.soap.partner.DescribeSObjectResult;
import com.sforce.soap.partner.Field;
import com.sforce.soap.partner.PartnerConnection;

public class ImportCommand implements Command {

	@Override
	public String describe() {
		// TODO Auto-generated method stub
		return "Imports a data set into Force.com Database";
	}

	@Override
	public void execute(CommandContext ctx) throws Exception {
		
		File dir = new File("./sampledata");
		if(!dir.exists()) {
			throw new Exception("Couldn't find directory: "+dir.getAbsolutePath());
		}
		List<String> files = Arrays.asList(dir.list());
		Collections.sort(files);
		
		RestConnection rc = ctx.getRestConnection();
		PartnerConnection pc = ctx.getPartnerConnection();
		
		for(String fn : files) {
			String objName = fn.substring(2,fn.length()-4);
			ctx.getCommandWriter().println(objName);
			
			// We expect to find a single external ID field and use that for the upsert
			
			DescribeSObjectResult res = pc.describeSObject(objName);
			String externalIdFieldName = "id";
			for(Field f : res.getFields()) {
				if(f.isExternalId()) {
					externalIdFieldName = f.getName();
					break;
				}
			}

			try {
				JobInfo job = new JobInfo();
				job.setContentType(ContentType.CSV);
				job.setObject(objName);
				job.setOperation(OperationEnum.upsert);
				job.setExternalIdFieldName(externalIdFieldName);
				job = rc.createJob(job);

				FileInputStream in = new FileInputStream(dir.getPath()+"/"+fn);
				InputStreamBatcher isb = new InputStreamBatcher(10000, in, null);
				int count=0;
				while(!isb.eof()) {
					BatchInfo bi = rc.createBatchFromStream(job, isb);
					isb.reset();
					count++;
					if(count>50) {
						ctx.getCommandWriter().println("Maximum of 50 batches reached. Aborting load");
						break;
					}
				}
				in.close();
				rc.closeJob(job.getId());
				ctx.getCommandWriter().println("Job created with ID "+job.getId()+". curl to get it:\n"+
					   "sh curl -s -H \"X-SFDC-Session: "+rc.getConfig().getSessionId().replace("!", "\\!")+"\" "+rc.getConfig().getRestEndpoint()+"/job/"+job.getId()+"\n");
				while(true) {
					try {
						Thread.sleep(1000);
					} catch(InterruptedException e) {}
					job = rc.getJobStatus(job.getId());
					if(job.getState().equals(JobStateEnum.Aborted) ||
							job.getState().equals(JobStateEnum.Failed)) {
						ctx.getCommandWriter().println("Job was aborted or failed");
						break;
					}
					if(job.getNumberBatchesInProgress()==0 && job.getNumberBatchesQueued()==0) {
						ctx.getCommandWriter().println("Finished loading "+objName);
						break;
					}
				}		
				
			} catch (AsyncApiException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		
		
		
		}
	}

	@Override
	public String name() {
		// TODO Auto-generated method stub
		return "import";
	}

}
