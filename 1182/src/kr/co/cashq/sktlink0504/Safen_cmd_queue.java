package kr.co.cashq.sktlink0504;

import java.sql.Connection;
import java.sql.SQLException;

import com.nostech.safen.SafeNo;

/**
 * safen_cmd_queue 테이블 관련 객체
 * @author pgs
 *
 */
public class Safen_cmd_queue {
	
	/**
	 * safen_cmd_queue 테이블의 데이터를 처리하기 위한 주요한 처리를 수행한다.
	 */
	public static void doMainProcess() {
		Connection con = DBConn.getConnection();

		if (con != null) {
			MyDataObject dao = new MyDataObject();
			MyDataObject dao2 = new MyDataObject();
			MyDataObject dao3 = new MyDataObject();
			MyDataObject dao4 = new MyDataObject();

			StringBuilder sb = new StringBuilder();

			sb.append("select exists(select 1 from safen_cmd_queue) a");

			try {
				dao.openPstmt(sb.toString());

				dao.setRs(dao.pstmt().executeQuery());

				if (dao.rs().next()) {
					
					SKT01.heart_beat = 1;
					
					if (dao.rs().getInt(1) == 1) {
						
						dao.tryClose();
						
						StringBuilder sb2 = new StringBuilder();
						String hist_table = DBConn.isExistTableYYYYMM();

						sb2.append("insert into "
								+ hist_table
								+ " select * from safen_cmd_queue where status_cd in ('s','e')");// 처리가
																									// 진행중인것은
																									// 포함하지
																									// 않는다.
						// insert into safen_cmd_hist_201607 select * from
						// safen_cmd_queue where status_cd != ''
						dao2.openPstmt(sb2.toString());

						int resultCnt2 = dao2.pstmt().executeUpdate();
						if(resultCnt2!=1) {
							Utils.getLogger().warning(dao2.getWarning(resultCnt2,1));
							DBConn.latest_warning = "ErrPOS027";
						}

						// region 3 start --->
						StringBuilder sb3 = new StringBuilder();

						sb3.append("delete from safen_cmd_queue where status_cd in ('s','e')");// 처리가
																								// 진행중인것은
																								// 지우지
																								// 않는다.

						// insert into safen_cmd_hist_201607 select * from
						// safen_cmd_queue where status_cd != ''
						dao3.openPstmt(sb3.toString());

						int resultCnt3 = dao3.pstmt().executeUpdate();
						if(resultCnt3!=1) {
							Utils.getLogger().warning(dao3.getWarning(resultCnt3,1));
							DBConn.latest_warning = "ErrPOS028";
						}
						// region 3 end <---

						// region 4 start --->
						StringBuilder sb4 = new StringBuilder();
						
						sb4.append("select * from safen_cmd_queue where status_cd = '' limit 1");
						dao4.openPstmt(sb4.toString());

						dao4.setRs(dao4.pstmt().executeQuery());

						if (dao4.rs().next()) {
							int seq = dao4.rs().getInt("seq");
							String safen = dao4.rs().getString("safen");
							String safen_in = dao4.rs().getString("safen_in");
							doMapping(seq, safen, safen_in);
						}
						// region 4 end <---
					} else {

						if (!"".equals(Env.confirmSafen)) {
							// cmq_queue에는 없는 경우라면
							SafeNo safeNo = new SafeNo();
							String retCode = "";
							try {
								retCode = safeNo.SafeNoAsk(
										Env.getInstance().CORP_CODE,
										Env.confirmSafen);
							} catch (Exception e) {
								Utils.getLogger().warning(e.getMessage());
								Utils.getLogger().warning(Utils.stack(e));
								DBConn.latest_warning = "ErrPOS029";
							}

							if (-1 < retCode.indexOf(Env.confirmSafen_in)) {// retCode
																			// =
																			// "01040421182,01040421182"
																			// 와
																			// 같은
																			// 형태로
																			// 리턴되는
																			// 식임
								Utils.getLogger().info(
										"OK 착신연결성공" + Env.confirmSafen + "->"
												+ Env.confirmSafen_in);
							} else {// 취소된 경우 recCode = "E401"이 리턴됨
								if (Env.NULL_TEL_NUMBER
										.equals(Env.confirmSafen_in)
										&& "E401".equals(retCode)) {
									Utils.getLogger().info(
											"OK 착신취소성공" + Env.confirmSafen
													+ ", retCode:[" + retCode
													+ "]");
								} else {
									Utils.getLogger().warning(
											"Error! " + Env.confirmSafen + "->"
													+ Env.confirmSafen_in
													+ "? retCode:[" + retCode
													+ "]");
									DBConn.latest_warning = "ErrPOS030";
								}
							}

							Env.confirmSafen = "";
						}
					}
				}				
			} catch (SQLException e) {
				Utils.getLogger().warning(e.getMessage());
				DBConn.latest_warning = "ErrPOS031";
				e.printStackTrace();
			} catch (Exception e) {
				Utils.getLogger().warning(e.getMessage());
				Utils.getLogger().warning(Utils.stack(e));
				DBConn.latest_warning = "ErrPOS032";
			}
			finally {
				dao.closePstmt();
				dao2.closePstmt();
				dao3.closePstmt();
				dao4.closePstmt();
			}
			
			//콜로그 마스터 정보의 레코드 1개를 갱신을 시도한다.
			Safen_master.doWark2();
		}
	}

