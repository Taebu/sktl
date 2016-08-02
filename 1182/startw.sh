# nohup /home/sktl/1182/startw.sh 로 호출해야 백그라운드로 구동됨
# 이것을 작업관리자에 등록하면 윈도우 서비스처럼 구동이 가능함.<단, 경로를 적절히 수정>
/home/sktl/1182/stop.sh >>"/home/sktl/1182/killme.txt"
ping 127.0.0.1 -c 4
ps -ef | grep 1182/bin | grep java | grep -v grep | awk '{print $2}' | xargs kill -9
/home/java/bin/java -Dfile.encoding=UTF-8 -classpath ".:/home/sktl/1182/bin:/home/sktl/1182/libs/mysql-connector-java-5.1.35.jar:/home/sktl/1182/libs/NosSafen_real_gcomapis_20151221.jar:/home/sktl/1182/libs/log4j-1.2.17.jar" kr.co.cashq.sktlink0504.SKT01 /home/sktl/1182 1>/dev/null 2>&1 &
