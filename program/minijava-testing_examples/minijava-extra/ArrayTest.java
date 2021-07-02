class ArrayTest{
    public static void main(String[] a){
        boolean n;
		int i; 
		i=0;
        n = new Test().start(i);
    }
}

class Test {

	public boolean start(int sz){
		int[] b; 
		int l;
		int i;
		b = new int[5];
		l = b.length;
		i = 0;
		while(i < (l)){
			b[i] = i;
			System.out.println(b[i]);
			i = i + 1;
		}
		return true;
	}

}
