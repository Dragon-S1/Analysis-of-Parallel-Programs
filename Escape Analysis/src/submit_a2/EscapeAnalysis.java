package submit_a2;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import dont_submit.EscapeQuery;
import soot.Body;
import soot.RefLikeType;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.DefinitionStmt;
import soot.jimple.EnterMonitorStmt;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.InvokeStmt;
import soot.jimple.NewExpr;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;

class FMap{
	Map<String, Set<String>> rho;
	Map<String, Map<String, Set<String>>> sigma;
	Map<String, Boolean> EM;
	List<Object> locals;
	
	FMap(List<Object> locals) {
		this.locals = locals;
		this.rho = new HashMap<String, Set<String>>();
		this.EM = new HashMap<String, Boolean>();
		for(Object it: locals) {
			rho.put(it.toString(), new HashSet<String>());
		}

		this.sigma = new HashMap<String, Map<String, Set<String>>>();
	}
	
	boolean isEqual(FMap other) {
		for(Map.Entry<String, Set<String>> e : other.rho.entrySet()) {
			if(!e.getValue().equals(rho.get(e.getKey()))) {
				return false;
			}
		}
		for(Map.Entry<String, Map<String, Set<String>>> e : other.sigma.entrySet()) {
			if(!sigma.containsKey(e.getKey())) {
				return false;
			}
			else {
				for(Map.Entry<String, Set<String>> f : e.getValue().entrySet()) {
					if(!f.getValue().equals(sigma.get(e.getKey()).get(f.getKey()))) {
						return false;
					}
				}
			}
		}
		return true;
	}
	
	FMap Copy() {
		FMap copy = new FMap(this.locals);
		for(Map.Entry<String, Set<String>> e : rho.entrySet()) {
			copy.rho.put(e.getKey(), new HashSet<String>(e.getValue()));
		}
		for(Map.Entry<String, Map<String, Set<String>>> e : sigma.entrySet()) {
			Map<String, Set<String>> fields = new HashMap<String, Set<String>>();
			for(Map.Entry<String, Set<String>> f : e.getValue().entrySet()) {
				fields.put(f.getKey(), new HashSet<String>(f.getValue()));
			}
			copy.sigma.put(e.getKey(), fields);
		}
		copy.EM = new HashMap<String, Boolean>(EM);
		return copy;
	}
}

class Flow {
    FMap IN;
    FMap OUT;
    
    Flow(List<Object> locals){
    	this.IN = new FMap(locals);
    	this.OUT = new FMap(locals);
    }
}

class ForwardAnalysis {
	Body body;
	UnitGraph G;
	List<Object> locals;
    Queue<Unit> worklist;
    Map<Unit, Flow> map;
    Set<String> Og;
    Unit end;
    Unit start;
    Boolean flag;
    Map<Unit, Set<String>> callee;
    Set<Unit> addCall;
    
    ForwardAnalysis(Body body){
    	this.body = body;
    	this.G = new BriefUnitGraph(body);
    	this.locals = Arrays.asList(body.getLocals().toArray());
    	this.worklist = new LinkedList<>();
    	this.map = new HashMap<Unit, Flow>();
    	this.Og = new HashSet<String>();
    	this.end = body.getUnits().getLast();
    	this.start = body.getUnits().getFirst();
    	this.flag = false;
    	this.callee = new HashMap<Unit, Set<String>>();
    	this.addCall = new HashSet<Unit>();
    }
    
    void init() {
    	for(Unit u: body.getUnits()) {
			worklist.add(u);
			map.put(u, new Flow(locals));
		}
    }
    
