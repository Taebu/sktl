package kr.co.cashq.sktlink0504;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.SQLException;

/**
 * 콜로그관련된 업무로직을 처리한다. safen_cdr 테이블과 관련된다.
 * 
 * @author pgs
 * 
 */
public class CallLogWorker {
	public static int nLogonAndCon = 0;//0:최초(날짜가 바뀌면 0으로 변경함), 1: 연결 성공 그리고 로그인 성공, 2:연결실패 혹은 로그인 실패	
	public static String strYMD_success = "";//SMS전송한 날짜 매주 금요일 오전 10시에 한 번만 보냄<nLogonAndCon변수와 관계가 있다.>
	
	public static String strYMD_fail = "";//SMS전송실패한 날짜 매일 한 번 <nLogonAndCon 변수와 관계가 있다>
	
	private static final int PACKET_LENGTH_8192 = 8192;
	private static final int N149 = 149;
	private static final int N24 = 24;
	private Socket socket;
	private BufferedInputStream bis;
	private BufferedOutputStream bos;
	private static CallLogWorker Instance;
	private boolean isConnected = false;
	private boolean isLogon;

	/** Creates a new instance of Worker */
	private CallLogWorker() {
		socket = null;
		bis = null;
		bos = null;
	}

	/**
	 * CallLog서버에 연결한다. 성공시 true를 리턴한다.
	 * 
	 * @param host
	 * @param port
	 * @return
	 */
	private boolean connect2CallLogServer(String host, int port) {
		try {
			InetAddress netHost;
			netHost = InetAddress.getByName(host);

			socket = new Socket(netHost, port);

			bis = new BufferedInputStream(socket.getInputStream());
			bos = new BufferedOutputStream(socket.getOutputStream());
			isConnected = true;
			nLogonAndCon = 1;
		} catch (UnknownHostException e) {
			Utils.getLogger().warning(e.getMessage());
			isConnected = false;
			nLogonAndCon = 2;
		} catch (IOException e) {
			Utils.getLogger().warning(e.getMessage());
			isConnected = false;
			nLogonAndCon = 2;
		} catch (Exception e) {
			Utils.getLogger().warning(e.getMessage());
			Utils.getLogger().warning(Utils.stack(e));
			isConnected = false;
			nLogonAndCon = 2;
		}
		Utils.getLogger().info(":connect:" + isConnected);
		return isConnected;
	}

	/**
	 * 메세지를 보낸다.
	 * 
	 * @param message
	 */
	private void sendMessage(String message) {
		try {
			if (isConnected == false) {
				connect2CallLogServer(Env.getInstance().CALL_LOG_SERVER_IP,
						Env.getInstance().CALL_LOG_SERVER_PORT);
			}
			if (isConnected) {
				bos.write(message.getBytes());
				bos.flush();
				Utils.getLogger().info("send:[" + message + "]");
			}
		} catch (IOException ioe) {
			Utils.getLogger().warning(ioe.getMessage());
			DBConn.latest_warning = "ErrPOS001";
		}
	}

	/**
	 * 메세지를 수신한다.
	 * 
	 * @return
	 */
	public String receiveMessage() {
		StringBuffer strbuf = new StringBuffer();
		try {
			if (isConnected) {
				byte[] buf = new byte[PACKET_LENGTH_8192];

				int read = 0;
				if ((read = bis.read(buf)) > 0) {// 여기서 응답없는 현상이 생길 수 있다. 특히 두번째
													// 진입시
													// 그럴 수 있다.
					String str = new String(buf);
					Utils.getLogger().info("rcv:[" + str.trim() + "]");
					strbuf.append(new String(buf, 0, read));
				}
			}
		} catch (IOException e) {
			Utils.getLogger().warning(e.getMessage());
			DBConn.latest_warning = "ErrPOS002";
		}

		return strbuf.toString();
	}

	/**
	 * 콜로그서버와의 연결을 종료한다.
	 */
	public void disconnectCallLogServer() {
		if (isConnected) {
			try {
				bis.close();
				bos.close();
				socket.close();
				isConnected = false;
				isLogon = false;
				Utils.getLogger().info(":disconnect:");
			} catch (IOException e) {
				Utils.getLogger().warning(e.getMessage());
				DBConn.latest_warning = "ErrPOS003";
			}
		}
	}

