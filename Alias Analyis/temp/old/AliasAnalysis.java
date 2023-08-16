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


class Rho{
	Map<String, Set<String>> rho;
	
	Rho(List<Object> locals) {
		this.rho = new HashMap<String, Set<String>>();
		for(Object it: locals) {
			rho.put(it.toString(), new HashSet<String>());
		}
	}
	
	boolean isEqual(Map<String, Set<String>> other) {
		for (Map.Entry<String, Set<String>> e : other.entrySet()) {
			if(!e.getValue().equals(rho.get(e.getKey()))) {
				return false;
			}
		}
		return true;
	}
	
	Map<String, Set<String>> Copy(){
		Map<String, Set<String>> copy = new HashMap<String, Set<String>>();
		for (Map.Entry<String, Set<String>> e : rho.entrySet()) {
			copy.put(e.getKey(), new HashSet<String>(e.getValue()));
		}
		return copy;
	}
}

class ForwardAnalysis {
	Body body;
	UnitGraph G;
	List<Object> locals;
    Queue<Unit> worklist;
    Map<Unit, Rho> map;
    Set<String> Og;
    Unit fin;
	Map<String, Map<String, Set<String>>> sigma;
    
    ForwardAnalysis(Body body){
    	this.body = body;
    	this.G = new BriefUnitGraph(body);
    	this.locals = Arrays.asList(body.getLocals().toArray());
    	this.worklist = new LinkedList<>();
    	this.map = new HashMap<Unit, Rho>();
    	this.Og = new HashSet<String>();
    	this.fin = body.getUnits().getLast();
		this.sigma = new HashMap<String, Map<String, Set<String>>>();
    }
    
    void genWorkList() {
    	for(Unit u: body.getUnits()) {
			worklist.add(u);
		}
    }
    
    void genFlow() {
    	for(Unit u: body.getUnits()) {
    		map.put(u, new Rho(locals));
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
			}
		}
		return 0;
    }
    
    void Analyse(){
    	Map<String, Set<String>> finin = map.get(fin).Copy();
    	while(!this.worklist.isEmpty()) {
			Unit u = worklist.poll();
			for(Unit v: G.getPredsOf(u)) {
				for (Map.Entry<String, Set<String>> e : map.get(v).rho.entrySet()) {
					map.get(u).rho.get(e.getKey()).addAll(e.getValue());
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
						map.get(u).rho.get(leftOp.toString()).add(newObj);
						Og.add(newObj);
						Map<String, Set<String>> fields = new HashMap<String, Set<String>>();
						for(SootField o:Scene.v().getSootClass(rightOp.getType().toString()).getFields()) {
							fields.put(o.getName(), new HashSet<String>());
						}
						sigma.put(newObj, fields);
						break;
					case 2:
						map.get(u).rho.put(leftOp.toString(), new HashSet<String>(map.get(u).rho.get(rightOp.toString())));
						break;
					case 3:
						Set<String> n = new HashSet<String>();
						for(String r : map.get(u).rho.get(((FieldRef) rightOp).getUseBoxes().get(0).getValue().toString())) {
							n.addAll(sigma.get(r).get(((FieldRef) rightOp).getField().getName()));
						}
						map.get(u).rho.put(leftOp.toString(), n);
						break;
					case 4:
						if(map.get(u).rho.get(((FieldRef) leftOp).getUseBoxes().get(0).getValue().toString()).size() <= 1) {
							for(String r: map.get(u).rho.get(((FieldRef) leftOp).getUseBoxes().get(0).getValue().toString())) {
								sigma.get(r).put(((FieldRef) leftOp).getField().getName(), new HashSet<String>(map.get(u).rho.get(rightOp.toString())));							}
						}
						else {
							for(String r: map.get(u).rho.get(((FieldRef) leftOp).getUseBoxes().get(0).getValue().toString())) {
								sigma.get(r).get(((FieldRef) leftOp).getField().getName()).addAll(map.get(u).rho.get(rightOp.toString()));
							}
						}
						break;
					case 5:
						map.get(u).rho.get(leftOp.toString()).addAll(Og);
						break;
				}
			}
			if(worklist.isEmpty()) {
				if(!map.get(fin).isEqual(finin)) {
					genWorkList();
					finin = map.get(fin).Copy();
				}
			}
		}
    }
    
    void populateAnswers() {
    	for(int i=0; i<A1.queryList.size(); i++) {
			if((body.getMethod().getDeclaringClass().toString().equals(A1.queryList.get(i).getClassName())) && (body.getMethod().getName().equals(A1.queryList.get(i).getMethodName()))) {
				if(Collections.disjoint(map.get(fin).rho.get(A1.queryList.get(i).getLeftVar()), map.get(fin).rho.get(A1.queryList.get(i).getRightVar()))){
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
		F.genWorkList();
		F.genFlow();
		F.Analyse();
		F.populateAnswers();	
	}
}