    int checkType(Unit u) {
		if((u instanceof DefinitionStmt) && !(u instanceof IdentityStmt)) {
			DefinitionStmt ds = (DefinitionStmt) u;
			Value leftOp = ds.getLeftOp();
			Value rightOp = ds.getRightOp();
			if((leftOp.getType() instanceof RefLikeType) && (rightOp.getType() instanceof RefLikeType)) {
				if(rightOp instanceof NewExpr) {
					//Alloc x = new
					return 1;
				}
				else if(locals.contains(leftOp) && locals.contains(rightOp)) {
					//Copy x = y
					return 2;
				}
//				else if(locals.contains(leftOp) && (rightOp instanceof FieldRef) && !Scene.v().getSootClass(rightOp.getType().toString()).isJavaLibraryClass()) {
				else if(locals.contains(leftOp) && (rightOp instanceof FieldRef)) {
					//Load x = y.f
					return 3;
				}
				else if((leftOp instanceof FieldRef) && locals.contains(rightOp)) {
					//Store x.f = y
					return 4;
				}
				else {
					return 0;
				}
			}
		}
		if(u instanceof InvokeStmt) {
			InvokeStmt i = (InvokeStmt) u;
			if(i.getInvokeExpr() instanceof VirtualInvokeExpr) {
				//Call
				return 5;
			}
		}
		return 0;
    }
    
    Set<String> reachableFromId(Unit u, String id){
		Set<String> ret = new HashSet<String>();
		for(String s : map.get(u).IN.rho.get(id)) {
			ret.add(s);
			ret.addAll(reachableFromObj(u, s));
		}
		return ret;
	}
	
	Set<String> reachableFromObj(Unit u, String obj){
		Queue<String> st = new LinkedList<>();
		st.add(obj);
		Set<String> ret = new HashSet<String>();
		while(!st.isEmpty()) {
			String o = st.poll();
			for(Map.Entry<String, Set<String>> e : map.get(u).IN.sigma.get(o).entrySet()) {
				for(String s : e.getValue()) {
					if(!ret.contains(s)) {
						st.add(s);
						ret.add(s);
					}
				}
			}	
		}
		return ret;
	}
    
