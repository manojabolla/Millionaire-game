del Friend\*.class
del Player\*.class
del GameServer\*.class

javac Friend\*.java
javac Player\*.java
javac GameServer\*.java

java GameServer\Server
pause
