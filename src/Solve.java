import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
public class Solve {
	static ArrayList<Point> points=new ArrayList<Point>();
	static Point center=new Point(0,0);
	static double radius=0;
	static double radius2=0;
	static boolean bool = false;
	static double r;
	public static String solve(List<Base_Station> stations, boolean rxl) throws IOException {
		points=new ArrayList<Point>();
		center=new Point(0,0);
		radius=0;
		radius2=0;
		bool = false;
		for(int i=0;i<stations.size();i++) {
			if(rxl&&stations.get(i).R>=4*550)
				continue;
			for(int j=i+1;j<stations.size();j++) {
				newPoints(stations.get(i),stations.get(j),false,false);
				newPoints(stations.get(i),stations.get(j),true,false);
				newPoints(stations.get(i),stations.get(j),false,true);
				newPoints(stations.get(i),stations.get(j),true,true);
			}
			points.add(new Point(stations.get(i).coord0.x,stations.get(i).coord0.y-stations.get(i).R-550));
			points.add(new Point(stations.get(i).coord0.x+stations.get(i).R+550,
					stations.get(i).coord0.y/Math.cos(stations.get(i).coord0.x/r)*Math.cos((stations.get(i).coord0.x+stations.get(i).R+550)/r)));
			points.add(new Point(stations.get(i).coord0.x,stations.get(i).coord0.y+stations.get(i).R+550));
			points.add(new Point(stations.get(i).coord0.x-stations.get(i).R-550,
					stations.get(i).coord0.y/Math.cos(stations.get(i).coord0.x/r)*Math.cos((stations.get(i).coord0.x-stations.get(i).R-550)/r)));
		}
		
		for(Base_Station station:stations) { 
			deletePoints(station);
		}
		
		
		String s="Point 0 0 0";
		if(points.isEmpty()) {
			if(stations.size()==1) {
				center=stations.get(0).coord0;
				radius=stations.get(0).R;
				radius2=stations.get(0).R+stations.get(0).deltaR;
				bool=true;
				s=(radius==0?"Circle "+center.x+" "+center.y+" "+radius2:"Bamble "+center.x+" "+center.y+" "+radius+" "+radius2);
			}
		}
		else {
			radius();
			s=(radius==0?"Point "+center.x+" "+center.y+" "+0:"Circle "+center.x+" "+center.y+" "+radius);
		}
		FileWriter fw = new FileWriter("Points.txt",false);
		for(Point p:points) {
			try {
				double x=p.x/r;
				fw.write(Math.toDegrees(x)+" " + Math.toDegrees(p.y/r/Math.cos(x)) +"\r\n");
			} catch (IOException ignored) {}
		}
		fw.close();
		if(!rxl) {
			bool=false;
		}
		
		return s;
		
	}
	public static String fullSearchRxl(List<Base_Station> stations) {
		double R_max=0;
		Point mcenter = new Point(0,0);
		try {
			Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();
			Connection connection=DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/test?serverTimezone=UTC","root","root");
			
			Base_Station stj = null;
			List<Base_Station> sts = new ArrayList<Base_Station>();
			for(int i=0;i<stations.size();i++) {
				if(stations.get(i).R<3*550&&stations.size()>2){
					stj=new Base_Station(stations.get(i).data);
					stj.recovering();
					stj.coord0=stations.get(i).coord0;
					sts.add(stj);
				}
			}
			Collections.sort(sts, new Comparator<Base_Station>(){
				@Override
				public int compare(Base_Station o2, Base_Station o1) {
					return (o1.Rxl>o2.Rxl?1:(o1.Rxl<o2.Rxl?-1:0));
				}
			});
			int j=sts.size();
			int k=j;
			int Rxl_max=sts.get(0).Rxl;
			stj=null;
			while(sts.size()>1) {
				int delta=0;
				while(delta<=70-Rxl_max) {
					for(Base_Station station:sts) {
						String query="SELECT destination, error FROM table_of_equals WHERE ASU="+(station.Rxl+delta<18?18:(station.Rxl+delta>70?70:station.Rxl+delta))+" AND GSM="+(station.Arfc>120?1800:900);
						ResultSet rs=connection.createStatement().executeQuery(query);
						if(rs.next()) {
							station.R=rs.getInt("destination")-rs.getInt("error")/2;
							station.deltaR=rs.getInt("error");
						}
					}
					solve(sts, true);
					delta++;
					if(radius>R_max) {
						R_max=radius;
						mcenter=center;
					}	
				}
				j--;
				if(k-j>1) {
					sts.add(j+1, stj);
				}
				if(R_max!=0&&(j==-1||sts.size()==k))
					break;
				if(j==-1) {
					sts.remove(sts.size()-1);
					j=sts.size()-1;
				}
				stj = new Base_Station(sts.get(j).data);
				stj.recovering();
				stj.coord0=sts.get(j).coord0;
				sts.remove(j);
			}
			for(Base_Station station:stations) {
				for(Base_Station station0:sts) {
					if(station0.CID==station.CID) {
						station.R=station0.R;
						break;
					}
				}
			}
			connection.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return (R_max==0?"Point "+mcenter.x+" "+mcenter.y+" "+0:"Circle "+mcenter.x+" "+mcenter.y+" "+R_max);
	}
	public static String searchRxl(List<Base_Station> stations, List<Base_Station> stationsOK) {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();
			Connection connection=DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/test?serverTimezone=UTC","root","root");
			int delta=0;
			for(Base_Station station:stationsOK) {
				int ASU;
				if(station.Arfc>120)
					ASU=58-31*station.R/550;
				else
					ASU=63-35*station.R/550;
				delta+=ASU-station.Rxl;
			}
			delta=-delta/stationsOK.size();
			for(Base_Station station:stations) {
				String query="SELECT destination, error FROM table_of_equals WHERE ASU="+(station.Rxl+delta<18?18:(station.Rxl+delta>70?70:station.Rxl+delta))+" AND GSM="+(station.Arfc>120?1800:900);	
				ResultSet rs=connection.createStatement().executeQuery(query);
					if(rs.next()&&station.R<4*550) {
						station.R=rs.getInt("destination")-rs.getInt("error")/2;
						station.deltaR=rs.getInt("error");
					}
					rs.close();
			}
			solve(stations, true);
			connection.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return (radius2==0?(radius==0?"Point "+center.x+" "+center.y+" "+0:"Circle "+center.x+" "+center.y+" "+radius):
			(radius==0?"Circle "+center.x+" "+center.y+" "+radius2:"Bamble "+center.x+" "+center.y+" "+radius+" "+radius2));
	}
	public static void radius() {
		double d1=0;
		double d2=0;
		/*double a1=0;
		double b1=0;
		double a2=0;
		double b2=0;
		double c1=0;
		double c2=0;*/
		Point p1=points.get(0);
		Point p2=new Point(0,0);
		Point p3=p2;
		/*for(Point p:points) {
			for(Point p0:new ArrayList<Point>(points.subList(points.indexOf(p)+1, points.size()))) {
				double des=Math.sqrt((p.x-p0.x)*(p.x-p0.x)+(p.y-p0.y/Math.cos(p0.x/r)*Math.cos(p.x/r))*(p.y-p0.y/Math.cos(p0.x/r)*Math.cos(p.x/r)))/2;
				if(des>d2) {
					if(des>d1) {
						a2=a1;
						b2=b1;
						c2=c1;
						a1=p.x-p0.x;
						b1=p.y-p0.y/Math.cos(p0.x/r)*Math.cos(p.x/r);
						c1=b1*(p.y-(b1)/2)+a1*(p.x-a1/2);
						d2=d1;
						d1=des;
					}
					else {
						d2=des;
						a2=p.x-p0.x;
						b2=p.y-p0.y/Math.cos(p0.x/r)*Math.cos(p.x/r);
						c2=b2*(p.y-(b2)/2)+a2*(p.x-a2/2);
					}
				}
			}
		}
		if(a1==0&&b1==0) {
			d1=points.get(0).x;
			d2=points.get(0).y;
		}
		else {
			if(a2==0&&b2==0) {
				d1=points.get(0).x-(points.get(0).x-points.get(1).x)/2;
				d2=points.get(0).y-(points.get(0).y-points.get(1).y)/2;
			}
			else {
				d1=(c2*b1-c1*b2)/(a2*b1-a1*b2);
				d2=(b1==0?(b2==0?points.get(0).y:(c2-a2*d1)/b2):(c1-a1*d1)/b1);
			}
		}*/
		double dmax=0;
		for(Point p:points) 
			for(Point p0:new ArrayList<Point>(points.subList(points.indexOf(p)+1, points.size()))) {
				double des=Math.sqrt((p.x-p0.x)*(p.x-p0.x)+(p.y-p0.y/Math.cos(p0.x/r)*Math.cos(p.x/r))*(p.y-p0.y/Math.cos(p0.x/r)*Math.cos(p.x/r)));
				if(des>dmax) {
					dmax=des;
					p1=p;
					p2=p0;
				}
				
			}
		dmax=0;
		for(Point p0:points) {
			if(p0==p1||p0==p2)
				continue;
			double des=Math.sqrt((p1.x-p0.x)*(p1.x-p0.x)+(p1.y-p0.y/Math.cos(p0.x/r)*Math.cos(p1.x/r))*(p1.y-p0.y/Math.cos(p0.x/r)*Math.cos(p1.x/r)));
			if(des>dmax) {
				dmax=des;
				p3=p0;
			}
			des=Math.sqrt((p2.x-p0.x)*(p2.x-p0.x)+(p2.y-p0.y/Math.cos(p0.x/r)*Math.cos(p2.x/r))*(p2.y-p0.y/Math.cos(p0.x/r)*Math.cos(p2.x/r)));
			if(des>dmax) {
				dmax=des;
				p3=p0;
			}
		}
		if(p3.equals(new Point(0,0))) {
			if(p2.equals(p3)) {
				d1=p1.x;
				d2=p1.y;
			}
			else {
				d1=p1.x-(p1.x-p2.x)/2;
				d2=p1.y-(p1.y-p2.y/Math.cos(p2.x/r)*Math.cos(p1.x/r))/2;
				d2=d2/Math.cos(p1.x/r)*Math.cos(d1/r);
			}
		}
		else {
			Point pm1=new Point(p1.x-(p1.x-p2.x)/2,(p1.y-(p1.y-p2.y/Math.cos(p2.x/r)*Math.cos(p1.x/r))/2)/Math.cos(p1.x/r)*Math.cos((p1.x-(p1.x-p2.x)/2)/r));
			Point pm0=new Point(p1.x-(p1.x-p3.x)/2,(p1.y-(p1.y-p3.y/Math.cos(p3.x/r)*Math.cos(p1.x/r))/2)/Math.cos(p1.x/r)*Math.cos((p1.x-(p1.x-p3.x)/2)/r));
			double des23=Math.sqrt((p2.x-p3.x)*(p2.x-p3.x)+(p2.y-p3.y/Math.cos(p3.x/r)*Math.cos(p2.x/r))*(p2.y-p3.y/Math.cos(p3.x/r)*Math.cos(p2.x/r)));
			double des01=Math.sqrt((pm0.x-pm1.x)*(pm0.x-pm1.x)+(pm0.y-pm1.y/Math.cos(pm1.x/r)*Math.cos(pm0.x/r))*(pm0.y-pm1.y/Math.cos(pm1.x/r)*Math.cos(pm0.x/r)));
			double des20=Math.sqrt((p2.x-pm0.x)*(p2.x-pm0.x)+(p2.y-pm0.y/Math.cos(pm0.x/r)*Math.cos(p2.x/r))*(p2.y-pm0.y/Math.cos(pm0.x/r)*Math.cos(p2.x/r)));
			double k=des01/des23;
			double des=des20/(k+1);
			d1=p2.x-(p2.x-pm0.x)/des20*des;
			d2=(p2.y-(p2.y-pm0.y/Math.cos(pm0.x/r)*Math.cos(p2.x/r))/des20*des)/Math.cos(p2.x/r)*Math.cos(d1/r);
			/*a1=p1.x-(p1.x-p2.x)/2-p3.x;
			b1=p3.y/Math.cos(p3.x/r)*Math.cos(p1.x/r)-(p1.y-(p1.y-p2.y/Math.cos(p2.x/r)*Math.cos(p1.x/r))/2);
			c1=a1*p3.y/Math.cos(p3.x/r)*Math.cos(p1.x/r)+b1*p3.x;
			a2=p1.x-(p1.x-p3.x)/2-p2.x;
			b2=p2.y/Math.cos(p2.x/r)*Math.cos(p1.x/r)-(p1.y-(p1.y-p3.y/Math.cos(p3.x/r)*Math.cos(p1.x/r))/2);
			c2=a1*p2.y/Math.cos(p2.x/r)*Math.cos(p1.x/r)+b1*p2.x;
			d1=(c2*b1-c1*b2)/(a2*b1-a1*b2);
			d2=(b1==0?(b2==0?p1.y:(c2-a2*d1)/b2):(c1-a1*d1)/b1);
			d2=d2/Math.cos(p1.x/r)*Math.cos(d1/r);*/
		}
		center = new Point(d1,d2);
		for(Point p:points) {
			double des=Math.ceil(Math.sqrt((p.x-center.x)*(p.x-center.x)+(p.y/Math.cos(p.x/r)*Math.cos(center.x/r)-center.y)*(p.y/Math.cos(p.x/r)*Math.cos(center.x/r)-center.y)));
			radius=(des>radius?des:radius);
		}
		bool=true;
	}
	public static void newPoints(Base_Station station1,Base_Station station2,boolean in1,boolean in2) {
		int r1=(in1?station1.R+station1.deltaR:station1.R);
		int r2=(in2?station2.R+station2.deltaR:station2.R);
		double dx=station2.coord0.x-station1.coord0.x;
		double dy=station2.coord0.y/Math.cos(station2.coord0.x/r)*Math.cos(station1.coord0.x/r)-station1.coord0.y;
		//double des=Math.ceil(Math.sqrt((dx)*(dx)+(dy)*(dy)));
		//System.out.println(des);
		if(dx==0&&dy==0) {return;}
		double c=(r1*r1-r2*r2+dx*dx+dy*dy)/2;
		if(dy==0) {
			double x=c/dx;
			double y1=Math.sqrt(r1*r1-x*x);
			double y2=-y1;
			Point p1=new Point(x+station1.coord0.x,(y1+station1.coord0.y)/Math.cos(station1.coord0.x/r)*Math.cos((x+station1.coord0.x))/r);
			Point p2=new Point(x+station1.coord0.x,(y2+station1.coord0.y)/Math.cos(station1.coord0.x/r)*Math.cos((x+station1.coord0.x))/r);
			points.add(p1);
			points.add(p2);
			return;
		}
		double a=1+(dx*dx)/(dy*dy);	
		double b=(-2)*dx*c/(dy*dy);
		double d=4*(r1*r1*dx*dx+r1*r1*dy*dy-c*c)/(dy*dy);
		if(d>0) {
			double x1=(-b+Math.sqrt(d))/2/a;
			double x2=(-b-Math.sqrt(d))/2/a;
			double y1=(c-x1*dx)/dy;
			double y2=(c-x2*dx)/dy;
			Point p1=new Point(x1+station1.coord0.x, (y1+station1.coord0.y)/Math.cos(station1.coord0.x/r)*Math.cos((x1+station1.coord0.x)/r));
			Point p2=new Point(x2+station1.coord0.x, (y2+station1.coord0.y)/Math.cos(station1.coord0.x/r)*Math.cos((x2+station1.coord0.x)/r));
			points.add(p1);
			points.add(p2);
		}
	}
	public static void deletePoints(Base_Station station) {
		ArrayList<Point> points0=new ArrayList<Point>();
		for(Point p:points) {
			double ri=Math.round(Math.sqrt((p.x-station.coord0.x)*(p.x-station.coord0.x)+(p.y*Math.cos(station.coord0.x/r)/Math.cos(p.x/r)-station.coord0.y)*(p.y*Math.cos(station.coord0.x/r)/Math.cos(p.x/r)-station.coord0.y)));
			if(ri<=station.R+station.deltaR&&ri>=station.R)
				points0.add(p);
		}
		points.clear();
		points=points0;
	}
}

class Point{
	double x;
	double y;
	Point(double x, double y){
		this.x=x;
		this.y=y;
	}
}
class Base_Station{
	String MCC;
	String MNC;
	String LAC;
	String CID;
	int Rxl;
	int RxlR;
	int R;
	int deltaR=550;
	int Arfc;
	Point coord0;
	String[] data;
	Base_Station(String[] base_data){
		/*coords base station from Yandex / Google with LAC and CID*/
		this.data=base_data;
	}
	public void recovering() {
		this.MCC=data[0];
		this.MNC=data[1];
		this.Rxl=(Integer.parseInt(data[2])>70?70:Integer.parseInt(data[2]));
		this.CID=data[3];
		this.Arfc=Integer.parseInt(data[4]);
		this.LAC=data[5];
		if(!data[6].equals("-1"))
			this.R=Integer.parseInt(data[6])*550;
		this.deltaR=550;
	}
}