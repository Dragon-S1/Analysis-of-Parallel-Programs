package submit_a3;

import java.util.*;

import javafx.util.Pair;
import soot.Body;
import soot.Local;
import soot.PointsToAnalysis;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootField;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.EnterMonitorStmt;
import soot.jimple.ExitMonitorStmt;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.spark.sets.P2SetVisitor;
import soot.jimple.spark.sets.PointsToSetInternal;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;

class Node{
	Unit unit;
	String object;
	String name;
	String caller;
	int type;
	Set<Node> LocalPred;
	Set<Node> LocalSucc;
	Set<Node> NotifyPred;
	Set<Node> NotifySucc;
	Set<Node> StartPred;
	Set<Node> StartSucc;
	Set<Node> WaitingPred;
	Set<Node> WaitingSucc;
	Set<Node> KILL;
	Set<Node> GEN;
	Set<Node> M;
	Set<Node> OUT;
	
	Node(Unit unit, String caller) {
        this.unit = unit;
        this.caller = caller;
        Id();
        this.LocalPred = new HashSet<Node>();
        this.LocalSucc = new HashSet<Node>();
        this.NotifyPred = new HashSet<Node>();
        this.NotifySucc = new HashSet<Node>();
        this.StartPred = new HashSet<Node>();
        this.StartSucc = new HashSet<Node>();
        this.WaitingPred = new HashSet<Node>();
        this.WaitingSucc = new HashSet<Node>();
        this.KILL = new HashSet<Node>();
        this.GEN = new HashSet<Node>();
        this.M = new HashSet<Node>();
        this.OUT = new HashSet<Node>();
    }
	
	int checkType(Unit u) {
		Stmt s = (Stmt) unit;
		if(s instanceof IdentityStmt) {
			return 1;
		}
		else if(s instanceof AssignStmt) {
			AssignStmt a = (AssignStmt) s;
			Value leftOp = a.getLeftOp();
			Value rightOp = a.getRightOp();
			if(leftOp instanceof FieldRef) {
				return 2;
			}
			if(rightOp instanceof FieldRef) {
				return 3;
			}
		}
		else if(s instanceof InvokeStmt) {
			InvokeStmt i = (InvokeStmt) s;
			if(i.getInvokeExpr() instanceof VirtualInvokeExpr) {
				return 4;
			}
		}
		else if(s instanceof IfStmt) {
			return 5;
		}
		else if(s instanceof EnterMonitorStmt) {
			return 6;
		}
		else if(s instanceof ExitMonitorStmt) {
			return 7;
		}
		else if(s instanceof ReturnVoidStmt) {
			return 8;
		}
		
		return 0;
	}
	
	void Id() {
		Stmt s = (Stmt) unit;
		this.type = checkType(this.unit);
		switch(this.type) {
			case 1:
				this.object = "*";
				this.name = "begin";
				break;
			case 2:
				AssignStmt aw = (AssignStmt) s;
				Value leftOp = aw.getLeftOp();
				FieldRef fw = (FieldRef) leftOp;
				this.object = fw.getUseBoxes().get(0).getValue().toString() + ":" + fw.getField().getName();
				this.name = "write";
				break;
			case 3:
				AssignStmt ae = (AssignStmt) s;
				Value rightOp = ae.getRightOp();
				FieldRef fr = (FieldRef) rightOp;
				this.object = fr.getUseBoxes().get(0).getValue().toString() + ":" + fr.getField().getName();
				this.name = "read";
				break;
			case 4:
				InvokeStmt i = (InvokeStmt) s;
				InvokeExpr e = i.getInvokeExpr();
				this.object = e.getUseBoxes().get(0).getValue().toString();
				this.name = e.getMethod().getName();
				break;
			case 5:
				this.object = "*";
				this.name = "if";
				break;
			case 6:
				EnterMonitorStmt men = (EnterMonitorStmt) s;
				this.object = men.getOp().toString();
				this.name = "entry";
				break;
			case 7:
				ExitMonitorStmt mex = (ExitMonitorStmt) s;
				this.object = mex.getOp().toString();
				this.name = "exit";
				break;
			case 8:
				this.object = "*";
				this.name = "end";
				break;
			default:
				break;	
		}
	}
	
	void print() {
		System.out.println(this.unit + ": ("+this.object+","+this.name+","+this.caller+")");
	}
}