    void Analyse(){
    	flag = false;
    	addCall.clear();
    	while(!this.worklist.isEmpty()) {
			Unit u = worklist.poll();
			
			FMap effect = new FMap(locals);
			for(Unit v: G.getPredsOf(u)) {
				for(Map.Entry<String, Set<String>> e : map.get(v).OUT.rho.entrySet()) {
					effect.rho.get(e.getKey()).addAll(e.getValue());
				}
				for(Map.Entry<String, Map<String, Set<String>>> e : map.get(v).OUT.sigma.entrySet()) {
					if(effect.sigma.containsKey(e.getKey())) {
						for(Map.Entry<String, Set<String>> f : e.getValue().entrySet()) {
							effect.sigma.get(e.getKey()).get(f.getKey()).addAll(f.getValue());
						}
					}
					else{
						Map<String, Set<String>> fields = new HashMap<String, Set<String>>();
						for(Map.Entry<String, Set<String>> f : e.getValue().entrySet()) {
							fields.put(f.getKey(), new HashSet<String>(f.getValue()));
						}
						effect.sigma.put(e.getKey(), fields);
					}
				}
				for(Map.Entry<String, Boolean> e: map.get(v).OUT.EM.entrySet()){
					if(effect.EM.containsKey(e.getKey())) {
						effect.EM.put(e.getKey(), effect.EM.get(e.getKey()) | e.getValue());
					}
					else {
						effect.EM.put(e.getKey(), e.getValue());
					}
				}
			}
			
			if(!map.get(u).IN.isEqual(effect)) {
				if(u.equals(start)) {
					map.get(u).OUT = map.get(u).IN.Copy();
				}
				else {
					map.get(u).IN = effect;
					map.get(u).OUT = effect.Copy();			
				}
				for(Unit v: G.getSuccsOf(u)) {
					if(!worklist.contains(v)) {
						worklist.add(v);						
					}
				}
			}
			
			int type = checkType(u);
			if(type!=0 && type!=5) {
				DefinitionStmt ds = (DefinitionStmt) u;
				Value leftOp = ds.getLeftOp();
				Value rightOp = ds.getRightOp();
				boolean escaped = false;
				switch(type) {
					case 1:
						//Alloc x = new
						String newObj = "O"+u.getJavaSourceStartLineNumber();
						//Rho
						map.get(u).OUT.rho.get(leftOp.toString()).add(newObj);
						Og.add(newObj);
						//Sigma
						Map<String, Set<String>> fields = new HashMap<String, Set<String>>();
						for(SootField o:Scene.v().getSootClass(rightOp.getType().toString()).getFields()) {
							fields.put(o.getName(), new HashSet<String>());
						}
						map.get(u).OUT.sigma.put(newObj, fields);
						//EM
						if(Scene.v().getSootClass(rightOp.getType().toString()).getSuperclass().getJavaStyleName().equals("Thread")) {
							//If new is of type thread
							map.get(u).OUT.EM.put(newObj, true);
						}
						else {
							map.get(u).OUT.EM.put(newObj, false);
						}
						break;
					case 2:
						//Copy x = y
						map.get(u).OUT.rho.put(leftOp.toString(), new HashSet<String>(map.get(u).IN.rho.get(rightOp.toString())));
						break;
					case 3:
						//Load x = y.f
						//Rho
						if(((FieldRef) rightOp).getField().isStatic()) {
							map.get(u).OUT.rho.put(leftOp.toString(), Og);
						}
						else {
							escaped = false;
							Set<String> n = new HashSet<String>();
							for(String r : map.get(u).IN.rho.get(((FieldRef) rightOp).getUseBoxes().get(0).getValue().toString())) {
								n.addAll(map.get(u).IN.sigma.get(r).get(((FieldRef) rightOp).getField().getName()));
								if(map.get(u).IN.EM.get(r)) {
									escaped = true;
								}
							}
							if(map.get(u).IN.rho.get(((FieldRef) rightOp).getUseBoxes().get(0).getValue().toString()).containsAll(Og) || escaped) {
								//y has Og or some O in rho(y) escaped
								map.get(u).OUT.rho.put(leftOp.toString(), Og);
							}
							else {
								//Otherwise
								map.get(u).OUT.rho.put(leftOp.toString(), n);
							}	
						}
						break;			
					case 4:
						//Store x.f = y
						if(((FieldRef) leftOp).getField().isStatic()) {
							//if f is class field
							Set<String> reachable = reachableFromId(u,rightOp.toString());
							for(String r : reachable) {
								map.get(u).OUT.EM.put(r, true);
							}
						}
						else {
							escaped = false;
							for(String r: map.get(u).IN.rho.get(((FieldRef) leftOp).getUseBoxes().get(0).getValue().toString())) {
								if(map.get(u).IN.EM.get(r)) {
									escaped = true;
									break;
								}
							}
							if(((FieldRef) leftOp).getField().isStatic() || escaped) {
								//if f is class field
								Set<String> reachable = reachableFromId(u,rightOp.toString());
								for(String r : reachable) {
									map.get(u).OUT.EM.put(r, true);
								}
							}
							else {
								for(String r: map.get(u).IN.rho.get(((FieldRef) leftOp).getUseBoxes().get(0).getValue().toString())) {
									map.get(u).OUT.sigma.get(r).get(((FieldRef) leftOp).getField().getName()).addAll(map.get(u).IN.rho.get(rightOp.toString()));
								}		
							}				
						}
						break;
				}
			}		
			if(type==5) {
				InvokeStmt i = (InvokeStmt) u;
				if(callee.containsKey(u)) {
					if(!callee.get(u).equals(map.get(u).IN.rho.get(i.getUseBoxes().get(0).getValue().toString()))) {
						addCall.add(u);
					}
				}
				else {
					callee.put(u, map.get(u).IN.rho.get(i.getUseBoxes().get(0).getValue().toString()));
					addCall.add(u);
				}
			}
		}
    }
}

class Callsite{
	SootMethod caller;
	Unit u;
	SootMethod callee;
	
	Callsite(SootMethod caller, Unit u, SootMethod callee){
		this.caller = caller;
		this.u = u;
		this.callee = callee;
	}
}

class InterProc {
	CallGraph C;
	Queue<Callsite> worklist;
	Map<String, ForwardAnalysis> FM;
	Map<String, Set<SootMethod>> Callee;
	
