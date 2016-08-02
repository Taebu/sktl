ps -ef | grep 1182/bin | grep java | grep -v grep | awk '{print $2}' | xargs kill -9