	/**
	 * 취소시는 safen_in010에 "1234567890"을 넣어야 함. 리턴코드4자리에 따른 의미
	 * 
	 * 0000:성공 처리(인증서버에서 요청 처리가 성공.) E101:Network 장애(인증서버와 연결 실패.) E102:System
	 * 장애(인증서버의 일시적 장애. 재시도 요망.) E201:제휴사 인증 실패(유효한 제휴사 코드가 아님.) E202:유효 기간
	 * 만료(제휴사와의 계약기간 만료.) E301:안심 번호 소진(유효한 안심번호 자원이 없음.) E401:Data Not
	 * Found(요청한 Data와 일치하는 Data가 없음.) E402:Data Overlap(요청한 Data가 이미 존재함.)
	 * E501:전문 오류(전문 공통부 혹은 본문의 Data가 비정상일 경우.) E502:전화 번호(오류 요청한 착신번호가 맵핑불가 번호일
	 * 경우.)
	 */
	private static String doMapping(int seq, String safen0504,
			String safen_in010) {

		String corpCode = Env.getInstance().CORP_CODE;
		String safeNum = null;
		String telNum1 = null;// "1234567890";
		String newNum1 = null;
		String telNum2 = null;
		String newNum2 = null;

		int mapping_option = 0;
		if (Env.NULL_TEL_NUMBER.equals(safen_in010)) {
			// 취소
			mapping_option = 2;

			String safen_in = getSafenInBySafen(safen0504);

			safeNum = safen0504;
			telNum1 = safen_in;
			newNum1 = Env.NULL_TEL_NUMBER;// "1234567890";;
			telNum2 = safen_in;
			newNum2 = Env.NULL_TEL_NUMBER;
		} else {
			// 등록 Create
			mapping_option = 1;
			safeNum = safen0504;
			telNum1 = Env.NULL_TEL_NUMBER;// "1234567890";
			newNum1 = safen_in010;
			telNum2 = Env.NULL_TEL_NUMBER;
			newNum2 = safen_in010;
		}

		// String groupCode = "anpr_1";
		String groupCode = "grp_1";
		
		groupCode = Safen_master.getGroupCode(safen0504);

		String reserved1 = "";
		String reserved2 = "";
		String retCode = "";

		SafeNo safeNo = new SafeNo();

		try {
			update_cmd_queue(seq, safen0504, safen_in010, mapping_option, "");
			retCode = safeNo.SafeNoMod(corpCode, safeNum, telNum1, newNum1,
					telNum2, newNum2, groupCode, reserved1, reserved2);
		} catch (Exception e) {
			Utils.getLogger().warning(e.getMessage());
			Utils.getLogger().warning(Utils.stack(e));
			DBConn.latest_warning = "ErrPOS033";
		}

		// 후처리
		if ("0000".equals(retCode)) {
			Safen_master.update_safen_master(safen0504, safen_in010,
					mapping_option);

			Env.confirmSafen = safen0504;
			Env.confirmSafen_in = safen_in010;// 취소인경우는 1234567890 임

		}
		update_cmd_queue(seq, safen0504, safen_in010, mapping_option, retCode);

		return retCode;
	}

