:: 이것을 작업관리자에 등록하면 윈도우 서비스처럼 구동이 가능함.<단, 경로를 적절히 수정>
start /b javaw -Dfile.encoding=UTF-8 -classpath "D:\Project43\sktlink0504\bin;D:\Project43\sktlink0504\libs\mysql-connector-java-5.1.35.jar;D:\Project43\sktlink0504\libs\NosSafen_real_gcomapis_20151221.jar;D:\Project43\sktlink0504\libs\log4j-1.2.17.jar" kr.co.cashq.sktlink0504.SKT01