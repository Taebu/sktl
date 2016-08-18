# sktl
에듀프레임 박규선 팀장과 공동 SKTL java socket 통신 프로그램

1\. 동기화 방법
```sql
/* 동기화 방법 업무절차 처음 sktl과 동기화 요청이 필요한 경우 */
safen_master 테이블의 status_cd='a' 로 설정하면 된다. 1초에 한 건씩 요청하여 하루면 86400건을 처리할 수 있다.
-- 프로그램중에서 자동 u로 갱신
/* status_cd='i' 이면서 update_dt가 하루전인데이터 */
```

2\. 번호요청
```sql
/*업무절차 처음 <착신전화번호에 따른 안심번호 추출 요청>*/
update safen_master set 
status_cd='i',
safen_in='01050421183',
update_dt=now() 
where status_cd='e' 
and group_cd !='test' 
order by dealed_dt limit 1;

update safen_master set status_cd='i',safen_in='01030372004',
update_dt=now() where status_cd='e' and group_cd !='test' 
order by dealed_dt limit 1;

select * from safen_master where safen_in='01030372004' and status_cd='i';
/*
select @safen:=safen from safen_master where safen_in='01030372004' and status_cd='i' limit 1;
select @safen;

select @safen:=safen from safen_master where safen_in='01068287822' and status_cd='i' limit 1;
select @safen;
*/
```


3\. 번호등록
```sql
/*등록*/
insert into safen_cmd_queue(safen,safen_in,create_dt) values('05041100000','01050421183',now());
insert into safen_cmd_queue(safen,safen_in,create_dt) values('05041110000','01050421183',now());
/*
insert into safen_cmd_queue(safen,safen_in,create_dt) values(@safen,'01050421183',now());
insert into safen_cmd_queue(safen,safen_in,create_dt) values(@safen,'01030372004',now());

insert into safen_cmd_queue(safen,safen_in,create_dt) values(@safen,'01068287822',now());
*/
```

4\. 번호취소
```sql
/*취소*/
insert into safen_cmd_queue(safen,safen_in,create_dt) values('05041100000','1234567890',now());
/*
insert into safen_cmd_queue(safen,safen_in,create_dt) values(@safen,'1234567890',now());
*/

/*
장애처리  예시(CASE 001)--처리도중 오류난 cmd_queue데이터 삭제하기--
만약 처리도중 데이터가 오류난게 있는 경우 1분전 이전에 등록된 데이터가 처리되지 않은 경우
select * from safen_cmd_queue where create_dt < DATE_SUB(now(),INTERVAL 1 minute) ;
+-----+-------------+-------------+-----------+-----------+---------------------+
| seq | safen       | safen_in    | status_cd | result_cd | create_dt           |
+-----+-------------+-------------+-----------+-----------+---------------------+
|   5 | 05041100000 | 01050421183 | i         |           | 2016-07-08 16:26:26 |
+-----+-------------+-------------+-----------+-----------+---------------------
+
1 row in set (0.00 sec)
*/

select @safen:='';
select @safen:=safen from safen_cmd_queue where status_cd='i' and create_dt < DATE_SUB(now(),INTERVAL 1 minute);
delete from safen_cmd_queue where status_cd = 'i' and safen=@safen;
update safen_master set status_cd='e' where safen=@safen;

/*장애처리 CASE 001 End */
```

SK텔링크 test server IP : 112.216.24.85

Port : 48880

[상용 환경 / mapping ]
고객사측 IP : ?? (다수 등록 가능)
SK텔링크  IP : 211.237.78.64, 211.237.78.65
Port : 48880

[상용 환경 / 콜로그 ]
고객사측 IP : ?? (1개만 등록 가능)
SK텔링크  IP : 211.237.78.46
Port : 65500
