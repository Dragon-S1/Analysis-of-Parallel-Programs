package submit_a1;

import java.util.*;

import soot.Body;
import soot.BodyTransformer;
import soot.RefLikeType;
import soot.Scene;
import soot.SootField;
import soot.Unit;
import soot.Value;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.NewExpr;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;


class FMap{
	Map<String, Set<String>> rho;
	Map<String, Map<String, Set<String>>> sigma;
	List<Object> locals;
	
	FMap(List<Object> locals) {
		this.locals = locals;
		this.rho = new HashMap<String, Set<String>>();
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
    Unit fin;
    
    ForwardAnalysis(Body body){
    	this.body = body;
    	this.G = new BriefUnitGraph(body);
    	this.locals = Arrays.asList(body.getLocals().toArray());
    	this.worklist = new LinkedList<>();
    	this.map = new HashMap<Unit, Flow>();
    	this.Og = new HashSet<String>();
    	this.fin = body.getUnits().getLast();
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
					return 1;
				}
				else if(locals.contains(leftOp) && locals.contains(rightOp)) {
					return 2;
				}
				else if(locals.contains(leftOp) && (rightOp instanceof FieldRef) && !Scene.v().getSootClass(rightOp.getType().toString()).isJavaLibraryClass()) {
					return 3;
				}
				else if((leftOp instanceof FieldRef) && locals.contains(rightOp)) {
					return 4;
				}
				else if(locals.contains(leftOp) && rightOp instanceof InvokeExpr) {
					return 5;
				}
				else {
					return 0;
				}
			}
		}
		return 0;
    }
    
    void Analyse(){
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
			}
			
			if(!map.get(u).IN.isEqual(effect)) {
				map.get(u).IN = effect;
				map.get(u).OUT = effect.Copy();
				for(Unit v: G.getSuccsOf(u)) {
					if(!worklist.contains(v)) {
						worklist.add(v);						
					}
				}
			}
			
			int type = checkType(u);
			if(type!=0) {
				DefinitionStmt ds = (DefinitionStmt) u;
				Value leftOp = ds.getLeftOp();
				Value rightOp = ds.getRightOp();
				switch(type) {
					case 1: 
						String newObj = "O"+u.getJavaSourceStartLineNumber();
						map.get(u).OUT.rho.get(leftOp.toString()).add(newObj);
						Og.add(newObj);
						Map<String, Set<String>> fields = new HashMap<String, Set<String>>();
						for(SootField o:Scene.v().getSootClass(rightOp.getType().toString()).getFields()) {
							fields.put(o.getName(), new HashSet<String>());
						}
						map.get(u).OUT.sigma.put(newObj, fields);
						break;
					case 2:
						map.get(u).OUT.rho.put(leftOp.toString(), new HashSet<String>(map.get(u).IN.rho.get(rightOp.toString())));
						break;
					case 3:
						Set<String> n = new HashSet<String>();
						for(String r : map.get(u).IN.rho.get(((FieldRef) rightOp).getUseBoxes().get(0).getValue().toString())) {
							n.addAll(map.get(u).IN.sigma.get(r).get(((FieldRef) rightOp).getField().getName()));
						}
						map.get(u).OUT.rho.put(leftOp.toString(), n);
						break;
					case 4:
						if(map.get(u).IN.rho.get(((FieldRef) leftOp).getUseBoxes().get(0).getValue().toString()).size() <= 1) {
							for(String r: map.get(u).IN.rho.get(((FieldRef) leftOp).getUseBoxes().get(0).getValue().toString())) {
								map.get(u).OUT.sigma.get(r).put(((FieldRef) leftOp).getField().getName(), new HashSet<String>(map.get(u).IN.rho.get(rightOp.toString())));							}
						}
						else {
							for(String r: map.get(u).IN.rho.get(((FieldRef) leftOp).getUseBoxes().get(0).getValue().toString())) {
								map.get(u).OUT.sigma.get(r).get(((FieldRef) leftOp).getField().getName()).addAll(map.get(u).IN.rho.get(rightOp.toString()));
							}
						}
						break;
					case 5:
						map.get(u).OUT.rho.get(leftOp.toString()).addAll(Og);
						break;
				}
			}
		}
    }
    
    void populateAnswers() {
    	for(int i=0; i<A1.queryList.size(); i++) {
			if((body.getMethod().getDeclaringClass().toString().equals(A1.queryList.get(i).getClassName())) && (body.getMethod().getName().equals(A1.queryList.get(i).getMethodName()))) {
				if(Collections.disjoint(map.get(fin).OUT.rho.get(A1.queryList.get(i).getLeftVar()), map.get(fin).OUT.rho.get(A1.queryList.get(i).getRightVar()))){
					A1.answers[i] = "No";
				}
				else {
					A1.answers[i] = "Yes";
				}
			}
		}
    }
}

public class AliasAnalysis extends BodyTransformer{

	@Override
	synchronized protected void internalTransform(Body arg0, String arg1, Map<String, String> arg2) {
		/*
		 * Implement your alias analysis here. A1.answers should include the Yes/No answers for 
		 * the queries
		 */
		ForwardAnalysis F = new ForwardAnalysis(arg0);
		F.init();
		F.Analyse();
		F.populateAnswers();	
	}
}
