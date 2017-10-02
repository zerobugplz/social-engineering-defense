import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.FileNotFoundException;
import java.io.BufferedReader;


import edu.stanford.nlp.ling.SentenceUtils;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.TregexPattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;


public class DetectPhishingMail{
	

	
	/*
	 * Extracting phishing keywords
	 * */
	private static void searchKeyword(List<TypedDependency> tdl, List<String> verb, List<String> obj) {
	    for(int i = 0; i < tdl.size(); i++) {
	    	String typeDepen = tdl.get(i).reln().toString();
	    	
	    	//verb
	    	if( verb.contains(typeDepen) ){
	    		System.out.print("verb :" + tdl.get(i).dep().originalText() + ">");
	    		System.out.println(tdl.get(i).gov().originalText());
	    	}
	    	//obj
	    	if( obj.contains(typeDepen) ) {
	    		System.out.print("obj :" + tdl.get(i).gov().originalText() + ">");
	    		System.out.println(tdl.get(i).dep().originalText());
	    	}
	    }
	}
	
	/*
	 * Extracting command sentence
	 * */
	private static boolean isImperative(Tree parse) {
		TregexPattern noNP = TregexPattern.compile("((@VP=verb > (S !> SBAR)) !$,,@NP)");
	    TregexMatcher n = noNP.matcher(parse);
	    while(n.find()) {
	    	String match = n.getMatch().firstChild().label().toString();
	    	
	    	//remove gerund, to + infinitiv
	    	if(match.equals("VP")) {
	    		match = n.getMatch().firstChild().firstChild().label().toString();
	    	}
	    	if(match.equals("TO") || match.equals("VBG")) {
	    		continue;
	    	}
	    	
	    	//imperative sentence
	    	return true;
	    }
	    return false;
	}
	private static boolean isSuggestion() {
		//
		return false;
	}
	private static boolean isSuggestion(Tree parse) {
		TregexPattern sug = TregexPattern.compile("((@VP=md > S ) $,,@NP=you )");
	    TregexMatcher s = sug.matcher(parse);
	    
	    while(s.find()) {
	    	String y = s.getNode("you").getChild(0).getChild(0).value();
	    	
	    	if(y.equals("you") || y.equals("You") || y.equals("YOU")) {
	    		return true;
	    	}
	    }
	    return false;
	}
	private static boolean isDesireExpression() {
		return false;
	}
	
	private static void detectCommand(LexicalizedParser lp, String sentence) {		   
		
		//penn tree
		TokenizerFactory<CoreLabel> tokenizerFactory =
	        PTBTokenizer.factory(new CoreLabelTokenFactory(), "");
	    Tokenizer<CoreLabel> tok =
	        tokenizerFactory.getTokenizer(new StringReader(sentence));
	    List<CoreLabel> rawWords = tok.tokenize();
	    Tree parse = lp.apply(rawWords);

	    //dependency
	    TreebankLanguagePack tlp = lp.treebankLanguagePack(); // PennTreebankLanguagePack for English
	    GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
	    GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
	    List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();
	   
	    //1. extracting imperative sentence    	
	    if(isImperative(parse)) {
	    	searchKeyword(tdl,Arrays.asList("nmod","nsubj","subjpass"), Arrays.asList("dobj"));
	    }
	    
	    //2. extracting suggestion sentence
	    if(isSuggestion()) {
	    	searchKeyword(tdl,Arrays.asList("nsubj","subjpass"), Arrays.asList("dobj"));
	    }
	
	    //3. extracting sentence including desire expression
	    if(isDesireExpression()) {
	    	// ���
	    }
	    	   
	}
	public static List<String> readArray(JsonReader reader) throws IOException {
		List<String> contents = new ArrayList<String>();
		
		reader.beginArray();
		while(reader.hasNext()) {
			contents.add(reader.nextString());
		}
		reader.endArray();
		return contents;
	}
	
	public static void main(String[] args) {
		String parserModel = "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz";
	    String fileName = System.getProperty("user.dir")+"\\src\\data";
		
	    LexicalizedParser lp = LexicalizedParser.loadModel(parserModel);
	    
		if(args.length == 0){
			Scanner scanner = null;
			try {
				scanner = new Scanner(System.in);
			    System.out.println("Select the input method\n 1: text input 2: text File 3:JSON File  >> ");
				int inputMethod = scanner.nextInt();
			    
				switch(inputMethod) {
				
			    //standard text input
				case 1: 
			    	while(scanner.hasNext()) {
				    	String value = scanner.nextLine();
				    	detectCommand(lp, value);
			    	}
			    break;
			    
			    //text input file
			    case 2:
			    	FileReader fr = null;
			    	BufferedReader br = null;
			    	try {
				    	fr = new FileReader(fileName + ".txt"); 
				    	br = new BufferedReader(fr);
				    	
				    	String value;
				    	while((value = br.readLine()) != null) {
				    		detectCommand(lp, value);
				    	}
			    	} catch(IOException e) {
			    		e.printStackTrace();
			    	} finally {
			    		try {
			    			if(br != null) br.close();
			    			if(fr != null) fr.close();
			    		}catch(IOException ex) {
			    			ex.printStackTrace();
			    		}
			    	}
			    	break;
			    	
			    //json input file
			    case 3:
			
			    	try {
			    		JsonReader reader = new JsonReader(new FileReader(fileName + ".json"));
			    		Gson gson = new GsonBuilder().create();
				    		
			    		reader.beginObject();
				    	while(reader.hasNext()) {
				    		String name = reader.nextName();
				    		List<String> sentences = readArray(reader);
				    		for(String value : sentences) {
				    			detectCommand(lp, value);
				    		}
				    	}
			    	}catch (FileNotFoundException e) {
			    		e.printStackTrace();
			    	}catch (IOException e) {
			    		e.printStackTrace();
			    	}
			    	break;
			    
			    default:
			    	System.out.println("wrong input");			    
				}
			}
			finally{
			    if(scanner != null) scanner.close();
			}
	    }
	}

}
