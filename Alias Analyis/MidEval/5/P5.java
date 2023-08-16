public class P5 {
	void m1(){
		A x,y,z;
		x = new A();
		boolean flag = true;
		if(flag){
			y = new A();
			x = x.foo(2);
		}
		else{
			z = x;
		}
	}

	void m2(){
		A x,y;
		x = new A();
		int i = 3;
		y = x.foo(i);
	}
}

class A{
	A f;
	int n;
	A foo(int i){
		A a = new A();
		a.n = i;
		return a;
	}
}