	/**
	 * DB작업을 수행한다.
	 * 
	 * @param paramVal
	 */
	public void doDBWork(String paramVal) {
		// "20004               00001031304             
		// 11822 
		// 01201607110009895037070****0618         05041100000         010****1183         
		// 2016071120551020160711205555201607112055252016071120555545    30    1
		// 02201607120009912425070****7111         05041111228                             
		// 2016071214435720160712144400                            3     0     F";
		// String strVal;
		// String loginSuccessMsg = "20004               0000";// 총24글자
		// if (paramVal.startsWith(Env.CALL_LOG_WORK_2000)) {
		// strVal = paramVal.substring(N24);
		// isLogon = true;
		// } else {
		// strVal = paramVal;
		// }
		packetProcess(paramVal);
	}

	/**
	 * @param strVal
	 * @throws NumberFormatException
	 */
	public void packetProcess(String strVal) throws NumberFormatException {
		String strHeader2;
		if (strVal.startsWith(Env.CALL_LOG_WORK_1031)) {
			strVal = strVal.substring(4);// 304 ...

			strHeader2 = strVal.substring(0, 6).trim();
			// strHeader3 = strVal.substring(6,10);

			strVal = strVal.substring(16);

			// 길이검증
			if (Integer.parseInt(strHeader2) < strVal.length()) {
				strVal = strVal.substring(0, Integer.parseInt(strHeader2));// 길이가
																			// 맞지
																			// 않아
																			// 이렇게
																			// 함.
																			// 중복
																			// 패킷은
																			// 뒤를
																			// 잘라버리도록
																			// 한다.
			}
			if (strVal.length() == Integer.parseInt(strHeader2)) {
				String corp_code = strVal.substring(0, 4);
				if (Env.getInstance().CORP_CODE.equals(corp_code))// 업체코드 일치여부
																	// 검증
				{
					strVal = strVal.substring(4);
					String strTuple_count = "";
					strTuple_count = Utils.substringVal(strVal, 0, 2);
					int iTuple_count = Integer.parseInt(strTuple_count);

					strVal = strVal.substring(2);

					// 튜플길이 검증
					if (strVal.length() == iTuple_count * N149) {
						StringBuilder sb_header = new StringBuilder();
						sb_header.append(Env.CALL_LOG_WORK_2031);// 처음은4글자임

						StringBuilder sb = new StringBuilder();
						sb.append(Env.getInstance().CORP_CODE);

						StringBuilder sb2 = new StringBuilder();
						String str_tuple_unit_response = "";
						String strVal1 = "";
						while (N149 <= strVal.length()) {
							strVal1 = strVal.substring(0, N149);
							str_tuple_unit_response = parseCallLog(strVal1);
							sb2.append(str_tuple_unit_response);
							strVal = strVal.substring(N149);
						}

						// 후검증 미처리 패킷 문제가 있는지에 따른 검증
						if (strVal.length() != 0) {
							Utils.getLogger().warning(
									"CallLog연동규약 변동 검토 필요함. strVal.length()=0이 아님=>"
											+ strVal.length());
							Utils.getLogger().warning(
									"나머지(찌꺼기 이거나 문제가 되는 패킷:[" + strVal + "]");
							DBConn.latest_warning = "ErrPOS004";
						}

						int i_myresponse_tuple_count = sb2.toString().length()
								/ N24;

						String str_myresponse_touble_count = String
								.valueOf(i_myresponse_tuple_count);

						sb.append(Utils.paddingLeft(2,
								str_myresponse_touble_count));

						// 자체검증
						if (0 < i_myresponse_tuple_count) {
							sb.append(sb2);// 검증후 전송 6.2.4 CallLog 전송 응답
							int body_len = sb.toString().length();
							sb_header.append(Utils.paddingLeft(6,
									("" + body_len).trim()));
							sb_header.append(Utils.space(10));
							sb_header.append(sb);
							sendMessage(sb_header.toString());
						}
					} else {
						Utils.getLogger().warning(
								"튜플 검증 오류! strVal.length() == iTuple_count => false, strVal.length() => "
										+ strVal.length()
										+ ", iTuple_count => " + iTuple_count);
						Utils.getLogger().warning("미처리패킷:[" + strVal + "]");
						DBConn.latest_warning = "ErrPOS005";
					}
				} else {
					Utils.getLogger().warning(
							"수신된 패킷의 업체코드 검증 오류! packet_body.substring(0,4) => "
									+ corp_code
									+ ", Env.getInstance().CORP_CODE) => "
									+ Env.getInstance().CORP_CODE);
					Utils.getLogger().warning("미처리패킷:[" + strVal + "]");
					DBConn.latest_warning = "ErrPOS006";
				}
			} else {
				Utils.getLogger().warning(
						"수신된 패킷의 길이검증 오류! strVal.length() =>" + strVal.length()
								+ ", Integer.parseInt(strHeader2) => "
								+ strHeader2);
				Utils.getLogger().warning("미처리패킷:[" + strVal + "]");
				DBConn.latest_warning = "ErrPOS007";
			}
		} else {
			if (4 <= strVal.length()) {
				if (strVal.startsWith(Env.CALL_LOG_WORK_7778)) {
					String strVal3 = strVal.substring(20);
					if (0 < strVal3.length()) {
						packetProcess(strVal3);
					}
				} else if (strVal.startsWith(Env.CALL_LOG_WORK_2000)) {
					String strLogonSuccessMsg = Env.CALL_LOG_WORK_2000
							+ Utils.paddingLeft(6, "4") + Utils.space(10)
							+ Env.CALL_LOG_RET_0000;

					if (strVal.startsWith(strLogonSuccessMsg)) {
						isLogon = true;
						nLogonAndCon = 1;
					}
					String strVal3 = strVal.substring(strLogonSuccessMsg
							.length());
					if (0 < strVal3.length()) {
						packetProcess(strVal3);
					}
				} else {
					Utils.getLogger().warning(
							"구현되지 않은 업무코드 =>" + strVal.substring(0, 4));
					DBConn.latest_warning = "ErrPOS008";
				}
			}
		}
	}