class SEG{
	Body body;
	String id;
	UnitGraph G;
	Node begin;
	Set<Node> N;
	Set<Node> startNodes;
	Map<String, Set<Node>> notifyNodes;
	Map<String, Set<Node>> waitingNodes;
	Map<String, Set<Node>> monitorObj;
	Map<String, Local> Locals;
	PointsToAnalysis pta;
	
	
	SEG(Body body, String id){
		this.body = body;
		this.id = id;
		this.G = new BriefUnitGraph(body);
		this.begin = new Node(G.getHeads().get(0), id);
		this.N = new HashSet<Node>();
		this.startNodes = new HashSet<Node>();
		this.notifyNodes = new HashMap<String, Set<Node>>();
		this.waitingNodes = new HashMap<String, Set<Node>>();
		this.monitorObj = new HashMap<String, Set<Node>>();
		this.Locals = new HashMap<String, Local>();
		this.pta = Scene.v().getPointsToAnalysis();
		
		for(Object l: this.body.getLocals().toArray()) {
			Locals.put(l.toString(), (Local)l);
		}
		Generate();
	}
	
	void Generate() {
		Queue<Node> q = new LinkedList<>();
		Set<Unit> vis = new HashSet<Unit>();
		List<Unit> heads = G.getHeads();
		
		Node curr = this.begin;
		q.add(curr);
		vis.add(heads.get(0));
		
		int numMonitor = 0;
		Set<String> monitorStart = new HashSet<String>();
		
		while(!q.isEmpty()) {
			Node U = q.poll();
			Unit u = U.unit;
			
			if(U.type!=0) {
				curr = U;
				this.N.add(curr);
				
				if(curr.name.equals("exit")) {
					monitorStart.remove(curr.object);
				}
				
				for(String obj: monitorStart) {
					addNode(monitorObj, curr, obj);
				}
				
				if (curr.name.equals("start")) {
					startNodes.add(curr);
				}
				else if(curr.name.equals("exit")) {
					numMonitor++;
					Node M = new Node(heads.get(2*numMonitor), id);
					curr.LocalSucc.add(M);
					q.add(M);
					vis.add(heads.get(2*numMonitor));
				}
				else if(curr.name.equals("wait")) {
					curr = handleWait(curr);
				}
				else if(curr.name.equals("notify") || curr.name.equals("notifyAll")) {
					addNode(notifyNodes, curr, curr.object);
				}
				else if(curr.name.equals("entry")) {
					monitorStart.add(curr.object);
				}
				
				List<Unit> succ = G.getSuccsOf(u);
				for(Unit v: succ) {
					Node V = new Node(v,id);
					V.LocalPred.add(curr);
					curr.LocalSucc.add(V);
					if(!vis.contains(v)) {
						q.add(V);
						vis.add(v);
					}
				}
			}
			else {
				List<Unit> succ = G.getSuccsOf(u);
				for(Unit v: succ) {
					Node V = new Node(v,id);
					if(!vis.contains(v)) {
						q.add(V);
						vis.add(v);
					}
				}
			}
		}
	}
	
	Node handleWait(Node wait) {
		Unit u = wait.unit;
		
		Node waiting = new Node(u,id);
		waiting.name = "waiting";
		
		wait.LocalSucc.add(waiting);
		waiting.LocalPred.add(wait);
		
		Node nentry = new Node(u,id);
		nentry.name = "notified-entry";
		
		waiting.LocalSucc.add(nentry);
		nentry.LocalPred.add(waiting);
		waiting.WaitingSucc.add(nentry);
		nentry.WaitingPred.add(waiting);
		
		addNode(waitingNodes,waiting, waiting.object);
		
		return nentry;
	}
	
	void addNode(Map<String, Set<Node>> map, Node n, String obj) {
		P2SetVisitor vis = new P2SetVisitor() {
			@Override
            public void visit(soot.jimple.spark.pag.Node node) {
				String name = "O"+node.getNumber();
				if(!map.containsKey(name)) {
					map.put(name, new HashSet<Node>());
				}
				map.get(name).add(n);
            }
          };
        PointsToSetInternal pti = (PointsToSetInternal)this.pta.reachingObjects(Locals.get(obj));
		pti.forall(vis);
	}
}

class MHP{
	Node head;
	Queue<Node> workList;
	Map<String, Local> Locals;
	Map<String, SootField> Fields;
	Map<String, Set<Node>> N;
	Map<Node, String> thread;
	Map<String, Set<Node>> notifyNodes;
	Map<String, Set<Node>> waitingNodes;
	Map<String, Set<Node>> monitorObj;
	PointsToAnalysis pta;
	
