public class P4 {
	void m1(){
		A x,y,z,p;
		x = new A();
		boolean flag = true;
		p = x;
		if(flag){
			x = new A();
			y = x;
		}
		else{
			x = new A();
			z = x;
		}
	}
}

class A{
	A f;
}