	private String parseCallLog(String str) {
		String v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11;

		int s = 0;
		v1 = str.substring(s, s = s + 20).trim();// 고유값20자리
		v2 = str.substring(s, s = s + 20).trim();// 발신번호
		v3 = str.substring(s, s = s + 20).trim();// 접속번호
		v4 = str.substring(s, s = s + 20).trim();// 착신번호
		v5 = str.substring(s, s = s + 14).trim();// 연결시작시간
		v6 = str.substring(s, s = s + 14).trim();// 연결종료시간
		v7 = str.substring(s, s = s + 14).trim();// 서비스시작시간
		v8 = str.substring(s, s = s + 14).trim();// 서비스종료시간
		v9 = str.substring(s, s = s + 6).trim();// 통화 시간
		v10 = str.substring(s, s = s + 6).trim();// 서비스 시간
		v11 = str.substring(s, s = s + 1).trim();// 통화결과
		/*
		 * 1:통화 성공, 2:착신 통화중, 3:착신 무응답, 4:착신측 회선부족, 5:착신번호 결번 혹은 유효하지않은 번호,
		 * 6:발/착신자 통화 연결 오류, B:착신 시도 중 발신측 호 종료, F:착신번호 없음
		 */
		String retVal = "";
		if (dbCallLogProcess(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11)) {
			retVal = Utils.paddingLeft(20, v1) + "0000";// 응답
		} else {
			retVal = Utils.paddingLeft(20, v1) + "0001";// 나중에 재전송해줄것을 당부하여 서버로
														// 다시보냄 DB처리를 하지 못하였음.
		}
		return retVal;
	}

