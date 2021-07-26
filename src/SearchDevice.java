import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class SearchDevice {
	private static final String DB_Driver = "com.mysql.cj.jdbc.Driver";
	private static final String DB_URL = "jdbc:mysql://127.0.0.1:3306/test?serverTimezone=UTC";
	static Connection connection;
	static double r=0;
	static String sp="";
	static String file = "BASES.txt";
	public static void main(String[] args) throws IOException {
		long start = new Date().getTime();
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		searchDevice(reader.readLine());
		/*System.out.println("The method of determining: "+sp);
		double x = Double.valueOf(answerStr[1])/r;
		double y = Double.valueOf(answerStr[2])/r/Math.cos(x);
		switch(answerStr[0]) {
		case "Point":{System.out.println("Location: "+new DecimalFormat("#0.0000000").format(Math.toDegrees(x))
				+" "+new DecimalFormat("#0.0000000").format(Math.toDegrees(y)));break;}
		case "Circle":{System.out.println("Location: "+new DecimalFormat("#0.0000000").format(Math.toDegrees(x))+" "
				+new DecimalFormat("#0.0000000").format(Math.toDegrees(y))+"\n"+"Radius: "+ Double.valueOf(answerStr[3]));break;}
		case "Bamble":{System.out.println("Location: "+new DecimalFormat("#0.0000000").format(Math.toDegrees(x))+" "
				+new DecimalFormat("#0.0000000").format(Math.toDegrees(y))+"\n"+"Radius1: "+ Double.valueOf(answerStr[3])+"\n"+"Radius2: "+ Double.valueOf(answerStr[4]));break;}
		default:{break;}
		}
		*/
		//String uri=new DecimalFormat("#0.0000000").format(Math.toDegrees(x)).replace(",", ".")+","+new DecimalFormat("#0.0000000").format(Math.toDegrees(y)).replace(",", ".");
		
		//java.awt.Desktop.getDesktop().browse(new URI("https://www.google.ru/maps/place/"+uri));
		java.awt.Desktop.getDesktop().browse(new File("JSmap.html").toURI());
		System.out.println("Timework: "+(new Date().getTime()-start)/1000 +"s");
	}
	public void connectDB() {
		try {
			Class.forName(DB_Driver).getDeclaredConstructor().newInstance();
			connection = DriverManager.getConnection(DB_URL);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public static void write(String[] args) {
		try {
			String sql = "INSERT base_station(id_device,datetime,data_bs) VALUES ("+args[0]+","+args[1]+","+args[2]+")";
			connection.prepareStatement(sql).execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static void searchDevice(String arg) throws IOException{
		String[] bases = arg.split(";");
		String[] realCoords=bases[bases.length-1].split(",");
		List<Base_Station> stations = new ArrayList<Base_Station>();
		for(String base:bases){
			if(bases[bases.length-1]==base)
				break;
			String[] base_data=base.split(",");
			base_data[6]=(base_data[6].equals("-1")?String.valueOf(-1):base_data[6]);
			Base_Station station=new Base_Station(base_data);
			station.recovering();
			stations.add(station);
		}
		
		List<Base_Station> stationsOK = new ArrayList<Base_Station>();
		ArrayList<String> CIDs = new ArrayList<String>(stations.size());
		for(Base_Station station:stations) {
			String bs_number = String.valueOf(Integer.parseInt(station.CID, 16)/10); //РїРѕСЃР»РµРґРЅСЏСЏ С†РёС„СЂР° CID - РЅРѕРјРµСЂ РёР·Р»СѓС‡Р°С‚РµР»СЏ
			if(!CIDs.contains(bs_number)) {
				CIDs.add(bs_number);
				stationsOK.add(station);
			}
			else {
				if(station.Rxl>stationsOK.get(CIDs.indexOf(bs_number)).Rxl) {
					stationsOK.set(CIDs.indexOf(bs_number),station);
				}
			}
		}
		CIDs.clear();
		stations.clear();
		Collections.sort(stationsOK, new Comparator<Base_Station>(){
			@Override
			public int compare(Base_Station o2, Base_Station o1) {
				return (o1.Arfc>o2.Arfc?1:(o1.Arfc<o2.Arfc?-1:0));
			}
		});
		stationsOK=stationsOK.subList(0, (stationsOK.size()>=6?6:stationsOK.size()));//РѕСЃС‚Р°РІР»СЏРµРј 6
		new FileWriter(file,false);
		for(Base_Station station:stationsOK) {
			Base_Station bs = new Base_Station(station.data);
			bs.recovering();
			stations.add(bs);
		}
		result(true, stationsOK);
		stationsOK=stations;
		result(false, stationsOK);
		FileWriter fw = new FileWriter(file,true);
		fw.write(realCoords[0]+" "+realCoords[1]);
		fw.close();
	}
	
	public static void result(boolean choice, List<Base_Station> stationsOK) throws IOException {
		List<Base_Station> stations = new ArrayList<Base_Station>();
		long a = 6378137;
		long b = 6356752;
		boolean def=true;
		double cosPhi=0;
		for(Base_Station station:stationsOK) {
			String coord=(choice?getYandexCoord(station.MCC,station.MNC,station.LAC,station.CID)+" Y":getGoogleCoord(station.MCC,station.MNC,station.LAC,station.CID)+" G");
			//System.out.println("Base Station "+station.CID+" coords: " +coord);
			def=(coord.equals(" Y")?false:def);
			def=(coord.equals(" G")?false:def);
			try {
				FileWriter fw = new FileWriter(file,true);
				fw.write(station.CID+" " +new DecimalFormat("#0.0000000").format(Double.valueOf(coord.split(" ")[0])).replace(",", ".")+" "+new DecimalFormat("#0.0000000").format(Double.valueOf(coord.split(" ")[1])).replace(",", ".") +"\r\n");
				fw.close();
			} catch (IOException ignored) {}
			double phi = Math.toRadians(Double.valueOf(coord.split(" ")[0]));
			double psi = Math.toRadians(Double.valueOf(coord.split(" ")[1]));
			//if(cosPhi==0) {
			r=a*b/Math.sqrt(b*b*Math.cos(phi)*Math.cos(phi)+a*a*Math.sin(phi)*Math.sin(phi));
				cosPhi = Math.cos(phi);
			//}
			station.coord0=new Point(phi*r,cosPhi*psi*r);
			
			//System.out.println("Base Station "+station.CID+" coords: " +station.coord0.x + ' ' + station.coord0.y+' '+cosPhi+' '+0.5022231252570837*psi*r);
			
			if(station.R>=0) {
				stations.add(station);
			}
		}
		Solve.r=r;
		String result = "Point 0 0 0";
		if(def) {
			if(stationsOK.size()==stations.size()) {
				result = Solve.solve(stationsOK, false);
				sp="RADIUS";
			}
			if(Double.valueOf(result.split(" ")[1])==0&&Double.valueOf(result.split(" ")[2])==0&&!stations.isEmpty()) {			
				result = Solve.searchRxl(stationsOK, stations);
				sp="Rxl & RADIUS";
			}
			if(Double.valueOf(result.split(" ")[1])==0&&Double.valueOf(result.split(" ")[2])==0) {			
				result = Solve.fullSearchRxl(stationsOK);
				sp="Rxl";
			}
			System.out.println("Successful");
		}
		FileWriter fw = new FileWriter(file,true);
		for(Base_Station station:stationsOK) {
			try {
				fw.write(station.CID+" " + station.R +" "+ station.deltaR +"\r\n");
			} catch (IOException ignored) {}
		}
		String[] answerStr = result.split(" ");
		double x = Double.valueOf(answerStr[1])/r;
		double y = Double.valueOf(answerStr[2])/r/Math.cos(x);
		fw.write(new DecimalFormat("#0.0000000").format(Math.toDegrees(x)).replace(",", ".")+" "+new DecimalFormat("#0.0000000").format(Math.toDegrees(y)).replace(",", ".")+" "+ Double.parseDouble(answerStr[3])+"\r\n");
		fw.close();
		answerStr[1]=new DecimalFormat("#0.0000000").format(Math.toDegrees(x)).replace(",", ".");
		answerStr[2]=new DecimalFormat("#0.0000000").format(Math.toDegrees(y)).replace(",", ".");
	}
	public static String getYandexCoord(String MCC, String MNC, String LAC, String CID) {
		String res="";
		try {
			URLConnection connection = new URL("http://mobile.maps.yandex.net/cellid_location/?&cellid="+Integer.parseInt(CID,16)
					+"&operatorid="+Integer.parseInt(MNC)+"&countrycode="+Integer.parseInt(MCC)+"&lac="+Integer.parseInt(LAC,16)).openConnection();
			BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String line;
			StringBuilder sb = new StringBuilder();
			while((line = br.readLine())!=null) {
				sb.append(line);
			}
			int begin = sb.toString().indexOf("latitude")+10;
			int end = sb.toString().indexOf(" ", begin)-1;
			res=sb.toString().substring(begin,end)+" ";
			begin = sb.toString().indexOf("longitude")+11;
			end = sb.toString().indexOf(" ", begin)-1;
			res+=sb.toString().substring(begin,end);
		} catch (IOException e) {
			System.out.println(e);
		}
		return res;
	}
	public static String getGoogleCoord(String MCC, String MNC, String LAC, String CID) {
		String res="";
		try {
			URLConnection connection = new URL("http://www.google.com/glm/mmap").openConnection();
			connection.setDoOutput(true);
			connection.connect();
			OutputStream out = connection.getOutputStream();
			writePlainData(out,Integer.parseInt(CID,16),Integer.parseInt(LAC,16),Integer.parseInt(MCC),Integer.parseInt(MNC));
			DataInputStream dis = new DataInputStream(connection.getInputStream());
			dis.readShort();
			dis.readByte();
			if(dis.readInt()==0) {
				double lat = (double)dis.readInt()/1000000;
				double lon = (double)dis.readInt()/1000000;
				res = String.valueOf(lat)+" "+String.valueOf(lon);
			}			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return res;
	}
	protected static void writePlainData(OutputStream out, int CID, int LAC, int MCC, int MNC) throws IOException //copy - paste Stadnichenko 
    {                 
        DataOutputStream dos = new DataOutputStream(out);
        dos.writeShort(0x0E); // Fct code                  
        dos.writeInt(0); // requesting 8 byte session                 
        dos.writeInt(0);                  
        dos.writeShort(0); // country code string                 
        dos.writeShort(0); // client descriptor string                 
        dos.writeShort(0); // version tag string                  
        dos.writeByte(0x1B); // Fct code                  
        dos.writeInt(0); // MNC?                 
        dos.writeInt(0); // MCC?                 
        dos.writeInt(3); // Radio Access Type (3=GSM, 5=UMTS)                  
        dos.writeShort(0); // length of provider name                  
        // provider name string                 
        dos.writeInt(CID); // CID                 
        dos.writeInt(LAC); // LAC                 
        dos.writeInt(MNC); // MNC                 
        dos.writeInt(MCC); // MCC                 
        dos.writeInt(-1); // always -1                 
        dos.writeInt(0); // rx level                  
        dos.flush();         
    }  
}