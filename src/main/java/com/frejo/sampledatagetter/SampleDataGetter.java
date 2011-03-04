package com.frejo.sampledatagetter;

import static com.freebase.json.JSON.a;
import static com.freebase.json.JSON.o;

import java.io.File;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.force.cliforce.Command;
import com.force.cliforce.CommandContext;
import com.force.cliforce.Plugin;
import com.freebase.api.Freebase;
import com.freebase.json.JSON;

public class SampleDataGetter implements Plugin {

    public List<Command> getCommands() {
        return Arrays.asList(new Command[]{new GetMusicCommand(),
        								   new ImportCommand()});
    }

    public String getName() {
        return "SampleDataGetter";
    }
    
    public static class GetMusicCommand implements Command {

    	//private static final SimpleDateFormat dfm = new SimpleDateFormat("'\"'yyyy-MM-dd'T'HH:mm:ss.SSSZ'\"'");
    	private static final SimpleDateFormat dfm = new SimpleDateFormat("'\"'yyyy-MM-ddZ'\"'");
    	private static final SimpleDateFormat yearOnly = new SimpleDateFormat("yyyy");
    	private static final SimpleDateFormat yearMonth = new SimpleDateFormat("yyyy-MM");
    	private static final SimpleDateFormat yearMonthDay = new SimpleDateFormat("yyyy-MM-dd");

        private String formatDate(String in) {
        	Date d = null;
        	try {
        		d = yearMonthDay.parse(in);
        	} catch (ParseException e) {
        		try {
        			d = yearMonth.parse(in);
        		} catch(ParseException e2) {
        			try {
        				d = yearOnly.parse(in);
        			} catch (ParseException e3) {
        				return "";
        			}
        		}
        	}
        	return dfm.format(d);
        }
        
        private String fixEscape(String in) {
        	return in.replace("\\/","/").replace("\"", "\"\"");
        }

        public String describe() {
            return "Retrieves some music albums from Freebase and stores them as CSV";
        }

        @Override
        public String name() {
            return "getmusic";
        }
        
        
        @SuppressWarnings("unchecked")
		public void execute(CommandContext ctx) throws Exception {
        	
            Freebase freebase = Freebase.getFreebase();
            JSON query = a(o(
            		"id", null,
            		"name", null,
            		"type", "/music/album",
            		"limit", 100,
            		"artist", a(o(
            				"id", null,
            				"name", null,
            				"origin", a(),
            				"active_start", null,
            			    "active_end", null
            			  )),
                    "genre", a(o(
            			    "id", null,
            			    "name", null
            			  )),
        			"*", a()
            			));
            JSON result = freebase.mqlread(query);

            List<String> probs = new ArrayList<String>();
            Map<String,JSON> artists = new HashMap<String,JSON>();
            Map<String,JSON> genres = new HashMap<String,JSON>();
            
            File f = new File("./sampledata");
            if(!f.exists()) {
            	f.mkdir();
            }
            

            PrintWriter out = new PrintWriter(new File("./sampledata/03album__c.csv"));

            ctx.getCommandWriter().println("=== ALBUMS ===");

            out.println("freebaseId__c,name,artist__r.freebaseId__c,releaseDate__c,genre__r.freebaseId__c");
            for(JSON obj : (List<JSON>) result.get("result").array()) {
            	try {
            		artists.put(fixEscape(obj.get("artist").get(0).get("id").string()), obj.get("artist").get(0));
            		genres.put(fixEscape(obj.get("genre").get(0).get("id").string()),obj.get("genre").get(0));
            		
            		out.println(fixEscape(obj.get("id").string())+",\""+
            				    fixEscape(obj.get("name").string())+"\","+
            				    fixEscape(obj.get("artist").get(0).get("id").string())+","+
            				    (obj.get("release_date").array().size()==0 ? "" : formatDate(obj.get("release_date").get(0).string()))+","+
            				    fixEscape(obj.get("genre").get(0).get("id").string()));
            	} catch (Throwable t) {
            		probs.add(t.getMessage()+": "+obj.toJSONString());
            	}
            }
            out.close();
            
            out = new PrintWriter(new File("./sampledata/02artist__c.csv"));

            ctx.getCommandWriter().println("=== ARTISTS ===");

            out.println("freebaseId__c,name,origin__c,activeStart__c,activeEnd__c");

            for(JSON a : artists.values()) {
            	if(a.get("name").string().startsWith("Royce")) {
            		ctx.getCommandWriter().println(a.get("name").string()+ " => "+fixEscape(a.get("name").string()));
            	}
            	try {
            		out.println(fixEscape(a.get("id").string())+","+
            					"\""+fixEscape(a.get("name").string())+"\","+
            				    (a.get("origin").array().size()==0 ? "" : a.get("origin").get(0))+","+
            					(a.get("active_start")==null ? "": formatDate(a.get("active_start").string()))+","+
            					(a.get("active_end")==null ? "": formatDate(a.get("active_end").string())));
            		
            	} catch (Throwable t) {
            		probs.add(t.getMessage()+": "+a.toJSONString());
            	}
            }
            out.close();
            
            out = new PrintWriter(new File("./sampledata/01genre__c.csv"));

            ctx.getCommandWriter().println("=== GENRE ===");
            out.println("freebaseId__c,name");
            for(JSON g : genres.values()) {
            	try {
            		out.println(fixEscape(g.get("id").string())+","+g.get("name"));
            	} catch (Throwable t) {
            		probs.add(t.getMessage()+": "+g.toJSONString());
            	}
            }
            out.close();

            ctx.getCommandWriter().println("Problems: ");
            for(String p : probs) {
            	ctx.getCommandWriter().println("    "+p);
            }
            ctx.getCommandWriter().println("End Problems: ");

            //ctx.getCommandWriter().println(result.toString());
        }
    }

    public static class GetGenresCommand implements Command {
        public String describe() {
            return "Retrieves all genres currently in Freebase";
        }

        @Override
        public String name() {
            return "genres";
        }

        @SuppressWarnings("unchecked")
		public void execute(CommandContext ctx) throws Exception {
        	
            Freebase freebase = Freebase.getFreebase();

            JSON query = a(o(
            		"id", null,
            		"name", null,
            		"type", "/music/genre"
            		));
            JSON result = freebase.mqlread(query);
            
            for(JSON obj : (List<JSON>) result.get("result").array()) {
            	ctx.getCommandWriter().println(obj.get("id")+" "+obj.get("name"));
            }
        	
        }
    }
}