	/**
	 * 서버에 다시보내주기를 요청하는 경우에는 false를 리턴함.
	 * 
	 * @param v1
	 * @param v2
	 * @param v3
	 * @param v4
	 * @param v5
	 * @param v6
	 * @param v7
	 * @param v8
	 * @param v9
	 * @param v10
	 * @param v11
	 * @return
	 */
	private boolean dbCallLogProcess(String v1, String v2, String v3,
			String v4, String v5, String v6, String v7, String v8, String v9,
			String v10, String v11) {

		boolean result = false;

		if (Env.getInstance().USE_FILTER == true && "".equals(v4)
				&& "F".equals(v11)) {
			// 아무것도 안함. 로그에 쌓지 않고 단지 마스터에 최근사용된날짜로 저장한다.
			result = true;
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append("insert into safen_cdr(conn_sdt, conn_edt, conn_sec, service_sdt, ");
			sb.append(" service_edt, service_sec, safen, safen_in, safen_out, billsec,");
			sb.append(" unique_id, account_cd, calllog_rec_file, rec_file_cd, status_cd, create_dt) values("
					+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())");

			MyDataObject dao = new MyDataObject();// PreparedStatement pstmt;

			try {
				dao.openPstmt(sb.toString());

				int n = 1;

				String dt1 = null, dt2 = null, dt3 = null, dt4 = null;

				dt1 = Utils.toDate(v5);
				dt2 = Utils.toDate(v6);
				dt3 = Utils.toDate(v7);
				dt4 = Utils.toDate(v8);

				dao.pstmt().setString(n++, dt1);
				dao.pstmt().setString(n++, dt2);

				dao.pstmt().setInt(n++, Integer.parseInt(v9));
				dao.pstmt().setString(n++, dt3);
				dao.pstmt().setString(n++, dt4);
				dao.pstmt().setInt(n++, Integer.parseInt(v10));
				dao.pstmt().setString(n++, v3);// 0504번호
				dao.pstmt().setString(n++, v4);// 착신번호
				dao.pstmt().setString(n++, v2);// 발신번호
				dao.pstmt().setInt(n++,
						Integer.parseInt(v9) - Integer.parseInt(v10));
				dao.pstmt().setString(n++, v1);
				dao.pstmt().setString(n++, Safen_master.getAccount_cd(v3));// account_cd
				dao.pstmt().setString(n++, "");// calllog_rec_file
				dao.pstmt().setString(n++, "1");// rec_file_cd
				dao.pstmt().setString(n++, v11);

				int cnt = 0;
				cnt = dao.pstmt().executeUpdate();

				if (cnt == 1) {
					result = true;
				}
			} catch (SQLException e) {
				if (-1 < e.getMessage().indexOf("uplicate")) {// 중복인 경우 성공으로
																// 간주한다./Duplicate
																// entry ...를
																// 리턴하기
																// 때문에
					// 단, 업데이트를 수행한다.? 그냥 무시한다.
					result = true;
				} else {
					Utils.getLogger().warning(e.getMessage());
					DBConn.latest_warning = "ErrPOS009";
					e.printStackTrace();
					result = false;
				}
			} catch (Exception e) {
				Utils.getLogger().warning(e.getMessage());
				Utils.getLogger().warning(Utils.stack(e));
				DBConn.latest_warning = "ErrPOS010";
			} finally {
				dao.closePstmt();
			}

		}

		Safen_master.dealed(v3);

		return result;
	}

	public boolean doLogon() {
		if (isLogon == false) {
			StringBuffer message = new StringBuffer();
			message.append(Env.CALL_LOG_WORK_1000);

			int body_length = 0;
			body_length = Env.getInstance().CORP_CODE.length();
			message.append(body_length);
			int char_leng = String.valueOf(body_length).length();
			message.append(Utils.space(6 - char_leng));

			message.append(Utils.space(10));

			message.append(Env.getInstance().CORP_CODE);

			sendMessage(new String(message));

			String strRetVal = "";

			strRetVal = receiveMessage();

			packetProcess(strRetVal);
			
			if(isLogon == false) {
				nLogonAndCon = 2;//로그인 실패로 봄
			}
		}
		Utils.getLogger().info(":logon:" + isLogon);
		return isLogon;
	}

	public static CallLogWorker getInstance() {
		if (Instance == null) {
			Instance = new CallLogWorker();
		}
		return Instance;
	}

	public String doMsgMain() {
		String strRetVal = "";
		if (isLogon) {
			StringBuffer message = new StringBuffer();
			message.append(Env.CALL_LOG_WORK_7777);
			message.append(Utils.paddingLeft(6, "0"));
			message.append(Utils.space(10));

			String msg = message.toString();
			if (msg.length() == 20) {
				sendMessage(msg);
			} else {
				Utils.getLogger().warning("메세지 길이가 20이 아닙니다.[" + msg + "]");
				DBConn.latest_warning = "ErrPOS011";
			}
			
			strRetVal = receiveMessage();

			if ((Env.CALL_LOG_WORK_2000 + Utils.paddingLeft(6, "4")
					+ Utils.space(10) + Env.CALL_LOG_RET_0002)
					.equals(strRetVal)) {
				// 로그인이 안된 것으로 보고 로그인을 시도함.
				doLogon();
			}
		}
		return strRetVal;
	}

}
