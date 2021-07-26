
public class hw {
	public static void main(String[] args) {
		long a = 6378137;
		long b = 6356752;
		double phi = Math.toRadians(89);
		double ri=a*b/Math.sqrt(b*b*Math.cos(phi)*Math.cos(phi)+a*a*Math.sin(phi)*Math.sin(phi));
		double r=Math.sqrt(((a*a*Math.cos(phi))*(a*a*Math.cos(phi))+(b*b*Math.sin(phi))*(b*b*Math.sin(phi)))/((a*Math.cos(phi))*(a*Math.cos(phi))+(b*Math.sin(phi))*(b*Math.sin(phi))));
		System.out.println(r+" "+ri);
	}
}