	MHP(){
		this.workList = new LinkedList<>();
		this.Locals = new HashMap<String, Local>();
		this.Fields = new HashMap<String, SootField>();
		this.N = new HashMap<String, Set<Node>>();
		this.thread = new HashMap<Node, String>();
		this.notifyNodes = new HashMap<String, Set<Node>>();
		this.waitingNodes = new HashMap<String, Set<Node>>();
		this.pta = Scene.v().getPointsToAnalysis();
	}
	
	void init() {
		Body m = Scene.v().getMainMethod().retrieveActiveBody();
		SEG main = new SEG(m, "main");
		this.head = main.begin;
		this.workList.add(head);
		this.workList.addAll(main.startNodes);
		
		this.N.put("main", main.N);
		for(Node v: main.N) {
			thread.put(v, "main");
		}
		for(Object l: m.getLocals().toArray()) {
			Locals.put(l.toString(), (Local)l);
		}
		for(Object f: Scene.v().getSootClass(m.getMethod().getDeclaringClass().toString()).getFields().toArray()) {
			SootField sf = (SootField)f;
			Fields.put(sf.getName(), sf);
		}	
		for(Node n: main.startNodes) {
			addStart(n);
		}
		
		addMap(this.notifyNodes, main.notifyNodes);
		addMap(this.waitingNodes, main.waitingNodes);
		addMap(this.monitorObj, main.monitorObj);
		
//		for(Map.Entry<String, Set<Node>> e: main.monitorObj.entrySet()) {
//			System.out.println(e.getKey());
//			for(Node n: e.getValue()) {
//				System.out.println(n.unit);
//			}
//		}
	}
	
	void addMap(Map<String, Set<Node>> map1, Map<String, Set<Node>> map2) {
		for(Map.Entry<String, Set<Node>> e: map2.entrySet()) {
			System.out.println(e.getKey());
			if(!map1.containsKey(e.getKey())) {
				map1.put(e.getKey(), new HashSet<Node>());
			}
			
			map1.get(e.getKey()).addAll(e.getValue());
		}
	}
	
	void addStart(Node n) {
		P2SetVisitor vis = new P2SetVisitor() {
			@Override
            public void visit(soot.jimple.spark.pag.Node node) {
				String name = "O"+node.getNumber();
				String className = node.getType().toString();
				Body t = Scene.v().getSootClass(className).getMethodByName("run").retrieveActiveBody();
				SEG th = new SEG(t, name);
				
				th.begin.StartPred.add(n);
				n.StartSucc.add(th.begin);
				
				N.put(name, th.N);
				for(Node v: th.N) {
					thread.put(v, name);
				}
				for(Object l: t.getLocals().toArray()) {
					Locals.put(l.toString(), (Local)l);
				}
				for(Object f: Scene.v().getSootClass(className).getFields().toArray()) {
					SootField sf = (SootField)f;
					Fields.put(sf.getName(), sf);
				}
				
				addMap(notifyNodes, th.notifyNodes);
				addMap(waitingNodes, th.waitingNodes);
				addMap(monitorObj, th.monitorObj);
            }
          };
        PointsToSetInternal pti = (PointsToSetInternal)this.pta.reachingObjects(Locals.get(n.object));
		pti.forall(vis);
	}
	
	Boolean alias(String s1, String s2) {
		
		Pair<String, String> p1 = getSplit(s1);
		Pair<String, String> p2 = getSplit(s2);
		Local l1 = this.Locals.get(p1.getKey());
		SootField f1 = this.Fields.get(p1.getValue());
		Local l2 = this.Locals.get(p2.getKey());
		SootField f2 = this.Fields.get(p2.getValue());
		
		return pta.reachingObjects(l1, f1).hasNonEmptyIntersection(pta.reachingObjects(l2, f2));
	}
	
	Pair<String,String> getSplit(String s) {
		String[] arr = s.split(":", 2);
		Pair<String, String> p = new Pair<String,String>(arr[0], arr[1]);
		return p;
	}
	
	void algorithm() {
		while(!this.workList.isEmpty()) {
			Node n = this.workList.poll();
			Set<Node> M_old = new HashSet<Node>(n.M);
			Set<Node> OUT_old = new HashSet<Node>(n.OUT);
			Set<Node> NotifySucc_old = new HashSet<Node>(n.NotifySucc);
			//Calc M
			
		}
	}
}

public class MhpAnalysis extends SceneTransformer{

	@Override
	protected void internalTransform(String phaseName, Map<String, String> options) {
		/*
		 * Implement your mhp analysis here
		 */
		MHP mhp = new MHP();
		mhp.init();
	}	
}

	