	/**
	 * 안심번호테이블을 갱신한다. 단, 이때 retCode가 공백이면 status_cd를 i로 넣고 진행중으로만 마킹하고 프로세스를
	 * 종료한다. retCode가 "0000"(성공)인경우에는 status_cd값을 "s"로 그렇지 않은 경우에는 "e"로 셋팅한 후 큐를
	 * 지우고 로그로 보낸다. 
	 * @param safen0504
	 * @param safen_in010
	 * @param mapping_option
	 * @param retCode
	 */
	private static void update_cmd_queue(int seq, String safen0504,
			String safen_in010, int mapping_option, String retCode) {

		MyDataObject dao = new MyDataObject();
		MyDataObject dao2 = new MyDataObject();
		MyDataObject dao3 = new MyDataObject();
		
		try {
			if ("".equals(retCode)) {
				StringBuilder sb = new StringBuilder();
				sb.append("update safen_cmd_queue set status_cd=? where seq=?");

				// status_cd 컬럼을 "i"<진행중>상태로 바꾼다.
				dao.openPstmt(sb.toString());

				dao.pstmt().setString(1, "i");
				dao.pstmt().setInt(2, seq);

				int cnt = dao.pstmt().executeUpdate();
				if(cnt!=1) {
					Utils.getLogger().warning(dao.getWarning(cnt,1));
					DBConn.latest_warning = "ErrPOS034";
				}

				dao.tryClose();

			} else {
				StringBuilder sb = new StringBuilder();
				sb.append("update safen_cmd_queue set status_cd=?,result_cd=? where seq=?");

				if ("0000".equals(retCode)) {
					// status_cd 컬럼을 "s"<성공>상태로 바꾼다.
					
					dao2.openPstmt(sb.toString());

					dao2.pstmt().setString(1, "s");
					dao2.pstmt().setString(2, retCode);
					dao2.pstmt().setInt(3, seq);

					int cnt = dao2.pstmt().executeUpdate();
					if(cnt!=1) {
						Utils.getLogger().warning(dao2.getWarning(cnt,1));
						DBConn.latest_warning = "ErrPOS035";
					}

					dao2.tryClose();
				} else {
					// status_cd 컬럼을 "e"<오류>상태로 바꾼다.
					dao3.openPstmt(sb.toString());

					dao3.pstmt().setString(1, "e");
					dao3.pstmt().setString(2, retCode);
					dao3.pstmt().setInt(3, seq);

					int cnt = dao3.pstmt().executeUpdate();
					if(cnt!=1) {
						Utils.getLogger().warning(dao3.getWarning(cnt,1));
						DBConn.latest_warning = "ErrPOS036";
					}					
					dao3.tryClose();
				}
			}
		} catch (SQLException e) {
			Utils.getLogger().warning(e.getMessage());
			DBConn.latest_warning = "ErrPOS037";
			e.printStackTrace();
		}
		catch (Exception e) {
			Utils.getLogger().warning(e.getMessage());
			DBConn.latest_warning = "ErrPOS038";
			Utils.getLogger().warning(Utils.stack(e));
		}
		finally {
			dao.closePstmt();
			dao2.closePstmt();
			dao3.closePstmt();
		}
	}

	/**
	 * 마스터 테이블에서 안심번호에 따른 착신번호를 리턴한다.
	 * @param safen0504
	 * @return
	 */
	private static String getSafenInBySafen(String safen0504) {
		String retVal = "";
		StringBuilder sb = new StringBuilder();

		MyDataObject dao = new MyDataObject();
		sb.append("select safen_in from safen_master where safen = ?");
		try {
			dao.openPstmt(sb.toString());
			dao.pstmt().setString(1, safen0504);
			
			dao.setRs (dao.pstmt().executeQuery());

			if (dao.rs().next()) {
				retVal = dao.rs().getString("safen_in");
			}			
		} catch (SQLException e) {
			Utils.getLogger().warning(e.getMessage());
			DBConn.latest_warning = "ErrPOS039";
			e.printStackTrace();
		}
		catch (Exception e) {
			Utils.getLogger().warning(e.getMessage());
			Utils.getLogger().warning(Utils.stack(e));
			DBConn.latest_warning = "ErrPOS040";
		}
		finally {
			dao.closePstmt();
		}

		return retVal;
	}
}