	InterProc(){
		this.C = Scene.v().getCallGraph();
    	this.worklist = new LinkedList<>();
    	FM = new HashMap<String, ForwardAnalysis>();
	}
	
	void init(SootMethod main) {
		worklist.add(new Callsite((SootMethod)null,(Unit)null,main));
	}
	
	void Analyse() {
		while(!worklist.isEmpty()){
			Callsite s = worklist.poll();
			if(!FM.containsKey(s.callee.getName())) {
				ForwardAnalysis F = new ForwardAnalysis(s.callee.retrieveActiveBody());
				F.init();
				if(!s.callee.getName().equals("main")) {
					Set<String>r = FM.get(s.caller.getName()).map.get(s.u).IN.rho.get(s.u.getUseBoxes().get(0).getValue().toString());
					F.map.get(F.start).IN.rho.put("this", new HashSet<String>(r));
					
					for(String o : r) {
						F.map.get(F.start).IN.sigma.put(o, FM.get(s.caller.getName()).map.get(s.u).IN.sigma.get(o));
						F.map.get(F.start).IN.EM.put(o, FM.get(s.caller.getName()).map.get(s.u).IN.EM.get(o));
					}
				}
				F.Analyse();
				FM.put(s.callee.getName(), F);
			}
			else {
				ForwardAnalysis F = FM.get(s.callee.getName());
				if(!s.callee.getName().equals("main")) {
					Set<String>r = FM.get(s.caller.getName()).map.get(s.u).IN.rho.get(s.u.getUseBoxes().get(0).getValue().toString());
					F.map.get(F.start).IN.rho.put("this", new HashSet<String>(r));
					
					for(String o : r) {
						F.map.get(F.start).IN.sigma.put(o, FM.get(s.caller.getName()).map.get(s.u).IN.sigma.get(o));	
					}
				}
				F.Analyse();
			}
			
			for(Unit v : FM.get(s.callee.getName()).addCall) {
				InvokeStmt i = (InvokeStmt) v;
				if(i.getInvokeExpr().getMethod().getName().equals("start") && Scene.v().getSootClass(i.getInvokeExpr().getMethod().getDeclaringClass().toString()).getJavaStyleName().equals("Thread")) {
					worklist.add(new Callsite(s.callee,v,Scene.v().getSootClass(v.getUseBoxes().get(0).getValue().getType().toString()).getMethodByName("run")));
				}
				else if(!(i.getInvokeExpr().getMethod().getName().equals("join") && Scene.v().getSootClass(i.getInvokeExpr().getMethod().getDeclaringClass().toString()).getJavaStyleName().equals("Thread"))){
					worklist.add(new Callsite(s.callee,v,i.getInvokeExpr().getMethod()));				
				}
			}
		}
	}
	
	void populateAnswers() {
		for(int i=0; i<A2.queryList.size(); i++) {
			EscapeQuery q = A2.queryList.get(i);
			Body body = Scene.v().getSootClass(q.getClassName()).getMethodByName(q.getMethodName()).retrieveActiveBody();
			Unit synch = null;
			
			for(Unit u : body.getUnits()) {
				if(u instanceof EnterMonitorStmt) {
					synch = u;
					break;
				}
			}
			
			boolean escaped = false;
			
			if(synch != null) {
				String id = synch.getUseBoxes().get(0).getValue().toString();
				Set<String> objs = FM.get(q.getMethodName()).map.get(synch).IN.rho.get(id);	
				for(String s : objs) {
					if(FM.get(q.getMethodName()).map.get(synch).IN.EM.get(s)) {
						escaped = true;
						break;
					}
				}
			}
			
			if(escaped) {
				A2.answers[i] = "No";
			}
			else {
				A2.answers[i] = "Yes";
			}
			
		}
	}
}


public class EscapeAnalysis extends SceneTransformer{

	@Override
	protected void internalTransform(String phaseName, Map<String, String> options) {
		/*
		 * Implement your escape analysis here
		 */
		
		InterProc P = new InterProc();
		P.init(Scene.v().getMainMethod());
		P.Analyse();
		P.populateAnswers();
		
		
	}
	
}
