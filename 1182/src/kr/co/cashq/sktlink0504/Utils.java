package kr.co.cashq.sktlink0504;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * 공통 Utils 함수 제공 오류코드 최고값: ErrPOS074
 * 
 * @author pgs
 * 
 */
public class Utils {

	private final static SimpleDateFormat sdf_yyyymmddhhmmss = new SimpleDateFormat(
			"yyyyMMddHHmmss");
	private final static SimpleDateFormat sdf_yyyy_mm_dd_hhmmss = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");

	// private final static SimpleDateFormat sdf_MM = new
	// SimpleDateFormat("MM");
	private final static SimpleDateFormat sdf_YMD = new SimpleDateFormat(
			"yyyyMMdd");
	private final static SimpleDateFormat sdf_HH = new SimpleDateFormat("HH");
	private final static SimpleDateFormat sdf_YMDH = new SimpleDateFormat(
			"yyyyMMddHH");
	private final static SimpleDateFormat sdf_YYYYMM = new SimpleDateFormat(
			"yyyyMM");

	private static String strHandler_pre = "";
	private static Logger logger = null;
	private static boolean checked_logs;

	/**
	 * 로거를 리턴한다.
	 * new File을 하기 전에 SKT01.strPrePath를 더해야 함에 유의해야 한다.
	 * linux에서 cd경로 문제 때문에
	 * 
	 * @return
	 */
	public static Logger getLogger() {
		String strHandler = getLoggerFilePath();

		File logsf = new File(SKT01.strPrePath + "logs");// logs폴더

		if (checked_logs == false && logsf.exists() == false) {
			logsf.mkdirs();
		}
		checked_logs = true;

		if (!strHandler_pre.equals(strHandler)) {
			strHandler_pre = strHandler;

			logger = Logger.getLogger(strHandler);
			logger.setLevel(Level.ALL);

			FileHandler fh = null;
			try {
				fh = new FileHandler(strHandler, true);
			} catch (SecurityException e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
			} catch (IOException e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
			}
			logger.addHandler(fh);
			logger.setLevel(Level.ALL);
			SimpleFormatter sf = new SimpleFormatter();
			fh.setFormatter(sf);
		}

		return logger;
	}

	/**
	 * pathSeparatorChar 는 윈도우에서는 ;이고, 리눅스에서는 :이다. separatorChar는 윈도우에서는 \이고
	 * 리눅스에서는 /이다.
	 * 
	 * @return
	 */
	public static String getLoggerFilePath() {
		return SKT01.strPrePath + "logs" + File.separatorChar + "log" + getYMD() + ".txt";
	}

	/**
	 * 8자리 날짜를 리턴한다. 예) 20160718
	 * 
	 * @return
	 */
	public static String getYMD() {
		// 해당월의 값을 리턴한다.
		Date now = new Date();
		String strDate = sdf_YMD.format(now);
		return strDate;
	}

	/**
	 * 해당년월의 값을 리턴한다. 예: 201607
	 * 
	 * @return
	 */
	public static String getYYYYMM() {
		// 해당년월의 값을 리턴한다.
		Date now = new Date();
		String strDate = sdf_YYYYMM.format(now);
		return strDate;
	}

	/**
	 * 년월일시간정보를 리턴한다. 예:)2016071817
	 * 
	 * @return
	 */
	public static String getYMDH() {
		Date now = new Date();
		String strDate = sdf_YMDH.format(now);
		return strDate;
	}

	/**
	 * 현재시간을 24시간 형식으로 리턴한다.
	 * 
	 * @return
	 */
	public static String getHH() {
		Date now = new Date();
		String strDate = sdf_HH.format(now);
		return strDate;
	}

	/**
	 * 숫자를 알파벳 암호화 한다. 9 -> z로 바꾼다.
	 * 
	 * @param str0504
	 * @return
	 */
	public static String encrypt0504(String str0504) {
		byte[] src = str0504.substring(3).getBytes();
		for (int i = 0; i < src.length; i++) {
			src[i] += 65;
		}
		return new String(src);
	}

	/**
	 * 공백의 수만큼의 문자열을 리턴한다.
	 * 
	 * @param cnt
	 * @return
	 */
	public static String space(int cnt) {
		if (cnt < 0)
			throw new RuntimeException(
					"space parameter is negative value error!");
		String val = "";
		for (int i = 0; i < cnt; i++) {
			val += Env.SPACE;
		}
		return val;
	}

	/**
	 * 문자열의 길이를 리턴한다.
	 * 
	 * @param str
	 * @return
	 */
	public static int getLen(String str) {
		return str.length();
	}

	/**
	 * 주어진 길이만큼 공백으로 채운 후 문자열을 왼쪽정렬로 하여 출력하여 결국 주어진 길이만큼의 길이로 리턴한다.
	 * 
	 * @param len
	 * @param str
	 * @return
	 */
	public static String paddingLeft(int len, String str) {
		String retVal = "";
		try {
			retVal = str + space(len - getLen(str));
		} catch (RuntimeException e) {
			Utils.getLogger().warning(e.getMessage());
			DBConn.latest_warning = "ErrPOS073";
		}
		return retVal;
	}

	/**
	 * substring연산을 수행한 후 trim으로 공백을 제거한 값을 리턴한다.
	 * 
	 * @param str
	 * @param s
	 * @param e
	 * @return
	 */
	public static String substringVal(String str, int s, int e) {
		return str.substring(s, e).trim();
	}

	/**
	 * Mysql datatime 자료형으로 처리할 수 있는 날짜로 변경한다. yyyyMMddHHmmss를 날짜로 변경한 후 날짜를
	 * MySql datetime처리 가능한 문자열로 변경한다.
	 * 
	 * @param str
	 * @return
	 */
	public static String toDate(String str) {
		String dt = null;
		if (!"".equals(str)) {
			Date date = null;
			try {
				date = sdf_yyyymmddhhmmss.parse(str);
			} catch (ParseException e) {
				Utils.getLogger().warning(e.getMessage());
				DBConn.latest_warning = "ErrPOS074";
				e.printStackTrace();
			}
			dt = sdf_yyyy_mm_dd_hhmmss.format(date);
		}
		return dt;
	}

	/**
	 * 스택오류를 문자열로 출력한다.
	 * 
	 * @param e
	 * @return
	 */
	public static String stack(Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		return sw.toString();
	}

	/**
	 * 스택오류를 문자열로 출력한다.
	 * 
	 * @param e
	 * @return
	 */
	public static String stack(Error e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		return sw.toString();
	}

	/**
	 * 현재의 요일을 리턴한다. 1:일, 2:월, 3:화, 4:수, 5:목, 6:금, 7:토
	 * 
	 * @return
	 */
	public static int getWeekDay() {
		Calendar oCalendar = Calendar.getInstance(); // 현재 날짜/시간 등의 각종 정보 얻기
		return oCalendar.get(Calendar.DAY_OF_WEEK);
	}

	public static String substring(String msg, int ifrom, int ito) 
	{
		int len = msg.length();
		if(len < ito) {
			ito = len;
		}
		return msg.substring(ifrom, ito);
	}
	


}
