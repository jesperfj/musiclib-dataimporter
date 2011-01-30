package com.frejo.sampledatagetter;

import java.io.*;
import java.util.*;

public class InputStreamBatcher extends InputStream {

	//private Logger logger = org.slf4j.LoggerFactory.getLogger(InputStreamBatcher.class);
	
	private int rowsPerBatch;

	private int lineCount = 0;

	private CSVReader reader;

	private boolean eof = false;
	private boolean eob = false;
	
	private byte[] currentLine;

	private byte[] headerLine;
	
	private Set<Integer> includedColumns = new HashSet<Integer>();

	private boolean inHeader = true;
	
	private int pos = 0;
	
	public InputStreamBatcher(int rowsPerBatch, InputStream wrapped, Map<String, String> mappings) throws IOException {
		super();
		this.rowsPerBatch = rowsPerBatch;
		reader = new CSVReader(new InputStreamReader(wrapped));
		reader.setMaxRowsInFile(Integer.MAX_VALUE);
		reader.setFileSizeInCharacters(Integer.MAX_VALUE);
		initMapper(mappings);
	}

	/**
	 * Called by the constructor to set things up. Will generate the header row byte array and
	 * a set containing the column numbers of the columns that should be mapped. This is all we
	 * need because we maintain the order of columns from source to target CSV. This method ends
	 * by reading the first row of data with a call to nextLine(). Rows are read "ahead". This
	 * is necessary to properly signal EOF.
	 * @param mappings
	 * @throws IOException
	 */
	private void initMapper(Map<String, String> mappings) throws IOException {

		List<String> headerRow = reader.nextRecord();
		StringBuilder b = new StringBuilder();
		boolean pastFirst=false;
        for(int i=0;i<headerRow.size();i++) {
        	// if called with null mappings, the header row is just the same
            String field = mappings==null ? headerRow.get(i) : mappings.get(headerRow.get(i));
            if(field!=null && field.length()>0) {
				if(pastFirst) b.append(",");
            	b.append(field);
                includedColumns.add(i);
				pastFirst = true;
            }
        }
        headerLine = b.toString().getBytes();
        //logger.debug("Header Row Generated: "+headerLine);
        nextLine();
	}

	/**
	 * This is called whenever we need a new line of source data. EOB and EOF and
	 * lineCount are all set by this method.
	 * @throws IOException
	 */
	private void nextLine() throws IOException {
		//logger.debug("nextLine() called");
		List<String> flist = reader.nextRecord();
		if(flist==null) {
			//logger.debug("No more data to read from wrapped stream");
			eob = true;
			eof = true;
			return;
		}
		StringBuilder b = new StringBuilder();
		boolean pastFirst=false;
		for(int i=0;i<flist.size();i++) {
			if(includedColumns.contains(i)) {
				b.append(pastFirst ? "," : "\n");
				if(flist.get(i)!=null) {
					b.append("\""+flist.get(i).replace("\"", "\"\"")+"\"");
				}
				pastFirst = true;
			}
		}
		currentLine = b.toString().getBytes();
		//logger.debug("Next line ready: "+b);
		if(lineCount == rowsPerBatch) {
			//logger.debug("lineCount reached batch limit: "+lineCount);
			eob = true;
		} else {
			lineCount++;
		}
	}

	/**
	 * Call reset after the reading process has reached "end of file" which is really
	 * just "end-of-batch". Reset will prepare this instance for reading another batch.
	 */
	@Override
	public void reset() {
		inHeader = true;
		pos = 0;
		eob = false;
		lineCount = 1;
	}

	/**
	 * Overrides read() on InputStream. This method will return the next byte of the
	 * current CSV batch. -1 will be returned either when end-of-batch or end-of-file
	 * has been reached. If -1 was returned and eof() returns false, there are more
	 * records to be read.
	 */
	@Override
	public int read() throws IOException {
		// TODO Auto-generated method stub

		if(eob || eof) {
			//logger.debug("read: eob="+eob+",eof="+eof);
			return -1;
		}
		
		if(inHeader) {
			if(pos<headerLine.length) {
				//if(logger.isDebugEnabled()) System.out.print((char) headerLine[pos]);
				return headerLine[pos++];
			} else {
				//logger.debug("read header done");
				inHeader = false;
				pos = 0;
			}
		} else {
			if(pos < currentLine.length) {
				//if(logger.isDebugEnabled()) System.out.print((char) currentLine[pos]);
				return currentLine[pos++];
			} else {
				//logger.debug("read line "+lineCount+" done");
				nextLine();
				pos = 0;
			}
		}
		return read();
	}

	/**
	 * 
	 * @return false if there is more data on the InputStream. This method may return
	 * false while read returns -1. This means the current batch is finished and reset()
	 * should be called to start the next batch before read will return data.
	 */
	public boolean eof() {
		// TODO Auto-generated method stub
		return eof;
	}

}
